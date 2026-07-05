ALTER TABLE warehouses
    ADD COLUMN storage_category VARCHAR(255) NULL,
    ADD COLUMN temperature_min DECIMAL(19, 2) NULL,
    ADD COLUMN temperature_max DECIMAL(19, 2) NULL,
    ADD COLUMN humidity_min DECIMAL(19, 2) NULL,
    ADD COLUMN humidity_max DECIMAL(19, 2) NULL;
