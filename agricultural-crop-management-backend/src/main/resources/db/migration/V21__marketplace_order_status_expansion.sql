-- ============================================================================
-- V21: Expand marketplace order status lifecycle
-- Renames: PENDING -> PENDING_PAYMENT, DELIVERING -> SHIPPED
-- Adds new statuses: PAYMENT_SUBMITTED, PAYMENT_VERIFIED, DELIVERED, REJECTED
-- ============================================================================

SET @marketplace_orders_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'marketplace_orders'
);

-- Rename existing status values when the marketplace schema is present.
SET @sql := IF(
    @marketplace_orders_exists > 0,
    'UPDATE marketplace_orders SET status = ''PENDING_PAYMENT'' WHERE status = ''PENDING''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @marketplace_orders_exists > 0,
    'UPDATE marketplace_orders SET status = ''SHIPPED'' WHERE status = ''DELIVERING''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Widen the status column to accommodate all new values.
SET @sql := IF(
    @marketplace_orders_exists > 0,
    'ALTER TABLE marketplace_orders MODIFY COLUMN status VARCHAR(30) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
