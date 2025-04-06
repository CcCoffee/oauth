package com.example.api_auth_server.controller;

import com.example.api_auth_server.migration.OAuth2MigrationTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2迁移控制器
 * 提供API接口用于触发数据迁移过程
 */
@RestController
@RequestMapping("/api/admin/migration")
public class MigrationController {

    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);

    @Autowired
    private OAuth2MigrationTool migrationTool;

    /**
     * 触发客户端详情迁移
     */
    @PostMapping("/clients")
    public ResponseEntity<Map<String, String>> migrateClients() {
        logger.info("收到客户端迁移请求");
        
        try {
            migrationTool.migrateClientDetails();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "客户端数据迁移成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("客户端迁移失败", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "客户端数据迁移失败：" + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 触发访问令牌迁移
     */
    @PostMapping("/tokens")
    public ResponseEntity<Map<String, String>> migrateTokens() {
        logger.info("收到令牌迁移请求");
        
        try {
            migrationTool.migrateAccessTokens();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "令牌数据迁移成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("令牌迁移失败", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "令牌数据迁移失败：" + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 触发全部数据迁移
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> migrateAll() {
        logger.info("收到全部数据迁移请求");
        
        try {
            migrationTool.migrateAll();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "所有数据迁移成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("全部数据迁移失败", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "数据迁移失败：" + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 