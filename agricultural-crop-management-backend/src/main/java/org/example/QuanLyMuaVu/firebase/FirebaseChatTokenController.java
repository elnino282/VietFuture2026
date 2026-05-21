package org.example.QuanLyMuaVu.firebase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/firebase")
@RequiredArgsConstructor
public class FirebaseChatTokenController {

    private final FirebaseChatTokenService firebaseChatTokenService;
    private final FirebaseChatContactService firebaseChatContactService;
    private final CurrentUserService currentUserService;

    @PostMapping("/chat-token")
    public ResponseEntity<FirebaseChatTokenResponse> createChatToken() {
        Long userId = currentUserService.getCurrentUserId();
        String role = currentUserService.getCurrentRole();

        FirebaseChatTokenResponse response = firebaseChatTokenService.createChatToken(userId, role);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat-contacts")
    public ApiResponse<List<FirebaseChatContactResponse>> getChatContacts(
                    @RequestParam(value = "q", required = false) String query,
                    @RequestParam(value = "userIds", required = false) List<Long> userIds,
                    @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResponse.success(firebaseChatContactService.findContacts(query, userIds, limit));
    }
}
