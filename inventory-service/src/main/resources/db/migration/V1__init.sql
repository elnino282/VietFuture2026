-- Inventory Service Initial Schema

CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    license_no VARCHAR(100),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(30)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supply_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    active_ingredient VARCHAR(150),
    unit VARCHAR(20),
    restricted_flag BOOLEAN
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supply_lots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supply_item_id INT NOT NULL,
    supplier_id INT,
    batch_code VARCHAR(100),
    expiry_date DATE,
    status VARCHAR(20),
    INDEX idx_supply_item (supply_item_id),
    INDEX idx_supplier (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    farm_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(20),
    province_id INT,
    ward_id INT,
    INDEX idx_farm (farm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id INT NOT NULL,
    zone VARCHAR(20),
    aisle VARCHAR(20),
    shelf VARCHAR(20),
    bin VARCHAR(20),
    INDEX idx_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supply_lot_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    location_id INT,
    quantity DECIMAL(10,2) NOT NULL,
    UNIQUE KEY uk_lot_warehouse_location (supply_lot_id, warehouse_id, location_id),
    INDEX idx_supply_lot (supply_lot_id),
    INDEX idx_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_movements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supply_lot_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    location_id INT,
    movement_type VARCHAR(10) NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    movement_date DATETIME NOT NULL,
    season_id INT,
    task_id INT,
    note TEXT,
    INDEX idx_supply_lot (supply_lot_id),
    INDEX idx_warehouse (warehouse_id),
    INDEX idx_season (season_id),
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_warehouse_lots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lot_code VARCHAR(100) NOT NULL UNIQUE,
    product_id INT,
    product_name VARCHAR(255) NOT NULL,
    product_variant VARCHAR(255),
    season_id INT,
    farm_id INT NOT NULL,
    plot_id INT NOT NULL,
    harvest_id INT UNIQUE,
    warehouse_id INT NOT NULL,
    location_id INT,
    harvested_at DATE NOT NULL,
    received_at DATETIME NOT NULL,
    unit VARCHAR(30) NOT NULL,
    initial_quantity DECIMAL(19,3) NOT NULL,
    on_hand_quantity DECIMAL(19,3) NOT NULL,
    grade VARCHAR(50),
    quality_status VARCHAR(50),
    traceability_data TEXT,
    note TEXT,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_farm (farm_id),
    INDEX idx_plot (plot_id),
    INDEX idx_season (season_id),
    INDEX idx_harvest (harvest_id),
    INDEX idx_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_warehouse_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lot_id INT NOT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    quantity DECIMAL(19,3) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    resulting_on_hand DECIMAL(19,3) NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    note TEXT,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    INDEX idx_lot (lot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_events (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
