package com.ar.sheetdb;

import java.time.LocalDate;

@Table(name="Person", id=1415932384)
public class Person extends GoogleSheet {
    @Column(name = "Id", order = 1)
    public Integer id;

    @Column(name = "Name", order = 2, formula=true)
    public String name;

    @Column(name = "Profit", order = 3)
    public Double profit;

    @Column(name = "Date", order = 4)
    public LocalDate date;

    @Column(name = "Active", order = 5)
    public Boolean active;

    @Override
    public String toString() {
        return "Person{" +
                "row=" + row +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", profit=" + profit +
                ", date=" + date +
                ", active=" + active +
                '}';
    }
}
