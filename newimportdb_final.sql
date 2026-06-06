-- =========================================================
-- ACM FINAL SEED-ONLY DATA
-- Includes merged FDN supplement storyline for farmer@acm.local
-- Entity-first / Hibernate-owned schema
-- Import after running the backend once with profile dev-reset
-- Main login accounts are created by ApplicationInitConfig:
--   admin@acm.local / admin123
--   farmer@acm.local / 12345678
--   employee@acm.local / 12345678
--   buyer@acm.local / 12345678
-- Additional seed-only actor accounts use password: Password@123
-- Hash algorithm: BCrypt strength 10
-- =========================================================

USE quanlymuavu;

SET NAMES utf8mb4;
SET @seed_password_hash = '$2a$10$EljjgW0yQXhSfXIlLWZikusZPXWBXBt6q9oNo9BjxPXWDDIGzQaTW';
SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- =========================================================
-- 0. Safety settings and seed-owned cleanup
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM marketplace_product_reviews WHERE id IS NOT NULL;
DELETE FROM marketplace_order_items WHERE id IS NOT NULL;
DELETE FROM marketplace_orders WHERE id IS NOT NULL;
DELETE FROM marketplace_order_groups WHERE id IS NOT NULL;
DELETE FROM marketplace_cart_items WHERE id IS NOT NULL;
DELETE FROM marketplace_carts WHERE id IS NOT NULL;
DELETE FROM marketplace_addresses WHERE id IS NOT NULL;
DELETE FROM marketplace_products WHERE id IS NOT NULL;

DELETE FROM product_warehouse_transactions WHERE id IS NOT NULL;
DELETE FROM product_warehouse_lots WHERE id IS NOT NULL;

DELETE FROM payroll_records WHERE id IS NOT NULL;
DELETE FROM task_progress_logs WHERE id IS NOT NULL;
DELETE FROM season_employees WHERE id IS NOT NULL;
DELETE FROM disease_treatments WHERE disease_treatment_id IS NOT NULL;
DELETE FROM disease_records WHERE disease_record_id IS NOT NULL;

DELETE FROM soil_tests WHERE id IS NOT NULL;
DELETE FROM irrigation_water_analyses WHERE id IS NOT NULL;
DELETE FROM nutrient_input_events WHERE id IS NOT NULL;
DELETE FROM crop_nitrogen_references WHERE id IS NOT NULL;

DELETE FROM document_recent_opens WHERE id IS NOT NULL;
DELETE FROM document_favorites WHERE id IS NOT NULL;
DELETE FROM documents WHERE document_id IS NOT NULL;
DELETE FROM notifications WHERE id IS NOT NULL;
DELETE FROM alerts WHERE id IS NOT NULL;
DELETE FROM incidents WHERE id IS NOT NULL;

DELETE FROM inventory_balances WHERE id IS NOT NULL;
DELETE FROM stock_movements WHERE id IS NOT NULL;
DELETE FROM stock_locations WHERE id IS NOT NULL;
DELETE FROM warehouses WHERE id IS NOT NULL;
DELETE FROM supply_lots WHERE id IS NOT NULL;
DELETE FROM supply_items WHERE id IS NOT NULL;
DELETE FROM suppliers WHERE id IS NOT NULL;

DELETE FROM expenses WHERE expense_id IS NOT NULL;
DELETE FROM harvests WHERE harvest_id IS NOT NULL;
DELETE FROM field_logs WHERE field_log_id IS NOT NULL;
DELETE FROM tasks WHERE task_id IS NOT NULL;
DELETE FROM seasons WHERE season_id IS NOT NULL;
DELETE FROM plots WHERE plot_id IS NOT NULL;
DELETE FROM farms WHERE farm_id IS NOT NULL;

DELETE FROM password_reset_tokens WHERE id IS NOT NULL;
DELETE FROM user_preferences WHERE id IS NOT NULL;
DELETE ur
FROM user_roles ur
JOIN users u ON u.user_id = ur.user_id
WHERE u.email IN (
    'farmer.binhminh@example.com',
    'employee.lanthao@example.com',
    'buyer.thucphamantoan@example.com'
)
  AND ur.user_id IS NOT NULL
  AND ur.role_id IS NOT NULL;
DELETE FROM users WHERE email IN (
    'farmer.binhminh@example.com',
    'employee.lanthao@example.com',
    'buyer.thucphamantoan@example.com'
);

DELETE FROM varieties WHERE id IS NOT NULL;
DELETE FROM crops WHERE crop_id IS NOT NULL;
DELETE FROM wards WHERE id IS NOT NULL;
DELETE FROM provinces WHERE id IS NOT NULL;

DELETE FROM audit_logs WHERE audit_log_id IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. Reference data
-- =========================================================

INSERT IGNORE INTO provinces (id, name, slug, type, name_with_type)
VALUES
    (79, 'Hồ Chí Minh', 'ho-chi-minh', 'thanh-pho', 'Thành phố Hồ Chí Minh'),
    (87, 'Đồng Tháp', 'dong-thap', 'tinh', 'Tỉnh Đồng Tháp'),
    (92, 'Cần Thơ', 'can-tho', 'thanh-pho', 'Thành phố Cần Thơ');

INSERT IGNORE INTO wards (id, name, slug, type, name_with_type, province_id)
VALUES
    (79001, 'Tân Phú', 'tan-phu', 'phuong', 'Phường Tân Phú', 79),
    (87001, 'Mỹ An', 'my-an', 'phuong', 'Phường Mỹ An', 87),
    (87002, 'An Phú Thuận', 'an-phu-thuan', 'xa', 'Xã An Phú Thuận', 87),
    (92001, 'Thới Lai', 'thoi-lai', 'phuong', 'Phường Thới Lai', 92);

SELECT @role_admin_id := role_id FROM roles WHERE role_code = 'ADMIN' LIMIT 1;
SELECT @role_farmer_id := role_id FROM roles WHERE role_code = 'FARMER' LIMIT 1;
SELECT @role_employee_id := role_id FROM roles WHERE role_code = 'EMPLOYEE' LIMIT 1;
SELECT @role_buyer_id := role_id FROM roles WHERE role_code = 'BUYER' LIMIT 1;

INSERT INTO crops (crop_name, description)
VALUES ('Lúa', 'Nhóm cây lương thực chủ lực tại Đồng bằng sông Cửu Long');
SET @crop_rice_id = LAST_INSERT_ID();

INSERT INTO crops (crop_name, description)
VALUES ('Ngô', 'Cây luân canh sau lúa, phù hợp đất phù sa nhẹ');
SET @crop_corn_id = LAST_INSERT_ID();

INSERT INTO crops (crop_name, description)
VALUES ('Rau ăn lá', 'Nhóm rau ngắn ngày phục vụ chuỗi cửa hàng thực phẩm an toàn');
SET @crop_leafy_id = LAST_INSERT_ID();

INSERT INTO crops (crop_name, description)
VALUES ('Đậu nành', 'Cây họ đậu cải tạo đất và bổ sung đạm sinh học');
SET @crop_soy_id = LAST_INSERT_ID();

INSERT INTO varieties (crop_id, name, description)
VALUES (@crop_rice_id, 'ST25', 'Giống lúa thơm chất lượng cao, phù hợp sản xuất gạo đặc sản');
SET @variety_st25_id = LAST_INSERT_ID();

INSERT INTO varieties (crop_id, name, description)
VALUES (@crop_rice_id, 'OM5451', 'Giống lúa ngắn ngày, năng suất ổn định');
SET @variety_om5451_id = LAST_INSERT_ID();

INSERT INTO varieties (crop_id, name, description)
VALUES (@crop_corn_id, 'LVN10', 'Giống ngô lai dùng cho luân canh sau vụ lúa');
SET @variety_corn_lvn10_id = LAST_INSERT_ID();

INSERT INTO varieties (crop_id, name, description)
VALUES (@crop_leafy_id, 'Cải xanh mưa sớm', 'Giống rau ăn lá ưa ẩm, chu kỳ thu hoạch ngắn');
SET @variety_leafy_id = LAST_INSERT_ID();

INSERT INTO varieties (crop_id, name, description)
VALUES (@crop_soy_id, 'AGS398', 'Giống đậu nành phục hồi đất và giảm phụ thuộc phân đạm khoáng');
SET @variety_soy_ags398_id = LAST_INSERT_ID();

INSERT INTO crop_nitrogen_references
    (crop_id, n_content_kg_per_kg_yield, source_reference, active, created_at, updated_at)
VALUES
    (@crop_rice_id, 0.012000, 'ACM internal reference 2026 - rice grain nitrogen', TRUE, '2026-01-02 08:00:00', '2026-01-02 08:00:00'),
    (@crop_corn_id, 0.011500, 'ACM internal reference 2026 - maize grain nitrogen', TRUE, '2026-01-02 08:10:00', '2026-01-02 08:10:00'),
    (@crop_leafy_id, 0.004800, 'ACM internal reference 2026 - leafy vegetables', TRUE, '2026-01-02 08:20:00', '2026-01-02 08:20:00'),
    (@crop_soy_id, 0.058000, 'ACM internal reference 2026 - soybean grain nitrogen', TRUE, '2026-01-02 08:30:00', '2026-01-02 08:30:00');

-- =========================================================
-- 2. Users, roles, and preferences
-- =========================================================

SELECT @admin_user_id := user_id FROM users WHERE email = 'admin@acm.local' LIMIT 1;
SELECT @farmer_anphu_user_id := user_id FROM users WHERE email = 'farmer@acm.local' LIMIT 1;
SELECT @employee_minhquan_user_id := user_id FROM users WHERE email = 'employee@acm.local' LIMIT 1;
SELECT @buyer_nongsanxanh_user_id := user_id FROM users WHERE email = 'buyer@acm.local' LIMIT 1;

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
VALUES ('farmer.binhminh', 'farmer.binhminh@example.com', '0902000002', 'Trần Thị Bình', @seed_password_hash, 'ACTIVE', 92, 92001, '2026-01-07 09:00:00');
SET @farmer_binhminh_user_id = LAST_INSERT_ID();

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
VALUES ('employee.lanthao', 'employee.lanthao@example.com', '0903000002', 'Võ Lan Thảo', @seed_password_hash, 'ACTIVE', 92, 92001, '2026-01-11 08:15:00');
SET @employee_lanthao_user_id = LAST_INSERT_ID();

INSERT INTO users (user_name, email, phone, full_name, password_hash, status, province_id, ward_id, joined_date)
VALUES ('buyer.thucphamantoan', 'buyer.thucphamantoan@example.com', '0904000002', 'Công ty Thực Phẩm An Toàn', @seed_password_hash, 'ACTIVE', 79, 79001, '2026-01-14 10:00:00');
SET @buyer_thucphamantoan_user_id = LAST_INSERT_ID();

INSERT INTO user_roles (user_id, role_id)
VALUES
    (@farmer_binhminh_user_id, @role_farmer_id),
    (@employee_lanthao_user_id, @role_employee_id),
    (@buyer_thucphamantoan_user_id, @role_buyer_id);

