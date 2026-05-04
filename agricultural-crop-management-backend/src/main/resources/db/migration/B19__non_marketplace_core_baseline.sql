-- Baseline migration for fresh MySQL 8 installations.
-- Purpose:
-- 1) Provide a reproducible non-marketplace schema snapshot equivalent to post-V19 state.
-- 2) Avoid executing legacy V3..V19 scripts that assume pre-existing bootstrap objects.
-- Notes:
-- - Marketplace schema/data is intentionally excluded.
-- - This script is for clean installs; existing environments with Flyway history are unaffected.

CREATE TABLE provinces (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    name_with_type VARCHAR(256) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wards (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    name_with_type VARCHAR(512) NOT NULL,
    province_id INT NOT NULL,
    CONSTRAINT fk_wards_province FOREIGN KEY (province_id) REFERENCES provinces(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    role_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(100) NOT NULL,
    role_name VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    CONSTRAINT uk_roles_code UNIQUE (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NULL,
    full_name VARCHAR(255) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    province_id INT NULL,
    ward_id INT NULL,
    joined_date DATETIME NULL,
    CONSTRAINT uk_users_username UNIQUE (user_name),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_province FOREIGN KEY (province_id) REFERENCES provinces(id),
    CONSTRAINT fk_users_ward FOREIGN KEY (ward_id) REFERENCES wards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crops (
    crop_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    crop_name VARCHAR(255) NOT NULL,
    description TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE varieties (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    crop_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    CONSTRAINT fk_varieties_crop FOREIGN KEY (crop_id) REFERENCES crops(crop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE farms (
    farm_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    farm_name VARCHAR(255) NOT NULL,
    province_id INT NOT NULL,
    ward_id INT NOT NULL,
    area DECIMAL(19,2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_farms_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_farms_province FOREIGN KEY (province_id) REFERENCES provinces(id),
    CONSTRAINT fk_farms_ward FOREIGN KEY (ward_id) REFERENCES wards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plots (
    plot_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    farm_id INT NOT NULL,
    plot_name VARCHAR(255) NOT NULL,
    area DECIMAL(19,2) NULL,
    soil_type VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_USE',
    boundary_geojson LONGTEXT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_plots_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_plots_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seasons (
    season_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_name VARCHAR(255) NOT NULL,
    plot_id INT NOT NULL,
    crop_id INT NOT NULL,
    variety_id INT NULL,
    start_date DATE NOT NULL,
    planned_harvest_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    initial_plant_count INT NOT NULL,
    current_plant_count INT NULL,
    expected_yield_kg DECIMAL(19,2) NULL,
    actual_yield_kg DECIMAL(19,2) NULL,
    budget_amount DECIMAL(19,2) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seasons_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_seasons_crop FOREIGN KEY (crop_id) REFERENCES crops(crop_id),
    CONSTRAINT fk_seasons_variety FOREIGN KEY (variety_id) REFERENCES varieties(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tasks (
    task_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    season_id INT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    planned_date DATE NULL,
    due_date DATE NULL,
    status VARCHAR(30) NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_tasks_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expenses (
    expense_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    season_id INT NOT NULL,
    task_id INT NULL,
    category VARCHAR(50) NULL,
    item_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    total_cost DECIMAL(19,2) NULL,
    amount DECIMAL(19,2) NULL,
    payment_status VARCHAR(30) NULL,
    note TEXT NULL,
    expense_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_expenses_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_expenses_task FOREIGN KEY (task_id) REFERENCES tasks(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE harvests (
    harvest_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    harvest_date DATE NOT NULL,
    quantity DECIMAL(19,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    grade VARCHAR(20) NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_harvests_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE suppliers (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    license_no VARCHAR(100) NULL,
    contact_email VARCHAR(255) NULL,
    contact_phone VARCHAR(30) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supply_items (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    active_ingredient VARCHAR(255) NULL,
    unit VARCHAR(30) NOT NULL,
    restricted_flag BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supply_lots (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supply_item_id INT NOT NULL,
    supplier_id INT NULL,
    batch_code VARCHAR(100) NOT NULL,
    expiry_date DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK',
    CONSTRAINT fk_supply_lots_item FOREIGN KEY (supply_item_id) REFERENCES supply_items(id),
    CONSTRAINT fk_supply_lots_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT uk_supply_lots_batch UNIQUE (batch_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouses (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    farm_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    province_id INT NULL,
    ward_id INT NULL,
    CONSTRAINT fk_warehouses_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_warehouses_province FOREIGN KEY (province_id) REFERENCES provinces(id),
    CONSTRAINT fk_warehouses_ward FOREIGN KEY (ward_id) REFERENCES wards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_locations (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    warehouse_id INT NOT NULL,
    zone VARCHAR(50) NULL,
    aisle VARCHAR(50) NULL,
    shelf VARCHAR(50) NULL,
    bin VARCHAR(50) NULL,
    CONSTRAINT fk_stock_locations_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_movements (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supply_lot_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    location_id INT NULL,
    movement_type VARCHAR(40) NOT NULL,
    quantity DECIMAL(19,3) NOT NULL,
    movement_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    season_id INT NULL,
    task_id INT NULL,
    note TEXT NULL,
    CONSTRAINT fk_stock_movements_lot FOREIGN KEY (supply_lot_id) REFERENCES supply_lots(id),
    CONSTRAINT fk_stock_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_stock_movements_location FOREIGN KEY (location_id) REFERENCES stock_locations(id),
    CONSTRAINT fk_stock_movements_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_stock_movements_task FOREIGN KEY (task_id) REFERENCES tasks(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_balances (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supply_lot_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    location_id INT NULL,
    quantity DECIMAL(19,3) NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_balances_lot FOREIGN KEY (supply_lot_id) REFERENCES supply_lots(id),
    CONSTRAINT fk_inventory_balances_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_inventory_balances_location FOREIGN KEY (location_id) REFERENCES stock_locations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE field_logs (
    field_log_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    log_date DATE NOT NULL,
    log_type VARCHAR(50) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_field_logs_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE incidents (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    reported_by BIGINT NOT NULL,
    incident_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    deadline DATE NULL,
    resolved_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_incidents_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alerts (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    farm_id INT NULL,
    season_id INT NULL,
    plot_id INT NULL,
    crop_id INT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    suggested_action_type VARCHAR(100) NULL,
    suggested_action_url VARCHAR(1000) NULL,
    recipient_farmer_ids TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    CONSTRAINT fk_alerts_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_alerts_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_alerts_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_alerts_crop FOREIGN KEY (crop_id) REFERENCES crops(crop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(1000) NULL,
    alert_id INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_notifications_alert FOREIGN KEY (alert_id) REFERENCES alerts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE documents (
    document_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    description TEXT NULL,
    crop VARCHAR(50) NULL,
    stage VARCHAR(50) NULL,
    topic VARCHAR(50) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NULL,
    document_type VARCHAR(50) NULL,
    view_count INT NOT NULL DEFAULT 0,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_favorites (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_document_favorites_user_document UNIQUE (user_id, document_id),
    CONSTRAINT fk_document_favorites_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_document_favorites_document FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_recent_opens (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_id INT NOT NULL,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_recent_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_document_recent_document FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_preferences (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'VND',
    weight_unit VARCHAR(10) NOT NULL DEFAULT 'kg',
    locale VARCHAR(20) NOT NULL DEFAULT 'vi-VN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    audit_log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_data TEXT NULL,
    reason VARCHAR(500) NULL,
    ip_address VARCHAR(45) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crop_nitrogen_references (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    crop_id INT NOT NULL,
    n_content_kg_per_kg_yield DECIMAL(12,6) NOT NULL,
    source_reference VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_crop_n_ref_crop FOREIGN KEY (crop_id) REFERENCES crops(crop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nutrient_input_events (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    plot_id INT NOT NULL,
    input_source VARCHAR(40) NOT NULL,
    n_kg DECIMAL(19,4) NOT NULL,
    applied_date DATE NULL,
    measured BOOLEAN NOT NULL DEFAULT TRUE,
    data_source VARCHAR(120) NULL,
    source_type VARCHAR(40) NULL,
    source_document VARCHAR(255) NULL,
    created_by_user_id BIGINT NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nutrient_input_event_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_nutrient_input_event_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_nutrient_input_event_user FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE irrigation_water_analyses (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    plot_id INT NOT NULL,
    sample_date DATE NOT NULL,
    nitrate_mg_per_l DECIMAL(19,4) NULL,
    ammonium_mg_per_l DECIMAL(19,4) NULL,
    total_n_mg_per_l DECIMAL(19,4) NULL,
    irrigation_volume_m3 DECIMAL(19,4) NOT NULL,
    measured BOOLEAN NOT NULL DEFAULT TRUE,
    source_type VARCHAR(40) NULL,
    source_document VARCHAR(255) NULL,
    lab_reference VARCHAR(255) NULL,
    note TEXT NULL,
    created_by_user_id BIGINT NULL,
    legacy_n_contribution_kg DECIMAL(19,4) NULL,
    legacy_event_id INT NULL,
    legacy_derived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_irrigation_analysis_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_irrigation_analysis_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_irrigation_analysis_user FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE soil_tests (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    plot_id INT NOT NULL,
    sample_date DATE NOT NULL,
    soil_organic_matter_pct DECIMAL(12,4) NULL,
    mineral_n_kg_per_ha DECIMAL(19,4) NOT NULL,
    nitrate_mg_per_kg DECIMAL(19,4) NULL,
    ammonium_mg_per_kg DECIMAL(19,4) NULL,
    measured BOOLEAN NOT NULL DEFAULT TRUE,
    source_type VARCHAR(40) NULL,
    source_document VARCHAR(255) NULL,
    lab_reference VARCHAR(255) NULL,
    note TEXT NULL,
    created_by_user_id BIGINT NULL,
    legacy_n_contribution_kg DECIMAL(19,4) NULL,
    legacy_event_id INT NULL,
    legacy_derived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_soil_test_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_soil_test_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_soil_test_user FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE season_employees (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    added_by_user_id BIGINT NULL,
    wage_per_task DECIMAL(15,2) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_season_employee UNIQUE (season_id, employee_user_id),
    CONSTRAINT fk_season_employee_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_season_employee_user FOREIGN KEY (employee_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_season_employee_added_by FOREIGN KEY (added_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_progress_logs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    progress_percent INT NOT NULL,
    note TEXT NULL,
    evidence_url VARCHAR(1000) NULL,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_progress_task FOREIGN KEY (task_id) REFERENCES tasks(task_id),
    CONSTRAINT fk_task_progress_employee FOREIGN KEY (employee_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payroll_records (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_user_id BIGINT NOT NULL,
    season_id INT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_assigned_tasks INT NOT NULL DEFAULT 0,
    total_completed_tasks INT NOT NULL DEFAULT 0,
    wage_per_task DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT NULL,
    CONSTRAINT uk_payroll_employee_season_period UNIQUE (employee_user_id, season_id, period_start, period_end),
    CONSTRAINT fk_payroll_employee FOREIGN KEY (employee_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_payroll_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_warehouse_lots (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lot_code VARCHAR(100) NOT NULL,
    product_id INT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_variant VARCHAR(255) NULL,
    season_id INT NULL,
    farm_id INT NOT NULL,
    plot_id INT NOT NULL,
    harvest_id INT NULL,
    warehouse_id INT NOT NULL,
    location_id INT NULL,
    harvested_at DATE NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unit VARCHAR(30) NOT NULL,
    initial_quantity DECIMAL(19,3) NOT NULL,
    on_hand_quantity DECIMAL(19,3) NOT NULL,
    grade VARCHAR(50) NULL,
    quality_status VARCHAR(50) NULL,
    traceability_data TEXT NULL,
    note TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_warehouse_lot_code UNIQUE (lot_code),
    CONSTRAINT uk_product_warehouse_harvest UNIQUE (harvest_id),
    CONSTRAINT fk_product_warehouse_lot_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_product_warehouse_lot_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id),
    CONSTRAINT fk_product_warehouse_lot_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_product_warehouse_lot_harvest FOREIGN KEY (harvest_id) REFERENCES harvests(harvest_id),
    CONSTRAINT fk_product_warehouse_lot_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_product_warehouse_lot_location FOREIGN KEY (location_id) REFERENCES stock_locations(id),
    CONSTRAINT fk_product_warehouse_lot_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_warehouse_transactions (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lot_id INT NOT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    quantity DECIMAL(19,3) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    resulting_on_hand DECIMAL(19,3) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id VARCHAR(100) NULL,
    note TEXT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_warehouse_tx_lot FOREIGN KEY (lot_id) REFERENCES product_warehouse_lots(id),
    CONSTRAINT fk_product_warehouse_tx_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    request_ip VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_province ON users(province_id);
CREATE INDEX idx_farms_user ON farms(user_id);
CREATE INDEX idx_farm_user_name_active ON farms(user_id, farm_name, active);
CREATE INDEX idx_plots_farm ON plots(farm_id);
CREATE INDEX idx_plots_status ON plots(status);
CREATE INDEX idx_season_plot_id ON seasons(plot_id);
CREATE INDEX idx_season_plot_name ON seasons(plot_id, season_name);
CREATE INDEX idx_season_status ON seasons(status);
CREATE INDEX idx_tasks_season_status ON tasks(season_id, status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_expense_category ON expenses(category);
CREATE INDEX idx_expense_date ON expenses(expense_date);
CREATE INDEX idx_expense_item_name ON expenses(item_name);
CREATE INDEX idx_harvests_season_date ON harvests(season_id, harvest_date);
CREATE INDEX idx_supply_lots_status ON supply_lots(status);
CREATE INDEX idx_stock_movements_lot_date ON stock_movements(supply_lot_id, movement_date);
CREATE INDEX idx_inventory_balances_lookup ON inventory_balances(supply_lot_id, warehouse_id, location_id);
CREATE INDEX idx_inventory_balances_warehouse ON inventory_balances(warehouse_id);
CREATE INDEX idx_field_logs_season_date ON field_logs(season_id, log_date);
CREATE INDEX idx_incidents_season_status ON incidents(season_id, status);
CREATE INDEX idx_incidents_deadline ON incidents(deadline);
CREATE INDEX idx_alert_status ON alerts(status);
CREATE INDEX idx_alert_farm ON alerts(farm_id);
CREATE INDEX idx_alert_season ON alerts(season_id);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at);
CREATE INDEX idx_notifications_read ON notifications(user_id, read_at);
CREATE INDEX idx_document_recent_user_opened ON document_recent_opens(user_id, opened_at);
CREATE INDEX idx_entity_lookup ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_performed_at ON audit_logs(performed_at);
CREATE INDEX idx_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_log_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_log_operation ON audit_logs(operation);
CREATE INDEX idx_crop_n_ref_crop_active ON crop_nitrogen_references(crop_id, active);
CREATE INDEX idx_nutrient_input_events_season_plot ON nutrient_input_events(season_id, plot_id);
CREATE INDEX idx_nutrient_input_events_source ON nutrient_input_events(input_source);
CREATE INDEX idx_nutrient_input_events_created_by_user ON nutrient_input_events(created_by_user_id);
CREATE INDEX idx_irrigation_analysis_season_plot ON irrigation_water_analyses(season_id, plot_id);
CREATE UNIQUE INDEX uq_irrigation_analysis_legacy_event ON irrigation_water_analyses(legacy_event_id);
CREATE INDEX idx_soil_test_season_plot ON soil_tests(season_id, plot_id);
CREATE UNIQUE INDEX uq_soil_test_legacy_event ON soil_tests(legacy_event_id);
CREATE INDEX idx_season_employee_season ON season_employees(season_id);
CREATE INDEX idx_season_employee_user ON season_employees(employee_user_id);
CREATE INDEX idx_task_progress_task ON task_progress_logs(task_id);
CREATE INDEX idx_task_progress_employee ON task_progress_logs(employee_user_id);
CREATE INDEX idx_task_progress_logged_at ON task_progress_logs(logged_at);
CREATE INDEX idx_payroll_employee ON payroll_records(employee_user_id);
CREATE INDEX idx_payroll_season ON payroll_records(season_id);
CREATE INDEX idx_payroll_period ON payroll_records(period_start, period_end);
CREATE INDEX idx_product_warehouse_lot_farm ON product_warehouse_lots(farm_id);
CREATE INDEX idx_product_warehouse_lot_season ON product_warehouse_lots(season_id);
CREATE INDEX idx_product_warehouse_lot_plot ON product_warehouse_lots(plot_id);
CREATE INDEX idx_product_warehouse_lot_harvested_at ON product_warehouse_lots(harvested_at);
CREATE INDEX idx_product_warehouse_lot_status ON product_warehouse_lots(status);
CREATE INDEX idx_product_warehouse_tx_lot ON product_warehouse_transactions(lot_id);
CREATE INDEX idx_product_warehouse_tx_type ON product_warehouse_transactions(transaction_type);
CREATE INDEX idx_product_warehouse_tx_created_at ON product_warehouse_transactions(created_at);
CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);

CREATE OR REPLACE VIEW vw_admin_season_risk AS
SELECT
    s.season_id AS season_id,
    s.season_name AS season_name,
    s.status AS season_status,
    p.plot_id AS plot_id,
    p.plot_name AS plot_name,
    f.farm_id AS farm_id,
    f.farm_name AS farm_name,
    COALESCE(incident_agg.incident_count, 0) AS incident_count,
    COALESCE(task_agg.overdue_task_count, 0) AS overdue_task_count,
    (COALESCE(incident_agg.incident_count, 0) + COALESCE(task_agg.overdue_task_count, 0)) AS risk_score
FROM seasons s
JOIN plots p ON p.plot_id = s.plot_id
JOIN farms f ON f.farm_id = p.farm_id
LEFT JOIN (
    SELECT i.season_id, COUNT(DISTINCT i.id) AS incident_count
    FROM incidents i
    GROUP BY i.season_id
) incident_agg ON incident_agg.season_id = s.season_id
LEFT JOIN (
    SELECT t.season_id, COUNT(DISTINCT t.task_id) AS overdue_task_count
    FROM tasks t
    WHERE t.status = 'OVERDUE'
    GROUP BY t.season_id
) task_agg ON task_agg.season_id = s.season_id;

CREATE OR REPLACE VIEW vw_admin_inventory_lot_farm AS
SELECT DISTINCT
    sm.supply_lot_id AS supply_lot_id,
    w.farm_id AS farm_id
FROM stock_movements sm
JOIN warehouses w ON w.id = sm.warehouse_id
WHERE sm.supply_lot_id IS NOT NULL
  AND w.farm_id IS NOT NULL;

CREATE OR REPLACE VIEW vw_admin_inventory_lot_expiry_base AS
SELECT
    f.farm_id AS farm_id,
    f.farm_name AS farm_name,
    sl.id AS supply_lot_id,
    sl.expiry_date AS expiry_date
FROM supply_lots sl
JOIN vw_admin_inventory_lot_farm lot_farm ON lot_farm.supply_lot_id = sl.id
JOIN farms f ON f.farm_id = lot_farm.farm_id
WHERE sl.expiry_date IS NOT NULL;
