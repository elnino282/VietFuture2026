CREATE DATABASE IF NOT EXISTS quanlymuavu;
CREATE DATABASE IF NOT EXISTS identity_db;
CREATE DATABASE IF NOT EXISTS crop_catalog_db;
CREATE DATABASE IF NOT EXISTS farm_db;
CREATE DATABASE IF NOT EXISTS season_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS marketplace_db;
CREATE DATABASE IF NOT EXISTS finance_db;
CREATE DATABASE IF NOT EXISTS incident_db;
CREATE DATABASE IF NOT EXISTS sustainability_db;

CREATE USER IF NOT EXISTS 'springuser'@'localhost' IDENTIFIED BY 'springpass';
GRANT ALL PRIVILEGES ON quanlymuavu.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON identity_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON identity_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON crop_catalog_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON crop_catalog_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON farm_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON farm_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON season_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON season_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON marketplace_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON marketplace_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON finance_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON finance_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON incident_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON incident_db.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON sustainability_db.* TO 'springuser'@'localhost';
GRANT ALL PRIVILEGES ON sustainability_db.* TO 'springuser'@'%';
FLUSH PRIVILEGES;



