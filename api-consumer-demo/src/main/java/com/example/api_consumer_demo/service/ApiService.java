package com.example.api_consumer_demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Service
public class ApiService {

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    public ApiService(WebClient webClient, OAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = webClient;
        this.authorizedClientService = authorizedClientService;
    }

    public Map<String, Object> getMessageWithClientCredentials(OAuth2AuthorizedClient authorizedClient) {
        try {
            return webClient
                    .get()
                    .uri("/api/message")
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "资源服务器返回错误");
            errorMap.put("status", e.getStatusCode().value());
            errorMap.put("message", e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "调用API时出错");
            errorMap.put("message", e.getMessage());
            
            // 记录异常信息
            System.err.println("调用API时发生异常: " + e.getMessage());
            e.printStackTrace();
            
            throw new RuntimeException("调用API时出错: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getMessageDirect() {
        try {
            return webClient
                    .get()
                    .uri("/api/message")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "资源服务器返回错误");
            errorMap.put("status", e.getStatusCode().value());
            errorMap.put("message", e.getResponseBodyAsString());
            System.err.println("API调用失败: " + e.getMessage());
            return errorMap;
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "调用API时出错");
            errorMap.put("message", e.getMessage());
            
            // 记录异常信息
            System.err.println("调用API时发生异常: " + e.getMessage());
            e.printStackTrace();
            
            return errorMap;
        }
    }
} 