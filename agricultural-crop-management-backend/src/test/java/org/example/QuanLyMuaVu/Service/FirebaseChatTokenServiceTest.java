package org.example.QuanLyMuaVu.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.example.QuanLyMuaVu.firebase.FirebaseChatTokenResponse;
import org.example.QuanLyMuaVu.firebase.FirebaseChatTokenService;
import org.example.QuanLyMuaVu.firebase.FirebaseTokenIssuer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirebaseChatTokenServiceTest {

    @Mock
    private FirebaseTokenIssuer firebaseTokenIssuer;

    @InjectMocks
    private FirebaseChatTokenService firebaseChatTokenService;

    @Test
    @DisplayName("createChatToken maps stable uid and minimal claims")
    void createChatToken_MapsStableUidAndClaims() {
        when(firebaseTokenIssuer.createCustomToken(eq("u_24"), eq(Map.of(
                "app_uid", "u_24",
                "role", "FARMER"))))
                .thenReturn("custom-token-24");

        FirebaseChatTokenResponse response = firebaseChatTokenService.createChatToken(24L, "FARMER");

        assertEquals("u_24", response.appUid());
        assertEquals("FARMER", response.role());
        assertEquals("custom-token-24", response.customToken());
    }

    @Test
    @DisplayName("createChatToken normalizes ROLE_ prefix and keeps claims minimal")
    void createChatToken_NormalizesRolePrefix() {
        when(firebaseTokenIssuer.createCustomToken(eq("u_7"), eq(Map.of(
                "app_uid", "u_7",
                "role", "EMPLOYEE"))))
                .thenReturn("custom-token-7");

        FirebaseChatTokenResponse response = firebaseChatTokenService.createChatToken(7L, "ROLE_EMPLOYEE");

        assertEquals("u_7", response.appUid());
        assertEquals("EMPLOYEE", response.role());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(firebaseTokenIssuer).createCustomToken(eq("u_7"), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals("u_7", captor.getValue().get("app_uid"));
        assertEquals("EMPLOYEE", captor.getValue().get("role"));
    }
}
