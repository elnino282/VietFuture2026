ALTER TABLE harvests
ADD COLUMN warehouse_received_date DATE NULL,
ADD COLUMN warehouse_receipt_status VARCHAR(50) NULL,
ADD COLUMN gross_wet_weight DECIMAL(19,2) NULL,
ADD COLUMN net_dry_weight DECIMAL(19,2) NULL;
