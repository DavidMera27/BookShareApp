package com.bookshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

@SpringBootApplication
@EnableReactiveMongoAuditing
public class AppLibrosApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppLibrosApplication.class, args);
	}

}
