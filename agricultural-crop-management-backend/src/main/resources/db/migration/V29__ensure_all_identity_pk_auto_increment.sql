-- Ensure all JPA GenerationType.IDENTITY primary keys have AUTO_INCREMENT.
-- This migration is idempotent:
-- - Only touches columns that exist in current schema
-- - Only alters columns whose EXTRA does not contain auto_increment
-- - Preserves existing column type via information_schema.column_type

SET @schema_name := DATABASE();
SET @old_fk_checks := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE tmp_identity_targets (
    table_name VARCHAR(64) NOT NULL,
    column_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (table_name, column_name)
);

INSERT INTO tmp_identity_targets (table_name, column_name)
VALUES
    ('alerts', 'id'),
    ('audit_logs', 'audit_log_id'),
    ('crops', 'crop_id'),
    ('crop_nitrogen_references', 'id'),
    ('disease_records', 'disease_record_id'),
    ('disease_treatments', 'disease_treatment_id'),
    ('document_favorites', 'id'),
    ('document_recent_opens', 'id'),
    ('documents', 'document_id'),
    ('expenses', 'expense_id'),
    ('farms', 'farm_id'),
    ('field_logs', 'field_log_id'),
    ('harvests', 'harvest_id'),
    ('incidents', 'id'),
    ('inventory_balances', 'id'),
    ('irrigation_water_analyses', 'id'),
    ('marketplace_addresses', 'id'),
    ('marketplace_cart_items', 'id'),
    ('marketplace_carts', 'id'),
    ('marketplace_order_groups', 'id'),
    ('marketplace_order_items', 'id'),
    ('marketplace_orders', 'id'),
    ('marketplace_product_reviews', 'id'),
    ('marketplace_products', 'id'),
    ('notifications', 'id'),
    ('nutrient_input_events', 'id'),
    ('password_reset_tokens', 'id'),
    ('payroll_records', 'id'),
    ('plots', 'plot_id'),
    ('product_warehouse_lots', 'id'),
    ('product_warehouse_transactions', 'id'),
    ('roles', 'role_id'),
    ('season_employees', 'id'),
    ('seasons', 'season_id'),
    ('soil_tests', 'id'),
    ('stock_locations', 'id'),
    ('stock_movements', 'id'),
    ('suppliers', 'id'),
    ('supply_items', 'id'),
    ('supply_lots', 'id'),
    ('task_progress_logs', 'id'),
    ('tasks', 'task_id'),
    ('user_preferences', 'id'),
    ('users', 'user_id'),
    ('varieties', 'id'),
    ('warehouses', 'id');

DELIMITER $$

CREATE PROCEDURE ensure_identity_pk_auto_increment()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_column_name VARCHAR(64);
    DECLARE v_column_type VARCHAR(255);
    DECLARE v_is_nullable VARCHAR(3);

    DECLARE cur_missing_auto_inc CURSOR FOR
        SELECT c.table_name, c.column_name, c.column_type, c.is_nullable
        FROM information_schema.columns c
        INNER JOIN tmp_identity_targets t
            ON t.table_name = c.table_name
            AND t.column_name = c.column_name
        WHERE c.table_schema = @schema_name
          AND c.extra NOT LIKE '%auto_increment%';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur_missing_auto_inc;

    fix_loop: LOOP
        FETCH cur_missing_auto_inc INTO v_table_name, v_column_name, v_column_type, v_is_nullable;
        IF done = 1 THEN
            LEAVE fix_loop;
        END IF;

        SET @ddl := CONCAT(
            'ALTER TABLE `', v_table_name, '` ',
            'MODIFY COLUMN `', v_column_name, '` ', v_column_type, ' ',
            IF(v_is_nullable = 'YES', 'NULL', 'NOT NULL'),
            ' AUTO_INCREMENT'
        );

        PREPARE alter_stmt FROM @ddl;
        EXECUTE alter_stmt;
        DEALLOCATE PREPARE alter_stmt;
    END LOOP;

    CLOSE cur_missing_auto_inc;
END$$

CALL ensure_identity_pk_auto_increment()$$
DROP PROCEDURE IF EXISTS ensure_identity_pk_auto_increment$$

DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS tmp_identity_targets;

SET FOREIGN_KEY_CHECKS = @old_fk_checks;
