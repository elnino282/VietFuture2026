-- Migration script for adding performance indexes to optimize Farm module queries

-- Index for Plot → Farm relationship (hasPlots query)
-- Speeds up: SELECT COUNT(p) > 0 FROM Plot p WHERE p.farm.id = ?
CREATE INDEX IF NOT EXISTS idx_plot_farm_id ON plot(farm_id);

-- Index for Season → Plot relationship (hasSeasons query)
-- Speeds up: SELECT COUNT(s) > 0 FROM Season s WHERE s.plot.farm.id = ?
CREATE INDEX IF NOT EXISTS idx_season_plot_id ON season(plot_id);

-- Composite index for unique constraint check on active farms
-- Speeds up: SELECT ... FROM farm WHERE user_id = ? AND name = ? AND active = true
-- Note: This index helps with the critical bug fix - checking name uniqueness only among active farms
CREATE INDEX IF NOT EXISTS idx_farm_user_name_active ON farm(user_id, name, active);

-- Notes:
-- 1. These indexes significantly improve query performance for farm deletion/restoration operations
-- 2. The idx_farm_owner_name_active index is especially important for the unique constraint fix
-- 3. Monitor index usage and adjust if cardinality changes significantly in production
