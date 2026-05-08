-- V21__disease_tracking_module.sql
-- Disease tracking module for season management

CREATE TABLE IF NOT EXISTS disease_records (
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
    CONSTRAINT fk_disease_records_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_disease_records_plot FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT fk_disease_records_crop FOREIGN KEY (crop_id) REFERENCES crops(crop_id),
    CONSTRAINT fk_disease_records_variety FOREIGN KEY (variety_id) REFERENCES varieties(id),
    CONSTRAINT fk_disease_records_reporter FOREIGN KEY (reported_by_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_disease_records_incident FOREIGN KEY (incident_id) REFERENCES incidents(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS disease_treatments (
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
    CONSTRAINT fk_disease_treatments_record FOREIGN KEY (disease_record_id) REFERENCES disease_records(disease_record_id),
    CONSTRAINT fk_disease_treatments_supply_item FOREIGN KEY (supply_item_id) REFERENCES supply_items(id),
    CONSTRAINT fk_disease_treatments_supply_lot FOREIGN KEY (supply_lot_id) REFERENCES supply_lots(id),
    CONSTRAINT fk_disease_treatments_expense FOREIGN KEY (expense_id) REFERENCES expenses(expense_id),
    CONSTRAINT fk_disease_treatments_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_disease_records_season_id ON disease_records(season_id);
CREATE INDEX IF NOT EXISTS idx_disease_records_status ON disease_records(status);
CREATE INDEX IF NOT EXISTS idx_disease_records_severity ON disease_records(severity);
CREATE INDEX IF NOT EXISTS idx_disease_records_detected_at ON disease_records(detected_at);
CREATE INDEX IF NOT EXISTS idx_disease_treatments_disease_record_id ON disease_treatments(disease_record_id);
