package com.example.legacy_api_auth_server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class ClientInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ClientInitializationService.class);

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @Override
    public void run(String... args) {
        initializeDefaultClients();
    }

    private void initializeDefaultClients() {
        try {
            // 添加不透明令牌客户端
            if (!clientRegistrationService.clientExists("opaque-client")) {
                logger.info("正在初始化 opaque-client...");
                clientRegistrationService.addClientDetails(
                        "opaque-client",
                        "opaque-secret",
                        "opaque-client-resource-id",
                        "message.read",
                        "client_credentials",
                        null,
                        null,
                        3600 * 24 * 365, // 365 days
                        null,
                        null,
                        null
                );
                logger.info("opaque-client 初始化完成");
            }

            // 添加JWT令牌客户端
            if (!clientRegistrationService.clientExists("jwt-client")) {
                logger.info("正在初始化 jwt-client...");
                clientRegistrationService.addClientDetails(
                        "jwt-client",
                        "jwt-secret",
                        "jwt-client-resource-id",
                        "message.read",
                        "client_credentials",
                        null,
                        null,
                        3600 * 24, // 24 hours
                        null,
                        null,
                        null
                );
                logger.info("jwt-client 初始化完成");
            }
        } catch (Exception e) {
            logger.error("初始化客户端失败", e);
        }
    }
} 