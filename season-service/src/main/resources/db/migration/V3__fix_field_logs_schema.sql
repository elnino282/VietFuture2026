-- Fix field_logs table schema to match JPA entity mapping
ALTER TABLE `field_logs` DROP COLUMN `content`;
ALTER TABLE `field_logs` ADD COLUMN `notes` TEXT NULL;
ALTER TABLE `field_logs` ADD COLUMN `created_by_user_id` BIGINT NULL;
