-- V6__extend_inventory_lot_summary.sql
-- Extend admin_inventory_lot_summary to support InventoryOnHandReport

ALTER TABLE admin_inventory_lot_summary ADD COLUMN warehouse_id INT;
ALTER TABLE admin_inventory_lot_summary ADD COLUMN warehouse_name VARCHAR(255);
ALTER TABLE admin_inventory_lot_summary ADD COLUMN quantity_on_hand DECIMAL(15, 4);

-- Inventory lot backfill moved to programmatic backfill runner
