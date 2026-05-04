package org.example.QuanLyMuaVu.Service;

public interface EmailSender {
    void sendHtml(String to, String subject, String htmlBody);
}
