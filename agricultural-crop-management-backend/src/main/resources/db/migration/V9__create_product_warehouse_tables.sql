CREATE TABLE IF NOT EXISTS product_warehouse_lots (
    id INT AUTO_INCREMENT PRIMARY KEY,
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
);

CREATE INDEX idx_product_warehouse_lot_farm ON product_warehouse_lots(farm_id);
CREATE INDEX idx_product_warehouse_lot_season ON product_warehouse_lots(season_id);
CREATE INDEX idx_product_warehouse_lot_plot ON product_warehouse_lots(plot_id);
CREATE INDEX idx_product_warehouse_lot_harvested_at ON product_warehouse_lots(harvested_at);
CREATE INDEX idx_product_warehouse_lot_status ON product_warehouse_lots(status);

CREATE TABLE IF NOT EXISTS product_warehouse_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
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
);

CREATE INDEX idx_product_warehouse_tx_lot ON product_warehouse_transactions(lot_id);
CREATE INDEX idx_product_warehouse_tx_type ON product_warehouse_transactions(transaction_type);
CREATE INDEX idx_product_warehouse_tx_created_at ON product_warehouse_transactions(created_at);

