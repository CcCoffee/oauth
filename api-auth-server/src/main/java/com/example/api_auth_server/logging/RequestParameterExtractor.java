package com.example.api_auth_server.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 请求参数提取器接口
 * 用于处理不同Content-Type的参数提取
 */
public interface RequestParameterExtractor {
    
    /**
     * 从HTTP请求中提取参数
     * @param request HTTP请求
     * @return 参数名称到值的映射
     */
    Map<String, Object> extractParameters(HttpServletRequest request);
    
    /**
     * 判断是否支持指定的Content-Type
     * @param contentType 内容类型
     * @return 是否支持
     */
    boolean supports(String contentType);
    
    /**
     * 获取支持的Content-Type列表
     * @return Content-Type数组
     */
    String[] getSupportedContentTypes();
} 