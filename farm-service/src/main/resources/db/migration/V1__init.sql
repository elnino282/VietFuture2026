CREATE TABLE IF NOT EXISTS provinces (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    name_with_type VARCHAR(256) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wards (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    name_with_type VARCHAR(512) NOT NULL,
    province_id INT NOT NULL,
    CONSTRAINT fk_wards_province FOREIGN KEY (province_id) REFERENCES provinces(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS farms (
    farm_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    farm_name VARCHAR(255) NOT NULL,
    province_id INT NOT NULL,
    ward_id INT NOT NULL,
    area DECIMAL(19,2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    latitude DECIMAL(10, 6) NULL,
    longitude DECIMAL(10, 6) NULL,
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_farms_province FOREIGN KEY (province_id) REFERENCES provinces(id),
    CONSTRAINT fk_farms_ward FOREIGN KEY (ward_id) REFERENCES wards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plots (
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
    CONSTRAINT fk_plots_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload LONGTEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_outbox_events_processed ON outbox_events(processed);
