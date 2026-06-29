package org.example.adminreporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdminReportingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminReportingApplication.class, args);
    }
}
