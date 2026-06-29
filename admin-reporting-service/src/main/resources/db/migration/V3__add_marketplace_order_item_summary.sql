-- V3__add_marketplace_order_item_summary.sql
-- Create read model table for admin reporting service to track marketplace order items per season

CREATE TABLE IF NOT EXISTS admin_marketplace_order_item_summary (
    item_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    season_id INT,
    quantity DECIMAL(15, 4),
    unit_price DECIMAL(15, 4),
    line_total DECIMAL(15, 4),
    CONSTRAINT fk_admin_marketplace_order_item_summary_order FOREIGN KEY (order_id) REFERENCES admin_marketplace_order_summary(order_id) ON DELETE CASCADE
);

-- Seed existing order items moved to programmatic backfill runner
