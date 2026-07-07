CREATE TABLE delivery_providers (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL UNIQUE COMMENT 'GHTK, GHN, NINJA_VAN, JT_EXPRESS',
    name          VARCHAR(100)  NOT NULL,
    supports_cold_chain BOOLEAN DEFAULT FALSE,
    supports_same_day    BOOLEAN DEFAULT FALSE,
    is_active     BOOLEAN     DEFAULT TRUE,
    api_endpoint  VARCHAR(500) NULL COMMENT 'Real API endpoint (null = demo mode)',
    api_key       VARCHAR(255) NULL,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_rates (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    provider_id    INT          NOT NULL,
    zone_from      VARCHAR(50)  NOT NULL COMMENT 'Tỉnh/thành gửi',
    zone_to         VARCHAR(50)  NOT NULL COMMENT 'Tỉnh/thành nhận',
    weight_min_kg   DECIMAL(10,2) NOT NULL DEFAULT 0,
    weight_max_kg   DECIMAL(10,2) NOT NULL,
    base_rate_vnd   DECIMAL(12,2) NOT NULL COMMENT 'Phí cơ bản (VNĐ)',
    per_kg_vnd     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Phí theo kg vượt',
    estimated_hours INT         DEFAULT 48 COMMENT 'Ước tính giao (giờ)',
    is_cold_chain   BOOLEAN     DEFAULT FALSE,
    cold_chain_fee_vnd DECIMAL(12,2) DEFAULT 0,

    INDEX idx_rates_provider (provider_id),
    INDEX idx_rates_zone (zone_from, zone_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_orders (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    marketplace_order_id BIGINT      NOT NULL COMMENT 'Link đến MarketplaceOrder',
    provider_id          INT         NOT NULL,
    tracking_number      VARCHAR(100) NULL,
    status               VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, PICKUP_SCHEDULED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED, CANCELLED',
    shipping_fee_vnd     DECIMAL(12,2) NOT NULL,
    estimated_delivery    DATETIME    NULL,
    actual_delivery      DATETIME    NULL,
    is_perishable        BOOLEAN     DEFAULT FALSE,
    requires_cold_chain   BOOLEAN     DEFAULT FALSE,
    recipient_name       VARCHAR(255) NOT NULL,
    recipient_phone      VARCHAR(20)  NOT NULL,
    recipient_address    VARCHAR(500) NOT NULL,
    recipient_province    VARCHAR(100) NOT NULL,
    weight_kg            DECIMAL(10,2) DEFAULT 0,
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_delivery_marketplace (marketplace_order_id),
    INDEX idx_delivery_status (status),
    INDEX idx_delivery_tracking (tracking_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed providers
INSERT INTO delivery_providers (id, code, name, supports_cold_chain, supports_same_day) VALUES
(1, 'GHTK', 'Giao hàng tiết kiệm', TRUE, TRUE),
(2, 'GHN', 'Giao hàng nhanh', TRUE, TRUE),
(3, 'NINJA_VAN', 'Ninja Van', FALSE, FALSE),
(4, 'JT_EXPRESS', 'J&T Express', FALSE, FALSE);

-- Seed rates for testing
-- From Lâm Đồng to other provinces
INSERT INTO delivery_rates (provider_id, zone_from, zone_to, weight_min_kg, weight_max_kg, base_rate_vnd, per_kg_vnd, estimated_hours, is_cold_chain, cold_chain_fee_vnd) VALUES
-- Lâm Đồng -> Lâm Đồng (Internal)
(1, 'Lâm Đồng', 'Lâm Đồng', 0, 50, 15000, 2000, 24, FALSE, 0),
(1, 'Lâm Đồng', 'Lâm Đồng', 0, 50, 25000, 3000, 4, TRUE, 15000), -- Cold chain same-day
(2, 'Lâm Đồng', 'Lâm Đồng', 0, 50, 16000, 2500, 24, FALSE, 0),
(2, 'Lâm Đồng', 'Lâm Đồng', 0, 50, 28000, 3500, 6, TRUE, 18000),
(3, 'Lâm Đồng', 'Lâm Đồng', 0, 100, 14000, 1800, 36, FALSE, 0),
(4, 'Lâm Đồng', 'Lâm Đồng', 0, 100, 13000, 1500, 36, FALSE, 0),

-- Lâm Đồng -> Hồ Chí Minh
(1, 'Lâm Đồng', 'Hồ Chí Minh', 0, 50, 30000, 5000, 36, FALSE, 0),
(1, 'Lâm Đồng', 'Hồ Chí Minh', 0, 50, 50000, 10000, 12, TRUE, 30000), -- Cold chain express
(2, 'Lâm Đồng', 'Hồ Chí Minh', 0, 50, 32000, 6000, 36, FALSE, 0),
(2, 'Lâm Đồng', 'Hồ Chí Minh', 0, 50, 55000, 12000, 16, TRUE, 35000),
(3, 'Lâm Đồng', 'Hồ Chí Minh', 0, 100, 28000, 4500, 48, FALSE, 0),
(4, 'Lâm Đồng', 'Hồ Chí Minh', 0, 100, 26000, 4000, 48, FALSE, 0),

-- Lâm Đồng -> Hà Nội
(1, 'Lâm Đồng', 'Hà Nội', 0, 50, 45000, 8000, 48, FALSE, 0),
(1, 'Lâm Đồng', 'Hà Nội', 0, 50, 80000, 15000, 24, TRUE, 50000), -- Cold chain express
(2, 'Lâm Đồng', 'Hà Nội', 0, 50, 48000, 9000, 48, FALSE, 0),
(2, 'Lâm Đồng', 'Hà Nội', 0, 50, 85000, 18000, 30, TRUE, 55000),
(3, 'Lâm Đồng', 'Hà Nội', 0, 100, 40000, 7000, 72, FALSE, 0),
(4, 'Lâm Đồng', 'Hà Nội', 0, 100, 38000, 6500, 72, FALSE, 0);
