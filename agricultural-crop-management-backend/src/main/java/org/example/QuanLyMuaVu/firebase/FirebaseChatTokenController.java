package org.example.QuanLyMuaVu.firebase;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/firebase")
@RequiredArgsConstructor
public class FirebaseChatTokenController {

    private final FirebaseChatTokenService firebaseChatTokenService;
    private final CurrentUserService currentUserService;

    @PostMapping("/chat-token")
    public ResponseEntity<FirebaseChatTokenResponse> createChatToken() {
        Long userId = currentUserService.getCurrentUserId();
        String role = currentUserService.getCurrentRole();

        FirebaseChatTokenResponse response = firebaseChatTokenService.createChatToken(userId, role);
        return ResponseEntity.ok(response);
    }
}
