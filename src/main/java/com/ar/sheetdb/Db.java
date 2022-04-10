package com.ar.sheetdb;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Db {

    public Sheets sheetService;
    public String spreadsheetId;
    public Integer rowFetchAtOnce;

    public Db(String credPath, String appName, String spreadsheetId, Integer rowFetchAtOnce) {
        try {
            this.spreadsheetId = spreadsheetId;
            this.rowFetchAtOnce = rowFetchAtOnce;
            JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
            List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
            NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials googleCredentials;
            try (InputStream inputSteam = Db.class.getResourceAsStream(credPath)) {
                googleCredentials = GoogleCredentials.fromStream(inputSteam).createScoped(SCOPES);
            }
            this.sheetService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(googleCredentials))
                    .setApplicationName(appName)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public <T extends GoogleSheet> void create(Class<T> type) {
        try {
            List<Request> requests = new ArrayList<>();
            String sheetName = AnnotationUtil.getTable(type);
            requests.add(new Request().setAddSheet(new AddSheetRequest().setProperties(new SheetProperties()
                    .setTitle(sheetName))));
            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
            sheetService.spreadsheets().batchUpdate(spreadsheetId, body).execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> List<T> getAll(Class<T> type) {
        String sheetName = AnnotationUtil.getTable(type);
        int columnCount = AnnotationUtil.getColumns(type).size();
        String finalColumn = Character.toString ((char) 65+columnCount);
        // 65 ascii value of A, so the code will work up to Z column only.
        String range = sheetName + "!A2:"+finalColumn+100;
        List<List<Object>> values;
        ValueRange response = null;
        List<T> result =  null;
        try {
            response = sheetService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            values = response.getValues();
            result = new ArrayList<>();
            for(int i =0; i<values.size(); i++){
                T model = AnnotationUtil.convert(values.get(i), type);
                model.row = i+2;
                result.add(model);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public <T extends GoogleSheet> void generateHeaders(Class<T> type) {
        String sheetName = AnnotationUtil.getTable(type);
        int columnCount = AnnotationUtil.getColumns(type).size();
        String finalColumn = Character.toString ((char) 65+columnCount-1);
        String range = sheetName + "!A1:" + finalColumn+ "1";
        List<Object> cols = Arrays.asList(AnnotationUtil.getColumns(type).toArray());
        List<List<Object>> updatedList = List.of(cols);
        ValueRange body = new ValueRange()
                .setValues(updatedList);
        try {
            UpdateValuesResponse result =
                    sheetService.spreadsheets().values().update(spreadsheetId, range, body)
                            .setValueInputOption("RAW")
                            .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void append(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        String sheetName = AnnotationUtil.getTable(type);
        int columnCount = AnnotationUtil.getColumns(type).size();
        String finalColumn = Character.toString ((char) 65+columnCount-1);
        String range = sheetName + "!A1:" + finalColumn+ "1";

        List<List<Object>> updatedList = AnnotationUtil.convert(obj);
        ValueRange body = new ValueRange()
                .setValues(updatedList);
        try {
            AppendValuesResponse result =
                    sheetService.spreadsheets().values().append(spreadsheetId, range, body)
                            .setValueInputOption("RAW")
                            .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void update(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        String sheetName = AnnotationUtil.getTable(type);
        int columnCount = AnnotationUtil.getColumns(type).size();
        String finalColumn = Character.toString ((char) 65+columnCount-1);
        String range = sheetName + "!A"+obj.row+":" + finalColumn+ obj.row;
        List<List<Object>> updatedList = AnnotationUtil.convert(obj);
        ValueRange body = new ValueRange()
                .setValues(updatedList);
        try {
            UpdateValuesResponse result =
                    sheetService.spreadsheets().values().update(spreadsheetId, range, body)
                            .setValueInputOption("RAW")
                            .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void delete(T obj) {
        try {
            BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest();
            Request request = new Request();
            request.setDeleteDimension(new DeleteDimensionRequest()
                    .setRange(new DimensionRange()
                            .setSheetId(AnnotationUtil.getSheetId(obj.getClass()))
                            .setDimension("ROWS")
                            .setStartIndex(obj.row - 1)
                            .setEndIndex(obj.row)
                    )
            );
            requestBody.setRequests(List.of(request));
            Sheets.Spreadsheets.BatchUpdate deleteRequest =
                    sheetService.spreadsheets().batchUpdate(spreadsheetId, requestBody);
            BatchUpdateSpreadsheetResponse deleteResponse = deleteRequest.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
