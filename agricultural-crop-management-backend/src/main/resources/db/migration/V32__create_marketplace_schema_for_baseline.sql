-- Ensure marketplace schema exists for baseline-first databases (B19 path).
-- B19 intentionally skipped legacy V15..V18 marketplace migrations, so we recreate
-- the final marketplace schema state expected by current JPA entities.

CREATE TABLE IF NOT EXISTS marketplace_products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NULL,
    slug VARCHAR(191) NOT NULL,
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
    farm_id INT NULL,
    season_id INT NULL,
    lot_id INT NOT NULL,
    traceable BOOLEAN NOT NULL DEFAULT TRUE,
    average_rating DOUBLE NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_products_slug UNIQUE (slug),
    CONSTRAINT uk_marketplace_products_lot UNIQUE (lot_id),
    CONSTRAINT fk_marketplace_products_farmer_user FOREIGN KEY (farmer_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_marketplace_products_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_marketplace_products_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_marketplace_products_lot FOREIGN KEY (lot_id) REFERENCES product_warehouse_lots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_carts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_carts_user UNIQUE (user_id),
    CONSTRAINT fk_marketplace_carts_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(19,3) NOT NULL,
    unit_price_snapshot DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_marketplace_cart_items_cart FOREIGN KEY (cart_id) REFERENCES marketplace_carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_cart_items_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_order_groups (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_order_groups_group_code UNIQUE (group_code),
    CONSTRAINT uk_marketplace_order_groups_idempotency UNIQUE (buyer_user_id, idempotency_key),
    CONSTRAINT fk_marketplace_order_groups_buyer_user FOREIGN KEY (buyer_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_group_id BIGINT NOT NULL,
    order_code VARCHAR(64) NOT NULL,
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
    CONSTRAINT uk_marketplace_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_marketplace_orders_order_group FOREIGN KEY (order_group_id) REFERENCES marketplace_order_groups(id),
    CONSTRAINT fk_marketplace_orders_buyer_user FOREIGN KEY (buyer_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_marketplace_orders_farmer_user FOREIGN KEY (farmer_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
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
    CONSTRAINT fk_marketplace_order_items_order FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_order_items_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id),
    CONSTRAINT fk_marketplace_order_items_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_marketplace_order_items_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_marketplace_order_items_lot FOREIGN KEY (lot_id) REFERENCES product_warehouse_lots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
    CONSTRAINT fk_marketplace_addresses_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_product_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NULL,
    buyer_user_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment TEXT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uq_review_order_item_buyer UNIQUE (order_item_id, buyer_user_id),
    CONSTRAINT fk_marketplace_reviews_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id),
    CONSTRAINT fk_marketplace_reviews_order FOREIGN KEY (order_id) REFERENCES marketplace_orders(id),
    CONSTRAINT fk_review_order_item FOREIGN KEY (order_item_id) REFERENCES marketplace_order_items(id),
    CONSTRAINT fk_marketplace_reviews_buyer_user FOREIGN KEY (buyer_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catch-up for databases where marketplace tables already existed before this migration.
SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_products'
          AND column_name = 'version'
    ),
    'SELECT 1',
    'ALTER TABLE marketplace_products ADD COLUMN version BIGINT NULL AFTER id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_addresses'
          AND column_name = 'deleted_at'
    ),
    'SELECT 1',
    'ALTER TABLE marketplace_addresses ADD COLUMN deleted_at TIMESTAMP NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND column_name = 'order_item_id'
    ),
    'SELECT 1',
    'ALTER TABLE marketplace_product_reviews ADD COLUMN order_item_id BIGINT NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND column_name = 'updated_at'
    ),
    'SELECT 1',
    'ALTER TABLE marketplace_product_reviews ADD COLUMN updated_at TIMESTAMP NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND column_name = 'hidden'
    ),
    'SELECT 1',
    'ALTER TABLE marketplace_product_reviews ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE marketplace_cart_items
    MODIFY COLUMN quantity DECIMAL(19,3) NOT NULL;

ALTER TABLE marketplace_order_items
    MODIFY COLUMN quantity DECIMAL(19,3) NOT NULL;
