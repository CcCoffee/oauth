package com.example.legacy_api_consumer_demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApiService {
    
    private final OAuth2RestTemplate jwtOAuth2RestTemplate;
    
    @Value("${api.server.url}")
    private String apiServerUrl;
    
    @Autowired
    public ApiService(
            @Qualifier("jwtOAuth2RestTemplate") OAuth2RestTemplate jwtOAuth2RestTemplate) {
        this.jwtOAuth2RestTemplate = jwtOAuth2RestTemplate;
    }
    
    public Map<String, Object> getMessageWithJwtToken() {
        try {
            String url = apiServerUrl + "/api/jwt/message";
            ResponseEntity<Map> response = jwtOAuth2RestTemplate.exchange(
                    url, HttpMethod.GET, null, Map.class);
            
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API调用失败");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }
} 