-- Marketplace schema skeleton (Batch 0)
-- Purpose: freeze core data contracts for catalog/cart/orders while keeping existing modules untouched.

CREATE TABLE IF NOT EXISTS marketplace_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(191) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(120) NULL,
    short_description VARCHAR(500) NULL,
    description TEXT NULL,
    price DECIMAL(19,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    stock_quantity INT NOT NULL,
    image_url VARCHAR(1024) NULL,
    image_urls_json TEXT NULL,
    farmer_user_id BIGINT NOT NULL,
    farm_id INT NULL,
    season_id INT NULL,
    lot_id INT NULL,
    traceable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_products_slug UNIQUE (slug),
    CONSTRAINT fk_marketplace_products_farmer_user FOREIGN KEY (farmer_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_marketplace_products_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_marketplace_products_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_marketplace_products_lot FOREIGN KEY (lot_id) REFERENCES product_warehouse_lots(id)
);

CREATE INDEX idx_marketplace_products_status ON marketplace_products(status);
CREATE INDEX idx_marketplace_products_category ON marketplace_products(category);
CREATE INDEX idx_marketplace_products_traceable ON marketplace_products(traceable);
CREATE INDEX idx_marketplace_products_farmer_user ON marketplace_products(farmer_user_id);
CREATE INDEX idx_marketplace_products_farm ON marketplace_products(farm_id);

CREATE TABLE IF NOT EXISTS marketplace_carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_carts_user UNIQUE (user_id),
    CONSTRAINT fk_marketplace_carts_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS marketplace_cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_snapshot DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_marketplace_cart_items_cart FOREIGN KEY (cart_id) REFERENCES marketplace_carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_cart_items_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id)
);

CREATE INDEX idx_marketplace_cart_items_product ON marketplace_cart_items(product_id);

CREATE TABLE IF NOT EXISTS marketplace_order_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_order_groups_group_code UNIQUE (group_code),
    CONSTRAINT uk_marketplace_order_groups_idempotency UNIQUE (buyer_user_id, idempotency_key),
    CONSTRAINT fk_marketplace_order_groups_buyer_user FOREIGN KEY (buyer_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS marketplace_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_group_id BIGINT NOT NULL,
    order_code VARCHAR(64) NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    farmer_user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
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
);

CREATE INDEX idx_marketplace_orders_buyer_user ON marketplace_orders(buyer_user_id);
CREATE INDEX idx_marketplace_orders_farmer_user ON marketplace_orders(farmer_user_id);
CREATE INDEX idx_marketplace_orders_status ON marketplace_orders(status);

CREATE TABLE IF NOT EXISTS marketplace_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(255) NOT NULL,
    product_slug_snapshot VARCHAR(191) NOT NULL,
    image_url_snapshot VARCHAR(1024) NULL,
    unit_price_snapshot DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
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
);

CREATE INDEX idx_marketplace_order_items_order ON marketplace_order_items(order_id);
CREATE INDEX idx_marketplace_order_items_product ON marketplace_order_items(product_id);

CREATE TABLE IF NOT EXISTS marketplace_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    CONSTRAINT fk_marketplace_addresses_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_marketplace_addresses_user ON marketplace_addresses(user_id);

CREATE TABLE IF NOT EXISTS marketplace_product_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_marketplace_reviews_product_order_buyer UNIQUE (product_id, order_id, buyer_user_id),
    CONSTRAINT fk_marketplace_reviews_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id),
    CONSTRAINT fk_marketplace_reviews_order FOREIGN KEY (order_id) REFERENCES marketplace_orders(id),
    CONSTRAINT fk_marketplace_reviews_buyer_user FOREIGN KEY (buyer_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_marketplace_reviews_product ON marketplace_product_reviews(product_id);
CREATE INDEX idx_marketplace_reviews_order ON marketplace_product_reviews(order_id);
