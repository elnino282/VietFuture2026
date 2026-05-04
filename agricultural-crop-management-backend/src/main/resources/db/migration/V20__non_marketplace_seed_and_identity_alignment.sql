-- Non-marketplace schema/data alignment for reproducible clean installs and tests.
-- Scope:
-- - Keep append-only Flyway approach (do not modify historical migrations).
-- - Align critical identity PK strategy with JPA IDENTITY.
-- - Seed minimal non-marketplace data for Admin/Farmer/Employee/Buyer + core demo domain.
-- - Do NOT seed marketplace flows.

SET @schema_name := DATABASE();

SET @stmt := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'roles'
          AND column_name = 'role_id'
          AND extra NOT LIKE '%auto_increment%'
    ),
    'ALTER TABLE roles MODIFY COLUMN role_id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @stmt := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'users'
          AND column_name = 'user_id'
          AND extra NOT LIKE '%auto_increment%'
    ),
    'ALTER TABLE users MODIFY COLUMN user_id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

INSERT INTO provinces (id, name, slug, type, name_with_type)
SELECT 24, 'Ha Noi', 'ha-noi', 'thanh-pho', 'Thanh pho Ha Noi'
WHERE NOT EXISTS (
    SELECT 1 FROM provinces WHERE id = 24
);

INSERT INTO wards (id, name, slug, type, name_with_type, province_id)
SELECT 25112, 'Phuong Demo', 'phuong-demo', 'phuong', 'Phuong Demo, Ha Noi', 24
WHERE NOT EXISTS (
    SELECT 1 FROM wards WHERE id = 25112
);

INSERT INTO roles (role_code, role_name, description)
VALUES
    ('ADMIN', 'Admin', 'Administrator user with full access'),
    ('FARMER', 'Farmer', 'Farmer user'),
    ('EMPLOYEE', 'Employee', 'Employee user'),
    ('BUYER', 'Buyer', 'Buyer user')
AS new
ON DUPLICATE KEY UPDATE
    role_name = new.role_name,
    description = new.description;

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'admin', 'admin@acm.local', '0900000000', 'Administrator',
       '$2a$10$7iN9nIqCTnm9sE2zrREqXu6KXcc6RTcTM2Dqx02qBS0NFjgIQ4442',
       'ACTIVE', 24, 25112, '2024-01-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'admin');

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'farmer', 'farmer@acm.local', '0901234567', 'Nguyen Van Farmer',
       '$2a$10$BzROX8TgxrKpb./sQD9w..VmxFh1AJjAQAH8mxhJfdmpb2C7aWLIy',
       'ACTIVE', 24, 25112, '2024-06-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'farmer');

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'employee', 'employee@acm.local', '0902234567', 'Nguyen Van Employee',
       '$2a$10$BzROX8TgxrKpb./sQD9w..VmxFh1AJjAQAH8mxhJfdmpb2C7aWLIy',
       'ACTIVE', 24, 25112, '2025-11-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'employee');

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'buyer', 'buyer@acm.local', '0903234000', 'Tran Thi Buyer',
       '$2a$10$BzROX8TgxrKpb./sQD9w..VmxFh1AJjAQAH8mxhJfdmpb2C7aWLIy',
       'ACTIVE', 24, 25112, '2025-12-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'buyer');

SET @admin_user_id := (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1);
SET @farmer_user_id := (SELECT user_id FROM users WHERE user_name = 'farmer' LIMIT 1);
SET @employee_user_id := (SELECT user_id FROM users WHERE user_name = 'employee' LIMIT 1);
SET @buyer_user_id := (SELECT user_id FROM users WHERE user_name = 'buyer' LIMIT 1);

INSERT INTO user_roles (user_id, role_id)
SELECT @admin_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'ADMIN'
  AND @admin_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = @admin_user_id
        AND ur.role_id = r.role_id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT @farmer_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'FARMER'
  AND @farmer_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = @farmer_user_id
        AND ur.role_id = r.role_id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT @employee_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'EMPLOYEE'
  AND @employee_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = @employee_user_id
        AND ur.role_id = r.role_id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT @buyer_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'BUYER'
  AND @buyer_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = @buyer_user_id
        AND ur.role_id = r.role_id
  );

