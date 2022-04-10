package com.ar.sheetdb;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationUtil {

    public static <T extends GoogleSheet> String getTable(Class<T> type) {
        return type.getAnnotation(Table.class).name();
    }

    public static <T extends GoogleSheet> int getSheetId(Class<T> type) {
        return type.getAnnotation(Table.class).id();
    }

    public static <T extends GoogleSheet> List<String> getColumns(Class<T> type) {
        return Arrays.stream(type.getDeclaredFields())
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(Column.class).order()))
                .map(f -> f.getAnnotation(Column.class).name()).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        System.out.println("Arya " + getTable(Person.class));
        System.out.println(getColumns(Person.class));
    }

    public static <T extends GoogleSheet> T convert(List<Object> x, Class<T> type) {
        T o = null;
        try {
            o = type.newInstance();
            List<Field> fields = Arrays.stream(type.getDeclaredFields())
                    .sorted(Comparator.comparingInt(f -> f.getAnnotation(Column.class).order())).collect(Collectors.toList());
            for (int i = 0; i< x.size(); i++){
                fields.get(i).set(o, x.get(i));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } 
        return o;
    }

    public static <T extends GoogleSheet> List<List<Object>> convert(T obj) {
        Class<? extends GoogleSheet> type = obj.getClass();
        List<Field> fields = Arrays.stream(type.getDeclaredFields())
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(Column.class).order())).collect(Collectors.toList());
        List<Object> row = new ArrayList<>();
        fields.forEach(f->{
            try {
                row.add(f.get(obj));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return List.of(row);
    }
}
