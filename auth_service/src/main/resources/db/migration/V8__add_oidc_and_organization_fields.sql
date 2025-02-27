-- 添加机构ID字段
ALTER TABLE users
    ADD COLUMN organization_id BIGINT,
    ADD INDEX idx_organization_id (organization_id);

-- 添加OIDC标准字段
ALTER TABLE users
    ADD COLUMN given_name VARCHAR(50),
    ADD COLUMN family_name VARCHAR(50),
    ADD COLUMN middle_name VARCHAR(50),
    ADD COLUMN preferred_username VARCHAR(50),
    ADD COLUMN profile VARCHAR(255),
    ADD COLUMN website VARCHAR(255),
    ADD COLUMN gender VARCHAR(10),
    ADD COLUMN birthdate VARCHAR(10),
    ADD COLUMN zoneinfo VARCHAR(40),
    ADD COLUMN locale VARCHAR(10),
    ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE;

-- 更新现有的ROLE_ORG_USER为ROLE_ORGANIZATION
UPDATE roles 
SET name = 'ROLE_ORGANIZATION', 
    description = '机构用户'
WHERE name = 'ROLE_ORG_USER'; 