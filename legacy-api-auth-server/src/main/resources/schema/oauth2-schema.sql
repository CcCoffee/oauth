-- OAuth2 数据库表 - Spring Security OAuth2 1.x和2.x版本使用的表结构

-- 用于存储客户端注册信息
CREATE TABLE IF NOT EXISTS oauth_client_details (
  client_id VARCHAR(256) PRIMARY KEY,                -- 客户端ID，用于唯一标识客户端应用程序，如'mobile-app'
  resource_ids VARCHAR(256),                         -- 客户端可以访问的资源ID列表，使用逗号分隔，如'api-resource,user-resource'
  client_secret VARCHAR(256),                        -- 客户端密钥的加密哈希值，如'$2a$10$vCXMWCn7fDZWOcLnIEhmK.74dvK1Eh8ae2WrWlhr2ETPLoxQctN4.'
  scope VARCHAR(256),                                -- 客户端允许请求的权限范围，使用逗号分隔，如'read,write,profile'
  authorized_grant_types VARCHAR(256),               -- 客户端允许使用的授权类型，使用逗号分隔，如'authorization_code,password,refresh_token'
  web_server_redirect_uri VARCHAR(256),              -- OAuth2回调URL，可以有多个，使用逗号分隔，如'https://client.example.org/callback'
  authorities VARCHAR(256),                          -- 客户端拥有的Spring Security权限，使用逗号分隔，如'ROLE_CLIENT,ROLE_TRUSTED_CLIENT'
  access_token_validity INTEGER,                     -- 访问令牌有效期，以秒为单位，如3600表示1小时
  refresh_token_validity INTEGER,                    -- 刷新令牌有效期，以秒为单位，如2592000表示30天
  additional_information VARCHAR(4096),              -- 附加信息，存储为JSON格式，如'{"company":"Example Inc.","department":"IT"}'
  autoapprove VARCHAR(256)                           -- 自动批准的权限范围，使用逗号分隔，如'read,profile'表示这些范围无需用户确认
);

-- 用于存储授权码（临时使用，授权码模式下需要）
CREATE TABLE IF NOT EXISTS oauth_code (
  code VARCHAR(256),                                 -- 授权码值，如'f81d4fae-7dec-11d0-a765-00a0c91e6bf6'
  authentication BYTEA                               -- 认证信息的序列化对象，包含授权请求的详情
                                                     -- 示例：包含用户名、客户端ID、请求范围等信息的Java序列化对象
                                                     -- 由于是二进制数据，通常以base64编码存储，实际内容取决于序列化机制
);

-- 用于存储访问令牌
CREATE TABLE IF NOT EXISTS oauth_access_token (
  token_id VARCHAR(256),                              -- 访问令牌的MD5哈希值，如'e8a14feb5d40cce0520ca5d9f7e0f1b3'
  token BYTEA,                                        -- 访问令牌的序列化对象
                                                      -- 示例：包含令牌值、过期时间、用户信息等的Java序列化对象
                                                      -- 例如OAuth2AccessToken序列化后的二进制数据
  authentication_id VARCHAR(256) PRIMARY KEY,         -- 认证ID，由客户端ID和用户名组合生成的唯一标识符，如'dc5891b1ce4c33f1b7aaee4c4cc12c6f'
  user_name VARCHAR(256),                             -- 用户名，如'john.doe@example.com'
  client_id VARCHAR(256),                             -- 客户端ID，如'mobile-app'
  authentication BYTEA,                               -- 认证信息的序列化对象
                                                      -- 示例：包含用户认证详情的Java序列化对象
                                                      -- 例如UsernamePasswordAuthenticationToken序列化后的二进制数据
  refresh_token VARCHAR(256)                          -- 刷新令牌的MD5哈希值，如'c5ba2b2e0be48a4bf3fbd5c42910c92c'
);

-- 用于存储刷新令牌
CREATE TABLE IF NOT EXISTS oauth_refresh_token (
  token_id VARCHAR(256),                              -- 刷新令牌的MD5哈希值，如'c5ba2b2e0be48a4bf3fbd5c42910c92c'
  token BYTEA,                                        -- 刷新令牌的序列化对象
                                                      -- 示例：包含令牌值和过期时间的Java序列化对象
                                                      -- 例如OAuth2RefreshToken序列化后的二进制数据
  authentication BYTEA                                -- 认证信息的序列化对象
                                                      -- 示例：包含原始认证请求的Java序列化对象
                                                      -- 例如UsernamePasswordAuthenticationToken序列化后的二进制数据
);

-- 用于记录client_id的批准操作
CREATE TABLE IF NOT EXISTS oauth_approvals (
  userId VARCHAR(256),                                -- 用户ID或用户名，如'john.doe@example.com'
  clientId VARCHAR(256),                              -- 客户端ID，如'mobile-app'
  scope VARCHAR(256),                                 -- 批准的权限范围，如'read'或'write'
  status VARCHAR(10),                                 -- 批准状态，如'APPROVED'或'DENIED'
  expiresAt TIMESTAMP,                                -- 批准过期时间，如'2023-12-31 23:59:59'
  lastModifiedAt TIMESTAMP                            -- 最后修改时间，如'2023-01-01 12:00:00'
);
