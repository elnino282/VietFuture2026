-- V5__documents_feature.sql
-- Documents feature: extend documents table, add favorites and recent opens tables, seed data

-- Modify documents table to add new columns
-- First check if columns exist and add if not
ALTER TABLE documents 
    ADD COLUMN IF NOT EXISTS url VARCHAR(1000) NOT NULL DEFAULT 'https://example.com',
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS crop VARCHAR(50),
    ADD COLUMN IF NOT EXISTS stage VARCHAR(50),
    ADD COLUMN IF NOT EXISTS topic VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Drop content column if exists (replaced by description)
ALTER TABLE documents DROP COLUMN IF EXISTS content;

-- Create document_favorites table
CREATE TABLE IF NOT EXISTS document_favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_document (user_id, document_id),
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

-- Create document_recent_opens table
CREATE TABLE IF NOT EXISTS document_recent_opens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_id INT NOT NULL,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE,
    INDEX idx_user_opened (user_id, opened_at DESC)
);

-- Seed data: 8 documents with variety of crop/stage/topic
INSERT INTO documents (title, url, description, crop, stage, topic, is_active, is_public, created_at)
VALUES
    ('Rice Planting Guide', 'https://example.com/rice-planting', 'Step-by-step rice planting guide covering soil preparation, seeding techniques, and initial care.', 'Rice', 'Planting', 'Best Practices', TRUE, TRUE, NOW()),
    ('Corn Pest Management', 'https://example.com/corn-pests', 'Comprehensive guide to common corn pests and effective treatment methods including organic options.', 'Corn', 'Growing', 'Pest Management', TRUE, TRUE, NOW()),
    ('Soil Preparation Basics', 'https://example.com/soil-prep', 'Learn how to properly prepare soil before sowing including testing, amendments, and tillage.', NULL, 'Planting', 'Soil Management', TRUE, TRUE, NOW()),
    ('Post-Harvest Storage Tips', 'https://example.com/storage', 'Best practices for storing harvested crops to reduce losses and maintain quality.', NULL, 'Post-Harvest', 'Best Practices', TRUE, TRUE, NOW()),
    ('Water Management 101', 'https://example.com/water', 'Irrigation scheduling tips and water conservation strategies for various crop types.', NULL, 'Growing', 'Water Management', TRUE, TRUE, NOW()),
    ('Wheat Harvest Checklist', 'https://example.com/wheat-harvest', 'Complete checklist for harvesting wheat including timing, equipment, and quality checks.', 'Wheat', 'Harvest', 'Best Practices', TRUE, TRUE, NOW()),
    ('Climate Adaptation Tips', 'https://example.com/climate', 'Strategies for building farm resilience against climate change and extreme weather events.', NULL, NULL, 'Climate Adaptation', TRUE, TRUE, NOW()),
    ('Barley Growing Guide', 'https://example.com/barley', 'Comprehensive barley growing guide covering all stages from planting to harvest.', 'Barley', 'Growing', 'Best Practices', TRUE, TRUE, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);
