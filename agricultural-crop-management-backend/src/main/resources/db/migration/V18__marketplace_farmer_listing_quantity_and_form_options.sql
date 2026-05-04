ALTER TABLE marketplace_products
    ADD COLUMN stock_quantity DECIMAL(19,3) NULL AFTER unit;

UPDATE marketplace_products p
JOIN product_warehouse_lots l ON l.id = p.lot_id
SET p.stock_quantity = GREATEST(COALESCE(l.on_hand_quantity, 0), 0);

ALTER TABLE marketplace_products
    MODIFY COLUMN stock_quantity DECIMAL(19,3) NOT NULL;

CREATE INDEX idx_marketplace_products_stock_quantity ON marketplace_products(stock_quantity);
