-- Add soft-delete support to marketplace_addresses
ALTER TABLE marketplace_addresses ADD COLUMN deleted_at TIMESTAMP NULL;

-- Composite index for efficient filtered queries (active addresses per user)
CREATE INDEX idx_marketplace_addresses_user_deleted ON marketplace_addresses(user_id, deleted_at);
