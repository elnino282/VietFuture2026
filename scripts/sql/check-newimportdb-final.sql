-- =========================================================
-- ACM final seed smoke checks
-- Read-only checks for Entity-first seeded database
-- =========================================================

USE quanlymuavu;

SET NAMES utf8mb4;

-- 1. Flyway table should not be present in this flow.
SELECT
    'flyway_schema_history_absent' AS check_name,
    CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS result,
    COUNT(*) AS matching_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'flyway_schema_history';

-- 2. Main table counts.
SELECT 'users' AS table_name, COUNT(*) AS total FROM users
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'user_roles', COUNT(*) FROM user_roles
UNION ALL SELECT 'farms', COUNT(*) FROM farms
UNION ALL SELECT 'plots', COUNT(*) FROM plots
UNION ALL SELECT 'seasons', COUNT(*) FROM seasons
UNION ALL SELECT 'tasks', COUNT(*) FROM tasks
UNION ALL SELECT 'field_logs', COUNT(*) FROM field_logs
UNION ALL SELECT 'expenses', COUNT(*) FROM expenses
UNION ALL SELECT 'harvests', COUNT(*) FROM harvests
UNION ALL SELECT 'incidents', COUNT(*) FROM incidents
UNION ALL SELECT 'disease_records', COUNT(*) FROM disease_records
UNION ALL SELECT 'suppliers', COUNT(*) FROM suppliers
UNION ALL SELECT 'supply_items', COUNT(*) FROM supply_items
UNION ALL SELECT 'supply_lots', COUNT(*) FROM supply_lots
UNION ALL SELECT 'stock_movements', COUNT(*) FROM stock_movements
UNION ALL SELECT 'inventory_balances', COUNT(*) FROM inventory_balances
UNION ALL SELECT 'soil_tests', COUNT(*) FROM soil_tests
UNION ALL SELECT 'nutrient_input_events', COUNT(*) FROM nutrient_input_events
UNION ALL SELECT 'product_warehouse_lots', COUNT(*) FROM product_warehouse_lots
UNION ALL SELECT 'product_warehouse_transactions', COUNT(*) FROM product_warehouse_transactions
UNION ALL SELECT 'marketplace_products', COUNT(*) FROM marketplace_products
UNION ALL SELECT 'marketplace_carts', COUNT(*) FROM marketplace_carts
UNION ALL SELECT 'marketplace_orders', COUNT(*) FROM marketplace_orders
UNION ALL SELECT 'marketplace_order_items', COUNT(*) FROM marketplace_order_items
UNION ALL SELECT 'marketplace_product_reviews', COUNT(*) FROM marketplace_product_reviews
UNION ALL SELECT 'season_employees', COUNT(*) FROM season_employees
UNION ALL SELECT 'task_progress_logs', COUNT(*) FROM task_progress_logs
UNION ALL SELECT 'payroll_records', COUNT(*) FROM payroll_records
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications
UNION ALL SELECT 'documents', COUNT(*) FROM documents
UNION ALL SELECT 'audit_logs', COUNT(*) FROM audit_logs;

-- 3. Orphan FK checks for core relationships.
SELECT 'farm_user_orphans' AS check_name, COUNT(*) AS total
FROM farms f
LEFT JOIN users u ON u.user_id = f.user_id
WHERE f.user_id IS NOT NULL AND u.user_id IS NULL
UNION ALL
SELECT 'plot_farm_orphans', COUNT(*)
FROM plots p
LEFT JOIN farms f ON f.farm_id = p.farm_id
WHERE f.farm_id IS NULL
UNION ALL
SELECT 'season_plot_orphans', COUNT(*)
FROM seasons s
LEFT JOIN plots p ON p.plot_id = s.plot_id
WHERE p.plot_id IS NULL
UNION ALL
SELECT 'task_user_orphans', COUNT(*)
FROM tasks t
LEFT JOIN users u ON u.user_id = t.user_id
WHERE u.user_id IS NULL
UNION ALL
SELECT 'expense_season_orphans', COUNT(*)
FROM expenses e
LEFT JOIN seasons s ON s.season_id = e.season_id
WHERE s.season_id IS NULL
UNION ALL
SELECT 'marketplace_product_lot_orphans', COUNT(*)
FROM marketplace_products p
LEFT JOIN product_warehouse_lots l ON l.id = p.lot_id
WHERE l.id IS NULL
UNION ALL
SELECT 'marketplace_order_item_orphans', COUNT(*)
FROM marketplace_order_items oi
LEFT JOIN marketplace_orders o ON o.id = oi.order_id
LEFT JOIN marketplace_products p ON p.id = oi.product_id
WHERE o.id IS NULL OR p.id IS NULL
UNION ALL
SELECT 'payroll_employee_orphans', COUNT(*)
FROM payroll_records pr
LEFT JOIN users u ON u.user_id = pr.employee_user_id
WHERE u.user_id IS NULL;

