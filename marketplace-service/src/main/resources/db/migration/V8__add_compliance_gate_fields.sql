-- Thêm các field phục vụ Marketplace Compliance Gate (Luồng G - BRD) cho bảng marketplace_products

ALTER TABLE marketplace_products 
ADD COLUMN compliance_claim VARCHAR(20) NULL;

ALTER TABLE marketplace_products 
ADD COLUMN certification_snapshot_json TEXT NULL;

ALTER TABLE marketplace_products 
ADD COLUMN harvest_safety_snapshot_json TEXT NULL;

ALTER TABLE marketplace_products 
ADD COLUMN compliance_checked_at DATETIME NULL;

