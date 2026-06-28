package org.example.marketplace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.entity.IdempotencyRecord;
import org.example.marketplace.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final int DEFAULT_EXPIRATION_HOURS = 24;

    final IdempotencyRecordRepository idempotencyRecordRepository;
    final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public <T> Optional<T> getExistingResponse(String key, String endpoint, Class<T> responseType) {
        return idempotencyRecordRepository.findByKeyValue(key)
                .filter(record -> record.getEndpoint().equals(endpoint))
                .filter(record -> record.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(record -> !"PROCESSING".equals(record.getResponseBody()))
                .map(record -> {
                    try {
                        return objectMapper.readValue(record.getResponseBody(), responseType);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize idempotent response for key: {}", key, e);
                        return null;
                    }
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordResponse(String key, String endpoint, Object response, int statusCode) {
        try {
            String responseBody = objectMapper.writeValueAsString(response);
            Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByKeyValue(key);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                record.setResponseBody(responseBody);
                record.setResponseStatus(statusCode);
                record.setExpiresAt(LocalDateTime.now().plusHours(DEFAULT_EXPIRATION_HOURS));
                idempotencyRecordRepository.saveAndFlush(record);
            } else {
                IdempotencyRecord record = IdempotencyRecord.builder()
                        .keyValue(key)
                        .endpoint(endpoint)
                        .responseBody(responseBody)
                        .responseStatus(statusCode)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusHours(DEFAULT_EXPIRATION_HOURS))
                        .build();
                idempotencyRecordRepository.saveAndFlush(record);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency key: {}", key, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquireLock(String key, String endpoint) {
        // Check if already exists
        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByKeyValue(key);
        if (existing.isPresent()) {
            return false;
        }

        // Try to create a placeholder record
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .keyValue(key)
                    .endpoint(endpoint)
                    .responseBody("PROCESSING")
                    .responseStatus(0) // 0 indicates processing
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(5)) // Short lock expiration
                    .build();
            idempotencyRecordRepository.saveAndFlush(record);
            return true;
        } catch (Exception e) {
            // Likely a duplicate key constraint violation
            log.debug("Failed to acquire idempotency lock for key: {}", key);
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteLock(String key) {
        try {
            idempotencyRecordRepository.findByKeyValue(key)
                    .ifPresent(record -> {
                        idempotencyRecordRepository.delete(record);
                        idempotencyRecordRepository.flush();
                    });
        } catch (Exception e) {
            log.warn("Failed to delete lock for key: {}", key, e);
        }
    }
}
