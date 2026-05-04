package org.example.QuanLyMuaVu.module.identity.config;

import java.util.Optional;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    private final IdentityQueryPort identityQueryPort;

    public AuditConfig(IdentityQueryPort identityQueryPort) {
        this.identityQueryPort = identityQueryPort;
    }

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            String username = authentication.getName();
            if (username == null || "anonymousUser".equals(username)) {
                return Optional.empty();
            }

            return identityQueryPort.findUserByUsername(username)
                    .map(org.example.QuanLyMuaVu.module.identity.entity.User::getId);
        };
    }
}
