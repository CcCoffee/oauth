package com.example.api_auth_server.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2迁移控制器
 * 提供API接口触发数据迁移
 */
@RestController
@RequestMapping("/api/migration")
public class OAuth2MigrationController {

    @Autowired
    private OAuth2MigrationTool migrationTool;
    
    /**
     * 迁移客户端数据
     * 需要管理员权限
     */
    @PostMapping("/clients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> migrateClients() {
        try {
            migrationTool.migrateClientDetails();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "客户端数据迁移成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "客户端数据迁移失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 执行完整迁移
     * 需要管理员权限
     */
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> migrateAll() {
        try {
            migrationTool.migrateAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OAuth2数据完整迁移成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "OAuth2数据迁移失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 