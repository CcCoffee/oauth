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

        // Basic Information
        docs.put("api_version", "1.0");
        docs.put("description", "OAuth2 Client Management API");

        // Add Client API
        Map<String, Object> addClientApi = new HashMap<>();
        addClientApi.put("url", "/api/clients");
        addClientApi.put("method", "POST");
        addClientApi.put("description", "Add a new OAuth2 client");
        addClientApi.put("auth_required", "Basic Auth (admin)");

        Map<String, Object> addClientParams = new HashMap<>();
        addClientParams.put("clientId", "Client ID (required)");
        addClientParams.put("clientSecret", "Client Secret (required)");
        addClientParams.put("resourceIds", "Resource ID list, comma-separated (required)");
        addClientParams.put("scope", "Authorization scope, comma-separated (required)");
        addClientParams.put("authorizedGrantTypes", "Authorized grant types, comma-separated (required)");
        addClientParams.put("webServerRedirectUri", "Redirect URI (optional)");
        addClientParams.put("authorities", "Authorities, comma-separated (optional)");
        addClientParams.put("accessTokenValidity", "Access token validity (seconds) (optional)");
        addClientParams.put("refreshTokenValidity", "Refresh token validity (seconds) (optional)");
        addClientParams.put("additionalInformation", "Additional information (optional)");
        addClientParams.put("autoApprove", "Auto approve scopes, comma-separated (optional)");

        addClientApi.put("parameters", addClientParams);

        // Check if Client Exists API
        Map<String, Object> checkClientApi = new HashMap<>();
        checkClientApi.put("url", "/api/clients/{clientId}/exists");
        checkClientApi.put("method", "GET");
        checkClientApi.put("description", "Check if the client exists");
        checkClientApi.put("auth_required", "Basic Auth (admin)");

        // Remove Client API
        Map<String, Object> removeClientApi = new HashMap<>();
        removeClientApi.put("url", "/api/clients/{clientId}");
        removeClientApi.put("method", "DELETE");
        removeClientApi.put("description", "Remove the client");
        removeClientApi.put("auth_required", "Basic Auth (admin)");

        // Integrate all APIs
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("add_client", addClientApi);
        endpoints.put("check_client_exists", checkClientApi);
        endpoints.put("remove_client", removeClientApi);

        docs.put("endpoints", endpoints);

        // Examples
        Map<String, Object> examples = new HashMap<>();

        // Add Client Example
        examples.put("add_client", "curl -X POST -u admin:admin123 'http://localhost:9001/api/clients?clientId=my-client&clientSecret=my-secret&resourceIds=legacy-api&scope=message.read&authorizedGrantTypes=client_credentials&accessTokenValidity=3600'");

        // Check Client Example
        examples.put("check_client_exists", "curl -u admin:admin123 'http://localhost:9001/api/clients/my-client/exists'");

        // Remove Client Example
        examples.put("remove_client", "curl -X DELETE -u admin:admin123 'http://localhost:9001/api/clients/my-client'");

        docs.put("examples", examples);

        return docs;
    }
}