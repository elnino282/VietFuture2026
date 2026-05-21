package org.example.QuanLyMuaVu.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.QuanLyMuaVu.firebase.FirebaseChatContactResponse;
import org.example.QuanLyMuaVu.firebase.FirebaseChatTokenController;
import org.example.QuanLyMuaVu.firebase.FirebaseChatContactService;
import org.example.QuanLyMuaVu.firebase.FirebaseChatTokenResponse;
import org.example.QuanLyMuaVu.firebase.FirebaseChatTokenService;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FirebaseChatTokenController.class)
class FirebaseChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseChatTokenService firebaseChatTokenService;

    @MockBean
    private FirebaseChatContactService firebaseChatContactService;

    @MockBean
    private CurrentUserService currentUserService;

    @Test
    @WithMockUser(roles = "FARMER")
    @DisplayName("POST /api/v1/firebase/chat-token returns custom token response contract")
    void issueChatToken_ReturnsContract() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(24L);
        when(currentUserService.getCurrentRole()).thenReturn("FARMER");
        when(firebaseChatTokenService.createChatToken(24L, "FARMER"))
                .thenReturn(new FirebaseChatTokenResponse("u_24", "FARMER", "chat-token-24"));

        mockMvc.perform(post("/api/v1/firebase/chat-token").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appUid").value("u_24"))
                .andExpect(jsonPath("$.role").value("FARMER"))
                .andExpect(jsonPath("$.customToken").value("chat-token-24"));

        verify(currentUserService).getCurrentUserId();
        verify(currentUserService).getCurrentRole();
        verify(firebaseChatTokenService).createChatToken(24L, "FARMER");
    }

    @Test
    void issueChatToken_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/firebase/chat-token").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(currentUserService, firebaseChatTokenService);
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void issueChatToken_anyAuthenticatedRole_isAllowed() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(51L);
        when(currentUserService.getCurrentRole()).thenReturn("GUEST");
        when(firebaseChatTokenService.createChatToken(51L, "GUEST"))
                .thenReturn(new FirebaseChatTokenResponse("u_51", "GUEST", "chat-token-51"));

        mockMvc.perform(post("/api/v1/firebase/chat-token").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appUid").value("u_51"));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    @DisplayName("GET /api/v1/firebase/chat-contacts returns wrapped contact list")
    void getChatContacts_ReturnsWrappedResult() throws Exception {
        List<FirebaseChatContactResponse> contacts = List.of(
                new FirebaseChatContactResponse(
                        2L,
                        "u_2",
                        "Farm Rep",
                        "Farm Rep",
                        "Green Farm",
                        "Ward A, Province B",
                        "FARMER"));

        when(firebaseChatContactService.findContacts("farm", List.of(2L, 4L), 5))
                .thenReturn(contacts);

        mockMvc.perform(get("/api/v1/firebase/chat-contacts")
                .param("q", "farm")
                .param("userIds", "2,4")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result[0].userId").value(2))
                .andExpect(jsonPath("$.result[0].farmName").value("Green Farm"));

        verify(firebaseChatContactService).findContacts("farm", List.of(2L, 4L), 5);
    }
}
