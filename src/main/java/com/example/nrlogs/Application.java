package com.example.nrlogs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * NR Log Search - a small web app to query New Relic logs in plain English.
 * Run with: ./gradlew bootRun   then open http://localhost:8080
 */
@SpringBootApplication
@EnableConfigurationProperties(NrLogsProperties.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
