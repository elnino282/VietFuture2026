-- ============================================================================
-- V21: Expand marketplace order status lifecycle
-- Renames: PENDING → PENDING_PAYMENT, DELIVERING → SHIPPED
-- Adds new statuses: PAYMENT_SUBMITTED, PAYMENT_VERIFIED, DELIVERED, REJECTED
-- ============================================================================

-- Rename existing status values
UPDATE marketplace_orders SET status = 'PENDING_PAYMENT' WHERE status = 'PENDING';
UPDATE marketplace_orders SET status = 'SHIPPED'         WHERE status = 'DELIVERING';

-- Widen the status column to accommodate all new values
ALTER TABLE marketplace_orders MODIFY COLUMN status VARCHAR(30) NOT NULL;
