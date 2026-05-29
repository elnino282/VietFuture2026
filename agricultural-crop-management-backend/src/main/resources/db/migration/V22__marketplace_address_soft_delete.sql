-- Add soft-delete support to marketplace_addresses when marketplace schema is present.

SET @marketplace_addresses_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'marketplace_addresses'
);

SET @sql := IF(
    @marketplace_addresses_exists > 0
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_addresses'
          AND column_name = 'deleted_at'
    ),
    'ALTER TABLE marketplace_addresses ADD COLUMN deleted_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @marketplace_addresses_exists > 0
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'marketplace_addresses'
          AND index_name = 'idx_marketplace_addresses_user_deleted'
    ),
    'CREATE INDEX idx_marketplace_addresses_user_deleted ON marketplace_addresses(user_id, deleted_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
