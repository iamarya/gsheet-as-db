package com.ar.sheetdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SheetDbApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(SheetDbApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}

}
