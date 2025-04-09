package com.example.legacy_api_auth_server.controller;

import com.example.legacy_api_auth_server.service.ClientRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientManagementController {

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    /**
     * Add a new OAuth2 client
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addClient(
            @RequestParam String clientId,
            @RequestParam String clientSecret,
            @RequestParam String resourceIds,
            @RequestParam String scope,
            @RequestParam String authorizedGrantTypes,
            @RequestParam(required = false) String webServerRedirectUri,
            @RequestParam(required = false) String authorities,
            @RequestParam(required = false) Integer accessTokenValidity,
            @RequestParam(required = false) Integer refreshTokenValidity,
            @RequestParam(required = false) String additionalInformation,
            @RequestParam(required = false) String autoApprove) {

        Map<String, Object> response = new HashMap<>();

        try {
            clientRegistrationService.addClientDetails(
                    clientId,
                    clientSecret,
                    resourceIds,
                    scope,
                    authorizedGrantTypes,
                    webServerRedirectUri,
                    authorities,
                    accessTokenValidity,
                    refreshTokenValidity,
                    additionalInformation,
                    autoApprove
            );

            response.put("status", "success");
            response.put("message", "Client added successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to add client: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check if the client exists
     */
    @GetMapping("/{clientId}/exists")
    public ResponseEntity<Map<String, Object>> checkClientExists(@PathVariable String clientId) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = clientRegistrationService.clientExists(clientId);
        
        response.put("clientId", clientId);
        response.put("exists", exists);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete the client
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> removeClient(@PathVariable String clientId) {
        Map<String, Object> response = new HashMap<>();
        
        boolean removed = clientRegistrationService.removeClientDetails(clientId);
        
        if (removed) {
            response.put("status", "success");
            response.put("message", "Client deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Specified client not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
} 