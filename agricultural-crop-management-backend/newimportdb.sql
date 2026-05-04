-- =========================================================
-- ACM Platform - Flyway-Compatible Demo Seed (MySQL 8)
-- =========================================================
-- Run after Flyway migrations have created the schema and baseline data.
-- This script is idempotent: every insert is guarded by natural keys or unique keys.
-- It intentionally does NOT create/drop tables, create/drop views, or reinsert Flyway-managed
-- baseline rows for roles and the default admin/farmer/employee/buyer users.
-- =========================================================

USE `quanlymuavu`;

SET NAMES utf8mb4;
SET @seed_now := TIMESTAMP('2026-05-05 08:00:00');

-- Flyway-managed baseline users/roles are resolved by natural keys, never hardcoded IDs.
SET @admin_user_id := (SELECT user_id FROM users WHERE user_name = 'admin' LIMIT 1);
SET @farmer_user_id := (SELECT user_id FROM users WHERE user_name = 'farmer' LIMIT 1);
SET @employee_user_id := (SELECT user_id FROM users WHERE user_name = 'employee' LIMIT 1);
SET @buyer_user_id := (SELECT user_id FROM users WHERE user_name = 'buyer' LIMIT 1);
SET @province_id := (SELECT id FROM provinces ORDER BY id LIMIT 1);
SET @ward_id := (SELECT id FROM wards WHERE province_id = @province_id ORDER BY id LIMIT 1);

-- Additional non-baseline users for marketplace, employee assignment, and account-status demos.
INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'farmer2', 'farmer2@acm.local', '0904234567', 'Lê Thị Nông Dân 2',
       '$2a$10$BzROX8TgxrKpb./sQD9w..VmxFh1AJjAQAH8mxhJfdmpb2C7aWLIy',
       'ACTIVE', @province_id, @ward_id, '2025-12-15 08:00:00'
    WHERE @province_id IS NOT NULL
  AND @ward_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'farmer2');

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'employee2', 'employee2@acm.local', '0903234567', 'Trần Thị Nhân Viên',
       '$2a$10$BzROX8TgxrKpb./sQD9w..VmxFh1AJjAQAH8mxhJfdmpb2C7aWLIy',
       'ACTIVE', @province_id, @ward_id, '2026-01-10 08:00:00'
    WHERE @province_id IS NOT NULL
  AND @ward_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'employee2');

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
SELECT 'buyer_locked', 'buyer.locked@acm.local', '0905555001', 'Phạm Văn Buyer Khóa',
       '$2a$10$BzROX8TgxrKpb./sQD9w..VmxFh1AJjAQAH8mxhJfdmpb2C7aWLIy',
       'LOCKED', @province_id, @ward_id, '2026-02-01 08:00:00'
    WHERE @province_id IS NOT NULL
  AND @ward_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users WHERE user_name = 'buyer_locked');

SET @farmer2_user_id := (SELECT user_id FROM users WHERE user_name = 'farmer2' LIMIT 1);
SET @employee2_user_id := (SELECT user_id FROM users WHERE user_name = 'employee2' LIMIT 1);
SET @buyer_locked_user_id := (SELECT user_id FROM users WHERE user_name = 'buyer_locked' LIMIT 1);

INSERT INTO user_roles (user_id, role_id)
SELECT @farmer2_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'FARMER'
  AND @farmer2_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @farmer2_user_id AND role_id = r.role_id);

INSERT INTO user_roles (user_id, role_id)
SELECT @employee2_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'EMPLOYEE'
  AND @employee2_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @employee2_user_id AND role_id = r.role_id);

INSERT INTO user_roles (user_id, role_id)
SELECT @buyer_locked_user_id, r.role_id
FROM roles r
WHERE r.role_code = 'BUYER'
  AND @buyer_locked_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @buyer_locked_user_id AND role_id = r.role_id);

SET @rice_crop_id := (
    SELECT crop_id
    FROM crops
    WHERE crop_name IN ('Rice', 'Lúa nước', 'Lúa')
       OR LOWER(crop_name) LIKE '%rice%'
       OR crop_name LIKE '%lúa%'
    ORDER BY crop_id
    LIMIT 1
);
SET @rice_variety_id := (SELECT id FROM varieties WHERE crop_id = @rice_crop_id ORDER BY id LIMIT 1);

