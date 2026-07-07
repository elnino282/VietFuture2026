-- Bảng ghi nhận lần phun thuốc BVTV
CREATE TABLE IF NOT EXISTS pesticide_records (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    season_id             INT          NOT NULL,
    plot_id               INT          NOT NULL,
    field_log_id          INT          NULL COMMENT 'Link đến field log nếu có',
    pesticide_name        VARCHAR(255)  NOT NULL COMMENT 'Tên thương mại',
    active_ingredient     VARCHAR(255)  NULL COMMENT 'Hoạt chất',
    phi_days              INT          NOT NULL COMMENT 'Số ngày cách ly theo khuyến cáo',
    harvest_allowed_date  DATE          GENERATED ALWAYS AS (application_date + INTERVAL phi_days DAY) STORED,
    application_date      DATE          NOT NULL,
    application_method    VARCHAR(100)  NULL COMMENT 'PHUN, TƯỚI, RẮC, NGÂM',
    dosage                VARCHAR(100)  NULL COMMENT 'Liều lượng đã dùng',
    target_pest           VARCHAR(255)  NULL COMMENT 'Đối tượng phòng trừ',
    note                  TEXT         NULL,
    created_by            BIGINT        NOT NULL,
    created_at            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pesticide_season (season_id),
    INDEX idx_pesticide_plot (plot_id),
    INDEX idx_pesticide_allowed_date (harvest_allowed_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng reference PHI của các hoạt chất phổ biến
CREATE TABLE IF NOT EXISTS pesticide_phi_reference (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    active_ingredient VARCHAR(255)  NOT NULL,
    pesticide_name    VARCHAR(255)  NULL COMMENT 'Tên thương mại phổ biến chứa hoạt chất này',
    phi_days          INT          NOT NULL COMMENT 'Số ngày PHI theo khuyến cáo',
    mrl_mg_per_kg     DECIMAL(10,4) NULL COMMENT 'Maximum Residue Limit',
    crop_type         VARCHAR(100)  NULL COMMENT 'Loại cây trồng áp dụng',
    source            VARCHAR(100)  DEFAULT 'EPA/CODEX' COMMENT 'Nguồn tham khảo',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ingredient_crop (active_ingredient, crop_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
