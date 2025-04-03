package com.example.legacy_api_provider_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@SpringBootApplication
@EnableResourceServer
public class LegacyApiProviderDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyApiProviderDemoApplication.class, args);
    }
} 