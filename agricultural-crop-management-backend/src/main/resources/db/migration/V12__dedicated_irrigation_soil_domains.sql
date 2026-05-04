-- Dedicated domain tables for irrigation-water-analysis and soil-test ingestion

CREATE TABLE IF NOT EXISTS irrigation_water_analyses (
    id INT AUTO_INCREMENT PRIMARY KEY,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_irrigation_analysis_season
        FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_irrigation_analysis_plot
        FOREIGN KEY (plot_id) REFERENCES plots(plot_id)
);

CREATE INDEX IF NOT EXISTS idx_irrigation_analysis_season_plot
    ON irrigation_water_analyses(season_id, plot_id);

CREATE TABLE IF NOT EXISTS soil_tests (
    id INT AUTO_INCREMENT PRIMARY KEY,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_soil_test_season
        FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_soil_test_plot
        FOREIGN KEY (plot_id) REFERENCES plots(plot_id)
);

CREATE INDEX IF NOT EXISTS idx_soil_test_season_plot
    ON soil_tests(season_id, plot_id);
