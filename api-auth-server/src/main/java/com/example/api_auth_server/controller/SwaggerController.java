package com.example.api_auth_server.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Swagger API document controller
 * Provides API document access and integrates the endpoint of Swagger UI
 */
@RestController
@RequestMapping("/api-docs")
public class SwaggerController {

    /**
     * Get API document
     * Return the YAML document of OpenAPI 3.0 specification
     */
    @GetMapping(value = "/api.yml", produces = "application/yaml")
    public ResponseEntity<String> getApiYaml() throws IOException {
        // Read the api.yml file from the resource directory
        Resource resource = new ClassPathResource("static/api.yml");
        String content = new String(Files.readAllBytes(resource.getFile().toPath()), StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(content);
    }
    
    /**
     * Provide Swagger UI interface
     * Redirect to Swagger Editor and load the local API document
     */
    @GetMapping("/swagger-ui")
    public String getSwaggerUI() {
        // Return HTML content to guide users to access Swagger Editor
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>API Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>OAuth2 Authorization Service API Document</h1>\n" +
                "    <p>Please click the link below to view the API document:</p>\n" +
                "    <a href=\"https://editor.swagger.io/?url=http://localhost:9000/api-docs/api.yml\" target=\"_blank\">View in Swagger Editor</a>\n" +
                "</body>\n" +
                "</html>";
    }
} 