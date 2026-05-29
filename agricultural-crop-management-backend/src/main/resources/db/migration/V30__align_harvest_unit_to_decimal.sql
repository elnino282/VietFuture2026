-- Align harvests.unit with application model (unit price as numeric value).
-- This migration is idempotent:
-- 1) If `unit` is currently textual, normalize invalid/non-numeric values to '1'.
-- 2) Ensure `unit` is DECIMAL(19,2) NOT NULL.

SET @harvest_unit_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'harvests'
      AND COLUMN_NAME = 'unit'
);

SET @harvest_unit_data_type := (
    SELECT DATA_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'harvests'
      AND COLUMN_NAME = 'unit'
    LIMIT 1
);

SET @harvest_unit_numeric_precision := (
    SELECT NUMERIC_PRECISION
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'harvests'
      AND COLUMN_NAME = 'unit'
    LIMIT 1
);

SET @harvest_unit_numeric_scale := (
    SELECT NUMERIC_SCALE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'harvests'
      AND COLUMN_NAME = 'unit'
    LIMIT 1
);

SET @harvest_unit_is_text := CASE
    WHEN @harvest_unit_data_type IN ('char', 'varchar', 'tinytext', 'text', 'mediumtext', 'longtext') THEN 1
    ELSE 0
END;

SET @normalize_harvest_unit_sql := CASE
    WHEN @harvest_unit_column_exists = 1 AND @harvest_unit_is_text = 1 THEN
        'UPDATE harvests
         SET unit = ''1''
         WHERE unit IS NULL
            OR TRIM(unit) = ''''
            OR TRIM(unit) NOT REGEXP ''^[0-9]+([.][0-9]+)?$'''
    ELSE 'SELECT 1'
END;

PREPARE normalize_harvest_unit_stmt FROM @normalize_harvest_unit_sql;
EXECUTE normalize_harvest_unit_stmt;
DEALLOCATE PREPARE normalize_harvest_unit_stmt;

SET @needs_harvest_unit_alter := CASE
    WHEN @harvest_unit_column_exists = 1
     AND NOT (
        @harvest_unit_data_type = 'decimal'
        AND @harvest_unit_numeric_precision = 19
        AND @harvest_unit_numeric_scale = 2
    ) THEN 1
    ELSE 0
END;

SET @alter_harvest_unit_sql := CASE
    WHEN @needs_harvest_unit_alter = 1 THEN
        'ALTER TABLE harvests MODIFY COLUMN unit DECIMAL(19,2) NOT NULL'
    ELSE 'SELECT 1'
END;

PREPARE alter_harvest_unit_stmt FROM @alter_harvest_unit_sql;
EXECUTE alter_harvest_unit_stmt;
DEALLOCATE PREPARE alter_harvest_unit_stmt;
