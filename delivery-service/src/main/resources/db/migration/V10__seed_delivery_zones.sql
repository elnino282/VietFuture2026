-- Seed 3 basic delivery zones into delivery_rates if not exist

-- HCM Nội thành
INSERT INTO delivery_rates (provider_id, zone_from, zone_to, weight_min_kg, weight_max_kg, base_rate_vnd, per_kg_vnd, estimated_hours, is_cold_chain)
SELECT 1, 'HCM', 'HCM_NOI_DO', 0, 5, 20000, 5000, 12, FALSE
WHERE NOT EXISTS (SELECT 1 FROM delivery_rates WHERE zone_to = 'HCM_NOI_DO');

-- HCM Tỉnh (Ngoại thành / Tỉnh lân cận)
INSERT INTO delivery_rates (provider_id, zone_from, zone_to, weight_min_kg, weight_max_kg, base_rate_vnd, per_kg_vnd, estimated_hours, is_cold_chain)
SELECT 1, 'HCM', 'HCM_TINH', 0, 5, 35000, 8000, 24, FALSE
WHERE NOT EXISTS (SELECT 1 FROM delivery_rates WHERE zone_to = 'HCM_TINH');

-- Toàn quốc
INSERT INTO delivery_rates (provider_id, zone_from, zone_to, weight_min_kg, weight_max_kg, base_rate_vnd, per_kg_vnd, estimated_hours, is_cold_chain)
SELECT 1, 'HCM', 'TOAN_QUOC', 0, 5, 50000, 15000, 72, FALSE
WHERE NOT EXISTS (SELECT 1 FROM delivery_rates WHERE zone_to = 'TOAN_QUOC');

