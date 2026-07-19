CREATE TABLE training_programs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    is_mandatory BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_training_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    work_team_id INT,
    training_program_id INT NOT NULL,
    trained_at DATE NOT NULL,
    trainer_name VARCHAR(255),
    evidence_urls JSON,
    certified_until DATE,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (training_program_id) REFERENCES training_programs(id)
);

-- Seed data
INSERT INTO training_programs (title, category, description, is_mandatory)
VALUES 
('An toàn sử dụng thuốc BVTV', 'SAFETY', 'Huấn luyện sử dụng thuốc bảo vệ thực vật an toàn theo chuẩn VietGAP', TRUE),
('Quy trình phân loại và thu hoạch', 'OPERATIONS', 'Đào tạo kỹ năng thu hoạch nông sản đảm bảo chất lượng', TRUE);

