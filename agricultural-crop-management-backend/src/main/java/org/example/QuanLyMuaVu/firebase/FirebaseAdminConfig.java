package org.example.QuanLyMuaVu.firebase;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseAdminConfig {

    @Bean
    FirebaseTokenIssuer firebaseTokenIssuer(FirebaseProperties properties) {
        return new FirebaseAdminTokenIssuer(properties);
    }
}