-- 4. Non-null timestamp checks for high-risk seed tables.
SELECT 'expenses.created_at_null' AS check_name, COUNT(*) AS total FROM expenses WHERE created_at IS NULL
UNION ALL SELECT 'marketplace_products.created_at_null', COUNT(*) FROM marketplace_products WHERE created_at IS NULL
UNION ALL SELECT 'marketplace_products.updated_at_null', COUNT(*) FROM marketplace_products WHERE updated_at IS NULL
UNION ALL SELECT 'marketplace_orders.created_at_null', COUNT(*) FROM marketplace_orders WHERE created_at IS NULL
UNION ALL SELECT 'marketplace_orders.updated_at_null', COUNT(*) FROM marketplace_orders WHERE updated_at IS NULL
UNION ALL SELECT 'product_warehouse_lots.created_at_null', COUNT(*) FROM product_warehouse_lots WHERE created_at IS NULL
UNION ALL SELECT 'product_warehouse_lots.updated_at_null', COUNT(*) FROM product_warehouse_lots WHERE updated_at IS NULL
UNION ALL SELECT 'product_warehouse_transactions.created_at_null', COUNT(*) FROM product_warehouse_transactions WHERE created_at IS NULL
UNION ALL SELECT 'task_progress_logs.logged_at_null', COUNT(*) FROM task_progress_logs WHERE logged_at IS NULL
UNION ALL SELECT 'payroll_records.generated_at_null', COUNT(*) FROM payroll_records WHERE generated_at IS NULL;

-- 5. Unique-key duplicate checks.
SELECT 'duplicate_user_email' AS check_name, COUNT(*) AS duplicate_groups
FROM (
    SELECT email FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1
) d
UNION ALL
SELECT 'duplicate_user_name', COUNT(*)
FROM (
    SELECT user_name FROM users WHERE user_name IS NOT NULL GROUP BY user_name HAVING COUNT(*) > 1
) d
UNION ALL
SELECT 'duplicate_role_code', COUNT(*)
FROM (
    SELECT role_code FROM roles GROUP BY role_code HAVING COUNT(*) > 1
) d
UNION ALL
SELECT 'duplicate_product_slug', COUNT(*)
FROM (
    SELECT slug FROM marketplace_products GROUP BY slug HAVING COUNT(*) > 1
) d
UNION ALL
SELECT 'duplicate_order_code', COUNT(*)
FROM (
    SELECT order_code FROM marketplace_orders GROUP BY order_code HAVING COUNT(*) > 1
) d
UNION ALL
SELECT 'duplicate_lot_code', COUNT(*)
FROM (
    SELECT lot_code FROM product_warehouse_lots GROUP BY lot_code HAVING COUNT(*) > 1
) d;

-- 6. Auto-increment state is inspected only, not modified.
SELECT
    table_name,
    auto_increment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'users',
      'roles',
      'farms',
      'plots',
      'seasons',
      'tasks',
      'marketplace_products',
      'marketplace_orders',
      'product_warehouse_lots'
  )
ORDER BY table_name;

-- 7. Seed account summary.
SELECT
    u.email,
    u.user_name,
    u.full_name,
    GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles,
    u.status
FROM users u
JOIN user_roles ur ON ur.user_id = u.user_id
JOIN roles r ON r.role_id = ur.role_id
WHERE u.email IN (
    'admin@acm.local',
    'farmer@acm.local',
    'farmer.binhminh@example.com',
    'employee@acm.local',
    'employee.lanthao@example.com',
    'buyer@acm.local',
    'buyer.thucphamantoan@example.com'
)
GROUP BY u.user_id, u.email, u.user_name, u.full_name, u.status
ORDER BY u.user_id;

-- 8. Main bootstrap account visibility.
SELECT
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