-- Corn is resolved separately so corn demo data does not point to the rice crop by mistake.
-- Fallback to rice keeps the script runnable on databases that only include the baseline rice crop.
SET @corn_crop_id := (
    SELECT crop_id
    FROM crops
    WHERE crop_name IN ('Corn', 'Maize', 'Ngô', 'Bắp')
       OR LOWER(crop_name) LIKE '%corn%'
       OR LOWER(crop_name) LIKE '%maize%'
       OR crop_name LIKE '%ngô%'
       OR crop_name LIKE '%bắp%'
    ORDER BY crop_id
    LIMIT 1
);
SET @corn_crop_id := COALESCE(@corn_crop_id, @rice_crop_id);
SET @corn_variety_id := COALESCE(
        (SELECT id FROM varieties WHERE crop_id = @corn_crop_id ORDER BY id LIMIT 1),
        @rice_variety_id
                        );

INSERT INTO farms (user_id, farm_name, province_id, ward_id, area, active)
SELECT @farmer_user_id, 'Nông trại Phú Điền', @province_id, @ward_id, 15.50, TRUE
    WHERE @farmer_user_id IS NOT NULL
  AND @province_id IS NOT NULL
  AND @ward_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM farms WHERE user_id = @farmer_user_id AND farm_name = 'Nông trại Phú Điền');

INSERT INTO farms (user_id, farm_name, province_id, ward_id, area, active)
SELECT @farmer2_user_id, 'Nông trại Cao Nguyên Xanh', @province_id, @ward_id, 12.80, TRUE
    WHERE @farmer2_user_id IS NOT NULL
  AND @province_id IS NOT NULL
  AND @ward_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM farms WHERE user_id = @farmer2_user_id AND farm_name = 'Nông trại Cao Nguyên Xanh');

SET @farm_id := (SELECT farm_id FROM farms WHERE user_id = @farmer_user_id AND farm_name = 'Nông trại Phú Điền' LIMIT 1);
SET @farm2_id := (SELECT farm_id FROM farms WHERE user_id = @farmer2_user_id AND farm_name = 'Nông trại Cao Nguyên Xanh' LIMIT 1);

INSERT INTO plots (farm_id, plot_name, area, soil_type, status, created_by, created_at, updated_at)
SELECT @farm_id, 'Lô A1 - Demo', 2.50, 'LOAM', 'IN_USE', @farmer_user_id, @seed_now, @seed_now
    WHERE @farm_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM plots WHERE farm_id = @farm_id AND plot_name = 'Lô A1 - Demo');

INSERT INTO plots (farm_id, plot_name, area, soil_type, status, created_by, created_at, updated_at)
SELECT @farm2_id, 'Lô C1 - Demo', 4.60, 'LOAM', 'IN_USE', @farmer2_user_id, @seed_now, @seed_now
    WHERE @farm2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM plots WHERE farm_id = @farm2_id AND plot_name = 'Lô C1 - Demo');

SET @plot_id := (SELECT plot_id FROM plots WHERE farm_id = @farm_id AND plot_name = 'Lô A1 - Demo' LIMIT 1);
SET @plot2_id := (SELECT plot_id FROM plots WHERE farm_id = @farm2_id AND plot_name = 'Lô C1 - Demo' LIMIT 1);

INSERT INTO seasons (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
                     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
SELECT '2026 - Vụ Lúa Hè Demo', @plot_id, @rice_crop_id, @rice_variety_id,
       '2026-04-20', '2026-07-25', NULL, 'ACTIVE',
       220000, 218000, 3300.00, NULL, 16500000.00, 'Vụ lúa Hè 2026 dùng để kiểm thử mùa vụ, công việc, chi phí và cảnh báo', @seed_now
    WHERE @plot_id IS NOT NULL
  AND @rice_crop_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM seasons WHERE plot_id = @plot_id AND season_name = '2026 - Vụ Lúa Hè Demo');

INSERT INTO seasons (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
                     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
SELECT '2026 - Vụ Ngô Cao Nguyên Demo', @plot2_id, @corn_crop_id, @corn_variety_id,
       '2026-03-01', '2026-06-15', '2026-06-20', 'COMPLETED',
       18000, 17500, 2600.00, 2650.00, 8000000.00, 'Vụ ngô đã hoàn tất, dùng để kiểm thử tồn kho nông sản và marketplace', @seed_now
    WHERE @plot2_id IS NOT NULL
  AND @corn_crop_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM seasons WHERE plot_id = @plot2_id AND season_name = '2026 - Vụ Ngô Cao Nguyên Demo');

