ALTER TABLE crops
ADD COLUMN default_storage_category VARCHAR(50) NULL,
ADD COLUMN requires_cold_chain BOOLEAN DEFAULT FALSE,
ADD COLUMN shelf_life_days INT NULL;

