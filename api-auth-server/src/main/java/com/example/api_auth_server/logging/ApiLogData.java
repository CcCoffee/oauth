package com.example.api_auth_server.logging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiLogData {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    private String traceId;
    private String spanId;
    private String endpoint;
    private String method;
    private Long duration;
    private String status;
    private String clientId;
    private String errorMessage;
    private Map<String, Object> parameters;
    
    public ApiLogData() {
        this.timestamp = LocalDateTime.now();
        this.traceId = MDC.get("traceId");
        this.spanId = MDC.get("spanId");
        
        if (this.traceId == null) {
            this.traceId = MDC.get("X-Trace-Id");
        }
        if (this.traceId == null) {
            this.traceId = MDC.get("trace-id");
        }
        if (this.spanId == null) {
            this.spanId = MDC.get("span-id");
        }
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public Long getDuration() {
        return duration;
    }
    
    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
