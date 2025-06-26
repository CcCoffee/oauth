package com.example.api_auth_server.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Component
public class MethodParameterExtractor {
    
    /**
     * 从AOP切点提取方法参数
     * @param joinPoint AOP切点
     * @return 参数名称到值的映射
     */
    public Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            Parameter[] params = method.getParameters();
            
            for (int i = 0; i < params.length && i < args.length; i++) {
                Parameter param = params[i];
                Object value = args[i];
                
                // 获取参数名
                String paramName = extractParameterName(param);
                
                // 过滤不需要记录的参数类型
                if (shouldIncludeParameter(param, value)) {
                    parameters.put(paramName, value);
                }
            }
            
            // 对敏感参数进行脱敏
            return maskSensitiveParameters(parameters);
            
        } catch (Exception e) {
            // 参数提取失败时返回空map，不影响主流程
            return new HashMap<>();
        }
    }
    
    /**
     * 提取参数名称，优先使用Spring注解
     */
    private String extractParameterName(Parameter param) {
        // 检查@RequestParam注解
        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.value().isEmpty()) {
            return requestParam.value();
        }
        if (requestParam != null && !requestParam.name().isEmpty()) {
            return requestParam.name();
        }
        
        // 检查@PathVariable注解
        PathVariable pathVariable = param.getAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isEmpty()) {
            return pathVariable.value();
        }
        if (pathVariable != null && !pathVariable.name().isEmpty()) {
            return pathVariable.name();
        }
        
        // 检查@RequestHeader注解
        RequestHeader requestHeader = param.getAnnotation(RequestHeader.class);
        if (requestHeader != null && !requestHeader.value().isEmpty()) {
            return requestHeader.value();
        }
        if (requestHeader != null && !requestHeader.name().isEmpty()) {
            return requestHeader.name();
        }
        
        // 检查@RequestBody注解
        RequestBody requestBody = param.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            return "requestBody";
        }
        
        // 如果没有注解，使用参数名
        return param.getName();
    }
    
    /**
     * 判断是否应该包含此参数
     */
    private boolean shouldIncludeParameter(Parameter param, Object value) {
        if (value == null) {
            return false;
        }
        
        // 排除HttpServletRequest、HttpServletResponse等
        Class<?> paramType = param.getType();
        String typeName = paramType.getName();
        
        if (typeName.startsWith("jakarta.servlet.") ||
            typeName.startsWith("javax.servlet.") ||
            typeName.startsWith("org.springframework.web.") ||
            typeName.startsWith("org.springframework.http.")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 对敏感参数进行脱敏
     */
    public Map<String, Object> maskSensitiveParameters(Map<String, Object> params) {
        // 使用SensitiveDataMasker的统一脱敏方法
        return SensitiveDataMasker.maskSensitiveParameters(params);
    }
} 