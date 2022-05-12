package com.ar.sheetdb.core;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;


public class Db {

    public Sheets sheetService;
    public String spreadsheetId;
    public Integer rowFetchAtOnce;
    BucketRateLimiter rateLimiter;
    public Db(String credPath, String appName, String spreadsheetId, Integer rowFetchAtOnce) {
        try {
            this.rateLimiter = new BucketRateLimiter(60, Duration.ofMinutes(1));
            this.spreadsheetId = spreadsheetId;
            this.rowFetchAtOnce = rowFetchAtOnce;
            JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
            List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
            NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials googleCredentials;
            try (InputStream inputSteam = new FileInputStream(credPath)) {
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
        rateLimiter.consume();
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
        rateLimiter.consume();
        List<T> result = null;
        try {
            //todo loop it until end of the row by fetching rowFetchAtOnce at a time
            String sheetName = AnnotationUtil.getTable(type);
            int columnCount = AnnotationUtil.getColumns(type).size();
            String finalColumn = Character.toString((char) 65 + columnCount - 1);
            // 65 ascii value of A, so the code will work up to Z column only.
            String range = sheetName + "!A2:" + finalColumn + rowFetchAtOnce;
            List<String> ranges = List.of(range);
            Sheets.Spreadsheets.Values.BatchGet request =
                    sheetService.spreadsheets().values().batchGet(spreadsheetId);
            request.setRanges(ranges);
            request.setValueRenderOption("UNFORMATTED_VALUE"); // FORMATTED_VALUE, UNFORMATTED_VALUE, FORMULA
            request.setDateTimeRenderOption("FORMATTED_STRING"); //SERIAL_NUMBER, FORMATTED_STRING
            BatchGetValuesResponse response = request.execute();
            List<List<Object>> values = response.getValueRanges().get(0).getValues();
            if(values==null){
                return Collections.emptyList();
            }
            //System.out.println(values);
            result = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                if (!values.get(i).stream().anyMatch(x -> x != null)) {
                    continue;
                }
                T model = AnnotationUtil.convert(values.get(i), type);
                model.setRow(i + 2);
                result.add(model);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void generateHeaders(Class<T> type) {
        //TODO set columnwise formatting also
        rateLimiter.consume();
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

    public <T extends GoogleSheet> void add(T obj) {
        List<? extends GoogleSheet> all = getAll(obj.getClass());
        int index = all.size()==0? 2: all.get(all.size() - 1).getRow() + 1;
        obj.setRow(index);
        update(obj);
    }

    @Deprecated
    public <T extends GoogleSheet> void save(T obj) {
        rateLimiter.consume();
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
        rateLimiter.consume();
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
                .setStartRowIndex(obj.getRow() - 1)
                .setEndRowIndex(obj.getRow()));

        cellReq.setRows(rowData);
        cellReq.setFields("userEnteredValue,userEnteredFormat.numberFormat");
        requests.add(new Request().setUpdateCells(cellReq));
        batchRequests.setRequests(requests);
        try {
            response = sheetService.spreadsheets().batchUpdate(spreadsheetId, batchRequests).execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GoogleSheet> void clear(T obj) {
        rateLimiter.consume();
        try {
            Class<? extends GoogleSheet> type = obj.getClass();
            String sheetName = AnnotationUtil.getTable(type);
            int columnCount = AnnotationUtil.getColumns(type).size();
            String finalColumn = Character.toString((char) 65 + columnCount - 1);
            String range = sheetName + "!A" + obj.getRow() + ":" + finalColumn + obj.getRow();
            AnnotationUtil.delete(obj);
            ClearValuesRequest requestBody = new ClearValuesRequest();
            Sheets.Spreadsheets.Values.Clear request = sheetService.spreadsheets().values().clear(spreadsheetId, range, requestBody);
            ClearValuesResponse response = request.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
