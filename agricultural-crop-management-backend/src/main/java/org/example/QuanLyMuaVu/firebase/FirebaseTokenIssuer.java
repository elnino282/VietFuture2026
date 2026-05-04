package org.example.QuanLyMuaVu.firebase;

import java.util.Map;

public interface FirebaseTokenIssuer {
    String createCustomToken(String appUid, Map<String, Object> claims);
}