INSERT INTO crops (crop_name, description)
SELECT 'Rice', 'Staple crop for demo/test data'
WHERE NOT EXISTS (SELECT 1 FROM crops WHERE crop_name = 'Rice');

SET @rice_crop_id := (SELECT crop_id FROM crops WHERE crop_name = 'Rice' ORDER BY crop_id ASC LIMIT 1);

INSERT INTO varieties (crop_id, name, description)
SELECT @rice_crop_id, 'ST25', 'Demo variety'
WHERE @rice_crop_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM varieties
      WHERE crop_id = @rice_crop_id
        AND name = 'ST25'
  );

SET @rice_variety_id := (
    SELECT id FROM varieties
    WHERE crop_id = @rice_crop_id
      AND name = 'ST25'
    ORDER BY id ASC LIMIT 1
);

INSERT INTO farms (user_id, farm_name, province_id, ward_id, area, active)
SELECT @farmer_user_id, 'Demo Farm', 24, 25112, 12.50, TRUE
WHERE @farmer_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM farms
      WHERE user_id = @farmer_user_id
        AND farm_name = 'Demo Farm'
  );

SET @demo_farm_id := (
    SELECT farm_id FROM farms
    WHERE user_id = @farmer_user_id
      AND farm_name = 'Demo Farm'
    ORDER BY farm_id ASC LIMIT 1
);

INSERT INTO plots (farm_id, plot_name, area, soil_type, status, created_by)
SELECT @demo_farm_id, 'Plot A1', 4.20, 'LOAM', 'IN_USE', @farmer_user_id
WHERE @demo_farm_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM plots
      WHERE farm_id = @demo_farm_id
        AND plot_name = 'Plot A1'
  );

SET @demo_plot_id := (
    SELECT plot_id FROM plots
    WHERE farm_id = @demo_farm_id
      AND plot_name = 'Plot A1'
    ORDER BY plot_id ASC LIMIT 1
);

INSERT INTO seasons (
    season_name, plot_id, crop_id, variety_id,
    start_date, planned_harvest_date, end_date,
    status, initial_plant_count, current_plant_count,
    expected_yield_kg, actual_yield_kg, budget_amount, notes
)
SELECT
    'Spring Demo 2026', @demo_plot_id, @rice_crop_id, @rice_variety_id,
    '2026-02-01', '2026-06-01', NULL,
    'ACTIVE', 1200, 1180,
    2500, NULL, 50000000, 'Seeded for non-marketplace demo flow'
WHERE @demo_plot_id IS NOT NULL
  AND @rice_crop_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM seasons
      WHERE plot_id = @demo_plot_id
        AND season_name = 'Spring Demo 2026'
  );

SET @demo_season_id := (
    SELECT season_id FROM seasons
    WHERE plot_id = @demo_plot_id
      AND season_name = 'Spring Demo 2026'
    ORDER BY season_id ASC LIMIT 1
);

INSERT INTO tasks (
    user_id, season_id, title, description, planned_date, due_date, status
)
SELECT
    @employee_user_id, @demo_season_id,
    'Monitor field moisture',
    'Daily moisture check for seeded plot',
    '2026-04-20', '2026-04-27', 'PENDING'
WHERE @employee_user_id IS NOT NULL
  AND @demo_season_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM tasks
      WHERE season_id = @demo_season_id
        AND title = 'Monitor field moisture'
  );

INSERT INTO suppliers (name, license_no, contact_email, contact_phone)
SELECT 'Default Supplier', 'SUP-DEMO-001', 'supplier@acm.local', '0908888888'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'Default Supplier');

SET @supplier_id := (
    SELECT id FROM suppliers
    WHERE name = 'Default Supplier'
    ORDER BY id ASC LIMIT 1
);

INSERT INTO supply_items (name, active_ingredient, unit, restricted_flag)
SELECT 'NPK 16-16-8', 'NPK', 'kg', FALSE
WHERE NOT EXISTS (SELECT 1 FROM supply_items WHERE name = 'NPK 16-16-8');

