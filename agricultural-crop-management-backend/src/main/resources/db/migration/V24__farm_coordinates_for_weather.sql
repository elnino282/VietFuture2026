SET @farms_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'farms'
);

SET @sql := IF(
    @farms_exists > 0
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'farms'
          AND column_name = 'latitude'
    ),
    'ALTER TABLE farms ADD COLUMN latitude DECIMAL(10, 6) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @farms_exists > 0
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'farms'
          AND column_name = 'longitude'
    ),
    'ALTER TABLE farms ADD COLUMN longitude DECIMAL(10, 6) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
