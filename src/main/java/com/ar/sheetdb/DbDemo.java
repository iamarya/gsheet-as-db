package com.ar.sheetdb;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DbDemo {

    @PostConstruct
    public void start() throws InterruptedException {
        Db db = new Db("/credentials.json",
                "Db Demo",
                "1TsPofNeYK1bBtdNfwySB2KlwXCnqDg11xu7RpZZxCF4", 100);

        //db.create(Person.class);
        //db.generateHeaders(Person.class);
        //System.out.println(db.getAll(Person.class));
        Person p = new Person();
        p.id = 123;
        p.name = "Teeera";
        p.profit = 3.33333345;
        p.row = 4;
        p.date = LocalDate.now();
        for (int i =0; i< 100; i++) {
            p.row = i+2;
            p.id=i;
            db.update(p);
        }
        //db.update(p);
        //db.clear(p);


    }

}
