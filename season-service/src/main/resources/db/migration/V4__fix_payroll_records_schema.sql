-- Fix payroll_records table schema to match JPA entity mapping
ALTER TABLE `payroll_records` DROP COLUMN `working_days`;
ALTER TABLE `payroll_records` DROP COLUMN `daily_wage`;
ALTER TABLE `payroll_records` DROP COLUMN `bonus_amount`;
ALTER TABLE `payroll_records` DROP COLUMN `deductions`;
ALTER TABLE `payroll_records` DROP COLUMN `status`;
ALTER TABLE `payroll_records` DROP COLUMN `approved_at`;
ALTER TABLE `payroll_records` DROP COLUMN `notes`;
ALTER TABLE `payroll_records` DROP COLUMN `expense_id`;

ALTER TABLE `payroll_records` ADD COLUMN `total_assigned_tasks` INT NOT NULL;
ALTER TABLE `payroll_records` ADD COLUMN `total_completed_tasks` INT NOT NULL;
ALTER TABLE `payroll_records` ADD COLUMN `wage_per_task` DECIMAL(15,2) NOT NULL;
ALTER TABLE `payroll_records` ADD COLUMN `note` TEXT NULL;