SET @season_id := (SELECT season_id FROM seasons WHERE plot_id = @plot_id AND season_name = '2026 - Vụ Lúa Hè Demo' LIMIT 1);
SET @season2_id := (SELECT season_id FROM seasons WHERE plot_id = @plot2_id AND season_name = '2026 - Vụ Ngô Cao Nguyên Demo' LIMIT 1);

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
SELECT @employee_user_id, @season_id, 'Kiểm tra hệ thống tưới demo', 'Bảo trì đường ống và đầu tưới',
       '2026-05-01', '2026-05-05', 'IN_PROGRESS', '2026-05-01', NULL, 'Đang thực hiện', @seed_now
    WHERE @employee_user_id IS NOT NULL
  AND @season_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tasks WHERE season_id = @season_id AND title = 'Kiểm tra hệ thống tưới demo');

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
SELECT @employee2_user_id, @season_id, 'Thu hoạch thử nghiệm demo', 'Thu sớm cho kênh online',
       '2026-06-01', '2026-06-03', 'PENDING', NULL, NULL, 'Chờ đến kỳ thu hoạch', @seed_now
    WHERE @employee2_user_id IS NOT NULL
  AND @season_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tasks WHERE season_id = @season_id AND title = 'Thu hoạch thử nghiệm demo');

SET @task_id := (SELECT task_id FROM tasks WHERE season_id = @season_id AND title = 'Kiểm tra hệ thống tưới demo' LIMIT 1);

INSERT INTO expenses (user_id, season_id, task_id, category, item_name, unit_price, quantity, total_cost, amount, payment_status, note, expense_date, created_at)
SELECT @farmer_user_id, @season_id, @task_id, 'OTHER', 'Sửa đường ống tưới',
       320000.00, 2, 640000.00, 640000.00, 'PENDING', 'Chi phí demo', '2026-05-02', @seed_now
    WHERE @farmer_user_id IS NOT NULL
  AND @season_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM expenses WHERE season_id = @season_id AND item_name = 'Sửa đường ống tưới' AND expense_date = '2026-05-02');

INSERT INTO harvests (season_id, harvest_date, quantity, unit, grade, note, created_at)
SELECT @season2_id, '2026-06-20', 940.00, 'kg', 'A', 'Thu hoạch ngô loại A cho marketplace', @seed_now
    WHERE @season2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM harvests WHERE season_id = @season2_id AND harvest_date = '2026-06-20' AND note = 'Thu hoạch ngô loại A cho marketplace');

SET @harvest2_id := (SELECT harvest_id FROM harvests WHERE season_id = @season2_id AND harvest_date = '2026-06-20' AND note = 'Thu hoạch ngô loại A cho marketplace' LIMIT 1);

INSERT INTO suppliers (name, license_no, contact_email, contact_phone)
SELECT 'Công ty TNHH Vật tư Nông nghiệp Xanh', 'BN-DEMO-001', 'vattuxanh@acm.local', '02743855666'
    WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'Công ty TNHH Vật tư Nông nghiệp Xanh');

SET @supplier_id := (SELECT id FROM suppliers WHERE name = 'Công ty TNHH Vật tư Nông nghiệp Xanh' LIMIT 1);

INSERT INTO supply_items (name, active_ingredient, unit, restricted_flag)
SELECT 'Phân NPK 16-16-8 Demo', 'N-P-K 16-16-8', 'kg', FALSE
    WHERE NOT EXISTS (SELECT 1 FROM supply_items WHERE name = 'Phân NPK 16-16-8 Demo');

SET @supply_item_id := (SELECT id FROM supply_items WHERE name = 'Phân NPK 16-16-8 Demo' LIMIT 1);

INSERT INTO supply_lots (supply_item_id, supplier_id, batch_code, expiry_date, status)
SELECT @supply_item_id, @supplier_id, 'NPK-DEMO-2026-01', '2027-12-31', 'IN_STOCK'
    WHERE @supply_item_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM supply_lots WHERE batch_code = 'NPK-DEMO-2026-01');

SET @supply_lot_id := (SELECT id FROM supply_lots WHERE batch_code = 'NPK-DEMO-2026-01' LIMIT 1);

