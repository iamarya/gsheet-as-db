package com.ar.sheetdb;

@Table(name="Person", id=1415932384)
public class Person extends GoogleSheet{
    @Column(name = "Id", order = 1)
    public String id;

    @Column(name = "Name", order = 2)
    public String name;

    @Override
    public String toString() {
        return "Person{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", row='" + row + '\'' +
                '}';
    }
}
