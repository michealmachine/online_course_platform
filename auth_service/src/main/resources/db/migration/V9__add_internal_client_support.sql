-- 添加内部客户端标识字段到oauth2_registered_client表
ALTER TABLE oauth2_registered_client
ADD COLUMN is_internal BOOLEAN DEFAULT FALSE,
ADD COLUMN auto_approve BOOLEAN DEFAULT FALSE;

-- 创建索引提高查询效率
CREATE INDEX idx_oauth2_registered_client_internal ON oauth2_registered_client(is_internal); 