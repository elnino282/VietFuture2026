-- Seed tiêu chuẩn VietGAP Trồng trọt TCVN 11892-1:2017
INSERT INTO certification_standards (code, name, type, description) VALUES
('VIETGAP-PLANTING-2024', 'VietGAP Trồng trọt 2024', 'VIETGAP_PLANTING',
 'Thực hành nông nghiệp tốt cho trồng trọt theo TCVN 11892-1:2017');

-- Seed checklist items (sample — 4 nhóm chính)
INSERT INTO certification_checklist_items (standard_id, item_code, category, description, is_mandatory, weight_pct, data_source_type, data_source_query) VALUES
-- NHÓM 1: Vùng sản xuất
(1, 'PA-001', 'PRODUCTION_AREA', 'Đất sản xuất không bị ô nhiễm, có kết quả phân tích đất trong vòng 12 tháng', TRUE, 5.00, 'SOIL_TEST', '{"seasonId": null, "freshnessDays": 365}'),
(1, 'PA-002', 'PRODUCTION_AREA', 'Nguồn nước tưới đạt QCVN 08-MT:2015/BTNMT', TRUE, 5.00, 'WATER_TEST', '{"seasonId": null, "freshnessDays": 365}'),
(1, 'PA-003', 'PRODUCTION_AREA', 'Vùng sản xuất có sơ đồ mặt bằng rõ ràng', TRUE, 3.00, 'MANUAL', NULL),

-- NHÓM 2: Giống cây trồng
(1, 'SE-001', 'SEED', 'Sử dụng giống có nguồn gốc rõ ràng, có giấy chứng nhận nguồn giống', TRUE, 4.00, 'FIELD_LOG', '{"logType": "SEEDING"}'),
(1, 'SE-002', 'SEED', 'Ghi chép ngày gieo trồng và nguồn giống', TRUE, 2.00, 'FIELD_LOG', '{"logType": "SEEDING"}'),

-- NHÓM 3: Canh tác
(1, 'CU-001', 'CULTIVATION', 'Ghi chép đầy đủ phân bón đã sử dụng (loại, lượng, ngày)', TRUE, 5.00, 'FIELD_LOG', '{"logType": "FERTILIZER_APPLICATION"}'),
(1, 'CU-002', 'CULTIVATION', 'Ghi chép đầy đủ thuốc BVTV đã sử dụng (tên, ngày phun, PHI)', TRUE, 5.00, 'FIELD_LOG', '{"logType": "PESTICIDE_APPLICATION"}'),
(1, 'CU-003', 'CULTIVATION', 'Tuân thủ thời gian cách ly (PHI) trước thu hoạch', TRUE, 5.00, 'PHI_CHECK', NULL),

-- NHÓM 4: Thu hoạch & Bảo quản
(1, 'HV-001', 'HARVEST', 'Thu hoạch đúng thời điểm, có nhật ký thu hoạch', TRUE, 3.00, 'FIELD_LOG', '{"logType": "HARVEST"}'),
(1, 'HV-002', 'HARVEST', 'Sản phẩm sau thu hoạch được bảo quản đúng cách, có hồ sơ kho', TRUE, 2.00, 'MANUAL', NULL);
