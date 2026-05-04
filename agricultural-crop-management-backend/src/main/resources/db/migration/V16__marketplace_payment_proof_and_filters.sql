ALTER TABLE marketplace_orders
    ADD COLUMN payment_verification_status VARCHAR(40) NOT NULL DEFAULT 'NOT_REQUIRED' AFTER payment_method,
    ADD COLUMN payment_proof_file_name VARCHAR(255) NULL AFTER payment_verification_status,
    ADD COLUMN payment_proof_content_type VARCHAR(150) NULL AFTER payment_proof_file_name,
    ADD COLUMN payment_proof_storage_path VARCHAR(1000) NULL AFTER payment_proof_content_type,
    ADD COLUMN payment_proof_uploaded_at TIMESTAMP NULL AFTER payment_proof_storage_path,
    ADD COLUMN payment_verified_at TIMESTAMP NULL AFTER payment_proof_uploaded_at,
    ADD COLUMN payment_verified_by_user_id BIGINT NULL AFTER payment_verified_at,
    ADD COLUMN payment_verification_note VARCHAR(500) NULL AFTER payment_verified_by_user_id;

UPDATE marketplace_orders
SET payment_verification_status = CASE
    WHEN payment_method = 'BANK_TRANSFER' THEN 'AWAITING_PROOF'
    ELSE 'NOT_REQUIRED'
END;

CREATE INDEX idx_marketplace_orders_payment_verification_status
    ON marketplace_orders(payment_verification_status);
