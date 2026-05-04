ALTER TABLE marketplace_cart_items
    MODIFY COLUMN quantity DECIMAL(19,3) NOT NULL;

ALTER TABLE marketplace_order_items
    MODIFY COLUMN quantity DECIMAL(19,3) NOT NULL;

UPDATE marketplace_products
SET status = 'HIDDEN'
WHERE lot_id IS NULL;

ALTER TABLE marketplace_products
    MODIFY COLUMN lot_id INT NOT NULL;

ALTER TABLE marketplace_products
    ADD CONSTRAINT uk_marketplace_products_lot UNIQUE (lot_id);

ALTER TABLE marketplace_products
    DROP COLUMN stock_quantity;
