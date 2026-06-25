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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    CONSTRAINT fk_tasks_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_progress_logs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    logged_at DATETIME NOT NULL,
    progress_percentage INT NOT NULL,
    notes TEXT NULL,
    CONSTRAINT fk_task_progress_task FOREIGN KEY (task_id) REFERENCES tasks(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE field_logs (
    field_log_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    log_date DATE NOT NULL,
    log_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_field_logs_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE harvests (
    harvest_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    harvest_date DATE NOT NULL,
    quantity DECIMAL(19,2) NOT NULL,
    unit DECIMAL(19,2) NOT NULL,
    grade VARCHAR(50) NULL,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_harvests_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disease_records (
    disease_record_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    plot_id INT NULL,
    crop_id INT NULL,
    variety_id INT NULL,
    reported_by_user_id BIGINT NOT NULL,
    incident_id INT NULL,
    disease_name VARCHAR(150) NOT NULL,
    symptom_summary TEXT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    detected_at DATETIME NOT NULL,
    affected_plant_count INT NULL,
    affected_area_value DECIMAL(14,3) NULL,
    affected_area_unit VARCHAR(20) NULL,
    evidence_url VARCHAR(1000) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_disease_records_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disease_treatments (
    disease_treatment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    disease_record_id INT NOT NULL,
    treated_at DATETIME NOT NULL,
    method VARCHAR(100) NOT NULL,
    supply_item_id INT NULL,
    supply_lot_id INT NULL,
    material_name VARCHAR(150) NULL,
    quantity_used DECIMAL(14,3) NULL,
    unit VARCHAR(20) NULL,
    cost_amount DECIMAL(19,2) NULL,
    expense_id INT NULL,
    effectiveness VARCHAR(20) NULL,
    result_summary TEXT NULL,
    next_review_at DATE NULL,
    notes TEXT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_disease_treatments_record FOREIGN KEY (disease_record_id) REFERENCES disease_records(disease_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE season_employees (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(50) NULL,
    target_wage DECIMAL(19,2) NULL,
    notes TEXT NULL,
    CONSTRAINT fk_season_employees_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payroll_records (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_user_id BIGINT NOT NULL,
    season_id INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    working_days INT NOT NULL,
    daily_wage DECIMAL(19,2) NOT NULL,
    bonus_amount DECIMAL(19,2) NULL,
    deductions DECIMAL(19,2) NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    notes TEXT NULL,
    expense_id INT NULL,
    CONSTRAINT fk_payroll_records_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
