package com.example.api_provider_demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/message")
    public Map<String, Object> getMessage(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个受保护的API端点，通过客户端凭证授权访问");
        response.put("subject", jwt.getSubject());
        response.put("authorities", jwt.getClaims());
        
        return response;
    }
} 