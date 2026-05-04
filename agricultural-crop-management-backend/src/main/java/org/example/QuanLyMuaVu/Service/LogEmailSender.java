package org.example.QuanLyMuaVu.Service;

import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogEmailSender implements EmailSender {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(token=)([^&\\s\"']+)", Pattern.CASE_INSENSITIVE);

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        String redactedBody = redactTokens(htmlBody);
        log.info("mail=log to={} subject={} body={}", to, subject, redactedBody);
    }

    private String redactTokens(String htmlBody) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return htmlBody;
        }
        return TOKEN_PATTERN.matcher(htmlBody).replaceAll("$1REDACTED");
    }
}
