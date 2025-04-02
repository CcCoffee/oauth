package com.example.api_provider_demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/opaque/message")
    public Map<String, Object> getOpaqueMessage(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个使用不透明令牌保护的API端点");
        
        if (authentication instanceof BearerTokenAuthentication) {
            BearerTokenAuthentication bearerAuth = (BearerTokenAuthentication) authentication;
            response.put("token_type", "opaque");
            response.put("attributes", bearerAuth.getTokenAttributes());
        }
        
        return response;
    }

    @GetMapping("/jwt/message")
    public Map<String, Object> getJwtMessage(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个使用JWT令牌保护的API端点");
        
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            response.put("token_type", "jwt");
            response.put("subject", jwt.getSubject());
            response.put("claims", jwt.getClaims());
        }
        
        return response;
    }
} 