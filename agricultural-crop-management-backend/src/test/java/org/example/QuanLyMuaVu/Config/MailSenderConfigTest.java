package org.example.QuanLyMuaVu.Config;

import org.example.QuanLyMuaVu.Service.EmailSender;
import org.example.QuanLyMuaVu.Service.LogEmailSender;
import org.example.QuanLyMuaVu.Service.SmtpEmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

class MailSenderConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(TestConfig.class, MailSenderConfig.class);

    @Test
    void whenMailDisabled_usesLogEmailSender() {
        contextRunner
                .withPropertyValues("app.mail.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSender.class);
                    assertThat(context.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class);
                });
    }

    @Test
    void whenMailEnabled_usesSmtpEmailSender() {
        contextRunner
                .withPropertyValues("app.mail.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSender.class);
                    assertThat(context.getBean(EmailSender.class)).isInstanceOf(SmtpEmailSender.class);
                });
    }

    @Configuration
    static class TestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }

        @Bean
        AppProperties appProperties() {
            return new AppProperties();
        }
    }
}