SET @supply_item_id := (
    SELECT id FROM supply_items
    WHERE name = 'NPK 16-16-8'
    ORDER BY id ASC LIMIT 1
);

INSERT INTO supply_lots (supply_item_id, supplier_id, batch_code, expiry_date, status)
SELECT @supply_item_id, @supplier_id, 'LOT-DEMO-001', '2027-12-31', 'IN_STOCK'
WHERE @supply_item_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM supply_lots WHERE batch_code = 'LOT-DEMO-001');

SET @supply_lot_id := (
    SELECT id FROM supply_lots
    WHERE batch_code = 'LOT-DEMO-001'
    ORDER BY id ASC LIMIT 1
);

INSERT INTO warehouses (farm_id, name, type, province_id, ward_id)
SELECT @demo_farm_id, 'Input Warehouse Demo', 'INPUT', 24, 25112
WHERE @demo_farm_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM warehouses
      WHERE farm_id = @demo_farm_id
        AND name = 'Input Warehouse Demo'
  );

SET @warehouse_id := (
    SELECT id FROM warehouses
    WHERE farm_id = @demo_farm_id
      AND name = 'Input Warehouse Demo'
    ORDER BY id ASC LIMIT 1
);

INSERT INTO stock_locations (warehouse_id, zone, aisle, shelf, bin)
SELECT @warehouse_id, 'A', '1', '1', '1'
WHERE @warehouse_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM stock_locations
      WHERE warehouse_id = @warehouse_id
        AND zone = 'A'
        AND aisle = '1'
        AND shelf = '1'
        AND bin = '1'
  );

SET @location_id := (
    SELECT id FROM stock_locations
    WHERE warehouse_id = @warehouse_id
      AND zone = 'A'
      AND aisle = '1'
      AND shelf = '1'
      AND bin = '1'
    ORDER BY id ASC LIMIT 1
);

INSERT INTO stock_movements (
    supply_lot_id, warehouse_id, location_id, movement_type, quantity, season_id, note
)
SELECT
    @supply_lot_id, @warehouse_id, @location_id,
    'IN', 100.000, @demo_season_id, 'Initial seed stock movement'
WHERE @supply_lot_id IS NOT NULL
  AND @warehouse_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM stock_movements
      WHERE supply_lot_id = @supply_lot_id
        AND warehouse_id = @warehouse_id
        AND movement_type = 'IN'
        AND quantity = 100.000
  );

INSERT INTO inventory_balances (supply_lot_id, warehouse_id, location_id, quantity)
SELECT @supply_lot_id, @warehouse_id, @location_id, 100.000
WHERE @supply_lot_id IS NOT NULL
  AND @warehouse_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances
      WHERE supply_lot_id = @supply_lot_id
        AND warehouse_id = @warehouse_id
        AND ((location_id IS NULL AND @location_id IS NULL) OR location_id = @location_id)
  );

INSERT INTO documents (
    title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type, is_pinned
)
SELECT
    'Rice Planting Guide',
    'https://example.com/rice-planting',
    'Seeded document for farmer non-marketplace demo/test flow.',
    'Rice', 'Planting', 'Best Practices',
    TRUE, TRUE, @admin_user_id, 'GUIDE', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM documents
    WHERE title = 'Rice Planting Guide'
);

INSERT INTO user_preferences (user_id, currency_code, weight_unit, locale)
SELECT @admin_user_id, 'VND', 'kg', 'vi-VN'
WHERE @admin_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_preferences WHERE user_id = @admin_user_id);

INSERT INTO user_preferences (user_id, currency_code, weight_unit, locale)
SELECT @farmer_user_id, 'VND', 'kg', 'vi-VN'
WHERE @farmer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_preferences WHERE user_id = @farmer_user_id);

INSERT INTO user_preferences (user_id, currency_code, weight_unit, locale)
SELECT @employee_user_id, 'VND', 'kg', 'vi-VN'
WHERE @employee_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_preferences WHERE user_id = @employee_user_id);

INSERT INTO user_preferences (user_id, currency_code, weight_unit, locale)
SELECT @buyer_user_id, 'VND', 'kg', 'vi-VN'
WHERE @buyer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_preferences WHERE user_id = @buyer_user_id);
