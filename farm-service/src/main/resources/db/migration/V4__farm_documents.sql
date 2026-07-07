CREATE TABLE farm_documents (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    farm_id         INT          NOT NULL,
    document_type   VARCHAR(50)   NOT NULL COMMENT 'LAND_CERTIFICATE, SOIL_TEST_REPORT, WATER_TEST_REPORT, PESTICIDE_RECORD, FERTILIZER_RECORD, HARVEST_LOG, INTERNAL_AUDIT, CERTIFICATE, OTHER',
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    file_url        VARCHAR(1000) NULL     COMMENT 'MinIO object URL',
    issued_date     DATE          NULL,
    expiry_date     DATE          NULL,
    verification_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, VERIFIED, REJECTED, EXPIRED',
    verified_by     BIGINT        NULL,
    verified_at     DATETIME      NULL,
    created_by      BIGINT        NOT NULL,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_farm_documents_farm_id (farm_id),
    INDEX idx_farm_documents_type (document_type),
    INDEX idx_farm_documents_status (verification_status),
    INDEX idx_farm_documents_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