INSERT INTO warehouses (farm_id, name, type, province_id, ward_id)
SELECT @farm_id, 'Kho vật tư demo', 'INPUT', @province_id, @ward_id
    WHERE @farm_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM warehouses WHERE farm_id = @farm_id AND name = 'Kho vật tư demo');

INSERT INTO warehouses (farm_id, name, type, province_id, ward_id)
SELECT @farm2_id, 'Kho nông sản demo', 'OUTPUT', @province_id, @ward_id
    WHERE @farm2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM warehouses WHERE farm_id = @farm2_id AND name = 'Kho nông sản demo');

SET @input_warehouse_id := (SELECT id FROM warehouses WHERE farm_id = @farm_id AND name = 'Kho vật tư demo' LIMIT 1);
SET @output_warehouse_id := (SELECT id FROM warehouses WHERE farm_id = @farm2_id AND name = 'Kho nông sản demo' LIMIT 1);

INSERT INTO stock_locations (warehouse_id, zone, aisle, shelf, bin)
SELECT @input_warehouse_id, 'Khu A', 'Hàng 1', 'Kệ 1', 'Ô 1'
    WHERE @input_warehouse_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM stock_locations WHERE warehouse_id = @input_warehouse_id AND zone = 'Khu A' AND aisle = 'Hàng 1' AND shelf = 'Kệ 1' AND bin = 'Ô 1');

INSERT INTO stock_locations (warehouse_id, zone, aisle, shelf, bin)
SELECT @output_warehouse_id, 'Khu C', 'Hàng 1', 'Kệ 1', 'Ô 1'
    WHERE @output_warehouse_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM stock_locations WHERE warehouse_id = @output_warehouse_id AND zone = 'Khu C' AND aisle = 'Hàng 1' AND shelf = 'Kệ 1' AND bin = 'Ô 1');

SET @input_location_id := (SELECT id FROM stock_locations WHERE warehouse_id = @input_warehouse_id AND zone = 'Khu A' AND aisle = 'Hàng 1' AND shelf = 'Kệ 1' AND bin = 'Ô 1' LIMIT 1);
SET @output_location_id := (SELECT id FROM stock_locations WHERE warehouse_id = @output_warehouse_id AND zone = 'Khu C' AND aisle = 'Hàng 1' AND shelf = 'Kệ 1' AND bin = 'Ô 1' LIMIT 1);

INSERT INTO stock_movements (supply_lot_id, warehouse_id, location_id, movement_type, quantity, movement_date, season_id, task_id, note)
SELECT @supply_lot_id, @input_warehouse_id, @input_location_id, 'IN', 200.000, '2026-05-01 08:00:00', NULL, NULL, 'Initial demo intake'
    WHERE @supply_lot_id IS NOT NULL
  AND @input_warehouse_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM stock_movements WHERE supply_lot_id = @supply_lot_id AND warehouse_id = @input_warehouse_id AND movement_type = 'IN' AND note = 'Initial demo intake');

INSERT INTO inventory_balances (supply_lot_id, warehouse_id, location_id, quantity)
SELECT @supply_lot_id, @input_warehouse_id, @input_location_id, 200.000
    WHERE @supply_lot_id IS NOT NULL
  AND @input_warehouse_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM inventory_balances WHERE supply_lot_id = @supply_lot_id AND warehouse_id = @input_warehouse_id AND location_id = @input_location_id);

INSERT INTO documents (title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type, view_count, is_pinned, created_at, updated_at)
SELECT 'Quy trình canh tác lúa demo', 'https://www.irri.org/rice-production-manual', 'Tài liệu tham khảo demo về quy trình canh tác lúa, dùng để kiểm tra module tài liệu.',
       'Rice', 'ALL', 'GUIDE', TRUE, TRUE, @admin_user_id, 'GUIDE', 0, TRUE, @seed_now, @seed_now
    WHERE @admin_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM documents WHERE title = 'Quy trình canh tác lúa demo');

SET @document_id := (SELECT document_id FROM documents WHERE title = 'Quy trình canh tác lúa demo' LIMIT 1);

INSERT INTO document_favorites (user_id, document_id, created_at)
SELECT @farmer_user_id, @document_id, @seed_now
    WHERE @farmer_user_id IS NOT NULL
  AND @document_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM document_favorites WHERE user_id = @farmer_user_id AND document_id = @document_id);

INSERT INTO document_recent_opens (user_id, document_id, opened_at)
SELECT @farmer_user_id, @document_id, @seed_now
    WHERE @farmer_user_id IS NOT NULL
  AND @document_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM document_recent_opens WHERE user_id = @farmer_user_id AND document_id = @document_id AND opened_at = @seed_now);

