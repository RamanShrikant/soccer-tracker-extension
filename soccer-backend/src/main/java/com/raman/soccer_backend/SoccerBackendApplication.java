package com.raman.soccer_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.raman")
public class SoccerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoccerBackendApplication.class, args);
    }
}
