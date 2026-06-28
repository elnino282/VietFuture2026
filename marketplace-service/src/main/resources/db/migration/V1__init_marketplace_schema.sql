-- Marketplace service schema - migrated from monolith
-- Contains all marketplace tables with denormalized ID + snapshot fields
-- (no JPA joins to external services: User, Farm, Season, ProductWarehouseLot)

CREATE TABLE IF NOT EXISTS marketplace_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version BIGINT NULL,
    slug VARCHAR(191) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(120) NULL,
    short_description VARCHAR(500) NULL,
    description TEXT NULL,
    price DECIMAL(19,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    stock_quantity DECIMAL(19,3) NOT NULL,
    image_url VARCHAR(1024) NULL,
    image_urls_json TEXT NULL,
    farmer_user_id BIGINT NOT NULL,
    farmer_display_name VARCHAR(255) NULL,
    farm_id INT NULL,
    farm_name VARCHAR(255) NULL,
    farm_region VARCHAR(255) NULL,
    season_id INT NULL,
    season_name VARCHAR(255) NULL,
    lot_id INT NULL,
    lot_code VARCHAR(120) NULL,
    catalog_snapshot TEXT NULL,
    traceable BOOLEAN NOT NULL DEFAULT TRUE,
    average_rating DOUBLE NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP NULL,
    status_reason VARCHAR(500) NULL,
    status_changed_at TIMESTAMP NULL,
    status_changed_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_marketplace_products_farm_id (farm_id),
    INDEX idx_marketplace_products_season_id (season_id),
    INDEX idx_marketplace_products_lot_id (lot_id),
    INDEX idx_marketplace_products_farmer_user_id (farmer_user_id),
    INDEX idx_marketplace_products_status (status),
    INDEX idx_marketplace_products_status_changed_at (status_changed_at)
);

CREATE TABLE IF NOT EXISTS marketplace_carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_marketplace_carts_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS marketplace_cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    farmer_user_id BIGINT NOT NULL,
    lot_id INT NULL,
    lot_code VARCHAR(120) NULL,
    product_name_snapshot VARCHAR(255) NULL,
    product_slug_snapshot VARCHAR(191) NULL,
    image_url_snapshot VARCHAR(1024) NULL,
    quantity DECIMAL(19,3) NOT NULL,
    unit_price_snapshot DECIMAL(19,2) NOT NULL,
    traceable_snapshot BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_cart_items_cart FOREIGN KEY (cart_id) REFERENCES marketplace_carts(id) ON DELETE CASCADE,
    UNIQUE KEY uk_marketplace_cart_items_cart_product (cart_id, product_id),
    INDEX idx_marketplace_cart_items_product_id (product_id)
);

CREATE TABLE IF NOT EXISTS marketplace_order_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_code VARCHAR(64) NOT NULL UNIQUE,
    buyer_user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    request_fingerprint VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_marketplace_order_groups_idempotency (buyer_user_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS marketplace_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_group_id BIGINT NOT NULL,
    order_code VARCHAR(64) NOT NULL UNIQUE,
    buyer_user_id BIGINT NOT NULL,
    farmer_user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
    payment_verification_status VARCHAR(40) NOT NULL DEFAULT 'NOT_REQUIRED',
    payment_proof_file_name VARCHAR(255) NULL,
    payment_proof_content_type VARCHAR(150) NULL,
    payment_proof_storage_path VARCHAR(1000) NULL,
    payment_proof_uploaded_at TIMESTAMP NULL,
    payment_verified_at TIMESTAMP NULL,
    payment_verified_by_user_id BIGINT NULL,
    payment_verification_note VARCHAR(500) NULL,
    shipping_recipient_name VARCHAR(255) NOT NULL,
    shipping_phone VARCHAR(30) NOT NULL,
    shipping_address_line VARCHAR(500) NOT NULL,
    note TEXT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    shipping_fee DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_marketplace_orders_order_group_id (order_group_id),
    INDEX idx_marketplace_orders_buyer_user_id (buyer_user_id),
    INDEX idx_marketplace_orders_farmer_user_id (farmer_user_id),
    INDEX idx_marketplace_orders_status (status),
    INDEX idx_marketplace_orders_payment_verification_status (payment_verification_status)
);

CREATE TABLE IF NOT EXISTS marketplace_order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    farmer_user_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(255) NOT NULL,
    product_slug_snapshot VARCHAR(191) NOT NULL,
    image_url_snapshot VARCHAR(1024) NULL,
    unit_price_snapshot DECIMAL(19,2) NOT NULL,
    quantity DECIMAL(19,3) NOT NULL,
    line_total DECIMAL(19,2) NOT NULL,
    traceable_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    farm_id INT NULL,
    season_id INT NULL,
    lot_id INT NULL,
    farm_name VARCHAR(255) NULL,
    season_name VARCHAR(255) NULL,
    lot_code VARCHAR(120) NULL,
    crop_name VARCHAR(255) NULL,
    published_at_snapshot TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_order_items_order FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    INDEX idx_marketplace_order_items_order_id (order_id),
    INDEX idx_marketplace_order_items_product_id (product_id),
    INDEX idx_marketplace_order_items_farm_id (farm_id),
    INDEX idx_marketplace_order_items_season_id (season_id),
    INDEX idx_marketplace_order_items_lot_id (lot_id)
);

CREATE TABLE IF NOT EXISTS marketplace_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    province VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    ward VARCHAR(120) NOT NULL,
    street VARCHAR(255) NOT NULL,
    detail VARCHAR(500) NULL,
    label VARCHAR(30) NOT NULL DEFAULT 'home',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_marketplace_addresses_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS marketplace_product_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NULL,
    buyer_user_id BIGINT NOT NULL,
    buyer_display_name VARCHAR(255) NULL,
    rating INT NOT NULL,
    comment TEXT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    hidden_reason VARCHAR(500) NULL,
    hidden_at TIMESTAMP NULL,
    hidden_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    UNIQUE KEY uq_review_order_item_buyer (order_item_id, buyer_user_id),
    INDEX idx_marketplace_reviews_product_id (product_id),
    INDEX idx_marketplace_reviews_order_id (order_id),
    INDEX idx_marketplace_reviews_buyer_user_id (buyer_user_id)
);

CREATE TABLE IF NOT EXISTS marketplace_product_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(1024) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_product_images_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id) ON DELETE CASCADE,
    INDEX idx_marketplace_product_images_product_id (product_id)
);

CREATE TABLE IF NOT EXISTS marketplace_order_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(64) NOT NULL,
    performed_by BIGINT NULL,
    performed_at DATETIME NOT NULL,
    snapshot_data_json TEXT NULL,
    reason VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    INDEX idx_marketplace_order_audit_logs_entity (entity_type, entity_id)
);