INSERT INTO user_preferences (user_id, currency_code, weight_unit, locale, created_at, updated_at)
VALUES
    (@admin_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:00:00', '2026-01-12 08:00:00'),
    (@farmer_anphu_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:05:00', '2026-01-12 08:05:00'),
    (@farmer_binhminh_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:10:00', '2026-01-12 08:10:00'),
    (@employee_minhquan_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:15:00', '2026-01-12 08:15:00'),
    (@employee_lanthao_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:20:00', '2026-01-12 08:20:00'),
    (@buyer_nongsanxanh_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:25:00', '2026-01-12 08:25:00'),
    (@buyer_thucphamantoan_user_id, 'VND', 'kg', 'vi-VN', '2026-01-12 08:30:00', '2026-01-12 08:30:00');

-- =========================================================
-- 3. Farms, plots, and seasons
-- =========================================================

INSERT INTO farms (user_id, farm_name, province_id, ward_id, area, latitude, longitude, average_rating, rating_count, active)
VALUES (@farmer_anphu_user_id, 'Trang trại Lúa Hữu Cơ An Phú', 87, 87002, 18.50, 10.531200, 105.621500, 4.8, 6, TRUE);
SET @farm_anphu_id = LAST_INSERT_ID();

INSERT INTO farms (user_id, farm_name, province_id, ward_id, area, latitude, longitude, average_rating, rating_count, active)
VALUES (@farmer_binhminh_user_id, 'Trang trại Rau Sạch Bình Minh', 92, 92001, 9.20, 10.038800, 105.525300, 4.7, 4, TRUE);
SET @farm_binhminh_id = LAST_INSERT_ID();

INSERT INTO plots (farm_id, plot_name, area, soil_type, boundary_geojson, status, created_by, created_at, updated_at)
VALUES (@farm_anphu_id, 'Thửa Lúa Đông Xuân 01', 7.20, 'Phù sa pha sét', '{"type":"Polygon","coordinates":[[[105.621,10.531],[105.623,10.531],[105.623,10.533],[105.621,10.533],[105.621,10.531]]]}', 'IN_USE', @farmer_anphu_user_id, '2026-01-15 07:30:00', '2026-05-20 17:30:00');
SET @plot_anphu_rice_id = LAST_INSERT_ID();

INSERT INTO plots (farm_id, plot_name, area, soil_type, boundary_geojson, status, created_by, created_at, updated_at)
VALUES (@farm_anphu_id, 'Thửa Ngô Luân Canh 02', 5.40, 'Phù sa nhẹ', '{"type":"Polygon","coordinates":[[[105.624,10.530],[105.626,10.530],[105.626,10.532],[105.624,10.532],[105.624,10.530]]]}', 'IN_USE', @farmer_anphu_user_id, '2026-01-15 07:45:00', '2026-05-20 17:30:00');
SET @plot_anphu_corn_id = LAST_INSERT_ID();

INSERT INTO plots (farm_id, plot_name, area, soil_type, boundary_geojson, status, created_by, created_at, updated_at)
VALUES (@farm_binhminh_id, 'Nhà Lưới Rau Ăn Lá 01', 2.10, 'Đất thịt nhẹ', '{"type":"Polygon","coordinates":[[[105.525,10.038],[105.526,10.038],[105.526,10.039],[105.525,10.039],[105.525,10.038]]]}', 'IN_USE', @farmer_binhminh_user_id, '2026-01-16 08:00:00', '2026-05-25 15:00:00');
SET @plot_binhminh_leafy_id = LAST_INSERT_ID();

INSERT INTO plots (farm_id, plot_name, area, soil_type, boundary_geojson, status, created_by, created_at, updated_at)
VALUES (@farm_binhminh_id, 'Thửa Đậu Nành Cải Tạo Đất 02', 3.60, 'Đất phù sa cổ', '{"type":"Polygon","coordinates":[[[105.527,10.037],[105.529,10.037],[105.529,10.039],[105.527,10.039],[105.527,10.037]]]}', 'FALLOW', @farmer_binhminh_user_id, '2026-01-16 08:20:00', '2026-05-25 15:00:00');
SET @plot_binhminh_soy_id = LAST_INSERT_ID();

INSERT INTO seasons
    (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
VALUES
    ('Mùa vụ Lúa Đông Xuân 2024-2025', @plot_anphu_rice_id, @crop_rice_id, @variety_st25_id,
     '2024-11-20', '2025-03-25', '2025-03-28', 'COMPLETED', 185000, 178500, 7000.00, 7240.00, 68000000.00,
     'Vụ nền để so sánh chi phí và năng suất lúa hữu cơ.', '2024-11-15 08:00:00');
SET @season_rice_2025_id = LAST_INSERT_ID();

INSERT INTO seasons
    (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
VALUES
    ('Mùa vụ Lúa Hè Thu 2026', @plot_anphu_rice_id, @crop_rice_id, @variety_om5451_id,
     '2026-04-18', '2026-08-05', NULL, 'ACTIVE', 176000, 171200, 6500.00, NULL, 72000000.00,
     'Đang theo dõi áp lực đạo ôn sau các đợt mưa đầu mùa.', '2026-04-10 07:30:00');
SET @season_rice_2026_id = LAST_INSERT_ID();

INSERT INTO seasons
    (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
VALUES
    ('Mùa vụ Ngô Luân Canh 2026', @plot_anphu_corn_id, @crop_corn_id, @variety_corn_lvn10_id,
     '2026-02-10', '2026-06-20', NULL, 'ACTIVE', 52000, 50300, 4100.00, NULL, 36500000.00,
     'Luân canh sau lúa để giảm áp lực sâu bệnh và cải thiện hữu cơ đất.', '2026-02-01 08:00:00');
SET @season_corn_2026_id = LAST_INSERT_ID();

INSERT INTO seasons
    (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
VALUES
    ('Mùa vụ Rau Xanh Mưa Sớm 2026', @plot_binhminh_leafy_id, @crop_leafy_id, @variety_leafy_id,
     '2026-05-05', '2026-06-20', NULL, 'ACTIVE', 24000, 23800, 1800.00, NULL, 18500000.00,
     'Vụ rau phục vụ đơn hàng định kỳ cho cửa hàng thực phẩm an toàn.', '2026-05-01 06:45:00');
SET @season_leafy_2026_id = LAST_INSERT_ID();

INSERT INTO seasons
    (season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
     initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
VALUES
    ('Mùa vụ Đậu Nành Cải Tạo Đất 2025-2026', @plot_binhminh_soy_id, @crop_soy_id, @variety_soy_ags398_id,
     '2025-12-05', '2026-03-18', '2026-03-22', 'COMPLETED', 64000, 61200, 2100.00, 2240.00, 22800000.00,
     'Cây họ đậu được dùng để giảm phụ thuộc phân đạm khoáng cho vụ rau tiếp theo.', '2025-12-01 07:00:00');
SET @season_soy_2026_id = LAST_INSERT_ID();

-- =========================================================
-- 4. Season workspace: tasks, logs, expenses, incidents, harvests
-- =========================================================

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
VALUES (@employee_minhquan_user_id, @season_rice_2026_id, 'Kiểm tra mực nước ruộng lúa sau mưa', 'Đo mực nước tại ba điểm thấp và ghi nhận dấu hiệu nghẹt rễ.', '2026-06-04', '2026-06-05', 'DONE', '2026-06-04', '2026-06-04', 'Mực nước ổn định, cần rút nhẹ bờ phía bắc.', '2026-06-01 07:00:00');
SET @task_rice_water_id = LAST_INSERT_ID();

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
VALUES (@employee_lanthao_user_id, @season_rice_2026_id, 'Bón phân hữu cơ vi sinh đợt 2', 'Bón theo hàng, ưu tiên khu vực sinh trưởng chậm.', '2026-06-07', '2026-06-08', 'IN_PROGRESS', '2026-06-07', NULL, 'Đang hoàn thành khu ruộng phía đông.', '2026-06-02 07:10:00');
SET @task_rice_fertilizer_id = LAST_INSERT_ID();

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
VALUES (@employee_minhquan_user_id, @season_rice_2026_id, 'Phun phòng đạo ôn bằng thuốc sinh học', 'Phun đúng liều, giữ khoảng cách an toàn với kênh tưới.', '2026-05-28', '2026-05-30', 'OVERDUE', NULL, NULL, 'Cần xử lý ngay vì độ ẩm ruộng cao.', '2026-05-24 09:00:00');
SET @task_rice_blast_id = LAST_INSERT_ID();

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
VALUES (@employee_lanthao_user_id, @season_corn_2026_id, 'Làm cỏ hàng ngô giai đoạn trổ cờ', 'Dọn cỏ ven rãnh và giữ lớp phủ hữu cơ.', '2026-06-10', '2026-06-12', 'PENDING', NULL, NULL, 'Chuẩn bị nhân công sáng sớm để tránh nắng.', '2026-06-03 08:00:00');
SET @task_corn_weeding_id = LAST_INSERT_ID();

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
VALUES (@employee_minhquan_user_id, @season_leafy_2026_id, 'Thu hoạch rau xanh lứa đầu', 'Cắt rau theo lô, cân riêng phần loại 1 và loại 2.', '2026-06-18', '2026-06-19', 'PENDING', NULL, NULL, 'Chuẩn bị thùng nhựa sạch và xe lạnh.', '2026-06-04 08:30:00');
SET @task_leafy_harvest_id = LAST_INSERT_ID();

INSERT INTO tasks (user_id, season_id, title, description, planned_date, due_date, status, actual_start_date, actual_end_date, notes, created_at)
VALUES (@employee_lanthao_user_id, @season_soy_2026_id, 'Phơi và sàng đậu nành sau thu hoạch', 'Giữ độ ẩm hạt dưới 13 phần trăm trước khi nhập kho.', '2026-03-22', '2026-03-24', 'DONE', '2026-03-22', '2026-03-24', 'Đã nhập kho lô đạt loại A.', '2026-03-18 08:00:00');
SET @task_soy_drying_id = LAST_INSERT_ID();

INSERT INTO field_logs (season_id, log_date, log_type, notes, created_by_user_id, created_at)
VALUES
    (@season_rice_2026_id, '2026-05-20', 'GROWTH', 'Lúa đẻ nhánh đều, khu giữa ruộng xanh đậm hơn mép bờ.', @farmer_anphu_user_id, '2026-05-20 17:30:00'),
    (@season_rice_2026_id, '2026-06-04', 'IRRIGATE', 'Điều tiết nước sau mưa, giữ mực nước 4-5 cm.', @employee_minhquan_user_id, '2026-06-04 11:00:00'),
    (@season_corn_2026_id, '2026-05-28', 'WEED', 'Làm cỏ ven rãnh, bổ sung lớp phủ rơm ở hàng giữa.', @employee_lanthao_user_id, '2026-05-28 16:40:00'),
    (@season_leafy_2026_id, '2026-06-01', 'PEST', 'Phát hiện rệp mềm rải rác, ưu tiên bẫy vàng trước khi phun sinh học.', @farmer_binhminh_user_id, '2026-06-01 10:20:00'),
    (@season_soy_2026_id, '2026-03-22', 'HARVEST', 'Thu hoạch đậu nành khi trái khô đồng đều, thất thoát thấp.', @employee_lanthao_user_id, '2026-03-22 17:10:00');

INSERT INTO expenses (user_id, season_id, task_id, category, item_name, unit_price, quantity, total_cost, amount, payment_status, note, expense_date, created_at)
VALUES
    (@farmer_anphu_user_id, @season_rice_2026_id, @task_rice_fertilizer_id, 'FERTILIZER', 'Phân hữu cơ vi sinh Mekong Bio', 185000.00, 18, 3330000.00, 3330000.00, 'PAID', 'Mua theo hợp đồng vật tư tháng 5.', '2026-05-18', '2026-05-18 09:00:00'),
    (@farmer_anphu_user_id, @season_rice_2026_id, @task_rice_blast_id, 'PESTICIDE', 'Chế phẩm sinh học phòng đạo ôn SafeRice', 240000.00, 6, 1440000.00, 1440000.00, 'UNPAID', 'Đợi đối chiếu sau khi phun xong.', '2026-05-27', '2026-05-27 14:00:00'),
    (@farmer_anphu_user_id, @season_corn_2026_id, @task_corn_weeding_id, 'LABOR', 'Công làm cỏ hàng ngô', 320000.00, 3, 960000.00, 960000.00, 'PENDING', 'Dự kiến trả cuối tuần.', '2026-06-03', '2026-06-03 09:30:00'),
    (@farmer_binhminh_user_id, @season_leafy_2026_id, NULL, 'SEED', 'Hạt giống cải xanh mưa sớm', 95000.00, 5, 475000.00, 475000.00, 'PAID', 'Gieo bổ sung khu nhà lưới phía tây.', '2026-05-04', '2026-05-04 07:15:00'),
    (@farmer_binhminh_user_id, @season_soy_2026_id, @task_soy_drying_id, 'LABOR', 'Công phơi và sàng đậu nành', 280000.00, 4, 1120000.00, 1120000.00, 'PAID', 'Hoàn tất sau khi nhập kho.', '2026-03-24', '2026-03-24 18:00:00');
SET @expense_rice_biofungicide_id = (
    SELECT expense_id FROM expenses
    WHERE season_id = @season_rice_2026_id AND item_name = 'Chế phẩm sinh học phòng đạo ôn SafeRice'
    LIMIT 1
);

INSERT INTO harvests (season_id, harvest_date, quantity, unit, grade, note, created_at)
VALUES (@season_rice_2025_id, '2025-03-28', 7240.00, 12500.00, 'A', 'Gạo thơm đạt độ ẩm sau sấy 13 phần trăm.', '2025-03-28 17:20:00');
SET @harvest_rice_2025_id = LAST_INSERT_ID();

INSERT INTO harvests (season_id, harvest_date, quantity, unit, grade, note, created_at)
VALUES (@season_soy_2026_id, '2026-03-22', 2240.00, 16000.00, 'A', 'Đậu nành khô đồng đều, tỷ lệ hạt vỡ thấp.', '2026-03-22 17:45:00');
SET @harvest_soy_2026_id = LAST_INSERT_ID();

INSERT INTO harvests (season_id, harvest_date, quantity, unit, grade, note, created_at)
VALUES (@season_leafy_2026_id, '2026-06-02', 620.00, 18000.00, 'B+', 'Thu tỉa lứa sớm phục vụ đơn đặt trước.', '2026-06-02 06:30:00');
SET @harvest_leafy_2026_id = LAST_INSERT_ID();

INSERT INTO incidents (season_id, reported_by, incident_type, severity, description, status, deadline, resolved_at, created_at)
VALUES (@season_rice_2026_id, @employee_minhquan_user_id, 'DISEASE', 'HIGH', 'Xuất hiện vết bệnh nghi đạo ôn trên lá sau mưa kéo dài.', 'IN_PROGRESS', '2026-06-08', NULL, '2026-06-01 09:20:00');
SET @incident_rice_blast_id = LAST_INSERT_ID();

INSERT INTO incidents (season_id, reported_by, incident_type, severity, description, status, deadline, resolved_at, created_at)
VALUES (@season_leafy_2026_id, @farmer_binhminh_user_id, 'PEST', 'MEDIUM', 'Rệp mềm xuất hiện ở luống rau gần cửa nhà lưới.', 'OPEN', '2026-06-07', NULL, '2026-06-01 10:15:00');
SET @incident_leafy_aphid_id = LAST_INSERT_ID();

INSERT INTO incidents (season_id, reported_by, incident_type, severity, description, status, deadline, resolved_at, created_at)
VALUES (@season_soy_2026_id, @employee_lanthao_user_id, 'WEATHER', 'LOW', 'Mưa trái mùa làm chậm tiến độ phơi đậu nành trong một ngày.', 'RESOLVED', '2026-03-24', '2026-03-24 16:00:00', '2026-03-23 08:30:00');
SET @incident_soy_weather_id = LAST_INSERT_ID();

INSERT INTO disease_records
    (season_id, plot_id, crop_id, variety_id, reported_by_user_id, incident_id, disease_name, symptom_summary,
     severity, status, detected_at, affected_plant_count, affected_area_value, affected_area_unit, evidence_url, notes, created_at, updated_at)
VALUES
    (@season_rice_2026_id, @plot_anphu_rice_id, @crop_rice_id, @variety_om5451_id, @employee_minhquan_user_id,
     @incident_rice_blast_id, 'Đạo ôn lá', 'Vết bệnh hình thoi nhỏ trên lá giai đoạn đẻ nhánh.',
     'HIGH', 'UNDER_TREATMENT', '2026-06-01 09:00:00', 430, 0.350, 'ha',
     '/seed-evidence/disease/rice-blast-2026-06-01.jpg', 'Ưu tiên chế phẩm sinh học và rút nước nhẹ.', '2026-06-01 09:30:00', '2026-06-03 16:00:00');
SET @disease_rice_blast_id = LAST_INSERT_ID();

-- =========================================================
-- 5. Input warehouse
-- =========================================================

INSERT INTO suppliers (name, license_no, contact_email, contact_phone)
VALUES ('Công ty Vật tư Nông nghiệp Mekong', 'VTNN-MK-2024-118', 'sales.mekong@example.com', '02923880001');
SET @supplier_mekong_id = LAST_INSERT_ID();

INSERT INTO suppliers (name, license_no, contact_email, contact_phone)
VALUES ('Hợp tác xã Sinh học Đồng Tháp', 'HTX-SHDT-2025-022', 'kinhdoanh.sinhhocdt@example.com', '02773880002');
SET @supplier_sinhhoc_id = LAST_INSERT_ID();

INSERT INTO supply_items (name, active_ingredient, unit, restricted_flag)
VALUES ('Phân hữu cơ vi sinh Mekong Bio', 'Hữu cơ 35%, vi sinh Bacillus', 'bao', FALSE);
SET @item_biofertilizer_id = LAST_INSERT_ID();

INSERT INTO supply_items (name, active_ingredient, unit, restricted_flag)
VALUES ('Chế phẩm sinh học phòng đạo ôn SafeRice', 'Trichoderma, Bacillus subtilis', 'chai', FALSE);
SET @item_saferice_id = LAST_INSERT_ID();

INSERT INTO supply_items (name, active_ingredient, unit, restricted_flag)
VALUES ('Hạt giống lúa OM5451 xác nhận', 'OM5451 cấp xác nhận', 'kg', FALSE);
SET @item_seed_rice_id = LAST_INSERT_ID();

INSERT INTO supply_items (name, active_ingredient, unit, restricted_flag)
VALUES ('Bẫy dính vàng kiểm soát rệp', 'Keo dính sinh học', 'tấm', FALSE);
SET @item_yellow_trap_id = LAST_INSERT_ID();

INSERT INTO supply_lots (supply_item_id, supplier_id, batch_code, expiry_date, status)
VALUES (@item_biofertilizer_id, @supplier_mekong_id, 'MK-BIO-2026-05-A', '2027-05-31', 'ACTIVE');
SET @lot_biofertilizer_id = LAST_INSERT_ID();

INSERT INTO supply_lots (supply_item_id, supplier_id, batch_code, expiry_date, status)
VALUES (@item_saferice_id, @supplier_sinhhoc_id, 'SR-BIO-2026-04-L2', '2026-07-15', 'ACTIVE');
SET @lot_saferice_id = LAST_INSERT_ID();

INSERT INTO supply_lots (supply_item_id, supplier_id, batch_code, expiry_date, status)
VALUES (@item_seed_rice_id, @supplier_mekong_id, 'OM5451-XN-2026-01', '2026-12-31', 'ACTIVE');
SET @lot_seed_rice_id = LAST_INSERT_ID();

INSERT INTO supply_lots (supply_item_id, supplier_id, batch_code, expiry_date, status)
VALUES (@item_yellow_trap_id, @supplier_sinhhoc_id, 'BAY-VANG-2026-03', '2028-03-31', 'ACTIVE');
SET @lot_yellow_trap_id = LAST_INSERT_ID();

INSERT INTO warehouses (farm_id, name, type, province_id, ward_id)
VALUES (@farm_anphu_id, 'Kho vật tư chính An Phú', 'INPUT', 87, 87002);
SET @warehouse_anphu_input_id = LAST_INSERT_ID();

INSERT INTO warehouses (farm_id, name, type, province_id, ward_id)
VALUES (@farm_anphu_id, 'Kho thành phẩm An Phú', 'PRODUCT', 87, 87002);
SET @warehouse_anphu_product_id = LAST_INSERT_ID();

INSERT INTO warehouses (farm_id, name, type, province_id, ward_id)
VALUES (@farm_binhminh_id, 'Kho tổng hợp Bình Minh', 'MIXED', 92, 92001);
SET @warehouse_binhminh_mixed_id = LAST_INSERT_ID();

INSERT INTO stock_locations (warehouse_id, zone, aisle, shelf, bin)
VALUES (@warehouse_anphu_input_id, 'A', '01', 'F1', 'B01');
SET @loc_anphu_input_a01_id = LAST_INSERT_ID();

INSERT INTO stock_locations (warehouse_id, zone, aisle, shelf, bin)
VALUES (@warehouse_anphu_product_id, 'P', '01', 'K1', 'G01');
SET @loc_anphu_product_p01_id = LAST_INSERT_ID();

INSERT INTO stock_locations (warehouse_id, zone, aisle, shelf, bin)
VALUES (@warehouse_binhminh_mixed_id, 'B', '02', 'R1', 'L01');
SET @loc_binhminh_b02_id = LAST_INSERT_ID();

INSERT INTO stock_movements (supply_lot_id, warehouse_id, location_id, movement_type, quantity, movement_date, season_id, task_id, note)
VALUES
    (@lot_biofertilizer_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 'IN', 60.000, '2026-05-18 08:30:00', @season_rice_2026_id, NULL, 'Nhập phân hữu cơ cho vụ lúa Hè Thu.'),
    (@lot_biofertilizer_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 'OUT', 18.000, '2026-06-07 07:00:00', @season_rice_2026_id, @task_rice_fertilizer_id, 'Xuất cho bón đợt 2.'),
    (@lot_saferice_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 'IN', 12.000, '2026-05-27 13:00:00', @season_rice_2026_id, NULL, 'Nhập chế phẩm phòng đạo ôn gần hạn.'),
    (@lot_saferice_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 'OUT', 4.000, '2026-06-02 06:30:00', @season_rice_2026_id, @task_rice_blast_id, 'Xuất xử lý điểm bệnh đầu tiên.'),
    (@lot_yellow_trap_id, @warehouse_binhminh_mixed_id, @loc_binhminh_b02_id, 'IN', 80.000, '2026-05-29 09:00:00', @season_leafy_2026_id, NULL, 'Nhập bẫy vàng cho nhà lưới.'),
    (@lot_yellow_trap_id, @warehouse_binhminh_mixed_id, @loc_binhminh_b02_id, 'OUT', 24.000, '2026-06-01 09:30:00', @season_leafy_2026_id, NULL, 'Treo bẫy sau khi phát hiện rệp mềm.'),
    (@lot_seed_rice_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 'IN', 520.000, '2026-03-20 08:00:00', @season_rice_2026_id, NULL, 'Nhập giống OM5451 cho Hè Thu.'),
    (@lot_seed_rice_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 'OUT', 410.000, '2026-04-15 06:00:00', @season_rice_2026_id, NULL, 'Xuất giống trước gieo sạ.');

INSERT INTO inventory_balances (supply_lot_id, warehouse_id, location_id, quantity)
VALUES
    (@lot_biofertilizer_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 42.00),
    (@lot_saferice_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 8.00),
    (@lot_seed_rice_id, @warehouse_anphu_input_id, @loc_anphu_input_a01_id, 110.00),
    (@lot_yellow_trap_id, @warehouse_binhminh_mixed_id, @loc_binhminh_b02_id, 56.00);

INSERT INTO disease_treatments
    (disease_record_id, treated_at, method, supply_item_id, supply_lot_id, material_name, quantity_used, unit,
     cost_amount, expense_id, effectiveness, result_summary, next_review_at, notes, created_by_user_id, created_at, updated_at)
VALUES
    (@disease_rice_blast_id, '2026-06-02 06:30:00', 'Phun chế phẩm sinh học buổi sáng sớm',
     @item_saferice_id, @lot_saferice_id, 'Chế phẩm sinh học phòng đạo ôn SafeRice', 4.000, 'chai',
     960000.00, @expense_rice_biofungicide_id, 'GOOD', 'Vết bệnh không lan thêm sau 48 giờ đầu.',
     '2026-06-08', 'Tiếp tục theo dõi khu ruộng phía đông.', @farmer_anphu_user_id, '2026-06-02 10:00:00', '2026-06-04 17:00:00');

-- =========================================================
-- 6. Sustainability
-- =========================================================

INSERT INTO soil_tests
    (season_id, plot_id, sample_date, soil_organic_matter_pct, mineral_n_kg_per_ha, nitrate_mg_per_kg,
     ammonium_mg_per_kg, legacy_n_contribution_kg, legacy_event_id, legacy_derived, measured, source_type,
     source_document, lab_reference, note, created_by_user_id, created_at)
VALUES
    (@season_rice_2025_id, @plot_anphu_rice_id, '2024-11-18', 2.4200, 54.3000, 18.5000, 6.2000, NULL, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/soil-rice-2024.pdf', 'LAB-DT-2024-118', 'Nền đất sau nhiều vụ lúa liên tục, FDN cao.', @farmer_anphu_user_id, '2024-11-19 09:00:00'),
    (@season_rice_2026_id, @plot_anphu_rice_id, '2026-04-12', 2.7800, 42.1000, 13.1000, 5.8000, NULL, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/soil-rice-2026.pdf', 'LAB-DT-2026-041', 'Đạm khoáng giảm sau điều chỉnh phân hữu cơ.', @farmer_anphu_user_id, '2026-04-13 09:00:00'),
    (@season_soy_2026_id, @plot_binhminh_soy_id, '2025-12-03', 2.9500, 36.4000, 9.2000, 4.1000, NULL, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/soil-soy-2025.pdf', 'LAB-CT-2025-205', 'Đất chuẩn bị luân canh cây họ đậu.', @farmer_binhminh_user_id, '2025-12-04 09:00:00');

INSERT INTO irrigation_water_analyses
    (season_id, plot_id, sample_date, nitrate_mg_per_l, ammonium_mg_per_l, total_n_mg_per_l, irrigation_volume_m3,
     legacy_n_contribution_kg, legacy_event_id, legacy_derived, measured, source_type, source_document, lab_reference,
     note, created_by_user_id, created_at)
VALUES
    (@season_rice_2026_id, @plot_anphu_rice_id, '2026-05-05', 1.8400, 0.2200, 2.0600, 620.0000, 1.2772, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/water-rice-2026.pdf', 'WATER-DT-2026-077', 'Kênh tưới có đóng góp đạm thấp.', @farmer_anphu_user_id, '2026-05-05 15:30:00'),
    (@season_leafy_2026_id, @plot_binhminh_leafy_id, '2026-05-08', 1.1200, 0.1800, 1.3000, 85.0000, 0.1105, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/water-leafy-2026.pdf', 'WATER-CT-2026-033', 'Nguồn nước giếng khoan sau lọc thô.', @farmer_binhminh_user_id, '2026-05-08 14:00:00');

INSERT INTO nutrient_input_events
    (season_id, plot_id, input_source, n_kg, applied_date, measured, data_source, source_type, source_document,
     note, created_by_user_id, created_at)
VALUES
    (@season_rice_2025_id, @plot_anphu_rice_id, 'MINERAL_FERTILIZER', 128.4000, '2024-12-12', TRUE, 'expense-entry', 'USER_ENTERED',
     '/seed-evidence/fdn/mineral-n-rice-2024.pdf', 'Vụ cũ còn phụ thuộc phân đạm khoáng.', @farmer_anphu_user_id, '2024-12-12 16:00:00'),
    (@season_rice_2026_id, @plot_anphu_rice_id, 'ORGANIC_FERTILIZER', 46.8000, '2026-05-18', TRUE, 'inventory-lot', 'USER_ENTERED',
     '/seed-evidence/fdn/organic-fertilizer-rice-2026.pdf', 'Tăng tỷ lệ hữu cơ để giảm FDN.', @farmer_anphu_user_id, '2026-05-18 16:00:00'),
    (@season_rice_2026_id, @plot_anphu_rice_id, 'IRRIGATION_WATER', 1.2772, '2026-05-05', TRUE, 'water-analysis', 'LAB_MEASURED',
     '/seed-evidence/fdn/water-rice-2026.pdf', 'Đạm từ nước tưới theo phân tích mẫu.', @farmer_anphu_user_id, '2026-05-05 16:30:00'),
    (@season_soy_2026_id, @plot_binhminh_soy_id, 'BIOLOGICAL_FIXATION', 126.0000, '2026-02-10', FALSE, 'system-estimate', 'SYSTEM_ESTIMATED',
     '/seed-evidence/fdn/soy-fixation-2026.pdf', 'Ước tính cố định đạm sinh học của đậu nành.', @farmer_binhminh_user_id, '2026-02-10 08:00:00'),
    (@season_leafy_2026_id, @plot_binhminh_leafy_id, 'CONTROL_SUPPLY', 4.2000, '2026-06-01', TRUE, 'pest-control-log', 'USER_ENTERED',
     '/seed-evidence/fdn/leafy-control-2026.pdf', 'Đầu vào kiểm soát rệp, không tính như phân bón chính.', @farmer_binhminh_user_id, '2026-06-01 11:00:00');

-- =========================================================
-- 7. Product warehouse and traceability
-- =========================================================

INSERT INTO product_warehouse_lots
    (lot_code, product_id, product_name, product_variant, season_id, farm_id, plot_id, harvest_id, warehouse_id, location_id,
     harvested_at, received_at, unit, initial_quantity, on_hand_quantity, grade, quality_status, traceability_data, note,
     status, created_by, created_at, updated_at)
VALUES
    ('PW-ANPHU-RICE-ST25-2025-001', NULL, 'Gạo thơm ST25 An Phú', 'Túi 5 kg', @season_rice_2025_id, @farm_anphu_id,
     @plot_anphu_rice_id, @harvest_rice_2025_id, @warehouse_anphu_product_id, @loc_anphu_product_p01_id,
     '2025-03-28', '2025-03-29 08:00:00', 'kg', 7240.000, 4820.000, 'A', 'PASSED',
     '{"farm":"Trang trại Lúa Hữu Cơ An Phú","plot":"Thửa Lúa Đông Xuân 01","season":"Mùa vụ Lúa Đông Xuân 2024-2025","harvest_date":"2025-03-28"}',
     'Lô gạo thơm truy xuất được từ mùa vụ hoàn thành.', 'IN_STOCK', @farmer_anphu_user_id, '2025-03-29 08:00:00', '2026-04-22 09:00:00');
SET @pwlot_rice_st25_id = LAST_INSERT_ID();

INSERT INTO product_warehouse_lots
    (lot_code, product_id, product_name, product_variant, season_id, farm_id, plot_id, harvest_id, warehouse_id, location_id,
     harvested_at, received_at, unit, initial_quantity, on_hand_quantity, grade, quality_status, traceability_data, note,
     status, created_by, created_at, updated_at)
VALUES
    ('PW-BINHMINH-SOY-AGS398-2026-001', NULL, 'Đậu nành AGS398 sấy khô', 'Bao 10 kg', @season_soy_2026_id, @farm_binhminh_id,
     @plot_binhminh_soy_id, @harvest_soy_2026_id, @warehouse_binhminh_mixed_id, @loc_binhminh_b02_id,
     '2026-03-22', '2026-03-24 08:00:00', 'kg', 2240.000, 320.000, 'A', 'PASSED',
     '{"farm":"Trang trại Rau Sạch Bình Minh","plot":"Thửa Đậu Nành Cải Tạo Đất 02","season":"Mùa vụ Đậu Nành Cải Tạo Đất 2025-2026","harvest_date":"2026-03-22"}',
     'Lô đậu nành còn ít, dùng cho cảnh báo tồn thấp.', 'IN_STOCK', @farmer_binhminh_user_id, '2026-03-24 08:00:00', '2026-05-30 09:00:00');
SET @pwlot_soy_ags398_id = LAST_INSERT_ID();

INSERT INTO product_warehouse_lots
    (lot_code, product_id, product_name, product_variant, season_id, farm_id, plot_id, harvest_id, warehouse_id, location_id,
     harvested_at, received_at, unit, initial_quantity, on_hand_quantity, grade, quality_status, traceability_data, note,
     status, created_by, created_at, updated_at)
VALUES
    ('PW-BINHMINH-LEAFY-2026-001', NULL, 'Rau cải xanh Bình Minh', 'Thùng 5 kg', @season_leafy_2026_id, @farm_binhminh_id,
     @plot_binhminh_leafy_id, @harvest_leafy_2026_id, @warehouse_binhminh_mixed_id, @loc_binhminh_b02_id,
     '2026-06-02', '2026-06-02 09:00:00', 'kg', 620.000, 410.000, 'B+', 'HOLD',
     '{"farm":"Trang trại Rau Sạch Bình Minh","plot":"Nhà Lưới Rau Ăn Lá 01","season":"Mùa vụ Rau Xanh Mưa Sớm 2026","harvest_date":"2026-06-02"}',
     'Đang giữ kiểm tra chất lượng trước khi mở bán rộng.', 'HOLD', @farmer_binhminh_user_id, '2026-06-02 09:00:00', '2026-06-03 10:00:00');
SET @pwlot_leafy_id = LAST_INSERT_ID();

INSERT INTO product_warehouse_lots
    (lot_code, product_id, product_name, product_variant, season_id, farm_id, plot_id, harvest_id, warehouse_id, location_id,
     harvested_at, received_at, unit, initial_quantity, on_hand_quantity, grade, quality_status, traceability_data, note,
     status, created_by, created_at, updated_at)
VALUES
    ('PW-ANPHU-CORN-LVN10-2026-001', NULL, 'Ngô hạt LVN10 An Phú', 'Bao 25 kg', @season_corn_2026_id, @farm_anphu_id,
     @plot_anphu_corn_id, NULL, @warehouse_anphu_product_id, @loc_anphu_product_p01_id,
     '2026-06-01', '2026-06-01 16:00:00', 'kg', 960.000, 0.000, 'B', 'PASSED',
     '{"farm":"Trang trại Lúa Hữu Cơ An Phú","plot":"Thửa Ngô Luân Canh 02","season":"Mùa vụ Ngô Luân Canh 2026","harvest_date":"2026-06-01"}',
     'Lô đã xuất hết cho hợp đồng sỉ.', 'DEPLETED', @farmer_anphu_user_id, '2026-06-01 16:00:00', '2026-06-04 10:00:00');
SET @pwlot_corn_id = LAST_INSERT_ID();

INSERT INTO product_warehouse_transactions
    (lot_id, transaction_type, quantity, unit, resulting_on_hand, reference_type, reference_id, note, created_by, created_at)
VALUES
    (@pwlot_rice_st25_id, 'RECEIPT_FROM_HARVEST', 7240.000, 'kg', 7240.000, 'HARVEST', @harvest_rice_2025_id, 'Nhập kho sau sấy và phân loại.', @farmer_anphu_user_id, '2025-03-29 08:05:00'),
    (@pwlot_rice_st25_id, 'STOCK_OUT', 2420.000, 'kg', 4820.000, 'WHOLESALE_ORDER', 'WS-2025-041', 'Xuất cho kênh bán sỉ trước khi mở marketplace.', @farmer_anphu_user_id, '2025-04-12 09:00:00'),
    (@pwlot_soy_ags398_id, 'RECEIPT_FROM_HARVEST', 2240.000, 'kg', 2240.000, 'HARVEST', @harvest_soy_2026_id, 'Nhập kho đậu nành sau phơi.', @farmer_binhminh_user_id, '2026-03-24 08:05:00'),
    (@pwlot_soy_ags398_id, 'STOCK_OUT', 1920.000, 'kg', 320.000, 'CONTRACT', 'CT-BM-2026-018', 'Xuất phần lớn cho hợp đồng chuỗi thực phẩm.', @farmer_binhminh_user_id, '2026-05-10 09:00:00'),
    (@pwlot_leafy_id, 'RECEIPT_FROM_HARVEST', 620.000, 'kg', 620.000, 'HARVEST', @harvest_leafy_2026_id, 'Nhập lứa rau thu tỉa sớm.', @farmer_binhminh_user_id, '2026-06-02 09:05:00'),
    (@pwlot_leafy_id, 'ADJUSTMENT', 410.000, 'kg', 410.000, 'QUALITY_CHECK', 'QC-BM-2026-0602', 'Loại ra phần rau dập sau kiểm tra.', @farmer_binhminh_user_id, '2026-06-03 10:00:00'),
    (@pwlot_corn_id, 'RECEIPT_FROM_HARVEST', 960.000, 'kg', 960.000, 'MANUAL', 'PARTIAL-CORN-2026-0601', 'Nhập lô ngô thu sớm.', @farmer_anphu_user_id, '2026-06-01 16:05:00'),
    (@pwlot_corn_id, 'STOCK_OUT', 960.000, 'kg', 0.000, 'WHOLESALE_ORDER', 'WS-2026-099', 'Xuất hết cho hợp đồng sỉ.', @farmer_anphu_user_id, '2026-06-04 10:00:00');

-- =========================================================
-- 8. Marketplace
-- =========================================================

INSERT INTO marketplace_products
    (version, slug, name, category, short_description, description, price, unit, stock_quantity, image_url, image_urls_json,
     farmer_user_id, farm_id, season_id, lot_id, traceable, average_rating, rating_count, status, published_at, created_at, updated_at)
VALUES
    (0, 'gao-thom-st25-an-phu-2025', 'Gạo thơm ST25 An Phú', 'RICE',
     'Gạo thơm hữu cơ từ vụ Đông Xuân đã hoàn thành.',
     'Sản phẩm có truy xuất mùa vụ, thửa ruộng, nhật ký canh tác và lô kho thành phẩm.',
     138000.00, 'kg', 4820.000, '/demo-evidence/products/rice grain.jpg',
     '["/demo-evidence/products/rice grain.jpg","/demo-evidence/products/rice field.jpg"]',
     @farmer_anphu_user_id, @farm_anphu_id, @season_rice_2025_id, @pwlot_rice_st25_id,
     TRUE, 0.0, 0, 'ACTIVE', '2026-04-10 08:00:00', '2026-04-09 09:00:00', '2026-05-28 08:00:00');
SET @product_rice_id = LAST_INSERT_ID();

INSERT INTO marketplace_products
    (version, slug, name, category, short_description, description, price, unit, stock_quantity, image_url, image_urls_json,
     farmer_user_id, farm_id, season_id, lot_id, traceable, average_rating, rating_count, status, published_at, created_at, updated_at)
VALUES
    (0, 'dau-nanh-ags398-binh-minh-2026', 'Đậu nành AGS398 Bình Minh', 'SOYBEAN',
     'Đậu nành khô từ vụ cải tạo đất, tồn kho thấp.',
     'Phù hợp làm sữa hạt, có hồ sơ đất và thông tin cố định đạm sinh học.',
     42000.00, 'kg', 320.000, '/demo-evidence/products/soybean.jpg',
     '["/demo-evidence/products/soybean.jpg","/demo-evidence/products/soybean field.jpg"]',
     @farmer_binhminh_user_id, @farm_binhminh_id, @season_soy_2026_id, @pwlot_soy_ags398_id,
     TRUE, 5.0, 1, 'ACTIVE', '2026-04-12 08:00:00', '2026-04-11 09:00:00', '2026-05-30 09:00:00');
SET @product_soy_id = LAST_INSERT_ID();

INSERT INTO marketplace_products
    (version, slug, name, category, short_description, description, price, unit, stock_quantity, image_url, image_urls_json,
     farmer_user_id, farm_id, season_id, lot_id, traceable, average_rating, rating_count, status, published_at, created_at, updated_at)
VALUES
    (0, 'rau-cai-xanh-binh-minh-2026', 'Rau cải xanh Bình Minh', 'VEGETABLE',
     'Lô rau nhà lưới đang chờ duyệt sau kiểm tra chất lượng.',
     'Dùng để kiểm tra quy trình nông hộ gửi sản phẩm lên duyệt và admin xử lý.',
     26000.00, 'kg', 410.000, '/seed-evidence/products/rau-cai-xanh-binh-minh-2026.jpg',
     '["/seed-evidence/products/rau-cai-xanh-binh-minh-2026.jpg"]',
     @farmer_binhminh_user_id, @farm_binhminh_id, @season_leafy_2026_id, @pwlot_leafy_id,
     TRUE, 0.0, 0, 'PENDING_REVIEW', NULL, '2026-06-03 11:00:00', '2026-06-03 11:00:00');
SET @product_leafy_id = LAST_INSERT_ID();

INSERT INTO marketplace_products
    (version, slug, name, category, short_description, description, price, unit, stock_quantity, image_url, image_urls_json,
     farmer_user_id, farm_id, season_id, lot_id, traceable, average_rating, rating_count, status, published_at, created_at, updated_at)
VALUES
    (0, 'ngo-hat-lvn10-an-phu-2026', 'Ngô hạt LVN10 An Phú', 'CORN',
     'Lô ngô đã bán hết, dùng cho trạng thái hết hàng.',
     'Sản phẩm giữ lịch sử truy xuất nhưng không còn tồn để đặt mới.',
     30000.00, 'kg', 0.000, '/demo-evidence/products/corn.jpg',
     '["/demo-evidence/products/corn.jpg","/demo-evidence/products/corn field.jpg"]',
     @farmer_anphu_user_id, @farm_anphu_id, @season_corn_2026_id, @pwlot_corn_id,
     TRUE, 0.0, 0, 'SOLD_OUT', '2026-06-02 08:00:00', '2026-06-01 18:00:00', '2026-06-04 10:15:00');
SET @product_corn_id = LAST_INSERT_ID();

INSERT INTO marketplace_carts (user_id, created_at, updated_at)
VALUES (@buyer_nongsanxanh_user_id, '2026-06-04 09:00:00', '2026-06-04 09:20:00');
SET @cart_nongsanxanh_id = LAST_INSERT_ID();

INSERT INTO marketplace_cart_items (cart_id, product_id, quantity, unit_price_snapshot, created_at, updated_at)
VALUES
    (@cart_nongsanxanh_id, @product_rice_id, 12.000, 138000.00, '2026-06-04 09:05:00', '2026-06-04 09:05:00'),
    (@cart_nongsanxanh_id, @product_soy_id, 8.000, 42000.00, '2026-06-04 09:20:00', '2026-06-04 09:20:00');

INSERT INTO marketplace_addresses
    (user_id, full_name, phone, province, district, ward, street, detail, label, is_default, created_at, updated_at, deleted_at)
VALUES
    (@buyer_nongsanxanh_user_id, 'Cửa hàng Nông Sản Xanh', '0904000001', 'Hồ Chí Minh', 'Quận 7', 'Tân Phú',
     '88 Nguyễn Lương Bằng', 'Nhận hàng tại cửa sau siêu thị mini', 'store', TRUE, '2026-04-18 08:00:00', '2026-05-20 08:00:00', NULL),
    (@buyer_thucphamantoan_user_id, 'Công ty Thực Phẩm An Toàn', '0904000002', 'Hồ Chí Minh', 'Thành phố Thủ Đức', 'Hiệp Bình Chánh',
     '15 Quốc lộ 13', 'Kho mát tầng trệt', 'warehouse', TRUE, '2026-04-19 08:00:00', '2026-05-22 08:00:00', NULL);

INSERT INTO marketplace_order_groups (group_code, buyer_user_id, idempotency_key, request_fingerprint, created_at)
VALUES ('MOG-2026-ANPHU-0001', @buyer_nongsanxanh_user_id, 'nongsanxanh-20260520-rice-soy', 'fp-nongsanxanh-20260520-rice-soy', '2026-05-20 09:00:00');
SET @order_group_1_id = LAST_INSERT_ID();

INSERT INTO marketplace_order_groups (group_code, buyer_user_id, idempotency_key, request_fingerprint, created_at)
VALUES ('MOG-2026-ANTOAN-0002', @buyer_thucphamantoan_user_id, 'thucphamantoan-20260604-bank-transfer', 'fp-thucphamantoan-20260604-bank-transfer', '2026-06-04 10:00:00');
SET @order_group_2_id = LAST_INSERT_ID();

INSERT INTO marketplace_orders
    (order_group_id, order_code, buyer_user_id, farmer_user_id, status, payment_method, payment_verification_status,
     payment_proof_file_name, payment_proof_content_type, payment_proof_storage_path, payment_proof_uploaded_at,
     payment_verified_at, payment_verified_by_user_id, payment_verification_note, shipping_recipient_name, shipping_phone,
     shipping_address_line, note, subtotal, shipping_fee, total_amount, created_at, updated_at)
VALUES
    (@order_group_1_id, 'MPO-2026-0001', @buyer_nongsanxanh_user_id, @farmer_binhminh_user_id, 'COMPLETED', 'COD', 'NOT_REQUIRED',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Cửa hàng Nông Sản Xanh', '0904000001',
     '88 Nguyễn Lương Bằng, Tân Phú, Quận 7, Hồ Chí Minh', 'Đơn hoàn tất để buyer đánh giá đậu nành.',
     840000.00, 45000.00, 885000.00, '2026-05-20 09:05:00', '2026-05-22 16:00:00');
SET @order_completed_soy_id = LAST_INSERT_ID();

INSERT INTO marketplace_order_items
    (order_id, product_id, product_name_snapshot, product_slug_snapshot, image_url_snapshot, unit_price_snapshot,
     quantity, line_total, traceable_snapshot, farm_id, season_id, lot_id)
VALUES
    (@order_completed_soy_id, @product_soy_id, 'Đậu nành AGS398 Bình Minh', 'dau-nanh-ags398-binh-minh-2026',
     '/demo-evidence/products/soybean.jpg', 42000.00, 20.000, 840000.00, TRUE, @farm_binhminh_id, @season_soy_2026_id, @pwlot_soy_ags398_id);
SET @order_item_completed_soy_id = LAST_INSERT_ID();

INSERT INTO marketplace_orders
    (order_group_id, order_code, buyer_user_id, farmer_user_id, status, payment_method, payment_verification_status,
     payment_proof_file_name, payment_proof_content_type, payment_proof_storage_path, payment_proof_uploaded_at,
     payment_verified_at, payment_verified_by_user_id, payment_verification_note, shipping_recipient_name, shipping_phone,
     shipping_address_line, note, subtotal, shipping_fee, total_amount, created_at, updated_at)
VALUES
    (@order_group_2_id, 'MPO-2026-0002', @buyer_thucphamantoan_user_id, @farmer_anphu_user_id, 'PAYMENT_SUBMITTED', 'BANK_TRANSFER', 'SUBMITTED',
     'proof-mpo-2026-0002.jpg', 'image/jpeg', 'storage/marketplace/payment-proofs/MPO-2026-0002/proof.jpg', '2026-06-04 10:15:00',
     NULL, NULL, 'Đang chờ admin xác minh giao dịch chuyển khoản.', 'Công ty Thực Phẩm An Toàn', '0904000002',
     '15 Quốc lộ 13, Hiệp Bình Chánh, Thành phố Thủ Đức, Hồ Chí Minh', 'Đơn đang chờ xác minh thanh toán.',
     2760000.00, 65000.00, 2825000.00, '2026-06-04 10:00:00', '2026-06-04 10:15:00');
SET @order_payment_submitted_rice_id = LAST_INSERT_ID();

INSERT INTO marketplace_order_items
    (order_id, product_id, product_name_snapshot, product_slug_snapshot, image_url_snapshot, unit_price_snapshot,
     quantity, line_total, traceable_snapshot, farm_id, season_id, lot_id)
VALUES
    (@order_payment_submitted_rice_id, @product_rice_id, 'Gạo thơm ST25 An Phú', 'gao-thom-st25-an-phu-2025',
     '/demo-evidence/products/rice grain.jpg', 138000.00, 20.000, 2760000.00, TRUE, @farm_anphu_id, @season_rice_2025_id, @pwlot_rice_st25_id);

INSERT INTO marketplace_orders
    (order_group_id, order_code, buyer_user_id, farmer_user_id, status, payment_method, payment_verification_status,
     payment_proof_file_name, payment_proof_content_type, payment_proof_storage_path, payment_proof_uploaded_at,
     payment_verified_at, payment_verified_by_user_id, payment_verification_note, shipping_recipient_name, shipping_phone,
     shipping_address_line, note, subtotal, shipping_fee, total_amount, created_at, updated_at)
VALUES
    (@order_group_1_id, 'MPO-2026-0003', @buyer_nongsanxanh_user_id, @farmer_anphu_user_id, 'PREPARING', 'BANK_TRANSFER', 'VERIFIED',
     'proof-mpo-2026-0003.png', 'image/png', 'storage/marketplace/payment-proofs/MPO-2026-0003/proof.png', '2026-05-21 09:20:00',
     '2026-05-21 10:05:00', @admin_user_id, 'Đã đối chiếu sao kê, cho phép chuẩn bị hàng.', 'Cửa hàng Nông Sản Xanh', '0904000001',
     '88 Nguyễn Lương Bằng, Tân Phú, Quận 7, Hồ Chí Minh', 'Đơn đã xác minh, farmer đang đóng gói.',
     1380000.00, 45000.00, 1425000.00, '2026-05-21 09:10:00', '2026-05-21 10:10:00');
SET @order_preparing_rice_id = LAST_INSERT_ID();

INSERT INTO marketplace_order_items
    (order_id, product_id, product_name_snapshot, product_slug_snapshot, image_url_snapshot, unit_price_snapshot,
     quantity, line_total, traceable_snapshot, farm_id, season_id, lot_id)
VALUES
    (@order_preparing_rice_id, @product_rice_id, 'Gạo thơm ST25 An Phú', 'gao-thom-st25-an-phu-2025',
     '/demo-evidence/products/rice field.jpg', 138000.00, 10.000, 1380000.00, TRUE, @farm_anphu_id, @season_rice_2025_id, @pwlot_rice_st25_id);

INSERT INTO marketplace_product_reviews
    (product_id, order_id, order_item_id, buyer_user_id, rating, comment, hidden, created_at, updated_at)
VALUES
    (@product_soy_id, @order_completed_soy_id, @order_item_completed_soy_id, @buyer_nongsanxanh_user_id, 5,
     'Đậu nành khô đều, đóng bao sạch và có đầy đủ thông tin truy xuất nguồn gốc.', FALSE, '2026-05-23 09:00:00', '2026-05-23 09:00:00');

-- =========================================================
-- 9. Employee, payroll, notifications, documents, audit
-- =========================================================

INSERT INTO season_employees (season_id, employee_user_id, added_by_user_id, wage_per_task, active, created_at)
VALUES
    (@season_rice_2026_id, @employee_minhquan_user_id, @farmer_anphu_user_id, 180000.00, TRUE, '2026-04-15 08:00:00'),
    (@season_rice_2026_id, @employee_lanthao_user_id, @farmer_anphu_user_id, 180000.00, TRUE, '2026-04-15 08:05:00'),
    (@season_corn_2026_id, @employee_lanthao_user_id, @farmer_anphu_user_id, 160000.00, TRUE, '2026-02-08 08:00:00'),
    (@season_leafy_2026_id, @employee_minhquan_user_id, @farmer_binhminh_user_id, 150000.00, TRUE, '2026-05-03 08:00:00'),
    (@season_soy_2026_id, @employee_lanthao_user_id, @farmer_binhminh_user_id, 170000.00, TRUE, '2025-12-04 08:00:00');

INSERT INTO task_progress_logs (task_id, employee_user_id, progress_percent, note, evidence_url, logged_at)
VALUES
    (@task_rice_water_id, @employee_minhquan_user_id, 100, 'Hoàn tất đo mực nước và gửi ảnh hiện trường.', '/seed-evidence/tasks/rice-water-2026-06-04.jpg', '2026-06-04 11:15:00'),
    (@task_rice_fertilizer_id, @employee_lanthao_user_id, 55, 'Đã bón xong khu phía đông, còn khu phía nam.', '/seed-evidence/tasks/rice-fertilizer-2026-06-07.jpg', '2026-06-07 10:30:00'),
    (@task_soy_drying_id, @employee_lanthao_user_id, 100, 'Đậu nành đã đạt độ khô trước khi nhập kho.', '/seed-evidence/tasks/soy-drying-2026-03-24.jpg', '2026-03-24 16:45:00');

INSERT INTO payroll_records
    (employee_user_id, season_id, period_start, period_end, total_assigned_tasks, total_completed_tasks,
     wage_per_task, total_amount, generated_at, note)
VALUES
    (@employee_minhquan_user_id, @season_rice_2026_id, '2026-06-01', '2026-06-30', 2, 1, 180000.00, 180000.00, '2026-06-05 18:00:00', 'Một việc đã hoàn tất, một việc quá hạn cần xử lý.'),
    (@employee_lanthao_user_id, @season_rice_2026_id, '2026-06-01', '2026-06-30', 1, 0, 180000.00, 0.00, '2026-06-05 18:05:00', 'Công việc bón phân đang thực hiện.'),
    (@employee_lanthao_user_id, @season_soy_2026_id, '2026-03-01', '2026-03-31', 1, 1, 170000.00, 170000.00, '2026-03-31 18:00:00', 'Hoàn tất phơi và sàng đậu nành.');

INSERT INTO alerts
    (type, severity, status, farm_id, season_id, plot_id, crop_id, title, message,
     suggested_action_type, suggested_action_url, recipient_farmer_ids, created_at, sent_at)
VALUES
    ('DISEASE_RISK', 'HIGH', 'OPEN', @farm_anphu_id, @season_rice_2026_id, @plot_anphu_rice_id, @crop_rice_id,
     'Nguy cơ đạo ôn tăng sau mưa', 'Độ ẩm ruộng cao và đã có ghi nhận vết bệnh trên lá lúa.',
     'OPEN_TASK', '/farmer/seasons', CONCAT('[', @farmer_anphu_user_id, ']'), '2026-06-01 09:45:00', '2026-06-01 09:50:00');
SET @alert_rice_blast_id = LAST_INSERT_ID();

INSERT INTO alerts
    (type, severity, status, farm_id, season_id, plot_id, crop_id, title, message,
     suggested_action_type, suggested_action_url, recipient_farmer_ids, created_at, sent_at)
VALUES
    ('INVENTORY_LOW_STOCK', 'MEDIUM', 'OPEN', @farm_binhminh_id, @season_soy_2026_id, @plot_binhminh_soy_id, @crop_soy_id,
     'Tồn kho đậu nành sắp thấp', 'Lô đậu nành AGS398 chỉ còn 320 kg sau các đơn gần đây.',
     'VIEW_WAREHOUSE', '/farmer/product-warehouse', CONCAT('[', @farmer_binhminh_user_id, ']'), '2026-05-30 09:30:00', '2026-05-30 09:35:00');
SET @alert_soy_lowstock_id = LAST_INSERT_ID();

INSERT INTO notifications (user_id, title, message, link, alert_id, created_at, read_at)
VALUES
    (@farmer_anphu_user_id, 'Cảnh báo đạo ôn trên lúa', 'Kiểm tra nhiệm vụ phun phòng đạo ôn đang quá hạn.', '/farmer/seasons', @alert_rice_blast_id, '2026-06-01 09:50:00', NULL),
    (@farmer_binhminh_user_id, 'Tồn kho đậu nành sắp thấp', 'Cân nhắc cập nhật sản phẩm marketplace hoặc nhập thêm hàng.', '/farmer/product-warehouse', @alert_soy_lowstock_id, '2026-05-30 09:35:00', '2026-05-30 10:00:00'),
    (@admin_user_id, 'Đơn chuyển khoản cần xác minh', 'Đơn MPO-2026-0002 đã gửi chứng từ thanh toán.', '/admin/marketplace/orders', NULL, '2026-06-04 10:18:00', NULL),
    (@buyer_nongsanxanh_user_id, 'Đơn hàng đã hoàn tất', 'Đơn MPO-2026-0001 đã giao thành công và có thể đánh giá.', '/buyer/orders', NULL, '2026-05-22 16:10:00', '2026-05-23 08:30:00'),
    (@employee_minhquan_user_id, 'Công việc mới trong vụ lúa', 'Bạn có nhiệm vụ kiểm tra nước và theo dõi đạo ôn trong tuần này.', '/employee/tasks', NULL, '2026-06-01 08:00:00', NULL);

INSERT INTO documents
    (title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type,
     view_count, is_pinned, created_at, updated_at)
VALUES
    ('Quy trình quản lý đạo ôn lúa theo hướng sinh học', '/demo-evidence/documents/Study_on_the_flowering_in_rice_plant_Oryza_sativa_.pdf',
     'Tài liệu tham khảo cho kiểm soát bệnh trong điều kiện mưa ẩm.', 'Lúa', 'Đẻ nhánh', 'Bệnh hại', TRUE, TRUE, @admin_user_id, 'GUIDE', 42, TRUE, '2026-01-20 08:00:00', '2026-05-28 09:00:00');
SET @document_rice_blast_id = LAST_INSERT_ID();

INSERT INTO documents
    (title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type,
     view_count, is_pinned, created_at, updated_at)
VALUES
    ('Hướng dẫn luân canh cây họ đậu để giảm phụ thuộc đạm khoáng', '/demo-evidence/documents/Soybean-Guide-2021-Print-Version.pdf',
     'Gợi ý quản lý FDN cho trang trại có cây đậu nành cải tạo đất.', 'Đậu nành', 'Toàn vụ', 'Bền vững', TRUE, TRUE, @admin_user_id, 'GUIDE', 31, FALSE, '2026-02-01 08:00:00', '2026-05-15 09:00:00');
SET @document_soy_fdn_id = LAST_INSERT_ID();

INSERT INTO documents
    (title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type,
     view_count, is_pinned, created_at, updated_at)
VALUES
    ('Lịch thời vụ rau và lúa khu vực Đồng bằng sông Cửu Long 2026', '/demo-evidence/documents/TB lịch thời vụ, cơ cấu giống vụ hè thu 2026.pdf',
     'Thông tin mùa vụ để đối chiếu kế hoạch sản xuất.', 'Nhiều cây trồng', 'Lập kế hoạch', 'Thời vụ', TRUE, TRUE, @admin_user_id, 'POLICY', 58, TRUE, '2026-02-12 08:00:00', '2026-05-30 09:00:00');
SET @document_calendar_id = LAST_INSERT_ID();

INSERT INTO document_favorites (user_id, document_id, created_at)
VALUES
    (@farmer_anphu_user_id, @document_rice_blast_id, '2026-06-01 10:00:00'),
    (@farmer_binhminh_user_id, @document_soy_fdn_id, '2026-05-25 09:00:00');

INSERT INTO document_recent_opens (user_id, document_id, opened_at)
VALUES
    (@farmer_anphu_user_id, @document_rice_blast_id, '2026-06-01 10:10:00'),
    (@farmer_anphu_user_id, @document_calendar_id, '2026-05-28 08:30:00'),
    (@farmer_binhminh_user_id, @document_soy_fdn_id, '2026-05-25 09:10:00');

INSERT INTO audit_logs
    (entity_type, entity_id, operation, performed_by, performed_at, snapshot_data, reason, ip_address)
VALUES
    ('USER', @farmer_anphu_user_id, 'BOOTSTRAP_ACCOUNT_USED', 'admin@acm.local', '2026-01-05 08:30:00',
     '{"email":"farmer@acm.local","role":"FARMER"}', 'Gắn dữ liệu seed vào tài khoản farmer mặc định của ApplicationInitConfig.', '127.0.0.1'),
    ('SEASON', @season_rice_2026_id, 'CREATE', 'farmer@acm.local', '2026-04-10 07:30:00',
     '{"season":"Mùa vụ Lúa Hè Thu 2026","status":"ACTIVE"}', 'Khởi tạo mùa vụ Hè Thu và kế hoạch chi phí.', '127.0.0.1'),
    ('MARKETPLACE_ORDER', @order_payment_submitted_rice_id, 'PAYMENT_PROOF_SUBMITTED', 'buyer.thucphamantoan@example.com', '2026-06-04 10:15:00',
     '{"order_code":"MPO-2026-0002","payment_verification_status":"SUBMITTED"}', 'Buyer gửi chứng từ thanh toán chờ admin xác minh.', '127.0.0.1'),
    ('PRODUCT_WAREHOUSE_LOT', @pwlot_soy_ags398_id, 'LOW_STOCK_REVIEW', 'farmer.binhminh@example.com', '2026-05-30 09:30:00',
     '{"lot_code":"PW-BINHMINH-SOY-AGS398-2026-001","on_hand_quantity":320}', 'Kiểm tra tồn kho thấp sau các đơn hàng.', '127.0.0.1');

-- =========================================================
-- 9A. FDN supplement data for farmer@acm.local
-- Goal: add a clear FDN storyline under the existing farmer account:
-- 2021-2023 high FDN -> 2024-2026 lower FDN after legume rotation.
-- Merged from fdn_supplement_farmer_acm.sql so one import creates all demo data.
-- =========================================================

-- Reset user variables so rerunning in the same MySQL session stays deterministic.
SET @farmer_id = NULL, @province_id = NULL, @ward_id = NULL, @farm_id = NULL, @old_fdn_plot_id = NULL, @fdn_plot_id = NULL;
SET @crop_rice_id = NULL, @crop_corn_id = NULL, @crop_soy_id = NULL;
SET @variety_rice_id = NULL, @variety_corn_id = NULL, @variety_soy_id = NULL;

-- 1) Resolve main farmer account and master data
SELECT @farmer_id := user_id
FROM users
WHERE email = 'farmer@acm.local'
LIMIT 1;

SELECT @province_id := COALESCE(province_id, 87),
       @ward_id := COALESCE(ward_id, 87002)
FROM users
WHERE user_id = @farmer_id
LIMIT 1;

SELECT @crop_rice_id := crop_id FROM crops WHERE crop_name IN ('Lúa', 'Lua', 'Rice') ORDER BY crop_id LIMIT 1;
SELECT @crop_corn_id := crop_id FROM crops WHERE crop_name IN ('Ngô', 'Ngo', 'Corn') ORDER BY crop_id LIMIT 1;
SELECT @crop_soy_id  := crop_id FROM crops WHERE crop_name IN ('Đậu nành', 'Dau nanh', 'Soybean') ORDER BY crop_id LIMIT 1;

SELECT @variety_rice_id := id FROM varieties WHERE crop_id = @crop_rice_id ORDER BY id LIMIT 1;
SELECT @variety_corn_id := id FROM varieties WHERE crop_id = @crop_corn_id ORDER BY id LIMIT 1;
SELECT @variety_soy_id  := id FROM varieties WHERE crop_id = @crop_soy_id  ORDER BY id LIMIT 1;

-- 2) Attach the demo plot to the existing An Phú farm; create fallback only if not found.
SELECT @farm_id := farm_id
FROM farms
WHERE user_id = @farmer_id
  AND farm_name = 'Trang trại Lúa Hữu Cơ An Phú'
LIMIT 1;

INSERT INTO farms (user_id, farm_name, province_id, ward_id, area, latitude, longitude, average_rating, rating_count, active)
SELECT @farmer_id, 'Trang trại Lúa Hữu Cơ An Phú', COALESCE(@province_id, 87), COALESCE(@ward_id, 87002),
       18.50, 10.531200, 105.621500, 4.8, 6, TRUE
WHERE @farm_id IS NULL;

SET @farm_id = COALESCE(@farm_id, LAST_INSERT_ID());

-- 3) Make the script idempotent: remove previous supplement data only.
SELECT @old_fdn_plot_id := plot_id
FROM plots
WHERE farm_id = @farm_id
  AND plot_name = 'Lô FDN Demo - Luân canh Lúa/Ngô/Đậu'
LIMIT 1;

DELETE nie
FROM nutrient_input_events nie
         JOIN seasons s ON s.season_id = nie.season_id
WHERE s.plot_id = @old_fdn_plot_id;

DELETE iwa
FROM irrigation_water_analyses iwa
         JOIN seasons s ON s.season_id = iwa.season_id
WHERE s.plot_id = @old_fdn_plot_id;

DELETE st
FROM soil_tests st
         JOIN seasons s ON s.season_id = st.season_id
WHERE s.plot_id = @old_fdn_plot_id;

DELETE FROM seasons WHERE plot_id = @old_fdn_plot_id;
DELETE FROM plots WHERE plot_id = @old_fdn_plot_id;

-- 4) Create one clear demo plot for FDN dashboard/testing.
INSERT INTO plots (farm_id, plot_name, area, soil_type, boundary_geojson, status, created_by, created_at, updated_at)
VALUES (
           @farm_id,
           'Lô FDN Demo - Luân canh Lúa/Ngô/Đậu',
           2.50,
           'Đất phù sa',
           '{"type":"Polygon","coordinates":[[[105.6210,10.5310],[105.6225,10.5310],[105.6225,10.5323],[105.6210,10.5323],[105.6210,10.5310]]]}',
           'IN_USE',
           @farmer_id,
           '2021-01-01 08:00:00',
           NOW()
       );
SET @fdn_plot_id = LAST_INSERT_ID();

-- 5) Seasons: high-FDN cereal baseline, then legume rotation improvement.
INSERT INTO seasons
(season_name, plot_id, crop_id, variety_id, start_date, planned_harvest_date, end_date, status,
 initial_plant_count, current_plant_count, expected_yield_kg, actual_yield_kg, budget_amount, notes, created_at)
VALUES
    ('FDN Demo 2021 - Ngô Xuân (FDN cao)', @fdn_plot_id, @crop_corn_id, @variety_corn_id,
     '2021-01-15', '2021-04-25', '2021-04-28', 'COMPLETED', 18000, 17500, 2500.00, 2450.00, 8500000.00,
     'Baseline năm 1: ngô phụ thuộc chủ yếu vào phân đạm khoáng, chưa có cây họ đậu.', '2021-01-01 08:00:00'),

    ('FDN Demo 2021 - Lúa Hè (FDN cao)', @fdn_plot_id, @crop_rice_id, @variety_rice_id,
     '2021-05-15', '2021-08-20', '2021-08-25', 'COMPLETED', 220000, 215000, 3100.00, 3150.00, 16000000.00,
     'Tiếp tục lúa sau ngô, thiếu nguồn cố định đạm sinh học nên FDN vẫn cao.', '2021-05-01 08:00:00'),

    ('FDN Demo 2022 - Lúa Đông Xuân (FDN cao)', @fdn_plot_id, @crop_rice_id, @variety_rice_id,
     '2022-01-10', '2022-04-20', '2022-04-25', 'COMPLETED', 225000, 219000, 3000.00, 3050.00, 16200000.00,
     'Năm 2 vẫn chưa luân canh họ đậu, lượng đạm khoáng chiếm tỷ trọng lớn.', '2022-01-01 08:00:00'),

    ('FDN Demo 2022 - Ngô Hè Thu (FDN cao)', @fdn_plot_id, @crop_corn_id, @variety_corn_id,
     '2022-05-10', '2022-08-20', '2022-08-25', 'COMPLETED', 18800, 18100, 2700.00, 2700.00, 8800000.00,
     'Ngô sau lúa không có nguồn N sinh học bổ sung, FDN vẫn ở vùng cảnh báo.', '2022-05-01 08:00:00'),

    ('FDN Demo 2023 - Lúa Hè Baseline (FDN cao)', @fdn_plot_id, @crop_rice_id, @variety_rice_id,
     '2023-05-15', '2023-08-20', '2023-08-25', 'COMPLETED', 230000, 224000, 3300.00, 3350.00, 17000000.00,
     'Baseline so sánh: lúa hè phụ thuộc mạnh vào phân khoáng trước khi đổi chiến lược.', '2023-05-01 08:00:00'),

    ('FDN Demo 2024 - Đậu nành Xuân (Bắt đầu giảm FDN)', @fdn_plot_id, @crop_soy_id, @variety_soy_id,
     '2024-01-15', '2024-04-20', '2024-04-25', 'COMPLETED', 135000, 130000, 1100.00, 1150.00, 6500000.00,
     'Lần đầu đưa cây họ đậu vào chu kỳ, bắt đầu tăng biological fixation và cải thiện đất.', '2024-01-01 08:00:00'),

    ('FDN Demo 2024 - Lúa Hè Thu (Sau đậu nành)', @fdn_plot_id, @crop_rice_id, @variety_rice_id,
     '2024-05-15', '2024-08-20', '2024-08-25', 'COMPLETED', 225000, 219000, 3200.00, 3280.00, 15800000.00,
     'Lúa sau đậu nành bắt đầu hưởng soil legacy, giảm phân khoáng so với baseline.', '2024-05-01 08:00:00'),

    ('FDN Demo 2025 - Đậu nành Hè (Tích lũy soil legacy)', @fdn_plot_id, @crop_soy_id, @variety_soy_id,
     '2025-05-15', '2025-08-20', '2025-08-25', 'COMPLETED', 140000, 136000, 1150.00, 1180.00, 6500000.00,
     'Đậu nành tiếp tục tạo nốt sần và tích lũy soil legacy cho vụ ngũ cốc sau.', '2025-05-01 08:00:00'),

    ('FDN Demo 2025 - Ngô Thu (Sau hai vụ họ đậu)', @fdn_plot_id, @crop_corn_id, @variety_corn_id,
     '2025-09-15', '2025-12-20', '2025-12-25', 'COMPLETED', 20000, 19500, 3050.00, 3100.00, 8200000.00,
     'Ngô sau hai vụ họ đậu cần ít phân khoáng hơn, FDN giảm rõ so với 2021-2023.', '2025-09-01 08:00:00'),

    ('FDN Demo 2026 - Đậu nành Xuân (Chốt cải tạo trước lúa)', @fdn_plot_id, @crop_soy_id, @variety_soy_id,
     '2026-01-15', '2026-05-10', '2026-05-10', 'COMPLETED', 145000, 140000, 1200.00, 1250.00, 6800000.00,
     'Vụ đậu nành ngay trước mùa lúa 2026, tạo nền soil legacy để kiểm tra FDN đầu vụ lúa.', '2026-01-01 08:00:00'),

    ('FDN Demo 2026 - Lúa Hè 26/05 (FDN đã giảm)', @fdn_plot_id, @crop_rice_id, @variety_rice_id,
     '2026-05-26', '2026-09-05', NULL, 'ACTIVE', 220000, 220000, 3350.00, NULL, 15500000.00,
     'Happy path: ngày 26/05/2026 nhập đủ phân bón, nước tưới và chất lượng đất để chứng minh FDN giảm nhờ luân canh cây họ đậu.', '2026-05-26 08:00:00');

-- 6) Aggregate nutrient inputs. Keep input_source values compatible with the current seed file.
INSERT INTO nutrient_input_events
(season_id, plot_id, input_source, n_kg, applied_date, measured, data_source, source_type, source_document,
 note, created_by_user_id, created_at)
VALUES
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2021 - Ngô Xuân (FDN cao)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 145.0000, '2021-01-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2021-corn-mineral.pdf', '2021 ngô xuân: phân khoáng cao do chưa có cây họ đậu.', @farmer_id, '2021-01-20 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2021 - Ngô Xuân (FDN cao)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 15.0000, '2021-01-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2021-corn-organic.pdf', '2021 ngô xuân: bổ sung hữu cơ thấp, chưa đủ kéo FDN xuống.', @farmer_id, '2021-01-20 08:05:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2021 - Lúa Hè (FDN cao)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 165.0000, '2021-05-25', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2021-rice-mineral.pdf', '2021 lúa hè: tiếp tục phụ thuộc phân khoáng.', @farmer_id, '2021-05-25 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2021 - Lúa Hè (FDN cao)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 18.0000, '2021-05-25', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2021-rice-organic.pdf', '2021 lúa hè: hữu cơ thấp, chưa có biological fixation.', @farmer_id, '2021-05-25 08:05:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2022 - Lúa Đông Xuân (FDN cao)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 170.0000, '2022-01-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2022-rice-mineral.pdf', '2022 lúa đông xuân: vẫn chưa có nguồn N sinh học.', @farmer_id, '2022-01-20 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2022 - Lúa Đông Xuân (FDN cao)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 20.0000, '2022-01-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2022-rice-organic.pdf', '2022 lúa đông xuân: hữu cơ chưa đủ bù nền đất nghèo đạm.', @farmer_id, '2022-01-20 08:05:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2022 - Ngô Hè Thu (FDN cao)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 155.0000, '2022-05-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2022-corn-mineral.pdf', '2022 ngô hè thu: FDN vẫn cao.', @farmer_id, '2022-05-20 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2022 - Ngô Hè Thu (FDN cao)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 15.0000, '2022-05-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2022-corn-organic.pdf', '2022 ngô hè thu: chưa có cây họ đậu trong chu kỳ.', @farmer_id, '2022-05-20 08:05:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2023 - Lúa Hè Baseline (FDN cao)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 165.0000, '2023-05-25', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2023-rice-mineral.pdf', 'Baseline 2023: phân khoáng rất cao để so sánh với 2026.', @farmer_id, '2023-05-25 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2023 - Lúa Hè Baseline (FDN cao)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 18.0000, '2023-05-25', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2023-rice-organic.pdf', 'Baseline 2023: hữu cơ thấp, soil legacy thấp.', @farmer_id, '2023-05-25 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2023 - Lúa Hè Baseline (FDN cao)'), @fdn_plot_id, 'IRRIGATION_WATER', 14.9500, '2023-05-25', TRUE, 'water-analysis', 'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-2023-rice-water.pdf', 'Baseline 2023: N từ nước tưới nhỏ so với phân bón.', @farmer_id, '2023-05-25 11:10:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Đậu nành Xuân (Bắt đầu giảm FDN)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 25.0000, '2024-01-22', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2024-soy-mineral.pdf', '2024 đậu nành: giảm mạnh phân khoáng.', @farmer_id, '2024-01-22 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Đậu nành Xuân (Bắt đầu giảm FDN)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 18.0000, '2024-01-22', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2024-soy-organic.pdf', '2024 đậu nành: bổ sung hữu cơ nền.', @farmer_id, '2024-01-22 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Đậu nành Xuân (Bắt đầu giảm FDN)'), @fdn_plot_id, 'BIOLOGICAL_FIXATION', 90.0000, '2024-01-22', TRUE, 'system-estimate', 'SYSTEM_ESTIMATED', '/seed-evidence/fdn/fdn-demo-2024-soy-fixation.pdf', '2024 đậu nành: bắt đầu có cố định đạm sinh học.', @farmer_id, '2024-01-22 08:10:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Lúa Hè Thu (Sau đậu nành)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 90.0000, '2024-05-25', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2024-rice-mineral.pdf', '2024 lúa sau đậu nành: giảm phân khoáng nhờ soil legacy.', @farmer_id, '2024-05-25 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Lúa Hè Thu (Sau đậu nành)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 18.0000, '2024-05-25', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2024-rice-organic.pdf', '2024 lúa sau đậu nành: giữ hữu cơ ổn định.', @farmer_id, '2024-05-25 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Lúa Hè Thu (Sau đậu nành)'), @fdn_plot_id, 'IRRIGATION_WATER', 11.2500, '2024-05-25', TRUE, 'water-analysis', 'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-2024-rice-water.pdf', '2024 lúa sau đậu nành: có mẫu nước tưới đo thực tế.', @farmer_id, '2024-05-25 11:10:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Đậu nành Hè (Tích lũy soil legacy)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 22.0000, '2025-05-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2025-soy-mineral.pdf', '2025 đậu nành: tiếp tục giảm phân khoáng.', @farmer_id, '2025-05-20 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Đậu nành Hè (Tích lũy soil legacy)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 18.0000, '2025-05-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2025-soy-organic.pdf', '2025 đậu nành: bổ sung hữu cơ vừa phải.', @farmer_id, '2025-05-20 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Đậu nành Hè (Tích lũy soil legacy)'), @fdn_plot_id, 'BIOLOGICAL_FIXATION', 105.0000, '2025-05-20', TRUE, 'system-estimate', 'SYSTEM_ESTIMATED', '/seed-evidence/fdn/fdn-demo-2025-soy-fixation.pdf', '2025 đậu nành: cố định đạm sinh học tăng.', @farmer_id, '2025-05-20 08:10:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Ngô Thu (Sau hai vụ họ đậu)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 75.0000, '2025-09-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2025-corn-mineral.pdf', '2025 ngô sau hai vụ họ đậu: giảm phân khoáng rõ.', @farmer_id, '2025-09-20 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Ngô Thu (Sau hai vụ họ đậu)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 15.0000, '2025-09-20', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2025-corn-organic.pdf', '2025 ngô: soil legacy hỗ trợ giảm FDN.', @farmer_id, '2025-09-20 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Ngô Thu (Sau hai vụ họ đậu)'), @fdn_plot_id, 'IRRIGATION_WATER', 8.9700, '2025-09-20', TRUE, 'water-analysis', 'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-2025-corn-water.pdf', '2025 ngô: mẫu nước tưới hỗ trợ giải thích FDN giảm.', @farmer_id, '2025-09-20 11:10:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Đậu nành Xuân (Chốt cải tạo trước lúa)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 20.0000, '2026-01-22', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2026-soy-mineral.pdf', '2026 đậu nành: chốt cải tạo trước lúa.', @farmer_id, '2026-01-22 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Đậu nành Xuân (Chốt cải tạo trước lúa)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 12.0000, '2026-01-22', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2026-soy-organic.pdf', '2026 đậu nành: hữu cơ nền trước lúa.', @farmer_id, '2026-01-22 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Đậu nành Xuân (Chốt cải tạo trước lúa)'), @fdn_plot_id, 'BIOLOGICAL_FIXATION', 110.0000, '2026-01-22', TRUE, 'system-estimate', 'SYSTEM_ESTIMATED', '/seed-evidence/fdn/fdn-demo-2026-soy-fixation.pdf', '2026 đậu nành: tạo nền soil legacy cho lúa hè.', @farmer_id, '2026-01-22 08:10:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Lúa Hè 26/05 (FDN đã giảm)'), @fdn_plot_id, 'MINERAL_FERTILIZER', 30.0000, '2026-05-26', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2026-rice-mineral.pdf', '2026 lúa hè 26/05: phân khoáng đã giảm mạnh so với baseline 2023.', @farmer_id, '2026-05-26 08:00:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Lúa Hè 26/05 (FDN đã giảm)'), @fdn_plot_id, 'ORGANIC_FERTILIZER', 10.0000, '2026-05-26', TRUE, 'fdn-demo', 'USER_ENTERED', '/seed-evidence/fdn/fdn-demo-2026-rice-organic.pdf', '2026 lúa hè 26/05: hữu cơ vừa phải.', @farmer_id, '2026-05-26 08:05:00'),
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Lúa Hè 26/05 (FDN đã giảm)'), @fdn_plot_id, 'IRRIGATION_WATER', 8.1250, '2026-05-26', TRUE, 'water-analysis', 'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-2026-rice-water.pdf', '2026 lúa hè 26/05: dữ liệu nước tưới đo thực tế.', @farmer_id, '2026-05-26 09:15:00');

-- 7) Dedicated water analysis rows used by FDN calculation/explanation screens.
INSERT INTO irrigation_water_analyses
(season_id, plot_id, sample_date, nitrate_mg_per_l, ammonium_mg_per_l, total_n_mg_per_l, irrigation_volume_m3,
 legacy_n_contribution_kg, legacy_event_id, legacy_derived, measured, source_type, source_document, lab_reference,
 note, created_by_user_id, created_at)
VALUES
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2023 - Lúa Hè Baseline (FDN cao)'), @fdn_plot_id,
     '2023-05-25', 9.0000, 2.5000, 11.5000, 1300.0000, 14.9500, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-water-2023.pdf', 'LAB-FDN-DEMO-IRR-230525',
     'Baseline 2023: mẫu nước tưới trước luân canh cây họ đậu.', @farmer_id, '2023-05-25 11:00:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Lúa Hè Thu (Sau đậu nành)'), @fdn_plot_id,
     '2024-05-15', 7.0000, 2.0000, 9.0000, 1250.0000, 11.2500, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-water-2024.pdf', 'LAB-FDN-DEMO-IRR-240515',
     'Sau vụ đậu nành đầu tiên: nước tưới được đo để tránh fallback estimation.', @farmer_id, '2024-05-15 11:00:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Ngô Thu (Sau hai vụ họ đậu)'), @fdn_plot_id,
     '2025-09-15', 6.0000, 1.8000, 7.8000, 1150.0000, 8.9700, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-water-2025.pdf', 'LAB-FDN-DEMO-IRR-250915',
     'Ngô sau hai vụ họ đậu: dữ liệu nước tưới hỗ trợ giải thích FDN giảm.', @farmer_id, '2025-09-15 11:00:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Lúa Hè 26/05 (FDN đã giảm)'), @fdn_plot_id,
     '2026-05-26', 4.5000, 2.0000, 6.5000, 1250.0000, 8.1250, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-water-2026.pdf', 'LAB-FDN-DEMO-IRR-260526',
     'Ngày bắt đầu vụ lúa 26/05/2026: nhập chỉ số nước tưới để tính N từ nước tưới bằng dữ liệu đo thực tế.', @farmer_id, '2026-05-26 09:15:00');

-- 8) Dedicated soil tests: baseline soil poor -> improved after legume rotation.
INSERT INTO soil_tests
(season_id, plot_id, sample_date, soil_organic_matter_pct, mineral_n_kg_per_ha, nitrate_mg_per_kg,
 ammonium_mg_per_kg, legacy_n_contribution_kg, legacy_event_id, legacy_derived, measured, source_type,
 source_document, lab_reference, note, created_by_user_id, created_at)
VALUES
    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2023 - Lúa Hè Baseline (FDN cao)'), @fdn_plot_id,
     '2023-05-25', 2.1000, 8.0000, 10.0000, 3.0000, 2.0000, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-soil-2023.pdf', 'LAB-FDN-DEMO-SOIL-230525',
     'Baseline 2023: đất sau nhiều vụ lúa/ngô liên tục, hữu cơ và mineral N thấp nên FDN cao.', @farmer_id, '2023-05-25 14:00:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2024 - Lúa Hè Thu (Sau đậu nành)'), @fdn_plot_id,
     '2024-05-15', 2.8000, 12.0000, 14.0000, 4.0000, 30.0000, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-soil-2024.pdf', 'LAB-FDN-DEMO-SOIL-240515',
     'Sau vụ đậu nành đầu tiên: soil legacy tăng, bắt đầu giảm nhu cầu bón đạm khoáng cho lúa.', @farmer_id, '2024-05-15 14:00:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2025 - Ngô Thu (Sau hai vụ họ đậu)'), @fdn_plot_id,
     '2025-09-15', 3.4000, 16.8000, 17.5000, 4.8000, 42.0000, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-soil-2025.pdf', 'LAB-FDN-DEMO-SOIL-250915',
     'Sau hai vụ họ đậu năm 2025: soil legacy đủ rõ để ngô giảm phân khoáng.', @farmer_id, '2025-09-15 14:00:00'),

    ((SELECT season_id FROM seasons WHERE plot_id=@fdn_plot_id AND season_name='FDN Demo 2026 - Lúa Hè 26/05 (FDN đã giảm)'), @fdn_plot_id,
     '2026-05-26', 3.9000, 18.0000, 18.0000, 5.0000, 45.0000, NULL, FALSE, TRUE,
     'LAB_MEASURED', '/seed-evidence/fdn/fdn-demo-soil-2026.pdf', 'LAB-FDN-DEMO-SOIL-260526',
     'Ngày bắt đầu vụ lúa 26/05/2026: chất lượng đất đã cải thiện sau đậu nành xuân, soil legacy giúp giảm FDN đầu vụ.', @farmer_id, '2026-05-26 10:00:00');

