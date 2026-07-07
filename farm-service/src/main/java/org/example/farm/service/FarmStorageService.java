package org.example.farm.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.farm.config.MinioConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${minio.public-url:http://localhost:9000}")
    private String publicUrl;

    public String storeDocument(MultipartFile file, Long userId) {
        validateFile(file, MAX_FILE_SIZE);

        String extension = getExtension(file.getOriginalFilename(), file.getContentType());
        String objectName = generateObjectName(userId, extension);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getFarmDocumentsBucket())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return buildObjectUrl(minioConfig.getFarmDocumentsBucket(), objectName);
        } catch (Exception e) {
            log.error("Failed to store farm document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store farm document: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (maxSize / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, WebP, and PDF files are allowed");
        }
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if ("application/pdf".equals(contentType)) {
            return ".pdf";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String generateObjectName(Long userId, String extension) {
        return "user-" + userId + "/" + UUID.randomUUID().toString() + extension;
    }

    private String buildObjectUrl(String bucket, String objectName) {
        return publicUrl + "/" + bucket + "/" + objectName;
    }
}
