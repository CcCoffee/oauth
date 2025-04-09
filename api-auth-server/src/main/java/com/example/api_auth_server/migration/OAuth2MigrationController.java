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
 * OAuth2 Migration Controller
 * Provides API endpoints to trigger data migration
 */
@RestController
@RequestMapping("/api/migration")
public class OAuth2MigrationController {

    @Autowired
    private OAuth2MigrationTool migrationTool;
    
    /**
     * Migrates client data
     * Requires admin role
     */
    @PostMapping("/clients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> migrateClients() {
        try {
            migrationTool.migrateClientDetails();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Client data migration successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Client data migration failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Executes full migration
     * Requires admin role
     */
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> migrateAll() {
        try {
            migrationTool.migrateAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OAuth2 data full migration successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "OAuth2 data migration failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 