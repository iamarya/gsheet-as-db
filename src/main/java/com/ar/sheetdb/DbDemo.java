package com.ar.sheetdb;

import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class DbDemo {

    @PostConstruct
    public void start(){
        Db db = new Db("/credentials.json",
                "Db Demo",
                "1TsPofNeYK1bBtdNfwySB2KlwXCnqDg11xu7RpZZxCF4", 100);

        //db.create(Person.class);
        db.generateHeaders(Person.class);
        System.out.println(db.getAll(Person.class));
        Person p = new Person();
        p.id = "4";
        p.name = "Mera";
        p.row = 2;
        //db.append(p);
        //db.update(p);
        //db.delete(p);
    }

}
