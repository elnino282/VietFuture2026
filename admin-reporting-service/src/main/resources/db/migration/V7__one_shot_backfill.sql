-- V7__one_shot_backfill.sql
-- One-shot migration to backfill read models from source tables.
-- This replaces the programmatic AdminReportingBackfillRunner for production deployments.
-- This migration runs once during deployment and is tracked by Flyway.
-- New data will be populated via event-driven sync (domain events → AdminEventListener).

-- NOTE: This migration assumes the source databases are accessible at deployment time.
-- For isolated database-per-service deployments, set spring.flyway.locations to skip this migration
-- and rely solely on event-driven sync.

-- Backfill Users
INSERT IGNORE INTO admin_user_summary (user_id, username, full_name, email, status, role_code)
SELECT u.user_id, u.user_name, u.full_name, u.email, u.status, r.role_code
FROM identity_db.users u
LEFT JOIN identity_db.user_roles ur ON u.user_id = ur.user_id
LEFT JOIN identity_db.roles r ON ur.role_id = r.role_id;

-- Backfill Farms
INSERT IGNORE INTO admin_farm_summary (farm_id, farm_name)
SELECT farm_id, farm_name FROM farm_db.farms;

-- Backfill Plots
INSERT IGNORE INTO admin_plot_summary (plot_id, plot_name, farm_id, area)
SELECT plot_id, plot_name, farm_id, COALESCE(area, 0) FROM farm_db.plots;

-- Backfill Seasons
INSERT IGNORE INTO admin_season_summary (season_id, season_name, plot_id, crop_id, crop_name, variety_id, variety_name, status, start_date, expected_yield_kg, actual_yield_kg)
SELECT s.season_id, s.season_name, s.plot_id, s.crop_id, c.crop_name, s.variety_id, v.name, s.status, s.start_date, s.expected_yield_kg, s.actual_yield_kg
FROM season_db.seasons s
LEFT JOIN crop_catalog_db.crops c ON s.crop_id = c.crop_id
LEFT JOIN crop_catalog_db.varieties v ON s.variety_id = v.id;

-- Backfill Incidents
INSERT IGNORE INTO admin_incident_summary (incident_id, season_id, status)
SELECT id, season_id, status FROM incident_db.incidents;

-- Backfill Tasks
INSERT IGNORE INTO admin_task_summary (task_id, season_id, status)
SELECT task_id, season_id, status FROM season_db.tasks WHERE season_id IS NOT NULL;

-- Backfill Alerts
INSERT IGNORE INTO admin_alert_summary (alert_id, season_id, type, severity, status)
SELECT id, season_id, type, severity, status FROM incident_db.alerts;

-- Backfill Inventory Lots (supply_lots based)
INSERT IGNORE INTO admin_inventory_lot_summary (lot_id, farm_id, farm_name, expiry_date)
SELECT sl.id, w.farm_id, COALESCE(f.farm_name, CONCAT('Farm ', w.farm_id)), sl.expiry_date
FROM inventory_db.supply_lots sl
JOIN inventory_db.inventory_balances ib ON sl.id = ib.supply_lot_id
JOIN inventory_db.warehouses w ON ib.warehouse_id = w.id
LEFT JOIN farm_db.farms f ON w.farm_id = f.farm_id;

-- Backfill Harvests
INSERT IGNORE INTO admin_harvest_summary (harvest_id, season_id, quantity, unit_price)
SELECT harvest_id, season_id, quantity, COALESCE(unit, 0) FROM season_db.harvests;

-- Backfill Expenses
INSERT IGNORE INTO admin_expense_summary (expense_id, season_id, total_cost, category, item_name, expense_date)
SELECT expense_id, season_id, COALESCE(total_cost, amount), category, item_name, expense_date
FROM finance_db.expenses;

-- Backfill Marketplace Orders
INSERT IGNORE INTO admin_marketplace_order_summary (order_id, order_code, buyer_id, buyer_name, total_amount, payment_status, status, payment_proof_uploaded_at, created_at)
SELECT o.id, o.order_code, o.buyer_user_id, COALESCE(u.full_name, u.user_name, CONCAT('Buyer ', o.buyer_user_id)), o.total_amount, o.payment_verification_status, o.status, o.payment_proof_uploaded_at, o.created_at
FROM marketplace_db.marketplace_orders o
LEFT JOIN identity_db.users u ON o.buyer_user_id = u.user_id;

-- Backfill Marketplace Products
INSERT IGNORE INTO admin_marketplace_product_summary (product_id, product_name, farm_id, farm_name, farmer_id, farmer_name, status, updated_at)
SELECT p.id, p.name, p.farm_id, p.farm_name, p.farmer_user_id, p.farmer_display_name, p.status, p.updated_at
FROM marketplace_db.marketplace_products p;

-- Backfill Audit Log Entries
INSERT IGNORE INTO admin_audit_log_entries (audit_log_id, entity_type, entity_id, operation, performed_by, performed_at, snapshot_data, reason, ip_address)
SELECT audit_log_id, entity_type, entity_id, operation, performed_by, performed_at, snapshot_data, reason, ip_address
FROM quanlymuavu.audit_logs;

-- Backfill Documents
INSERT IGNORE INTO admin_documents (document_id, title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type, view_count, is_pinned, created_at, updated_at)
SELECT document_id, title, url, description, crop, stage, topic, is_active, is_public, created_by, document_type, view_count, is_pinned, created_at, updated_at
FROM quanlymuavu.documents;

-- Backfill Marketplace Order Items
INSERT IGNORE INTO admin_marketplace_order_item_summary (item_id, order_id, season_id, quantity, unit_price, line_total)
SELECT id, order_id, season_id, quantity, unit_price_snapshot, line_total
FROM marketplace_db.marketplace_order_items;

-- Backfill Active Status to Farm Summary
UPDATE admin_farm_summary afs
JOIN farm_db.farms f ON afs.farm_id = f.farm_id
SET afs.active = f.active;

-- Backfill Incident Summary Extra Fields
UPDATE admin_incident_summary s
JOIN incident_db.incidents i ON s.incident_id = i.id
SET s.incident_type = i.incident_type,
    s.severity = i.severity,
    s.resolved_at = i.resolved_at,
    s.created_at = i.created_at;

-- Backfill Inventory Lot Summary Extra Fields (product_warehouse_lots based)
INSERT IGNORE INTO admin_inventory_lot_summary (lot_id, farm_id, farm_name, warehouse_id, warehouse_name, quantity_on_hand)
SELECT
    pwl.id AS lot_id,
    pwl.farm_id,
    COALESCE(f.farm_name, CONCAT('Farm ', pwl.farm_id)) AS farm_name,
    pwl.warehouse_id,
    w.name AS warehouse_name,
    pwl.on_hand_quantity AS quantity_on_hand
FROM inventory_db.product_warehouse_lots pwl
JOIN inventory_db.warehouses w ON pwl.warehouse_id = w.id
LEFT JOIN farm_db.farms f ON pwl.farm_id = f.farm_id
ON DUPLICATE KEY UPDATE
    farm_name = VALUES(farm_name),
    warehouse_id = VALUES(warehouse_id),
    warehouse_name = VALUES(warehouse_name),
    quantity_on_hand = VALUES(quantity_on_hand);
