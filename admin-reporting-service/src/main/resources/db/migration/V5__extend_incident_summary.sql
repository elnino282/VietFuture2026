-- V5__extend_incident_summary.sql
-- Extend admin_incident_summary to store extra fields for statistics reporting

ALTER TABLE admin_incident_summary ADD COLUMN incident_type VARCHAR(100);
ALTER TABLE admin_incident_summary ADD COLUMN severity VARCHAR(50);
ALTER TABLE admin_incident_summary ADD COLUMN resolved_at TIMESTAMP NULL;
ALTER TABLE admin_incident_summary ADD COLUMN created_at TIMESTAMP NULL;

-- Incident summary backfill moved to programmatic backfill runner
