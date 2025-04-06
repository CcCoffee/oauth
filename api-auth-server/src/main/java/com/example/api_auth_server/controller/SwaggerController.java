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
 * Swagger API文档控制器
 * 提供API文档访问及集成Swagger UI的端点
 */
@RestController
@RequestMapping("/api-docs")
public class SwaggerController {

    /**
     * 获取API文档
     * 返回OpenAPI 3.0规范的YAML文档
     */
    @GetMapping(value = "/api.yml", produces = "application/yaml")
    public ResponseEntity<String> getApiYaml() throws IOException {
        // 从资源目录读取api.yml文件
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
     * 提供Swagger UI界面
     * 重定向到Swagger Editor，并加载本地API文档
     */
    @GetMapping("/swagger-ui")
    public String getSwaggerUI() {
        // 返回HTML内容，引导用户访问Swagger Editor
        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>API文档</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>OAuth2授权服务API文档</h1>\n" +
                "    <p>请点击下面的链接查看API文档：</p>\n" +
                "    <a href=\"https://editor.swagger.io/?url=http://localhost:9000/api-docs/api.yml\" target=\"_blank\">在Swagger Editor中查看</a>\n" +
                "</body>\n" +
                "</html>";
    }
} 