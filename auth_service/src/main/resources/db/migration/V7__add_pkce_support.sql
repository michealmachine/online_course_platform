-- 添加PKCE相关字段到oauth2_authorization_code表
CREATE TABLE oauth2_authorization_code (
    id VARCHAR(36) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    code VARCHAR(255) NOT NULL,
    scope VARCHAR(1000),
    redirect_uri VARCHAR(1000),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    code_challenge VARCHAR(128),
    code_challenge_method VARCHAR(10)
); 