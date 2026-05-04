package org.example.QuanLyMuaVu.Config;

import org.example.QuanLyMuaVu.Service.EmailSender;
import org.example.QuanLyMuaVu.Service.LogEmailSender;
import org.example.QuanLyMuaVu.Service.SmtpEmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Configuration for email sending.
 * Uses LogEmailSender by default (just logs emails).
 * Uses SmtpEmailSender when app.mail.enabled=true.
 */
@Configuration
public class MailSenderConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true", matchIfMissing = false)
    public EmailSender smtpEmailSender(JavaMailSender mailSender, AppProperties appProperties) {
        return new SmtpEmailSender(mailSender, appProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
    public EmailSender logEmailSender() {
        return new LogEmailSender();
    }
}
