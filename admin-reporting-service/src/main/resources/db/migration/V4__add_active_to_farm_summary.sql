-- V4__add_active_to_farm_summary.sql
ALTER TABLE admin_farm_summary ADD COLUMN active BOOLEAN DEFAULT TRUE;

-- Active status backfill moved to programmatic backfill runner
