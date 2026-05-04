-- Support idempotent backfill from legacy nutrient_input_events
-- to dedicated irrigation_water_analyses / soil_tests domains.

ALTER TABLE irrigation_water_analyses
    ADD COLUMN IF NOT EXISTS legacy_n_contribution_kg DECIMAL(19,4) NULL,
    ADD COLUMN IF NOT EXISTS legacy_event_id INT NULL,
    ADD COLUMN IF NOT EXISTS legacy_derived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE soil_tests
    ADD COLUMN IF NOT EXISTS legacy_n_contribution_kg DECIMAL(19,4) NULL,
    ADD COLUMN IF NOT EXISTS legacy_event_id INT NULL,
    ADD COLUMN IF NOT EXISTS legacy_derived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_irrigation_analysis_legacy_event
    ON irrigation_water_analyses(legacy_event_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_soil_test_legacy_event
    ON soil_tests(legacy_event_id);
