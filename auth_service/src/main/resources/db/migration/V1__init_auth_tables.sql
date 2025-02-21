-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- 插入默认角色
INSERT INTO roles (name, description) VALUES 
('ROLE_USER', '普通用户'),
('ROLE_ADMIN', '系统管理员'),
('ROLE_AUDITOR', '审核员'),
('ROLE_ORG_USER', '机构用户')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 插入基础权限
INSERT INTO permissions (name, description) VALUES 
('USER_READ', '查看用户信息'),
('USER_CREATE', '创建用户'),
('USER_UPDATE', '更新用户信息'),
('USER_DELETE', '删除用户'),
('ROLE_READ', '查看角色信息'),
('ROLE_CREATE', '创建角色'),
('ROLE_UPDATE', '更新角色'),
('ROLE_DELETE', '删除角色')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 为ROLE_ADMIN分配所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id; 