-- =====================================================
-- V8: Certification Audit Workflow
-- Mở rộng vòng đời chứng nhận VietGAP: audit ngoài,
-- nonconformity, corrective action, cấp giấy, periodic review
-- =====================================================

-- 1. Bảng đợt đánh giá (audit) bởi tổ chức chứng nhận
CREATE TABLE certification_audits (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id             INT          NOT NULL,
    audit_type            VARCHAR(30)  NOT NULL COMMENT 'INITIAL, PERIODIC, RE_AUDIT',
    scheduled_date        DATE         NULL,
    auditor_user_id       BIGINT       NULL,
    auditor_org_name      VARCHAR(255) NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED, IN_PROGRESS, PASSED, FAILED',
    interview_notes       TEXT         NULL,
    sample_collection_notes TEXT       NULL,
    conducted_at          DATETIME     NULL,
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_audits_record (record_id),
    INDEX idx_audits_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Bảng điểm không phù hợp phát hiện khi audit
CREATE TABLE certification_nonconformities (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id              BIGINT       NOT NULL,
    checklist_item_id     INT          NULL COMMENT 'FK tới certification_checklist_items nếu liên quan tiêu chí cụ thể',
    severity              VARCHAR(20)  NOT NULL COMMENT 'MINOR, MAJOR, CRITICAL',
    description           TEXT         NOT NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, CORRECTIVE_ACTION_SUBMITTED, RESOLVED, REJECTED',
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_nc_audit (audit_id),
    INDEX idx_nc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bảng kế hoạch khắc phục + bằng chứng
CREATE TABLE certification_corrective_actions (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nonconformity_id      BIGINT       NOT NULL,
    plan_description      TEXT         NULL,
    evidence_urls         TEXT         NULL COMMENT 'JSON array of URLs',
    applies_from_season_id INT         NULL COMMENT 'Liên kết mùa vụ tương lai nếu cần thời gian chứng minh',
    submitted_by_user_id  BIGINT       NULL,
    submitted_at          DATETIME     NULL,
    reviewed_by_user_id   BIGINT       NULL,
    review_result         VARCHAR(20)  NULL COMMENT 'ACCEPTED, REJECTED',
    review_note           TEXT         NULL,
    reviewed_at           DATETIME     NULL,
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_ca_nonconformity (nonconformity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Mở rộng bảng certification_records (additive, cột nullable)
ALTER TABLE certification_records
    ADD COLUMN certificate_number VARCHAR(100) NULL AFTER auditor_notes,
    ADD COLUMN certificate_document_id INT NULL AFTER certificate_number,
    ADD COLUMN next_periodic_review_date DATE NULL AFTER certificate_document_id,
    ADD COLUMN published_at DATETIME NULL AFTER next_periodic_review_date,
    ADD COLUMN published_by_user_id BIGINT NULL AFTER published_at;

-- Mở rộng cột status để chứa các trạng thái mới (tăng length)
ALTER TABLE certification_records
    MODIFY COLUMN status VARCHAR(40) DEFAULT 'IN_PROGRESS';

