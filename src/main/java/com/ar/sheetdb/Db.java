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
        List<T> result = null;
        try {
            String sheetName = AnnotationUtil.getTable(type);
            int columnCount = AnnotationUtil.getColumns(type).size();
            String finalColumn = Character.toString((char) 65 + columnCount - 1);
            // 65 ascii value of A, so the code will work up to Z column only.
            String range = sheetName + "!A2:" + finalColumn + 100;
            List<String> ranges = List.of(range);
            Sheets.Spreadsheets.Values.BatchGet request =
                    sheetService.spreadsheets().values().batchGet(spreadsheetId);
            request.setRanges(ranges);
            request.setValueRenderOption("UNFORMATTED_VALUE"); // FORMATTED_VALUE, UNFORMATTED_VALUE, FORMULA
            request.setDateTimeRenderOption("FORMATTED_STRING"); //SERIAL_NUMBER, FORMATTED_STRING
            BatchGetValuesResponse response = request.execute();
            List<List<Object>> values = response.getValueRanges().get(0).getValues();
            result = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                if (!values.get(i).stream().anyMatch(x -> x != null)) {
                    continue;
                }
                T model = AnnotationUtil.convert(values.get(i), type);
                model.row = i + 2;
                result.add(model);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void generateHeaders(Class<T> type) {
        //TODO set columnwise formatting also
        String sheetName = AnnotationUtil.getTable(type);
        int columnCount = AnnotationUtil.getColumns(type).size();
        String finalColumn = Character.toString((char) 65 + columnCount - 1);
        String range = sheetName + "!A1:" + finalColumn + "1";
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

    public <T extends GoogleSheet> void save(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        List<RowData> rowData = new ArrayList<RowData>();
        RowData row = AnnotationUtil.convert(obj);
        rowData.add(row);

        BatchUpdateSpreadsheetRequest batchRequests = new BatchUpdateSpreadsheetRequest();
        BatchUpdateSpreadsheetResponse response;
        List<Request> requests = new ArrayList<>();

        AppendCellsRequest appendCellReq = new AppendCellsRequest();
        appendCellReq.setSheetId(AnnotationUtil.getSheetId(type));
        appendCellReq.setRows(rowData);
        appendCellReq.setFields("userEnteredValue,userEnteredFormat.numberFormat");
        requests.add(new Request().setAppendCells(appendCellReq));
        batchRequests.setRequests(requests);
        try {
            response = sheetService.spreadsheets().batchUpdate(spreadsheetId, batchRequests).execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void update(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        int columnCount = AnnotationUtil.getColumns(type).size();

        List<RowData> rowData = new ArrayList<RowData>();
        RowData row = AnnotationUtil.convert(obj);
        rowData.add(row);

        BatchUpdateSpreadsheetRequest batchRequests = new BatchUpdateSpreadsheetRequest();
        BatchUpdateSpreadsheetResponse response;
        List<Request> requests = new ArrayList<>();

        UpdateCellsRequest cellReq = new UpdateCellsRequest();
        cellReq.setRange(new GridRange().setSheetId(AnnotationUtil.getSheetId(type))
                .setStartColumnIndex(0)
                .setEndColumnIndex(columnCount)
                .setStartRowIndex(obj.row - 1)
                .setEndRowIndex(obj.row));

        cellReq.setRows(rowData);
        cellReq.setFields("userEnteredValue,userEnteredFormat.numberFormat");
        requests.add(new Request().setUpdateCells(cellReq));
        batchRequests.setRequests(requests);
        try {
            response = sheetService.spreadsheets().batchUpdate(spreadsheetId, batchRequests).execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            response = sheetService.spreadsheets().batchUpdate(spreadsheetId, batchRequests).execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void delete(T obj) {
        try {
            Class<? extends GoogleSheet> type = obj.getClass();
            String sheetName = AnnotationUtil.getTable(type);
            int columnCount = AnnotationUtil.getColumns(type).size();
            String finalColumn = Character.toString((char) 65 + columnCount - 1);
            String range = sheetName + "!A" + obj.row + ":" + finalColumn + obj.row;
            AnnotationUtil.delete(obj);
            ClearValuesRequest requestBody = new ClearValuesRequest();
            Sheets.Spreadsheets.Values.Clear request = sheetService.spreadsheets().values().clear(spreadsheetId, range, requestBody);
            ClearValuesResponse response = request.execute();
            /*
            This will delete the row. Not prefered as I am using rownum for querying. so instead clearing the row.
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
            BatchUpdateSpreadsheetResponse deleteResponse = deleteRequest.execute();*/
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
