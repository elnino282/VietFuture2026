ALTER TABLE crops
ADD COLUMN category VARCHAR(50) NULL,
ADD COLUMN post_harvest_delay_days INT NULL,
ADD COLUMN shelf_life_days INT NULL;
