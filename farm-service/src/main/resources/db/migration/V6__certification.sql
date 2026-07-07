-- Bảng tiêu chuẩn chứng nhận
CREATE TABLE certification_standards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(30)  NOT NULL COMMENT 'VIETGAP_PLANTING, VIETGAP_AQUACULTURE, ORGANIC, GLOBALGAP',
    version      VARCHAR(20)  DEFAULT '1.0',
    description  TEXT,
    is_active    BOOLEAN     DEFAULT TRUE,
    created_at   DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng checklist item
CREATE TABLE certification_checklist_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    standard_id      INT          NOT NULL,
    item_code       VARCHAR(50)  NOT NULL,
    category        VARCHAR(50)  NOT NULL COMMENT 'PRODUCTION_AREA, SEED, CULTIVATION, HARVEST, etc.',
    description     TEXT         NOT NULL,
    is_mandatory    BOOLEAN     DEFAULT TRUE,
    weight_pct       DECIMAL(5,2) DEFAULT 1.00 COMMENT 'Trọng số trong tính compliance score',
    data_source_type VARCHAR(30)  NULL COMMENT 'FIELD_LOG, SOIL_TEST, WATER_TEST, DISEASE_RECORD, MANUAL',
    data_source_query VARCHAR(500) NULL COMMENT 'Query pattern để auto-fill',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_items_standard (standard_id),
    INDEX idx_items_category (category),
    UNIQUE KEY uk_standard_item (standard_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng record chứng nhận của từng farm
CREATE TABLE certification_records (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    farm_id           INT          NOT NULL,
    standard_id       INT          NOT NULL,
    compliance_score  DECIMAL(5,2) NULL COMMENT 'Tính tự động từ checklist items',
    status            VARCHAR(20)   DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS, READY_TO_APPLY, APPLIED, CERTIFIED, REJECTED, EXPIRED',
    applied_at        DATETIME     NULL,
    certified_at      DATETIME     NULL,
    expiry_date       DATE         NULL,
    auditor_notes      TEXT         NULL,
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_records_farm (farm_id),
    INDEX idx_records_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng trạng thái từng checklist item của farm
CREATE TABLE certification_item_statuses (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    record_id      INT          NOT NULL,
    checklist_item_id INT        NOT NULL,
    status         VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PASS, FAIL, PENDING, NOT_APPLICABLE',
    evidence_url   VARCHAR(1000) NULL,
    notes          TEXT         NULL,
    checked_at     DATETIME     NULL,
    checked_by     BIGINT       NULL,
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_record_item (record_id, checklist_item_id),
    INDEX idx_item_statuses_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
