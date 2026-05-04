package org.example.QuanLyMuaVu.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        String projectId,
        String serviceAccountJson,
        String serviceAccountPath
) {
}
