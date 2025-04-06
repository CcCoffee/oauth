package com.example.api_auth_server.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OAuth2迁移命令行工具
 * 可通过启动参数 --oauth2.migration.enabled=true 触发自动迁移
 */
@Component
@ConditionalOnProperty(name = "oauth2.migration.enabled", havingValue = "true")
public class OAuth2MigrationCLI implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2MigrationCLI.class);

    @Autowired
    private OAuth2MigrationTool migrationTool;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("启动OAuth2数据迁移...");
        
        try {
            migrationTool.migrateAll();
            logger.info("OAuth2数据迁移完成");
        } catch (Exception e) {
            logger.error("OAuth2数据迁移失败", e);
            throw e;
        }
    }
} 