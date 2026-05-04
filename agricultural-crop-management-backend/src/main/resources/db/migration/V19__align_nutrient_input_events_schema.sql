-- Align nutrient_input_events with current JPA mapping.
-- Backward-compatible for databases created before source metadata columns were introduced.
-- NOTE: MySQL 8 does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS.

SET @schema_name := DATABASE();

SET @stmt := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'nutrient_input_events'
          AND column_name = 'source_type'
    ),
    'SELECT 1',
    'ALTER TABLE nutrient_input_events ADD COLUMN source_type VARCHAR(40) NULL'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'nutrient_input_events'
          AND column_name = 'source_document'
    ),
    'SELECT 1',
    'ALTER TABLE nutrient_input_events ADD COLUMN source_document VARCHAR(255) NULL'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'nutrient_input_events'
          AND column_name = 'created_by_user_id'
    ),
    'SELECT 1',
    'ALTER TABLE nutrient_input_events ADD COLUMN created_by_user_id BIGINT NULL'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'nutrient_input_events'
          AND index_name = 'idx_nutrient_input_events_created_by_user'
    ),
    'SELECT 1',
    'CREATE INDEX idx_nutrient_input_events_created_by_user ON nutrient_input_events(created_by_user_id)'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;
