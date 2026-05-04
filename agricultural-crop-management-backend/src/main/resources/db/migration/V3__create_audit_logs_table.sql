-- Migration script for creating audit_logs table
-- This table stores audit trail for critical operations (SOFT_DELETE, HARD_DELETE, RESTORE, CREATE, UPDATE)

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL COMMENT 'Type of entity (e.g., FARM, PLOT, SEASON)',
    entity_id INT NOT NULL COMMENT 'ID of the affected entity',
    operation VARCHAR(50) NOT NULL COMMENT 'Operation type (CREATE, UPDATE, SOFT_DELETE, HARD_DELETE, RESTORE)',
    performed_by VARCHAR(255) NOT NULL COMMENT 'Username of the user who performed the operation',
    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp of the operation',
    snapshot_data TEXT COMMENT 'JSON snapshot of entity state BEFORE the operation',
    reason VARCHAR(500) COMMENT 'Optional user-provided reason for the operation',
    ip_address VARCHAR(45) COMMENT 'IP address of the request (supports IPv4 and IPv6)',
    
    -- Indexes for common query patterns
    INDEX idx_entity_lookup (entity_type, entity_id) COMMENT 'Fast lookup by entity type and ID',
    INDEX idx_performed_at (performed_at) COMMENT 'Fast lookup by timestamp for retention/archival',
    INDEX idx_performed_by (performed_by) COMMENT 'Fast lookup by user for user activity audit'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='Audit trail for critical operations - retention policy: 2 years';

-- Notes for Production Deployment:
-- 1. snapshot_data may contain PII (farm addresses, owner info) - ensure GDPR compliance
-- 2. Consider encryption at rest for snapshot_data column
-- 3. Implement automated archival for records older than 1-2 years
-- 4. Monitor table growth and set up partitioning if needed (e.g., by performed_at)
