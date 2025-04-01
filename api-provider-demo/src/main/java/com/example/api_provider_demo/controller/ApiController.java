package com.example.api_provider_demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/api/message")
    public Map<String, Object> getMessage(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个受保护的API端点！");
        response.put("subject", jwt.getSubject());
        response.put("authorities", jwt.getClaims());
        
        return response;
    }
} 