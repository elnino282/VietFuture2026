-- Thêm fields phân loại chất lượng, đóng gói, sơ chế cho bảng harvests
ALTER TABLE harvests ADD COLUMN quality_grade VARCHAR(20) DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN quality_notes TEXT DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN sub_standard_quantity DECIMAL(19,3) DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN sub_standard_disposition VARCHAR(30) DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN packaging_type VARCHAR(30) DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN packaging_count INT DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN processing_type VARCHAR(30) DEFAULT NULL;
ALTER TABLE harvests ADD COLUMN crop_category VARCHAR(30) DEFAULT NULL;
