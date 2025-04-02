package com.example.api_auth_server.config;

import org.springframework.security.crypto.keygen.StringKeyGenerator;

import java.util.UUID;

public class UUIDKeyGenerator implements StringKeyGenerator {
    @Override
    public String generateKey() {
        return UUID.randomUUID().toString().toLowerCase();
    }
}
