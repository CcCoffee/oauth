package com.example.api_auth_server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TokenCleanupService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public TokenCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Scheduled(cron = "0 */5 * * * *") // 每5分钟执行一次
    public void cleanupExpiredTokens() {
        // 删除过期的访问令牌记录
        String sql = "DELETE FROM oauth2_authorization WHERE access_token_expires_at < CURRENT_TIMESTAMP";
        int deletedCount = jdbcTemplate.update(sql);
        
        if (deletedCount > 0) {
            System.out.println("已清理 " + deletedCount + " 条过期的令牌记录");
        }
    }
}