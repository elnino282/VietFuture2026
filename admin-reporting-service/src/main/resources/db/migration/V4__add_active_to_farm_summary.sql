-- V4__add_active_to_farm_summary.sql
ALTER TABLE admin_farm_summary ADD COLUMN active BOOLEAN DEFAULT TRUE;

UPDATE admin_farm_summary afs
JOIN farm_db.farms f ON afs.farm_id = f.farm_id
SET afs.active = f.active;
