-- 添加type和scope字段
ALTER TABLE permissions
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'API',
    ADD COLUMN scope VARCHAR(50);

-- 更新现有权限记录的type字段
UPDATE permissions SET type = 'API' WHERE type IS NULL;

-- 预置OAuth2相关权限
INSERT INTO permissions (name, description, resource, action, type, scope) VALUES
('oauth2_read_profile', '读取用户档案', 'profile', 'read', 'OAUTH2', 'read'),
('oauth2_write_profile', '更新用户档案', 'profile', 'write', 'OAUTH2', 'write'); 