CREATE DATABASE IF NOT EXISTS quanlymuavu;
CREATE DATABASE IF NOT EXISTS identity_db;
CREATE DATABASE IF NOT EXISTS crop_catalog_db;
CREATE DATABASE IF NOT EXISTS farm_db;
CREATE DATABASE IF NOT EXISTS season_db;
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
FLUSH PRIVILEGES;



