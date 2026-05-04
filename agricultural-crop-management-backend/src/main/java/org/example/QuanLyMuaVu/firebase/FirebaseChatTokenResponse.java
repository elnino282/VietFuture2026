package org.example.QuanLyMuaVu.firebase;

public record FirebaseChatTokenResponse(
        String appUid,
        String role,
        String customToken
) {
}
