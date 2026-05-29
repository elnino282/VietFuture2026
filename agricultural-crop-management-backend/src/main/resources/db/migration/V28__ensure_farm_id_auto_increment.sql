-- Ensure farms.farm_id uses AUTO_INCREMENT for JPA IDENTITY inserts.
-- Some imported schemas define farm_id as plain INT NOT NULL PRIMARY KEY.
-- MySQL requires dropping referencing foreign keys before modifying a referenced PK column.

SET @schema_name := DATABASE();

SET @needs_fix := (
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'farms'
          AND column_name = 'farm_id'
          AND extra NOT LIKE '%auto_increment%'
    )
);

-- Drop FKs referencing farms(farm_id) only when fix is needed.
SET @stmt := IF(
    @needs_fix = 1
    AND EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'alerts'
          AND constraint_name = 'fk_alerts_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE alerts DROP FOREIGN KEY fk_alerts_farm',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'marketplace_order_items'
          AND constraint_name = 'fk_marketplace_order_items_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE marketplace_order_items DROP FOREIGN KEY fk_marketplace_order_items_farm',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'marketplace_products'
          AND constraint_name = 'fk_marketplace_products_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE marketplace_products DROP FOREIGN KEY fk_marketplace_products_farm',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'plots'
          AND constraint_name = 'fk_plots_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE plots DROP FOREIGN KEY fk_plots_farm',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'product_warehouse_lots'
          AND constraint_name = 'fk_product_warehouse_lot_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE product_warehouse_lots DROP FOREIGN KEY fk_product_warehouse_lot_farm',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'warehouses'
          AND constraint_name = 'fk_warehouses_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE warehouses DROP FOREIGN KEY fk_warehouses_farm',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- Fix the primary key generation behavior.
SET @stmt := IF(
    @needs_fix = 1,
    'ALTER TABLE farms MODIFY COLUMN farm_id INT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- Re-create dropped FKs when they are missing.
SET @stmt := IF(
    @needs_fix = 1
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'alerts'
          AND constraint_name = 'fk_alerts_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE alerts ADD CONSTRAINT fk_alerts_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'marketplace_order_items'
          AND constraint_name = 'fk_marketplace_order_items_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE marketplace_order_items ADD CONSTRAINT fk_marketplace_order_items_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'marketplace_products'
          AND constraint_name = 'fk_marketplace_products_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE marketplace_products ADD CONSTRAINT fk_marketplace_products_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'plots'
          AND constraint_name = 'fk_plots_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE plots ADD CONSTRAINT fk_plots_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'product_warehouse_lots'
          AND constraint_name = 'fk_product_warehouse_lot_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE product_warehouse_lots ADD CONSTRAINT fk_product_warehouse_lot_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    @needs_fix = 1
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = @schema_name
          AND table_name = 'warehouses'
          AND constraint_name = 'fk_warehouses_farm'
          AND constraint_type = 'FOREIGN KEY'
    ),
    'ALTER TABLE warehouses ADD CONSTRAINT fk_warehouses_farm FOREIGN KEY (farm_id) REFERENCES farms(farm_id)',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;
