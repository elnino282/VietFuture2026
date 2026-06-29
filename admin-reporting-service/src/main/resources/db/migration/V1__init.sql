-- V1__init.sql
-- Create read model schemas for Admin Reporting Service

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_user_summary (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    status VARCHAR(50),
    role_code VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS admin_farm_summary (
    farm_id INT PRIMARY KEY,
    farm_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_plot_summary (
    plot_id INT PRIMARY KEY,
    plot_name VARCHAR(255) NOT NULL,
    farm_id INT NOT NULL,
    area DECIMAL(15, 4) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_season_summary (
    season_id INT PRIMARY KEY,
    season_name VARCHAR(255) NOT NULL,
    plot_id INT NOT NULL,
    crop_id INT,
    crop_name VARCHAR(255),
    variety_id INT,
    variety_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    start_date DATE,
    expected_yield_kg DECIMAL(15, 4),
    actual_yield_kg DECIMAL(15, 4)
);

CREATE TABLE IF NOT EXISTS admin_incident_summary (
    incident_id INT PRIMARY KEY,
    season_id INT NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_task_summary (
    task_id INT PRIMARY KEY,
    season_id INT NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_alert_summary (
    alert_id INT PRIMARY KEY,
    season_id INT,
    type VARCHAR(100),
    severity VARCHAR(50),
    status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS admin_inventory_lot_summary (
    lot_id INT PRIMARY KEY,
    farm_id INT NOT NULL,
    farm_name VARCHAR(255) NOT NULL,
    expiry_date DATE
);

CREATE TABLE IF NOT EXISTS admin_harvest_summary (
    harvest_id INT PRIMARY KEY,
    season_id INT NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    unit_price DECIMAL(15, 4) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_expense_summary (
    expense_id INT PRIMARY KEY,
    season_id INT NOT NULL,
    total_cost DECIMAL(15, 4) NOT NULL,
    category VARCHAR(100),
    item_name VARCHAR(255),
    expense_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_marketplace_order_summary (
    order_id BIGINT PRIMARY KEY,
    order_code VARCHAR(100),
    buyer_id BIGINT,
    buyer_name VARCHAR(255),
    total_amount DECIMAL(15, 4),
    payment_status VARCHAR(50),
    status VARCHAR(50),
    payment_proof_uploaded_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_marketplace_product_summary (
    product_id BIGINT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    farm_id INT,
    farm_name VARCHAR(255),
    farmer_id BIGINT,
    farmer_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS admin_audit_log_entries (
    audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    snapshot_data TEXT,
    reason VARCHAR(500),
    ip_address VARCHAR(45)
);

CREATE TABLE IF NOT EXISTS admin_documents (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    description TEXT,
    crop VARCHAR(50),
    stage VARCHAR(50),
    topic VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    document_type VARCHAR(50),
    view_count INT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
);
