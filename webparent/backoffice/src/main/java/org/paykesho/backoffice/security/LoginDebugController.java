package org.paykesho.backoffice.security;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginDebugController {

    @PostMapping("/debug/login-attempt")
    public Map<String, Object> debugLoginAttempt(HttpServletRequest request) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        System.out.println("=== LOGIN DEBUG: POST request received ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Request Method: " + request.getMethod());
        System.out.println("Content Type: " + request.getContentType());
        
        // Log all parameters
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
            System.out.println("Parameter: " + paramName + " = " + paramValue);
        }
        
        // Log all headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
            System.out.println("Header: " + headerName + " = " + headerValue);
        }
        
        debugInfo.put("url", request.getRequestURL().toString());
        debugInfo.put("method", request.getMethod());
        debugInfo.put("parameters", params);
        debugInfo.put("headers", headers);
        debugInfo.put("contentType", request.getContentType());
        
        System.out.println("=== END LOGIN DEBUG ===");
        
        return debugInfo;
    }
    
    @GetMapping("/debug/test-endpoint")
    public Map<String, Object> testEndpoint() {
        System.out.println("=== TEST ENDPOINT HIT ===");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test endpoint is working");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
