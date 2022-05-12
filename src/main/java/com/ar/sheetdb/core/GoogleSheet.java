package com.ar.sheetdb.core;

public abstract class GoogleSheet {
    private Integer row;

    public GoogleSheet() {

    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }
}
