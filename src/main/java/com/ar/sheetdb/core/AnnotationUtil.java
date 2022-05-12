package com.ar.sheetdb.core;

import com.ar.sheetdb.example.Person;
import com.google.api.services.sheets.v4.model.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class AnnotationUtil {

    public static <T extends GoogleSheet> String getTable(Class<T> type) {
        return type.getAnnotation(Table.class).name();
    }

    public static <T extends GoogleSheet> int getSheetId(Class<T> type) {
        return type.getAnnotation(Table.class).id();
    }

    public static <T extends GoogleSheet> List<String> getColumns(Class<T> type) {
        return Arrays.stream(type.getFields())
                .filter(f -> f.getAnnotation(Column.class) != null)
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(Column.class).order()))
                .map(f -> f.getAnnotation(Column.class).name()).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        System.out.println("Arya " + getTable(Person.class));
        System.out.println(getColumns(Person.class));
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    //read data
    public static <T extends GoogleSheet> T convert(List<Object> x, Class<T> type) {
        T o = null;
        try {
            o = type.newInstance();
            List<Field> fields = Arrays.stream(type.getFields())
                    .filter(f -> f.getAnnotation(Column.class) != null)
                    .sorted(Comparator.comparingInt(f -> f.getAnnotation(Column.class).order()))
                    .collect(Collectors.toList());
            for (int i = 0; i < x.size(); i++) {
                String value = String.valueOf(x.get(i));
                if(value.equals("")){
                    fields.get(i).set(o, null);
                } else if (fields.get(i).getType().equals(Integer.class)) {
                    fields.get(i).set(o, Integer.parseInt(value));
                }else if (fields.get(i).getType().equals(Double.class)) {
                    fields.get(i).set(o, Double.parseDouble(value));
                }else if (fields.get(i).getType().equals(LocalDate.class)) {
                    fields.get(i).set(o, LocalDate.parse(value, formatter));
                }else if (fields.get(i).getType().equals(Boolean.class)) {
                    fields.get(i).set(o, Boolean.valueOf(value));
                }else if (fields.get(i).getType().equals(String.class)) {
                    fields.get(i).set(o, x.get(i));
                }else if (fields.get(i).getType().isEnum()) {
                    Method m = fields.get(i).getType().getMethod("valueOf", String.class);
                    fields.get(i).set(o, m.invoke(null, value));
                } else {
                    throw new RuntimeException("Field is unknown type");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return o;
    }

    public static <T extends GoogleSheet> void delete(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        List<Field> fields = Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.getAnnotation(Column.class) != null).collect(Collectors.toList());
        fields.forEach(f -> {
            try {
                f.set(obj, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static LocalDate baseDate = LocalDate.parse("30-Dec-1899", formatter);

    //write data
    public static <T extends GoogleSheet> RowData convert(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        List<CellData> cellData = new ArrayList<CellData>();

        List<Field> fields = Arrays.stream(type.getFields())
                .filter(f -> f.getAnnotation(Column.class) != null)
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(Column.class).order())).collect(Collectors.toList());

        fields.forEach(f -> {
            try {
                CellData cell = new CellData();
                if(f.get(obj)==null){
                    cell.setUserEnteredValue(new ExtendedValue().setStringValue(null));
                } else if (f.getType().equals(Integer.class)) {
                    cell.setUserEnteredValue(new ExtendedValue().setNumberValue(Double.valueOf((Integer) f.get(obj))));
                } else if (f.getType().equals(Double.class)) {
                    cell.setUserEnteredValue(new ExtendedValue().setNumberValue((Double) f.get(obj)));
                    cell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("0.00")));
                } else if (f.getType().equals(LocalDate.class)) {
                    LocalDate date = (LocalDate) f.get(obj);
                    long days = DAYS.between(baseDate, date);
                    cell.setUserEnteredValue(new ExtendedValue().setNumberValue(Double.valueOf(days)));
                    cell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("DATE").setPattern("dd-MMM-yyyy")));
                } else if (f.getType().equals(Boolean.class)) {
                    cell.setUserEnteredValue(new ExtendedValue().setBoolValue((Boolean) f.get(obj)));
                } else if (f.getType().equals(String.class)) {
                    if (f.getAnnotation(Column.class).formula()) {
                        cell.setUserEnteredValue(new ExtendedValue().setFormulaValue(String.valueOf(f.get(obj))));
                    } else {
                        cell.setUserEnteredValue(new ExtendedValue().setStringValue(String.valueOf(f.get(obj))));
                    }
                } else if(f.getType().isEnum()){
                    cell.setUserEnteredValue(new ExtendedValue().setStringValue(String.valueOf(f.get(obj))));
                } else {
                    throw new RuntimeException("Field type is not supported "+ f.getType());
                }
                cellData.add(cell);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new RowData().setValues(cellData);
    }
}
