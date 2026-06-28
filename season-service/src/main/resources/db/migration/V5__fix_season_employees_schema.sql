-- Fix season_employees table schema to match JPA entity mapping
ALTER TABLE `season_employees` DROP COLUMN `assigned_at`;
ALTER TABLE `season_employees` DROP COLUMN `role`;
ALTER TABLE `season_employees` DROP COLUMN `target_wage`;
ALTER TABLE `season_employees` DROP COLUMN `notes`;

ALTER TABLE `season_employees` ADD COLUMN `created_at` DATETIME NULL;
ALTER TABLE `season_employees` ADD COLUMN `wage_per_task` DECIMAL(15,2) NULL;
ALTER TABLE `season_employees` ADD COLUMN `added_by_user_id` BIGINT NULL;
