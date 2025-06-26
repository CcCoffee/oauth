package com.example.api_auth_server.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

//@Aspect
//@Component
public class ControllerLoggingAspect {
    
    private static final Logger apiLogger = LoggerFactory.getLogger("API_LOG");
    private final ObjectMapper objectMapper;
    private final MethodParameterExtractor parameterExtractor;
    
    public ControllerLoggingAspect(ObjectMapper objectMapper, MethodParameterExtractor parameterExtractor) {
        this.objectMapper = objectMapper;
        this.parameterExtractor = parameterExtractor;
    }
    
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        ApiLogData logData = new ApiLogData();
        
        try {
            // Get request information
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                logData.setEndpoint(request.getRequestURI());
                logData.setMethod(request.getMethod());
                
                // Extract method parameters
                Map<String, Object> parameters = parameterExtractor.extractParameters(joinPoint);
                logData.setParameters(parameters);
                
                // Extract client ID from parameters or request
                String clientId = extractClientId(parameters, request);
                logData.setClientId(clientId);
            }
            
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Calculate duration and extract status from result
            long duration = System.currentTimeMillis() - startTime;
            logData.setDuration(duration);
            
            // Extract status code from result
            String statusCode = extractStatusCode(result);
            logData.setStatus(statusCode);
            
            // Log successful API call
            logApiCall(logData);
            
            return result;
            
        } catch (Exception e) {
            // Calculate duration and set error status
            long duration = System.currentTimeMillis() - startTime;
            logData.setDuration(duration);
            logData.setStatus("500");
            logData.setErrorMessage(e.getMessage());
            
            // Log failed API call
            logApiCall(logData);
            
            throw e;
        }
    }
    
    /**
     * 从方法返回值中提取HTTP状态码
     */
    private String extractStatusCode(Object result) {
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            return String.valueOf(responseEntity.getStatusCode().value());
        }
        
        // 如果不是ResponseEntity，默认返回200
        return "200";
    }
    
    private void logApiCall(ApiLogData logData) {
        try {
            String jsonLog = objectMapper.writeValueAsString(logData);
            apiLogger.info(jsonLog);
        } catch (Exception e) {
            apiLogger.error("Failed to serialize API log data", e);
        }
    }
    
    private String extractClientId(Map<String, Object> parameters, HttpServletRequest request) {
        // First try to get client_id from method parameters
        Object clientIdParam = parameters.get("client_id");
        if (clientIdParam != null) {
            return clientIdParam.toString();
        }
        
        // Fall back to extracting from request (including Basic Auth)
        String authorizationHeader = request.getHeader("Authorization");
        String clientId = SensitiveDataMasker.extractClientId(null, request.getQueryString(), authorizationHeader);
        return clientId;
    }
}