INSERT INTO user_preferences (user_id, currency_code, weight_unit, locale, created_at, updated_at)
SELECT @farmer2_user_id, 'VND', 'kg', 'vi-VN', @seed_now, @seed_now
    WHERE @farmer2_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_preferences WHERE user_id = @farmer2_user_id);

INSERT INTO incidents (season_id, reported_by, incident_type, severity, status, description, deadline, resolved_at, created_at)
SELECT @season_id, @farmer_user_id, 'DISEASE', 'HIGH', 'OPEN', 'Đốm lá xuất hiện trên lô demo', '2026-05-10', NULL, @seed_now
    WHERE @season_id IS NOT NULL
  AND @farmer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM incidents WHERE season_id = @season_id AND description = 'Đốm lá xuất hiện trên lô demo');

INSERT INTO field_logs (season_id, log_date, log_type, notes, created_at)
SELECT @season_id, '2026-05-05', 'IRRIGATE', 'Tưới demo sau gieo sạ', @seed_now
    WHERE @season_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM field_logs WHERE season_id = @season_id AND log_date = '2026-05-05' AND log_type = 'IRRIGATE');

INSERT INTO crop_nitrogen_references (crop_id, n_content_kg_per_kg_yield, source_reference, active, created_at, updated_at)
SELECT @rice_crop_id, 0.013500, 'VN-RICE-DEMO-N-REF-2026', TRUE, @seed_now, @seed_now
    WHERE @rice_crop_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM crop_nitrogen_references WHERE crop_id = @rice_crop_id AND source_reference = 'VN-RICE-DEMO-N-REF-2026');

INSERT INTO nutrient_input_events (season_id, plot_id, input_source, n_kg, applied_date, measured, data_source, source_type, source_document, created_by_user_id, note, created_at)
SELECT @season_id, @plot_id, 'MINERAL_FERTILIZER', 85.0000, '2026-05-01', TRUE, 'user_entered', 'USER_ENTERED', NULL, @farmer_user_id, 'Demo mineral N input', @seed_now
    WHERE @season_id IS NOT NULL
  AND @plot_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM nutrient_input_events WHERE season_id = @season_id AND input_source = 'MINERAL_FERTILIZER' AND applied_date = '2026-05-01' AND note = 'Demo mineral N input');

SET @nutrient_event_id := (SELECT id FROM nutrient_input_events WHERE season_id = @season_id AND input_source = 'MINERAL_FERTILIZER' AND applied_date = '2026-05-01' AND note = 'Demo mineral N input' LIMIT 1);

INSERT INTO irrigation_water_analyses (season_id, plot_id, sample_date, nitrate_mg_per_l, ammonium_mg_per_l, total_n_mg_per_l, irrigation_volume_m3,
                                       legacy_n_contribution_kg, legacy_event_id, legacy_derived, measured, source_type, source_document,
                                       lab_reference, note, created_by_user_id, created_at)
SELECT @season_id, @plot_id, '2026-05-02', 7.5000, 1.5000, NULL, 1100.0000,
       NULL, NULL, FALSE, TRUE, 'LAB_MEASURED', 'https://www.fao.org/4/t7202e/t7202e.pdf',
       'LAB-DEMO-IRR-260502', 'Mẫu nước tưới demo', @farmer_user_id, @seed_now
    WHERE @season_id IS NOT NULL
  AND @plot_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM irrigation_water_analyses WHERE season_id = @season_id AND plot_id = @plot_id AND sample_date = '2026-05-02' AND lab_reference = 'LAB-DEMO-IRR-260502');

INSERT INTO soil_tests (season_id, plot_id, sample_date, soil_organic_matter_pct, mineral_n_kg_per_ha, nitrate_mg_per_kg, ammonium_mg_per_kg,
                        legacy_n_contribution_kg, legacy_event_id, legacy_derived, measured, source_type, source_document,
                        lab_reference, note, created_by_user_id, created_at)
SELECT @season_id, @plot_id, '2026-05-03', 3.2000, 11.0000, 14.0000, 4.0000,
       NULL, NULL, FALSE, TRUE, 'LAB_MEASURED', 'https://www.fao.org/3/i3794en/I3794en.pdf',
       'LAB-DEMO-SOIL-260503', 'Mẫu đất demo', @farmer_user_id, @seed_now
    WHERE @season_id IS NOT NULL
  AND @plot_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM soil_tests WHERE season_id = @season_id AND plot_id = @plot_id AND sample_date = '2026-05-03' AND lab_reference = 'LAB-DEMO-SOIL-260503');

