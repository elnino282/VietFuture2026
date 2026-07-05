package org.example.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class IncidentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncidentServiceApplication.class, args);
    }
}
