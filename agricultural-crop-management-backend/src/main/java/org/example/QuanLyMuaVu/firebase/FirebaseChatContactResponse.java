package org.example.QuanLyMuaVu.firebase;

public record FirebaseChatContactResponse(
                Long userId,
                String firebaseUid,
                String displayName,
                String representativeName,
                String farmName,
                String address,
                String role) {
}
