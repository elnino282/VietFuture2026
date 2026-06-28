-- Add traceability snapshots to marketplace_products table
ALTER TABLE `marketplace_products` 
    ADD COLUMN `lot_warehouse_name` VARCHAR(255) NULL AFTER `lot_code`,
    ADD COLUMN `lot_storage_location` VARCHAR(255) NULL AFTER `lot_warehouse_name`,
    ADD COLUMN `lot_harvest_date` DATETIME NULL AFTER `lot_storage_location`,
    ADD COLUMN `lot_received_at` DATETIME NULL AFTER `lot_harvest_date`,
    ADD COLUMN `lot_grade` VARCHAR(50) NULL AFTER `lot_received_at`,
    ADD COLUMN `lot_initial_quantity` DECIMAL(19,3) NULL AFTER `lot_grade`,
    ADD COLUMN `plot_id` INT NULL AFTER `lot_initial_quantity`,
    ADD COLUMN `plot_name` VARCHAR(255) NULL AFTER `plot_id`,
    ADD COLUMN `plot_area` DECIMAL(19,4) NULL AFTER `plot_name`,
    ADD COLUMN `crop_name` VARCHAR(255) NULL AFTER `plot_area`;

-- Add traceability snapshots to marketplace_order_items table
ALTER TABLE `marketplace_order_items`
    ADD COLUMN `lot_warehouse_name` VARCHAR(255) NULL AFTER `lot_code`,
    ADD COLUMN `lot_storage_location` VARCHAR(255) NULL AFTER `lot_warehouse_name`,
    ADD COLUMN `lot_harvest_date` DATETIME NULL AFTER `lot_storage_location`,
    ADD COLUMN `lot_received_at` DATETIME NULL AFTER `lot_harvest_date`,
    ADD COLUMN `lot_grade` VARCHAR(50) NULL AFTER `lot_received_at`,
    ADD COLUMN `lot_initial_quantity` DECIMAL(19,3) NULL AFTER `lot_grade`,
    ADD COLUMN `plot_id` INT NULL AFTER `lot_initial_quantity`,
    ADD COLUMN `plot_name` VARCHAR(255) NULL AFTER `plot_id`,
    ADD COLUMN `plot_area` DECIMAL(19,4) NULL AFTER `plot_name`;
