-- 修改 oauth2_registered_client 表
ALTER TABLE oauth2_registered_client
ADD COLUMN post_logout_redirect_uris varchar(1000) DEFAULT NULL;

-- 修改 oauth2_authorization 表的字段类型
ALTER TABLE oauth2_authorization
MODIFY COLUMN attributes LONGTEXT,
MODIFY COLUMN authorization_code_value LONGTEXT,
MODIFY COLUMN authorization_code_metadata LONGTEXT,
MODIFY COLUMN access_token_value LONGTEXT,
MODIFY COLUMN access_token_metadata LONGTEXT,
MODIFY COLUMN refresh_token_value LONGTEXT,
MODIFY COLUMN refresh_token_metadata LONGTEXT;

-- 添加 OIDC 相关字段
ALTER TABLE oauth2_authorization
ADD COLUMN oidc_id_token_value LONGTEXT DEFAULT NULL,
ADD COLUMN oidc_id_token_issued_at timestamp DEFAULT NULL,
ADD COLUMN oidc_id_token_expires_at timestamp DEFAULT NULL,
ADD COLUMN oidc_id_token_metadata LONGTEXT DEFAULT NULL,
ADD COLUMN oidc_id_token_claims LONGTEXT DEFAULT NULL,
ADD COLUMN user_code_value varchar(100) DEFAULT NULL,
ADD COLUMN user_code_issued_at timestamp DEFAULT NULL,
ADD COLUMN user_code_expires_at timestamp DEFAULT NULL,
ADD COLUMN user_code_metadata LONGTEXT DEFAULT NULL,
ADD COLUMN device_code_value varchar(100) DEFAULT NULL,
ADD COLUMN device_code_issued_at timestamp DEFAULT NULL,
ADD COLUMN device_code_expires_at timestamp DEFAULT NULL,
ADD COLUMN device_code_metadata LONGTEXT DEFAULT NULL;

-- 添加索引
CREATE INDEX idx_oauth2_authorization_device_code ON oauth2_authorization(device_code_value);
CREATE INDEX idx_oauth2_authorization_user_code ON oauth2_authorization(user_code_value); 