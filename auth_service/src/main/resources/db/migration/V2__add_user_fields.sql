-- 添加用户基本信息字段
ALTER TABLE users
    ADD COLUMN nickname VARCHAR(50),
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN avatar VARCHAR(255);

-- 添加安全相关字段
ALTER TABLE users
    ADD COLUMN account_locked BOOLEAN DEFAULT FALSE,
    ADD COLUMN login_attempts INT DEFAULT 0,
    ADD COLUMN lock_time TIMESTAMP;

-- 添加审计字段
ALTER TABLE users
    ADD COLUMN last_login_time TIMESTAMP,
    ADD COLUMN last_login_ip VARCHAR(50); 