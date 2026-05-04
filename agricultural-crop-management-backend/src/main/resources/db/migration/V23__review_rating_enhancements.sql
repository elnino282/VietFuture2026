-- V23: Review & Rating enhancements
-- Adds per-order-item review tracking, moderation flag, and denormalized rating columns.

-- 1. Enhance marketplace_product_reviews table
ALTER TABLE marketplace_product_reviews ADD COLUMN order_item_id BIGINT;
ALTER TABLE marketplace_product_reviews ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE marketplace_product_reviews ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Foreign key to order items
ALTER TABLE marketplace_product_reviews
    ADD CONSTRAINT fk_review_order_item
    FOREIGN KEY (order_item_id) REFERENCES marketplace_order_items(id);

-- Prevent duplicate reviews: one review per order-item per buyer
ALTER TABLE marketplace_product_reviews
    ADD CONSTRAINT uq_review_order_item_buyer
    UNIQUE (order_item_id, buyer_user_id);

-- Backfill order_item_id for any existing reviews (best-effort match)
UPDATE marketplace_product_reviews r
SET order_item_id = (
    SELECT oi.id FROM marketplace_order_items oi
    WHERE oi.order_id = r.order_id AND oi.product_id = r.product_id
    LIMIT 1
)
WHERE r.order_item_id IS NULL;

-- 2. Denormalized rating columns on marketplace_products
ALTER TABLE marketplace_products ADD COLUMN average_rating DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE marketplace_products ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0;

-- 3. Denormalized rating columns on farms
ALTER TABLE farms ADD COLUMN average_rating DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE farms ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0;

-- 4. Backfill denormalized product ratings from existing reviews
UPDATE marketplace_products p
SET average_rating = sub.avg_rating, rating_count = sub.cnt
FROM (
    SELECT product_id, AVG(rating) AS avg_rating, COUNT(*) AS cnt
    FROM marketplace_product_reviews
    WHERE hidden = FALSE
    GROUP BY product_id
) sub
WHERE p.id = sub.product_id;

-- 5. Backfill denormalized farm ratings
UPDATE farms f
SET average_rating = sub.avg_rating, rating_count = sub.cnt
FROM (
    SELECT mp.farm_id, AVG(r.rating) AS avg_rating, COUNT(*) AS cnt
    FROM marketplace_product_reviews r
    JOIN marketplace_products mp ON mp.id = r.product_id
    WHERE r.hidden = FALSE AND mp.farm_id IS NOT NULL
    GROUP BY mp.farm_id
) sub
WHERE f.farm_id = sub.farm_id;
