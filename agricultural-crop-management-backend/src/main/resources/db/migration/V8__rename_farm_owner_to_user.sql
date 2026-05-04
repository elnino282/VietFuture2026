-- =========================================================
-- V8: Rename owner_id to user_id in farms table
-- =========================================================
-- This migration standardizes the foreign key column name
-- from 'owner_id' to 'user_id' for consistency with other tables.
-- =========================================================

-- Rename the column
ALTER TABLE farms CHANGE COLUMN owner_id user_id BIGINT;

-- Update index (drop old, create new)
DROP INDEX IF EXISTS idx_farm_owner_name_active ON farms;
CREATE INDEX idx_farm_user_name_active ON farms(user_id, farm_name, active);