-- FDN supplement verification: should return demo seasons and counts for farmer@acm.local.
SELECT
    u.email,
    f.farm_name,
    p.plot_name,
    s.season_name,
    s.status,
    s.start_date,
    COUNT(DISTINCT nie.id) AS nutrient_event_count,
    COUNT(DISTINCT st.id) AS soil_test_count,
    COUNT(DISTINCT iwa.id) AS water_analysis_count,
    ROUND(SUM(CASE WHEN nie.input_source IN ('MINERAL_FERTILIZER','ORGANIC_FERTILIZER') THEN nie.n_kg ELSE 0 END), 2) AS fertilizer_n_kg,
    ROUND(SUM(COALESCE(nie.n_kg, 0)), 2) AS nutrient_event_n_kg
FROM users u
         JOIN farms f ON f.user_id = u.user_id
         JOIN plots p ON p.farm_id = f.farm_id
         JOIN seasons s ON s.plot_id = p.plot_id
         LEFT JOIN nutrient_input_events nie ON nie.season_id = s.season_id
         LEFT JOIN soil_tests st ON st.season_id = s.season_id
         LEFT JOIN irrigation_water_analyses iwa ON iwa.season_id = s.season_id
WHERE u.email = 'farmer@acm.local'
  AND p.plot_name = 'Lô FDN Demo - Luân canh Lúa/Ngô/Đậu'
