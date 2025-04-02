package com.example.api_consumer_demo.controller;

import com.example.api_consumer_demo.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ApiService apiService;

    @Autowired
    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/opaque")
    public ResponseEntity<?> testOpaque(@RegisteredOAuth2AuthorizedClient("opaque-client") 
                                      OAuth2AuthorizedClient authorizedClient) {
        try {
            Map<String, Object> result = apiService.getMessageWithOpaqueToken(authorizedClient);
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API调用失败");
            errorResponse.put("status", e.getStatusCode().value());
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "内部服务器错误");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/jwt")
    public ResponseEntity<?> testJwt(@RegisteredOAuth2AuthorizedClient("jwt-client") 
                                      OAuth2AuthorizedClient authorizedClient) {
        try {
            Map<String, Object> result = apiService.getMessageWithJwtToken(authorizedClient);
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API调用失败");
            errorResponse.put("status", e.getStatusCode().value());
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "内部服务器错误");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/direct")
    public ResponseEntity<?> direct() {
        Map<String, Object> result = apiService.getMessageDirect();
        if (result.containsKey("error")) {
            return ResponseEntity.status(500).body(result);
        }
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "API消费者演示");
        info.put("status", "正常运行");
        info.put("version", "1.0.0");
        return ResponseEntity.ok(info);
    }
} 