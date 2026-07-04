package org.example.season;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "org.example.season.client")
public class SeasonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeasonApplication.class, args);
    }
}
