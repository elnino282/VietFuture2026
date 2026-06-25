package org.example.season;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeasonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeasonApplication.class, args);
    }
}
