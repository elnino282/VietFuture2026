package org.example.QuanLyMuaVu.firebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FirebaseAdminTokenIssuerTest {

    @Test
    @DisplayName("createCustomToken throws clear service-unavailable error when firebase env is missing")
    void createCustomToken_WhenConfigMissing_ThrowsServiceUnavailable() {
        FirebaseProperties properties = new FirebaseProperties("", "", "");
        FirebaseAdminTokenIssuer issuer = new FirebaseAdminTokenIssuer(properties);

        AppException exception = assertThrows(AppException.class,
                () -> issuer.createCustomToken("u_24", Map.of("app_uid", "u_24", "role", "FARMER")));

        assertEquals(ErrorCode.FIREBASE_CHAT_UNAVAILABLE, exception.getErrorCode());
    }
}
