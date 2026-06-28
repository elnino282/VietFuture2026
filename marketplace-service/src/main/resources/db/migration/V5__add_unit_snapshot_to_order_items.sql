-- Add unit_snapshot column to marketplace_order_items and marketplace_cart_items tables
ALTER TABLE marketplace_order_items 
ADD COLUMN unit_snapshot VARCHAR(50) NOT NULL DEFAULT 'kg' AFTER unit_price_snapshot;

ALTER TABLE marketplace_cart_items 
ADD COLUMN unit_snapshot VARCHAR(50) NOT NULL DEFAULT 'kg' AFTER unit_price_snapshot;
