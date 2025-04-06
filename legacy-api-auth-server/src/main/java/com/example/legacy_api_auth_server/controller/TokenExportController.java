package com.example.legacy_api_auth_server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/token")
public class TokenExportController {

    private static final Logger logger = LoggerFactory.getLogger(TokenExportController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/export-csv")
    public ResponseEntity<byte[]> exportTokensToCsv() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM oauth_access_token");
            
            List<Map<String, Object>> exportableTokens = new ArrayList<>();
            
            for (Map<String, Object> row : rows) {
                Map<String, Object> exportableToken = new HashMap<>();
                
                // 复制原始数据
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // 对token和authentication列进行特殊处理
                    if ("token".equals(key) && value instanceof byte[]) {
                        try {
                            OAuth2AccessToken accessToken = (OAuth2AccessToken) SerializationUtils.deserialize((byte[]) value);
                            String tokenJson = objectMapper.writeValueAsString(accessToken);
                            exportableToken.put(key, tokenJson);
                        } catch (Exception e) {
                            logger.error("反序列化token失败", e);
                            exportableToken.put(key, "序列化错误");
                        }
                    } else if ("authentication".equals(key) && value instanceof byte[]) {
                        try {
                            OAuth2Authentication authentication = (OAuth2Authentication) SerializationUtils.deserialize((byte[]) value);
                            String authJson = objectMapper.writeValueAsString(authentication);
                            exportableToken.put(key, authJson);
                        } catch (Exception e) {
                            logger.error("反序列化authentication失败", e);
                            exportableToken.put(key, "序列化错误");
                        }
                    } else {
                        // 其他列直接添加
                        exportableToken.put(key, value != null ? value.toString() : "");
                    }
                }
                
                exportableTokens.add(exportableToken);
            }
            
            // 生成CSV
            CsvMapper csvMapper = new CsvMapper();
            List<String> columns = List.of(
                    "token_id", "token", "authentication_id", "user_name", 
                    "client_id", "authentication", "refresh_token"
            );
            
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (String column : columns) {
                schemaBuilder.addColumn(column);
            }
            CsvSchema schema = schemaBuilder.build().withHeader();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            csvMapper.writer(schema).writeValues(baos).writeAll(exportableTokens);
            
            // 设置HTTP响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "oauth_access_tokens.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());
            
        } catch (IOException e) {
            logger.error("导出CSV文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 