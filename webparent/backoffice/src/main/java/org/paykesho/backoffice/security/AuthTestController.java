package org.paykesho.backoffice.security;

import org.commons.entity.User;
import org.paykesho.backoffice.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthTestController {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/test/db-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test database connection
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                result.put("database_connection", isValid ? "SUCCESS" : "FAILED");
                result.put("database_url", connection.getMetaData().getURL());
            }
            
            // Test user count query
            long userCount = userRepository.count();
            result.put("user_count", userCount);
            result.put("user_repository", "SUCCESS");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/test/user-lookup")
    public ResponseEntity<Map<String, Object>> testUserLookup(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            User user = userRepository.getUserByEmail(email);
            if (user != null) {
                result.put("user_found", true);
                result.put("user_id", user.getId());
                result.put("user_email", user.getEmail());
                result.put("user_enabled", user.isEnabled());
                result.put("user_locked", !user.isAccountNonLocked());
                result.put("failed_attempts", user.getFailedAttempt());
            } else {
                result.put("user_found", false);
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/test/set-password")
    public ResponseEntity<Map<String, Object>> setTestPassword(
            @RequestParam String email, 
            @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            User user = userRepository.getUserByEmail(email);
            if (user != null) {
                // Encode the new password
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                
                // Reset failed attempts and unlock account
                user.setFailedAttempt(0);
                user.setAccountNonLocked(true);
                user.setLockTime(null);
                
                // Save the user
                userRepository.save(user);
                
                result.put("success", true);
                result.put("message", "Password updated for user: " + email);
                result.put("encoded_password", encodedPassword);
            } else {
                result.put("success", false);
                result.put("message", "User not found: " + email);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
}
