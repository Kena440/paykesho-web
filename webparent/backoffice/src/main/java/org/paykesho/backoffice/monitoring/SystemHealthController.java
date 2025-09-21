package org.paykesho.backoffice.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * System health monitoring endpoint for comprehensive system status
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private DataSource dataSource;

    @GetMapping("/health/comprehensive")
    public ResponseEntity<Map<String, Object>> getComprehensiveHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> services = new HashMap<>();

        try {
            // Check database connectivity
            services.put("database", checkDatabaseHealth());

            // Check microservices
            services.put("payment-service", checkServiceHealth("http://localhost:8084/actuator/health"));
            services.put("notification-service", checkServiceHealth("http://localhost:8085/actuator/health"));
            services.put("file-service", checkServiceHealth("http://localhost:8086/actuator/health"));
            services.put("organization-portal", checkServiceHealth("http://localhost:8083/actuator/health"));
            services.put("merchant-portal", checkServiceHealth("http://localhost:8082/actuator/health"));
            services.put("client-portal", checkServiceHealth("http://localhost:8081/actuator/health"));

            // Check system resources
            services.put("memory", checkMemoryHealth());
            services.put("disk", checkDiskHealth());

            health.put("services", services);
            health.put("overall-status", calculateOverallStatus(services));
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("Error checking comprehensive health: {}", e.getMessage(), e);
            health.put("error", "Health check failed");
            health.put("overall-status", "DOWN");
            return ResponseEntity.internalServerError().body(health);
        }
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5); // 5 second timeout
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("url", connection.getMetaData().getURL());
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    private Map<String, Object> checkServiceHealth(String healthUrl) {
        Map<String, Object> serviceHealth = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(healthUrl, (Class<Map<String, Object>>) (Class<?>) Map.class);
            serviceHealth.put("status", response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
            serviceHealth.put("response-time", System.currentTimeMillis());
        } catch (Exception e) {
            serviceHealth.put("status", "DOWN");
            serviceHealth.put("error", e.getMessage());
        }
        return serviceHealth;
    }

    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> memoryHealth = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double memoryUsagePercent = ((double) usedMemory / maxMemory) * 100;

        memoryHealth.put("max-memory-mb", maxMemory / 1024 / 1024);
        memoryHealth.put("used-memory-mb", usedMemory / 1024 / 1024);
        memoryHealth.put("free-memory-mb", freeMemory / 1024 / 1024);
        memoryHealth.put("usage-percent", Math.round(memoryUsagePercent * 100.0) / 100.0);
        memoryHealth.put("status", memoryUsagePercent > 90 ? "CRITICAL" : memoryUsagePercent > 75 ? "WARNING" : "OK");

        return memoryHealth;
    }

    private Map<String, Object> checkDiskHealth() {
        Map<String, Object> diskHealth = new HashMap<>();
        try {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            double diskUsagePercent = ((double) usedSpace / totalSpace) * 100;

            diskHealth.put("total-space-gb", totalSpace / 1024 / 1024 / 1024);
            diskHealth.put("used-space-gb", usedSpace / 1024 / 1024 / 1024);
            diskHealth.put("free-space-gb", freeSpace / 1024 / 1024 / 1024);
            diskHealth.put("usage-percent", Math.round(diskUsagePercent * 100.0) / 100.0);
            diskHealth.put("status", diskUsagePercent > 90 ? "CRITICAL" : diskUsagePercent > 80 ? "WARNING" : "OK");

        } catch (Exception e) {
            diskHealth.put("status", "ERROR");
            diskHealth.put("error", e.getMessage());
        }
        return diskHealth;
    }

    private String calculateOverallStatus(Map<String, Object> services) {
        boolean hasDown = false;
        boolean hasWarning = false;

        for (Object service : services.values()) {
            if (service instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> serviceMap = (Map<String, Object>) service;
                String status = (String) serviceMap.get("status");

                if ("DOWN".equals(status) || "CRITICAL".equals(status)) {
                    hasDown = true;
                } else if ("WARNING".equals(status)) {
                    hasWarning = true;
                }
            }
        }

        if (hasDown) return "DOWN";
        if (hasWarning) return "WARNING";
        return "UP";
    }
}