GROUP BY u.email, f.farm_name, p.plot_name, s.season_id, s.season_name, s.status, s.start_date
ORDER BY s.start_date;

-- =========================================================
-- 10. Verification queries
-- =========================================================

SELECT 'users' AS table_name, COUNT(*) AS total FROM users
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'farms', COUNT(*) FROM farms
UNION ALL SELECT 'plots', COUNT(*) FROM plots
UNION ALL SELECT 'seasons', COUNT(*) FROM seasons
UNION ALL SELECT 'tasks', COUNT(*) FROM tasks
UNION ALL SELECT 'field_logs', COUNT(*) FROM field_logs
UNION ALL SELECT 'expenses', COUNT(*) FROM expenses
UNION ALL SELECT 'harvests', COUNT(*) FROM harvests
UNION ALL SELECT 'incidents', COUNT(*) FROM incidents
UNION ALL SELECT 'suppliers', COUNT(*) FROM suppliers
UNION ALL SELECT 'supply_items', COUNT(*) FROM supply_items
UNION ALL SELECT 'supply_lots', COUNT(*) FROM supply_lots
UNION ALL SELECT 'stock_movements', COUNT(*) FROM stock_movements
UNION ALL SELECT 'inventory_balances', COUNT(*) FROM inventory_balances
UNION ALL SELECT 'soil_tests', COUNT(*) FROM soil_tests
UNION ALL SELECT 'nutrient_input_events', COUNT(*) FROM nutrient_input_events
UNION ALL SELECT 'product_warehouse_lots', COUNT(*) FROM product_warehouse_lots
UNION ALL SELECT 'marketplace_products', COUNT(*) FROM marketplace_products
UNION ALL SELECT 'marketplace_orders', COUNT(*) FROM marketplace_orders
UNION ALL SELECT 'marketplace_product_reviews', COUNT(*) FROM marketplace_product_reviews
UNION ALL SELECT 'season_employees', COUNT(*) FROM season_employees
UNION ALL SELECT 'payroll_records', COUNT(*) FROM payroll_records
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications
UNION ALL SELECT 'documents', COUNT(*) FROM documents
UNION ALL SELECT 'audit_logs', COUNT(*) FROM audit_logs;

