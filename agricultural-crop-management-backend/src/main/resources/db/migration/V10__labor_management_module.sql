INSERT INTO roles (role_code, role_name, description)
SELECT 'EMPLOYEE', 'Employee', 'Employee user'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'EMPLOYEE');

CREATE TABLE IF NOT EXISTS season_employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    added_by_user_id BIGINT NULL,
    wage_per_task DECIMAL(15,2) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_season_employee UNIQUE (season_id, employee_user_id),
    CONSTRAINT fk_season_employee_season FOREIGN KEY (season_id) REFERENCES seasons(season_id),
    CONSTRAINT fk_season_employee_user FOREIGN KEY (employee_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_season_employee_added_by FOREIGN KEY (added_by_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_season_employee_season ON season_employees(season_id);
CREATE INDEX idx_season_employee_user ON season_employees(employee_user_id);

CREATE TABLE IF NOT EXISTS task_progress_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    employee_user_id BIGINT NOT NULL,
    progress_percent INT NOT NULL,
    note TEXT NULL,
    evidence_url VARCHAR(1000) NULL,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_progress_task FOREIGN KEY (task_id) REFERENCES tasks(task_id),
    CONSTRAINT fk_task_progress_employee FOREIGN KEY (employee_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_task_progress_task ON task_progress_logs(task_id);
CREATE INDEX idx_task_progress_employee ON task_progress_logs(employee_user_id);
CREATE INDEX idx_task_progress_logged_at ON task_progress_logs(logged_at);

CREATE TABLE IF NOT EXISTS payroll_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_user_id BIGINT NOT NULL,
    season_id INT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_assigned_tasks INT NOT NULL DEFAULT 0,
    total_completed_tasks INT NOT NULL DEFAULT 0,
    wage_per_task DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT NULL,
    CONSTRAINT uk_payroll_employee_season_period UNIQUE (employee_user_id, season_id, period_start, period_end),
    CONSTRAINT fk_payroll_employee FOREIGN KEY (employee_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_payroll_season FOREIGN KEY (season_id) REFERENCES seasons(season_id)
);

CREATE INDEX idx_payroll_employee ON payroll_records(employee_user_id);
CREATE INDEX idx_payroll_season ON payroll_records(season_id);
CREATE INDEX idx_payroll_period ON payroll_records(period_start, period_end);

