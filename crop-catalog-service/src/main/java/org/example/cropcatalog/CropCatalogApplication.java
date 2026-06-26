package org.example.cropcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CropCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CropCatalogApplication.class, args);
    }
}