INSERT INTO season_employees (season_id, employee_user_id, added_by_user_id, wage_per_task, active, created_at)
SELECT @season_id, @employee_user_id, @farmer_user_id, 210000.00, b'1', @seed_now
    WHERE @season_id IS NOT NULL
  AND @employee_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM season_employees WHERE season_id = @season_id AND employee_user_id = @employee_user_id);

INSERT INTO task_progress_logs (task_id, employee_user_id, progress_percent, note, evidence_url, logged_at)
SELECT @task_id, @employee_user_id, 75, 'Cập nhật tiến độ demo', 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=80', @seed_now
    WHERE @task_id IS NOT NULL
  AND @employee_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM task_progress_logs WHERE task_id = @task_id AND employee_user_id = @employee_user_id AND progress_percent = 75);

INSERT INTO payroll_records (employee_user_id, season_id, period_start, period_end, total_assigned_tasks, total_completed_tasks, wage_per_task, total_amount, generated_at, note)
SELECT @employee_user_id, @season_id, '2026-05-01', '2026-05-31', 2, 1, 210000.00, 210000.00, @seed_now, 'Lương demo tháng 05/2026'
    WHERE @employee_user_id IS NOT NULL
  AND @season_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM payroll_records WHERE employee_user_id = @employee_user_id AND season_id = @season_id AND period_start = '2026-05-01' AND period_end = '2026-05-31');

INSERT INTO product_warehouse_lots (lot_code, product_id, product_name, product_variant, season_id, farm_id, plot_id, harvest_id, warehouse_id, location_id,
                                    harvested_at, received_at, unit, initial_quantity, on_hand_quantity, grade, quality_status, traceability_data, note, status,
                                    created_by, created_at, updated_at)
SELECT 'LOT-CORN-DEMO-2026', 5001, 'Ngô ngọt Cao Nguyên Xanh', 'Loại A', @season2_id, @farm2_id, @plot2_id, @harvest2_id, @output_warehouse_id, @output_location_id,
       '2026-06-20', '2026-06-20 18:00:00', 'kg', 940.000, 900.000, 'A', 'FRESH',
       '{"route":"plot-C1->output-warehouse","crop":"corn","grade":"A"}', 'Lô demo cho marketplace', 'IN_STOCK', @farmer2_user_id, @seed_now, @seed_now
    WHERE @season2_id IS NOT NULL
  AND @farm2_id IS NOT NULL
  AND @plot2_id IS NOT NULL
  AND @harvest2_id IS NOT NULL
  AND @output_warehouse_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM product_warehouse_lots WHERE lot_code = 'LOT-CORN-DEMO-2026');

SET @product_lot_id := (SELECT id FROM product_warehouse_lots WHERE lot_code = 'LOT-CORN-DEMO-2026' LIMIT 1);

INSERT INTO product_warehouse_transactions (lot_id, transaction_type, quantity, unit, resulting_on_hand, reference_type, reference_id, note, created_by, created_at)
SELECT @product_lot_id, 'RECEIPT_FROM_HARVEST', 940.000, 'kg', 940.000, 'HARVEST', CAST(@harvest2_id AS CHAR), 'Receipt from demo harvest', @farmer2_user_id, @seed_now
    WHERE @product_lot_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM product_warehouse_transactions WHERE lot_id = @product_lot_id AND transaction_type = 'RECEIPT_FROM_HARVEST' AND reference_type = 'HARVEST');

INSERT INTO marketplace_products (slug, name, category, short_description, description, price, unit, stock_quantity, image_url, image_urls_json,
                                  farmer_user_id, farm_id, season_id, lot_id, traceable, status, published_at, created_at, updated_at)
