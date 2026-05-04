package org.example.QuanLyMuaVu.module.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetRateLimiter {

    private final int maxRequests;
    private final Duration window;

    private final Map<String, Deque<Instant>> emailRequests = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> ipRequests = new ConcurrentHashMap<>();

    public PasswordResetRateLimiter(
            @Value("${app.password-reset.rate-limit.max:3}") int maxRequests,
            @Value("${app.password-reset.rate-limit.window-minutes:15}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public boolean isAllowed(String email, String ip) {
        boolean emailAllowed = isAllowedForKey(normalize(email), emailRequests);
        boolean ipAllowed = isAllowedForKey(normalize(ip), ipRequests);
        return emailAllowed && ipAllowed;
    }

    private boolean isAllowedForKey(String key, Map<String, Deque<Instant>> store) {
        if (key == null || key.isBlank()) {
            return true;
        }

        Deque<Instant> deque = store.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                return false;
            }
            deque.addLast(now);
        }
        return true;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
