package org.example.farm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FarmApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmApplication.class, args);
    }
}
