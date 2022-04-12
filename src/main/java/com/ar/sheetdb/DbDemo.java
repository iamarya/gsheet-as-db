package com.ar.sheetdb;

import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;

@Component
public class DbDemo {

    @PostConstruct
    public void start(){
        Db db = new Db("/credentials.json",
                "Db Demo",
                "1TsPofNeYK1bBtdNfwySB2KlwXCnqDg11xu7RpZZxCF4", 100);

        //db.create(Person.class);
        //db.generateHeaders(Person.class);
        System.out.println(db.getAll(Person.class));
        Person p = new Person();
        p.id = 123;
        p.name = "Teeera";
        p.profit = 3.33333345;
        p.row = 4;
        p.date = LocalDate.now();
        //db.save(p);
        db.update(p);
        //db.delete(p);

    }

}
