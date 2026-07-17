-- Thêm fields đóng gói và sơ chế cho bảng product_warehouse_lots
ALTER TABLE product_warehouse_lots ADD COLUMN packaging_type VARCHAR(30) DEFAULT NULL;
ALTER TABLE product_warehouse_lots ADD COLUMN packaging_count INT DEFAULT NULL;
ALTER TABLE product_warehouse_lots ADD COLUMN processing_type VARCHAR(30) DEFAULT NULL;
