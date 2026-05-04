package org.example.QuanLyMuaVu.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;

final class FirebaseAdminTokenIssuer implements FirebaseTokenIssuer {

    private static final String FIREBASE_CHAT_APP_NAME = "acm-firebase-chat";

    private final FirebaseProperties properties;
    private final Object lock = new Object();
    private volatile FirebaseApp firebaseApp;

    FirebaseAdminTokenIssuer(FirebaseProperties properties) {
        this.properties = properties;
    }

    @Override
    public String createCustomToken(String appUid, Map<String, Object> claims) {
        FirebaseApp app = getOrInitializeFirebaseApp();

        try {
            return FirebaseAuth.getInstance(app).createCustomToken(appUid, claims);
        } catch (FirebaseAuthException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private FirebaseApp getOrInitializeFirebaseApp() {
        FirebaseApp cachedApp = firebaseApp;
        if (cachedApp != null) {
            return cachedApp;
        }

        synchronized (lock) {
            if (firebaseApp != null) {
                return firebaseApp;
            }
            firebaseApp = initializeFirebaseApp();
            return firebaseApp;
        }
    }

    private FirebaseApp initializeFirebaseApp() {
        FirebaseApp existingApp = findExistingFirebaseApp();
        if (existingApp != null) {
            return existingApp;
        }

        try (InputStream credentialStream = openCredentialStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream);

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);
            String projectId = properties.projectId();
            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId.trim());
            }

            return FirebaseApp.initializeApp(optionsBuilder.build(), FIREBASE_CHAT_APP_NAME);
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FIREBASE_CHAT_UNAVAILABLE);
        }
    }

    private FirebaseApp findExistingFirebaseApp() {
        return FirebaseApp.getApps().stream()
                .filter(app -> FIREBASE_CHAT_APP_NAME.equals(app.getName()))
                .findFirst()
                .orElse(null);
    }

    private InputStream openCredentialStream() throws IOException {
        String serviceAccountJson = properties.serviceAccountJson();
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            byte[] rawJson = serviceAccountJson.trim().getBytes(StandardCharsets.UTF_8);
            return new ByteArrayInputStream(rawJson);
        }

        String serviceAccountPath = properties.serviceAccountPath();
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            Path credentialPath = Path.of(serviceAccountPath.trim());
            return Files.newInputStream(credentialPath);
        }

        throw new AppException(ErrorCode.FIREBASE_CHAT_UNAVAILABLE);
    }
}
