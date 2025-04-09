package com.example.legacy_api_consumer_demo.controller;

import com.example.legacy_api_consumer_demo.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/jwt")
    public ResponseEntity<?> testJwt() {
        Map<String, Object> result = apiService.getMessageWithJwtToken();
        if (result.containsKey("error")) {
            return ResponseEntity.status(500).body(result);
        }
        return ResponseEntity.ok(result);
    }
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Legacy API Consumer Demo");
        info.put("status", "Running normally");
        info.put("version", "1.0.0");
        return ResponseEntity.ok(info);
    }
} 