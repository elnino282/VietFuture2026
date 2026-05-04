-- ═══════════════════════════════════════════════════════════════════════════════
-- V6__season_expense_br_compliance.sql
-- Migration for Season and Expense modules Business Rules compliance
-- Demo Gen Code.docx requirements
-- ═══════════════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────────────
-- EXPENSE TABLE ENHANCEMENTS
-- BR: Add new fields for expense management
-- ─────────────────────────────────────────────────────────────────────────────

-- Add category column for expense classification
ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT 'OTHER';

-- Add optional task reference (task must belong to same season)
ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS task_id INT NULL,
ADD CONSTRAINT fk_expense_task 
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE SET NULL;

-- Add direct amount field (alternative to unitPrice * quantity calculation)
ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS amount DECIMAL(15,2) NULL;

-- Add note field for expense descriptions
ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS note TEXT NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- INDEXES FOR EXPENSE SEARCH (BR17)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_expense_category ON expenses(category);
CREATE INDEX IF NOT EXISTS idx_expense_date ON expenses(expense_date);
CREATE INDEX IF NOT EXISTS idx_expense_item_name ON expenses(item_name);

-- ─────────────────────────────────────────────────────────────────────────────
-- SEASON NAME UNIQUENESS SUPPORT (BR8/BR12)
-- Note: MySQL doesn't support partial indexes like PostgreSQL
-- Uniqueness is enforced at application layer via SeasonRepository query
-- This index helps with the query performance
-- ─────────────────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_season_plot_name ON seasons(plot_id, season_name);
CREATE INDEX IF NOT EXISTS idx_season_status ON seasons(status);

-- ─────────────────────────────────────────────────────────────────────────────
-- ACCESS DENIED AUDIT LOG SUPPORT (BR1)
-- ─────────────────────────────────────────────────────────────────────────────

-- Ensure audit_logs can store ACCESS_DENIED entity types
-- (Table already exists from V3, just add index for faster lookups)
CREATE INDEX IF NOT EXISTS idx_audit_log_entity_type ON audit_logs(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON audit_logs(operation);
