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
     * 添加新的OAuth2客户端
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
            response.put("message", "客户端添加成功");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "添加客户端失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 检查客户端是否存在
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
     * 删除客户端
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> removeClient(@PathVariable String clientId) {
        Map<String, Object> response = new HashMap<>();
        
        boolean removed = clientRegistrationService.removeClientDetails(clientId);
        
        if (removed) {
            response.put("status", "success");
            response.put("message", "客户端删除成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "找不到指定的客户端");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
} 