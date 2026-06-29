-- V2__backfill_read_models.sql
-- Seeds the read-only reporting service summaries from monolithic and other service schemas.

-- 1. Users Summary
INSERT IGNORE INTO admin_user_summary (user_id, username, full_name, email, status, role_code)
SELECT u.user_id, u.user_name, u.full_name, u.email, u.status, r.role_code
FROM identity_db.users u
LEFT JOIN identity_db.user_roles ur ON u.user_id = ur.user_id
LEFT JOIN identity_db.roles r ON ur.role_id = r.role_id;

-- 2. Farms Summary
INSERT IGNORE INTO admin_farm_summary (farm_id, farm_name)
SELECT farm_id, farm_name FROM farm_db.farms;

-- 3. Plots Summary
INSERT IGNORE INTO admin_plot_summary (plot_id, plot_name, farm_id, area)
SELECT plot_id, plot_name, farm_id, COALESCE(area, 0) FROM farm_db.plots;

-- 4. Seasons Summary
INSERT IGNORE INTO admin_season_summary (season_id, season_name, plot_id, crop_id, crop_name, variety_id, variety_name, status, start_date, expected_yield_kg, actual_yield_kg)
SELECT s.season_id, s.season_name, s.plot_id, s.crop_id, c.crop_name, s.variety_id, v.name, s.status, s.start_date, s.expected_yield_kg, s.actual_yield_kg
FROM season_db.seasons s
LEFT JOIN crop_catalog_db.crops c ON s.crop_id = c.crop_id
LEFT JOIN crop_catalog_db.varieties v ON s.variety_id = v.id;

-- 5. Incidents Summary
INSERT IGNORE INTO admin_incident_summary (incident_id, season_id, status)
SELECT id, season_id, status FROM incident_db.incidents;

-- 6. Tasks Summary
INSERT IGNORE INTO admin_task_summary (task_id, season_id, status)
SELECT task_id, season_id, status FROM season_db.tasks WHERE season_id IS NOT NULL;

-- 7. Alerts Summary
INSERT IGNORE INTO admin_alert_summary (alert_id, season_id, type, severity, status)
SELECT id, season_id, type, severity, status FROM incident_db.alerts;

-- 8. Inventory Lots Summary
INSERT IGNORE INTO admin_inventory_lot_summary (lot_id, farm_id, farm_name, expiry_date)
SELECT sl.id, w.farm_id, COALESCE(f.farm_name, CONCAT('Farm ', w.farm_id)), sl.expiry_date
FROM inventory_db.supply_lots sl
JOIN inventory_db.inventory_balances ib ON sl.id = ib.supply_lot_id
JOIN inventory_db.warehouses w ON ib.warehouse_id = w.id
LEFT JOIN farm_db.farms f ON w.farm_id = f.farm_id;

-- 9. Harvests Summary
INSERT IGNORE INTO admin_harvest_summary (harvest_id, season_id, quantity, unit_price)
SELECT harvest_id, season_id, quantity, COALESCE(unit, 0) FROM season_db.harvests;

-- 10. Expenses Summary
INSERT IGNORE INTO admin_expense_summary (expense_id, season_id, total_cost, category, item_name, expense_date)
SELECT expense_id, season_id, COALESCE(total_cost, amount), category, item_name, expense_date
FROM finance_db.expenses;

-- 11. Marketplace Orders Summary
INSERT IGNORE INTO admin_marketplace_order_summary (order_id, order_code, buyer_id, buyer_name, total_amount, payment_status, status, payment_proof_uploaded_at, created_at)
SELECT o.id, o.order_code, o.buyer_user_id, COALESCE(u.full_name, u.user_name, CONCAT('Buyer ', o.buyer_user_id)), o.total_amount, o.payment_verification_status, o.status, o.payment_proof_uploaded_at, o.created_at
FROM marketplace_db.marketplace_orders o
LEFT JOIN identity_db.users u ON o.buyer_user_id = u.user_id;

-- 12. Marketplace Products Summary
INSERT IGNORE INTO admin_marketplace_product_summary (product_id, product_name, farm_id, farm_name, farmer_id, farmer_name, status, updated_at)
SELECT p.id, p.name, p.farm_id, p.farm_name, p.farmer_user_id, p.farmer_display_name, p.status, p.updated_at
FROM marketplace_db.marketplace_products p;

-- 13. Audit Log Entries
INSERT IGNORE INTO admin_audit_log_entries (audit_log_id, entity_type, entity_id, operation, performed_by, performed_at, snapshot_data, reason, ip_address)
SELECT audit_log_id, entity_type, entity_id, operation, performed_by, performed_at, snapshot_data, reason, ip_address
FROM quanlymuavu.audit_logs;

-- 14. Documents
INSERT IGNORE INTO admin_documents (document_id, title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type, view_count, is_pinned, created_at, updated_at)
SELECT document_id, title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type, view_count, is_pinned, created_at, updated_at
FROM quanlymuavu.documents;
