package org.example.QuanLyMuaVu.firebase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FirebaseChatTokenService {

    private final FirebaseTokenIssuer firebaseTokenIssuer;

    public FirebaseChatTokenResponse createChatToken(Long userId, String role) {
        String appUid = buildAppUid(userId);
        String normalizedRole = normalizeRole(role);

        Map<String, Object> claims = new HashMap<>();
        claims.put("app_uid", appUid);
        claims.put("role", normalizedRole);

        String customToken = firebaseTokenIssuer.createCustomToken(appUid, claims);
        return new FirebaseChatTokenResponse(appUid, normalizedRole, customToken);
    }

    static String buildAppUid(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id is required to build Firebase UID.");
        }
        return "u_" + userId;
    }

    static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = role.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, "ROLE_".length())) {
            normalized = normalized.substring("ROLE_".length());
        }
        normalized = normalized.trim();

        return normalized.isEmpty()
                ? "UNKNOWN"
                : normalized.toUpperCase(Locale.ROOT);
    }
}
