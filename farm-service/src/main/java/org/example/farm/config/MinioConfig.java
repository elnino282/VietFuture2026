package org.example.farm.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.url:http://localhost:9000}")
    private String minioUrl;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket.farm-documents:farm-documents}")
    private String farmDocumentsBucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();

        initializeBuckets(client);
        return client;
    }

    private void initializeBuckets(MinioClient client) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(farmDocumentsBucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(farmDocumentsBucket).build());
                log.info("Created MinIO bucket: {}", farmDocumentsBucket);
            }

            // Set public read-only policy
            String policy = "{\n" +
                    "  \"Version\": \"2012-10-17\",\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": { \"AWS\": [ \"*\" ] },\n" +
                    "      \"Action\": [ \"s3:GetObject\" ],\n" +
                    "      \"Resource\": [ \"arn:aws:s3:::" + farmDocumentsBucket + "/*\" ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            client.setBucketPolicy(
                    io.minio.SetBucketPolicyArgs.builder()
                            .bucket(farmDocumentsBucket)
                            .config(policy)
                            .build()
            );
            log.info("Configured public read-only policy for bucket: {}", farmDocumentsBucket);
        } catch (Exception e) {
            log.warn("Could not initialize MinIO farm documents bucket. Error: {}", e.getMessage());
        }
    }

    public String getFarmDocumentsBucket() {
        return farmDocumentsBucket;
    }
}
