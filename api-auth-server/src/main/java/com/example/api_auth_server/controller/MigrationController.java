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
 * OAuth2 Migration Controller
 * Provides API endpoints to trigger data migration processes
 */
@RestController
@RequestMapping("/api/admin/migration")
public class MigrationController {

    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);

    @Autowired
    private OAuth2MigrationTool migrationTool;

    /**
     * Triggers client details migration
     */
    @PostMapping("/clients")
    public ResponseEntity<Map<String, String>> migrateClients() {
        logger.info("Received client migration request");
        
        try {
            migrationTool.migrateClientDetails();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Client data migration successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Client migration failed", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Client data migration failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Triggers access token migration
     */
    @PostMapping("/tokens")
    public ResponseEntity<Map<String, String>> migrateTokens() {
        logger.info("Received token migration request");
        
        try {
            migrationTool.migrateAccessTokens();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Token data migration successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token migration failed", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Token data migration failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Triggers all data migration
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> migrateAll() {
        logger.info("Received all data migration request");
        
        try {
            migrationTool.migrateAll();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All data migration successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("All data migration failed", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Data migration failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 