SELECT
    'orphan_marketplace_order_items' AS check_name,
    COUNT(*) AS total
FROM marketplace_order_items oi
LEFT JOIN marketplace_orders o ON o.id = oi.order_id
LEFT JOIN marketplace_products p ON p.id = oi.product_id
WHERE o.id IS NULL OR p.id IS NULL;

SELECT
    'seed_accounts' AS info,
    email,
    user_name,
    status
FROM users
WHERE email IN (
    'admin@acm.local',
    'farmer@acm.local',
    'farmer.binhminh@example.com',
    'employee@acm.local',
    'employee.lanthao@example.com',
    'buyer@acm.local',
    'buyer.thucphamantoan@example.com'
)
ORDER BY user_id;

SELECT
    'bootstrap_visibility' AS info,
    u.email,
    (SELECT COUNT(*) FROM farms f WHERE f.user_id = u.user_id) AS owned_farms,
    (SELECT COUNT(*) FROM seasons s JOIN plots p ON p.plot_id = s.plot_id JOIN farms f ON f.farm_id = p.farm_id WHERE f.user_id = u.user_id) AS owned_seasons,
    (SELECT COUNT(*) FROM tasks t WHERE t.user_id = u.user_id) AS assigned_tasks,
    (SELECT COUNT(*) FROM marketplace_orders o WHERE o.buyer_user_id = u.user_id) AS buyer_orders,
    (SELECT COUNT(*) FROM marketplace_carts c WHERE c.user_id = u.user_id) AS carts,
    (SELECT COUNT(*) FROM notifications n WHERE n.user_id = u.user_id) AS notifications
FROM users u
WHERE u.email IN (
    'admin@acm.local',
    'farmer@acm.local',
    'employee@acm.local',
    'buyer@acm.local'
)
ORDER BY u.user_id;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