SELECT 'ngo-ngot-cao-nguyen-xanh-demo', 'Ngô ngọt Cao Nguyên Xanh Demo', 'CORN',
       'Ngô ngọt loại A, thu hoạch từ lô C1, phù hợp bán lẻ và kiểm thử đơn hàng.',
       'Sản phẩm demo có truy xuất nguồn gốc từ Nông trại Cao Nguyên Xanh. Dữ liệu dùng để test catalog, giỏ hàng, đơn hàng, đánh giá và tồn kho marketplace.',
       170000.00, 'kg', 900.000,
       'https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=1200&q=80',
       '["https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=1200&q=80","https://images.unsplash.com/photo-1601593768797-76d085bc9b09?auto=format&fit=crop&w=1200&q=80"]',
       @farmer2_user_id, @farm2_id, @season2_id, @product_lot_id, TRUE, 'ACTIVE', '2026-05-05 08:00:00', @seed_now, @seed_now
    WHERE @farmer2_user_id IS NOT NULL
  AND @farm2_id IS NOT NULL
  AND @season2_id IS NOT NULL
  AND @product_lot_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_products WHERE slug = 'ngo-ngot-cao-nguyen-xanh-demo');

SET @marketplace_product_id := (SELECT id FROM marketplace_products WHERE slug = 'ngo-ngot-cao-nguyen-xanh-demo' LIMIT 1);

INSERT INTO marketplace_carts (user_id, created_at, updated_at)
SELECT @buyer_user_id, @seed_now, @seed_now
    WHERE @buyer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_carts WHERE user_id = @buyer_user_id);

SET @cart_id := (SELECT id FROM marketplace_carts WHERE user_id = @buyer_user_id LIMIT 1);

INSERT INTO marketplace_cart_items (cart_id, product_id, quantity, unit_price_snapshot, created_at, updated_at)
SELECT @cart_id, @marketplace_product_id, 1.500, 170000.00, @seed_now, @seed_now
    WHERE @cart_id IS NOT NULL
  AND @marketplace_product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_cart_items WHERE cart_id = @cart_id AND product_id = @marketplace_product_id);

INSERT INTO marketplace_addresses (user_id, full_name, phone, province, district, ward, street, detail, label, is_default, created_at, updated_at)
SELECT @buyer_user_id, 'Trần Thị Buyer', '0903234000', 'Đồng Tháp', 'Cao Lãnh', 'Mỹ An', '123 Đường Demo', 'Căn góc, gần trường học', 'home', TRUE, @seed_now, @seed_now
    WHERE @buyer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_addresses WHERE user_id = @buyer_user_id AND label = 'home' AND street = '123 Đường Demo');

INSERT INTO marketplace_order_groups (group_code, buyer_user_id, idempotency_key, request_fingerprint, created_at)
SELECT 'MOG-DEMO-2026-0001', @buyer_user_id, 'demo-order-20260505', 'fp-demo-order-20260505', @seed_now
    WHERE @buyer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_order_groups WHERE group_code = 'MOG-DEMO-2026-0001');

SET @order_group_id := (SELECT id FROM marketplace_order_groups WHERE group_code = 'MOG-DEMO-2026-0001' LIMIT 1);

INSERT INTO marketplace_orders (order_group_id, order_code, buyer_user_id, farmer_user_id, status, payment_method, payment_verification_status,
                                payment_proof_file_name, payment_proof_content_type, payment_proof_storage_path, payment_proof_uploaded_at,
                                payment_verified_at, payment_verified_by_user_id, payment_verification_note, shipping_recipient_name, shipping_phone,
                                shipping_address_line, note, subtotal, shipping_fee, total_amount, created_at, updated_at)
SELECT @order_group_id, 'MPO-DEMO-2026-0001', @buyer_user_id, @farmer2_user_id,
       'DELIVERED', 'COD', 'NOT_REQUIRED', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
       'Trần Thị Buyer', '0903234000', '123 Đường Demo, Mỹ An, Cao Lãnh, Đồng Tháp',
       'Đơn COD đã giao để kiểm thử luồng đánh giá sản phẩm.', 255000.00, 20000.00, 275000.00, @seed_now, @seed_now
    WHERE @order_group_id IS NOT NULL
  AND @buyer_user_id IS NOT NULL
  AND @farmer2_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_orders WHERE order_code = 'MPO-DEMO-2026-0001');

SET @order_id := (SELECT id FROM marketplace_orders WHERE order_code = 'MPO-DEMO-2026-0001' LIMIT 1);

INSERT INTO marketplace_order_items (order_id, product_id, product_name_snapshot, product_slug_snapshot, image_url_snapshot, unit_price_snapshot,
                                     quantity, line_total, traceable_snapshot, farm_id, season_id, lot_id)
