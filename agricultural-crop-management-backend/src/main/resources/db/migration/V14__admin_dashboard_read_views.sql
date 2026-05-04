-- Read-model views for dashboard/reporting workloads.
-- Naming convention:
--   vw_<module>_<bounded_context>_<purpose>
-- This migration is intentionally read-only (no table mutation).

CREATE OR REPLACE VIEW vw_admin_season_risk AS
SELECT
    s.season_id AS season_id,
    s.season_name AS season_name,
    s.status AS season_status,
    p.plot_id AS plot_id,
    p.plot_name AS plot_name,
    f.farm_id AS farm_id,
    f.farm_name AS farm_name,
    COALESCE(incident_agg.incident_count, 0) AS incident_count,
    COALESCE(task_agg.overdue_task_count, 0) AS overdue_task_count,
    (COALESCE(incident_agg.incident_count, 0) + COALESCE(task_agg.overdue_task_count, 0)) AS risk_score
FROM seasons s
JOIN plots p ON p.plot_id = s.plot_id
JOIN farms f ON f.farm_id = p.farm_id
LEFT JOIN (
    SELECT i.season_id, COUNT(DISTINCT i.id) AS incident_count
    FROM incidents i
    GROUP BY i.season_id
) incident_agg ON incident_agg.season_id = s.season_id
LEFT JOIN (
    SELECT t.season_id, COUNT(DISTINCT t.task_id) AS overdue_task_count
    FROM tasks t
    WHERE t.status = 'OVERDUE'
    GROUP BY t.season_id
) task_agg ON task_agg.season_id = s.season_id;

CREATE OR REPLACE VIEW vw_admin_inventory_lot_farm AS
SELECT DISTINCT
    sm.supply_lot_id AS supply_lot_id,
    w.farm_id AS farm_id
FROM stock_movements sm
JOIN warehouses w ON w.id = sm.warehouse_id
WHERE sm.supply_lot_id IS NOT NULL
  AND w.farm_id IS NOT NULL;

CREATE OR REPLACE VIEW vw_admin_inventory_lot_expiry_base AS
SELECT
    f.farm_id AS farm_id,
    f.farm_name AS farm_name,
    sl.id AS supply_lot_id,
    sl.expiry_date AS expiry_date
FROM supply_lots sl
JOIN vw_admin_inventory_lot_farm lot_farm ON lot_farm.supply_lot_id = sl.id
JOIN farms f ON f.farm_id = lot_farm.farm_id
WHERE sl.expiry_date IS NOT NULL;
