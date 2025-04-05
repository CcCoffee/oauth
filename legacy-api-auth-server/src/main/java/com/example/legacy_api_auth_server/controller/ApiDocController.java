package com.example.legacy_api_auth_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/docs")
public class ApiDocController {

    @GetMapping("/clients")
    public Map<String, Object> getClientApiDocs() {
        Map<String, Object> docs = new HashMap<>();

        // 基本信息
        docs.put("api_version", "1.0");
        docs.put("description", "OAuth2客户端管理API");

        // 添加客户端API
        Map<String, Object> addClientApi = new HashMap<>();
        addClientApi.put("url", "/api/clients");
        addClientApi.put("method", "POST");
        addClientApi.put("description", "添加新的OAuth2客户端");
        addClientApi.put("auth_required", "Basic Auth (admin)");

        Map<String, Object> addClientParams = new HashMap<>();
        addClientParams.put("clientId", "客户端ID (必填)");
        addClientParams.put("clientSecret", "客户端密钥 (必填)");
        addClientParams.put("resourceIds", "资源ID列表，多个值用逗号分隔 (必填)");
        addClientParams.put("scope", "授权范围，多个值用逗号分隔 (必填)");
        addClientParams.put("authorizedGrantTypes", "授权类型，多个值用逗号分隔 (必填)");
        addClientParams.put("webServerRedirectUri", "重定向URI (可选)");
        addClientParams.put("authorities", "权限，多个值用逗号分隔 (可选)");
        addClientParams.put("accessTokenValidity", "访问令牌有效期（秒） (可选)");
        addClientParams.put("refreshTokenValidity", "刷新令牌有效期（秒） (可选)");
        addClientParams.put("additionalInformation", "附加信息 (可选)");
        addClientParams.put("autoApprove", "自动批准的范围，多个值用逗号分隔 (可选)");

        addClientApi.put("parameters", addClientParams);

        // 检查客户端是否存在API
        Map<String, Object> checkClientApi = new HashMap<>();
        checkClientApi.put("url", "/api/clients/{clientId}/exists");
        checkClientApi.put("method", "GET");
        checkClientApi.put("description", "检查客户端是否存在");
        checkClientApi.put("auth_required", "Basic Auth (admin)");

        // 删除客户端API
        Map<String, Object> removeClientApi = new HashMap<>();
        removeClientApi.put("url", "/api/clients/{clientId}");
        removeClientApi.put("method", "DELETE");
        removeClientApi.put("description", "删除客户端");
        removeClientApi.put("auth_required", "Basic Auth (admin)");

        // 整合所有API
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("add_client", addClientApi);
        endpoints.put("check_client_exists", checkClientApi);
        endpoints.put("remove_client", removeClientApi);

        docs.put("endpoints", endpoints);

        // 示例
        Map<String, Object> examples = new HashMap<>();

        // 添加客户端示例
        examples.put("add_client", "curl -X POST -u admin:admin123 'http://localhost:9001/api/clients?clientId=my-client&clientSecret=my-secret&resourceIds=legacy-api&scope=message.read&authorizedGrantTypes=client_credentials&accessTokenValidity=3600'");

        // 检查客户端示例
        examples.put("check_client_exists", "curl -u admin:admin123 'http://localhost:9001/api/clients/my-client/exists'");

        // 删除客户端示例
        examples.put("remove_client", "curl -X DELETE -u admin:admin123 'http://localhost:9001/api/clients/my-client'");

        docs.put("examples", examples);

        return docs;
    }
}