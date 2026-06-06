-- Add actor metadata for field logs so employee-created logs can be distinguished.

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'field_logs'
          AND column_name = 'created_by_user_id'
    ),
    'SELECT 1',
    'ALTER TABLE field_logs ADD COLUMN created_by_user_id BIGINT NULL AFTER notes'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE field_logs fl
JOIN seasons s ON s.season_id = fl.season_id
JOIN plots p ON p.plot_id = s.plot_id
LEFT JOIN farms f ON f.farm_id = p.farm_id
SET fl.created_by_user_id = COALESCE(f.user_id, p.created_by)
WHERE fl.created_by_user_id IS NULL;

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'field_logs'
          AND index_name = 'idx_field_logs_created_by_user'
    ),
    'SELECT 1',
    'CREATE INDEX idx_field_logs_created_by_user ON field_logs(created_by_user_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = DATABASE()
          AND table_name = 'field_logs'
          AND constraint_name = 'fk_field_logs_created_by_user'
    ),
    'SELECT 1',
    'ALTER TABLE field_logs ADD CONSTRAINT fk_field_logs_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
