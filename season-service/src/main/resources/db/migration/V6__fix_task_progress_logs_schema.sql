-- Fix task_progress_logs table schema to match JPA entity mapping
ALTER TABLE `task_progress_logs` DROP COLUMN `progress_percentage`;
ALTER TABLE `task_progress_logs` DROP COLUMN `notes`;

ALTER TABLE `task_progress_logs` ADD COLUMN `progress_percent` INT NOT NULL;
ALTER TABLE `task_progress_logs` ADD COLUMN `note` TEXT NULL;
ALTER TABLE `task_progress_logs` ADD COLUMN `evidence_url` VARCHAR(1000) NULL;
