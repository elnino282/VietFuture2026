-- Sustainability FDN/NUE support schema
-- Product rule config stays in application.yml (app.sustainability.*)

ALTER TABLE plots
    ADD COLUMN IF NOT EXISTS boundary_geojson LONGTEXT NULL;

CREATE TABLE IF NOT EXISTS nutrient_input_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    plot_id INT NOT NULL,
    input_source VARCHAR(40) NOT NULL,
    n_kg DECIMAL(19,4) NOT NULL,
    applied_date DATE NULL,
    measured BOOLEAN NOT NULL DEFAULT TRUE,
    data_source VARCHAR(120) NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nutrient_input_event_season
        FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_nutrient_input_event_plot
        FOREIGN KEY (plot_id) REFERENCES plots(plot_id)
);

CREATE INDEX IF NOT EXISTS idx_nutrient_input_events_season_plot
    ON nutrient_input_events(season_id, plot_id);
CREATE INDEX IF NOT EXISTS idx_nutrient_input_events_source
    ON nutrient_input_events(input_source);

CREATE TABLE IF NOT EXISTS crop_nitrogen_references (
    id INT AUTO_INCREMENT PRIMARY KEY,
    crop_id INT NOT NULL,
    n_content_kg_per_kg_yield DECIMAL(12,6) NOT NULL,
    source_reference VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_crop_n_ref_crop
        FOREIGN KEY (crop_id) REFERENCES crops(crop_id)
);

CREATE INDEX IF NOT EXISTS idx_crop_n_ref_crop_active
    ON crop_nitrogen_references(crop_id, active);
