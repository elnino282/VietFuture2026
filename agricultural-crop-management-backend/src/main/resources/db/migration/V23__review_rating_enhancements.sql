-- V23: Review & Rating enhancements.
-- Safely applies only when related marketplace tables are present.

SET @marketplace_product_reviews_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'marketplace_product_reviews'
);

SET @marketplace_order_items_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'marketplace_order_items'
);

SET @marketplace_products_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'marketplace_products'
);

SET @farms_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'farms'
);

-- 1) Enhance marketplace_product_reviews table.
SET @sql := IF(
    @marketplace_product_reviews_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND column_name = 'order_item_id'
    ),
    'ALTER TABLE marketplace_product_reviews ADD COLUMN order_item_id BIGINT',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @marketplace_product_reviews_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND column_name = 'updated_at'
    ),
    'ALTER TABLE marketplace_product_reviews ADD COLUMN updated_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @marketplace_product_reviews_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND column_name = 'hidden'
    ),
    'ALTER TABLE marketplace_product_reviews ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Foreign key to order items.
SET @sql := IF(
    @marketplace_product_reviews_exists > 0
    AND @marketplace_order_items_exists > 0
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND constraint_name = 'fk_review_order_item'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE marketplace_product_reviews ADD CONSTRAINT fk_review_order_item FOREIGN KEY (order_item_id) REFERENCES marketplace_order_items(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Prevent duplicate reviews: one review per order-item per buyer.
SET @sql := IF(
    @marketplace_product_reviews_exists > 0
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'marketplace_product_reviews'
          AND constraint_name = 'uq_review_order_item_buyer'
          AND constraint_type = 'UNIQUE'
    ),
    'ALTER TABLE marketplace_product_reviews ADD CONSTRAINT uq_review_order_item_buyer UNIQUE (order_item_id, buyer_user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill order_item_id for existing rows (best-effort match).
SET @sql := IF(
    @marketplace_product_reviews_exists > 0
    AND @marketplace_order_items_exists > 0,
    'UPDATE marketplace_product_reviews r
     SET order_item_id = (
         SELECT oi.id
         FROM marketplace_order_items oi
         WHERE oi.order_id = r.order_id
           AND oi.product_id = r.product_id
         LIMIT 1
     )
     WHERE r.order_item_id IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Denormalized rating columns on marketplace_products.
SET @sql := IF(
    @marketplace_products_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_products'
          AND column_name = 'average_rating'
    ),
    'ALTER TABLE marketplace_products ADD COLUMN average_rating DOUBLE PRECISION NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @marketplace_products_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_products'
          AND column_name = 'rating_count'
    ),
    'ALTER TABLE marketplace_products ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Denormalized rating columns on farms.
SET @sql := IF(
    @farms_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'farms'
          AND column_name = 'average_rating'
    ),
    'ALTER TABLE farms ADD COLUMN average_rating DOUBLE PRECISION NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @farms_exists > 0
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'farms'
          AND column_name = 'rating_count'
    ),
    'ALTER TABLE farms ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Backfill denormalized product ratings.
SET @sql := IF(
    @marketplace_products_exists > 0
    AND @marketplace_product_reviews_exists > 0,
    'UPDATE marketplace_products p
     JOIN (
         SELECT product_id, AVG(rating) AS avg_rating, COUNT(*) AS cnt
         FROM marketplace_product_reviews
         WHERE hidden = FALSE
         GROUP BY product_id
     ) sub ON p.id = sub.product_id
     SET p.average_rating = sub.avg_rating,
         p.rating_count = sub.cnt',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) Backfill denormalized farm ratings.
SET @sql := IF(
    @farms_exists > 0
    AND @marketplace_products_exists > 0
    AND @marketplace_product_reviews_exists > 0,
    'UPDATE farms f
     JOIN (
         SELECT mp.farm_id, AVG(r.rating) AS avg_rating, COUNT(*) AS cnt
         FROM marketplace_product_reviews r
         JOIN marketplace_products mp ON mp.id = r.product_id
         WHERE r.hidden = FALSE
           AND mp.farm_id IS NOT NULL
         GROUP BY mp.farm_id
     ) sub ON f.farm_id = sub.farm_id
     SET f.average_rating = sub.avg_rating,
         f.rating_count = sub.cnt',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
