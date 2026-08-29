package com.wikicollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class WikiCollectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(WikiCollectionApplication.class, args);
    }
}