package com.example.api_auth_server.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class ControllerLoggingAspect {
    
    private static final Logger apiLogger = LoggerFactory.getLogger("API_LOG");
    private final ObjectMapper objectMapper;
    
    public ControllerLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
                
                // Extract client ID from request
                String requestBody = getRequestBody(joinPoint);
                String clientId = SensitiveDataMasker.extractClientId(requestBody, request.getQueryString());
                logData.setClientId(clientId);
            }
            
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Calculate duration and set success status
            long duration = System.currentTimeMillis() - startTime;
            logData.setDuration(duration);
            logData.setStatus("200");
            
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
    
    private void logApiCall(ApiLogData logData) {
        try {
            String jsonLog = objectMapper.writeValueAsString(logData);
            apiLogger.info(jsonLog);
        } catch (Exception e) {
            apiLogger.error("Failed to serialize API log data", e);
        }
    }
    
    private String getRequestBody(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null && arg.toString().contains("=")) {
                return SensitiveDataMasker.maskSensitiveData(arg.toString());
            }
        }
        return null;
    }
}