SELECT @order_id, @marketplace_product_id, 'Ngô ngọt Cao Nguyên Xanh Demo', 'ngo-ngot-cao-nguyen-xanh-demo', 'https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=1200&q=80',
       170000.00, 1.500, 255000.00, TRUE, @farm2_id, @season2_id, @product_lot_id
    WHERE @order_id IS NOT NULL
  AND @marketplace_product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_order_items WHERE order_id = @order_id AND product_id = @marketplace_product_id);

SET @order_item_id := (SELECT id FROM marketplace_order_items WHERE order_id = @order_id AND product_id = @marketplace_product_id LIMIT 1);

INSERT INTO marketplace_product_reviews (product_id, order_id, order_item_id, buyer_user_id, rating, comment, created_at, updated_at, hidden)
SELECT @marketplace_product_id, @order_id, @order_item_id, @buyer_user_id, 5, 'Ngô tươi, hạt đều và giao đúng số lượng. Phù hợp để kiểm thử đánh giá 5 sao.', @seed_now, @seed_now, FALSE
    WHERE @marketplace_product_id IS NOT NULL
  AND @order_id IS NOT NULL
  AND @order_item_id IS NOT NULL
  AND @buyer_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM marketplace_product_reviews WHERE order_item_id = @order_item_id AND buyer_user_id = @buyer_user_id);

UPDATE marketplace_products p
SET p.average_rating = COALESCE((SELECT AVG(r.rating) FROM marketplace_product_reviews r WHERE r.product_id = p.id AND r.hidden = FALSE), 0),
    p.rating_count = COALESCE((SELECT COUNT(*) FROM marketplace_product_reviews r WHERE r.product_id = p.id AND r.hidden = FALSE), 0)
WHERE p.id = @marketplace_product_id;

UPDATE farms f
SET f.average_rating = COALESCE((
                                    SELECT AVG(r.rating)
                                    FROM marketplace_product_reviews r
                                             JOIN marketplace_products p ON p.id = r.product_id
                                    WHERE p.farm_id = f.farm_id AND r.hidden = FALSE
                                ), 0),
    f.rating_count = COALESCE((
                                  SELECT COUNT(*)
                                  FROM marketplace_product_reviews r
                                           JOIN marketplace_products p ON p.id = r.product_id
                                  WHERE p.farm_id = f.farm_id AND r.hidden = FALSE
                              ), 0)
WHERE f.farm_id = @farm2_id;

INSERT INTO alerts (type, severity, status, farm_id, season_id, plot_id, crop_id, title, message, suggested_action_type, suggested_action_url, recipient_farmer_ids, created_at, sent_at)
SELECT 'TASK_OVERDUE', 'HIGH', 'NEW', @farm_id, @season_id, @plot_id, @rice_crop_id,
       'Công việc demo cần xử lý', 'Task demo đang cần cập nhật tiến độ', 'VIEW_TASK', CONCAT('/tasks/', @task_id), CAST(@farmer_user_id AS CHAR), @seed_now, NULL
    WHERE @farm_id IS NOT NULL
  AND @season_id IS NOT NULL
  AND @plot_id IS NOT NULL
  AND @task_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM alerts WHERE season_id = @season_id AND title = 'Công việc demo cần xử lý');

SET @alert_id := (SELECT id FROM alerts WHERE season_id = @season_id AND title = 'Công việc demo cần xử lý' LIMIT 1);

INSERT INTO notifications (user_id, title, message, link, alert_id, created_at, read_at)
SELECT @farmer_user_id, 'Công việc demo cần xử lý', 'Task demo đang cần cập nhật tiến độ', CONCAT('/tasks/', @task_id), @alert_id, @seed_now, NULL
    WHERE @farmer_user_id IS NOT NULL
  AND @alert_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM notifications WHERE user_id = @farmer_user_id AND alert_id = @alert_id);

INSERT INTO audit_logs (entity_type, entity_id, operation, performed_by, performed_at, snapshot_data, reason, ip_address)
SELECT 'MARKETPLACE_ORDER', @order_id, 'CREATE', 'buyer', @seed_now,
       JSON_OBJECT('status', 'DELIVERED', 'paymentMethod', 'COD', 'orderCode', 'MPO-DEMO-2026-0001'),
       'Tạo đơn demo sau Flyway', '127.0.0.1'
    WHERE @order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM audit_logs WHERE entity_type = 'MARKETPLACE_ORDER' AND entity_id = @order_id AND operation = 'CREATE');
