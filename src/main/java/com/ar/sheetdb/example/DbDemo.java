package com.ar.sheetdb.example;

import com.ar.sheetdb.core.Db;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Component
public class DbDemo {

    @Value("${gsheet.credential}")
    private String credentialPath;

    @PostConstruct
    public void start() throws InterruptedException {
        Db db = new Db(credentialPath,
                "Db Demo",
                "1TsPofNeYK1bBtdNfwySB2KlwXCnqDg11xu7RpZZxCF4", 100);

        //db.create(Person.class);
        //db.generateHeaders(Person.class);
        System.out.println(db.getAll(Person.class));
        Person p = new Person();
        p.id = 123;
        p.name = "=upper(\"arya\")";
        p.profit = 3.33333345;
        p.row = 4;
        p.active = true;
        p.date = LocalDate.now();
        db.update(p);
        for (int i =0; i< 200; i++) {
            p.row = i+2;
            p.id=i;
            if(p.row%2==0)
                p.active=false;
            //db.update(p);
        }
        //db.update(p);
        //db.clear(p);
//        Titrator t = new Titrator(5, Duration.ofSeconds(5));
//        t.consume();
//        t.consume();
//        Thread.sleep(20*1000);
//        int i =0;
//        while (true){
//            t.consume();
//            System.out.println(LocalDateTime.now()+ " Test"+ i++);
//        }
    }

}
