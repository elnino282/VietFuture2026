-- Create work_teams table
CREATE TABLE IF NOT EXISTS work_teams (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id BIGINT NOT NULL,
    team_name VARCHAR(255) NOT NULL,
    team_leader_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create work_team_members table
CREATE TABLE IF NOT EXISTS work_team_members (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    work_team_id BIGINT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    CONSTRAINT fk_work_team_members_team FOREIGN KEY (work_team_id) REFERENCES work_teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add work_team_id column to tasks table (for assigning tasks to teams)
SET @add_work_team_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE tasks ADD COLUMN work_team_id BIGINT NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tasks'
      AND COLUMN_NAME = 'work_team_id'
);
PREPARE stmt FROM @add_work_team_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add estimated_days column to tasks table
SET @add_estimated_days = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE tasks ADD COLUMN estimated_days INT NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tasks'
      AND COLUMN_NAME = 'estimated_days'
);
PREPARE stmt FROM @add_estimated_days;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add plot_id column to tasks table (for assigning tasks to specific plots/zones)
SET @add_plot_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE tasks ADD COLUMN plot_id BIGINT NULL',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tasks'
      AND COLUMN_NAME = 'plot_id'
);
PREPARE stmt FROM @add_plot_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
