# File Service Configuration Guide

## Overview

The file service URL is now configurable across all modules using application properties, making it easy to switch between development, staging, and production environments.

## Configuration Property

All modules use the following property:
```properties
file.service.url=<URL>
```

Or in YAML:
```yaml
file:
  service:
    url: <URL>
```

## Configuration by Environment

### Development (Local)

**Backoffice** - `webparent/backoffice/src/main/resources/application.properties`:
```properties
file.service.url=http://localhost:8086
```

**Client Portal** - `webparent/client-portal/src/main/resources/application.properties`:
```properties
file.service.url=http://localhost:8086
```

### Docker/Production

**Backoffice** - `deploy/backoffice/application.properties`:
```properties
file.service.url=http://file-service:8086
```

**Client Portal** - `deploy/client-portal/application.yml`:
```yaml
file:
  service:
    url: http://file-service:8086
```

**Merchant Portal** - `deploy/merchant-portal/application.yml`:
```yaml
file:
  service:
    url: http://file-service:8086
```

**Organization Portal** - `deploy/organization-portal/application.yml`:
```yaml
file:
  service:
    url: http://file-service:8086
```

## How It Works

### In Code

The `FileUploadService` classes use Spring's `@Value` annotation to inject the configuration:

```java
@Service
public class FileUploadService {
    
    @Value("${file.service.url:http://localhost:8086}")
    private String fileServiceUrl;
    
    // Uses fileServiceUrl for API calls
    restTemplate.exchange(fileServiceUrl + "/uploads-api", ...);
    restTemplate.exchange(fileServiceUrl + "/access-api", ...);
}
```

### Default Value

If the property is not set, it defaults to `http://localhost:8086`:
```java
@Value("${file.service.url:http://localhost:8086}")
```

## Environment Override

You can override the configuration using environment variables:

### Docker Compose
```yaml
services:
  backoffice:
    environment:
      - FILE_SERVICE_URL=http://file-service:8086
```

### Command Line
```bash
java -jar backoffice.jar --file.service.url=http://custom-file-service:8086
```

### System Property
```bash
java -Dfile.service.url=http://custom-file-service:8086 -jar backoffice.jar
```

## Migration from Hardcoded URL

### Before
```java
// Hardcoded URL - not flexible
ResponseEntity<String> response = restTemplate.exchange(
    "http://localhost:8086/uploads-api", 
    HttpMethod.POST, 
    requestEntity, 
    String.class
);
```

### After
```java
// Configurable URL - environment-specific
@Value("${file.service.url:http://localhost:8086}")
private String fileServiceUrl;

ResponseEntity<String> response = restTemplate.exchange(
    fileServiceUrl + "/uploads-api", 
    HttpMethod.POST, 
    requestEntity, 
    String.class
);
```

## Benefits

### 1. **Environment Flexibility**
- Easy to switch between dev, staging, and production
- No code changes needed for different environments

### 2. **Configuration Management**
- Centralized configuration in properties files
- Easy to update without recompilation

### 3. **Docker Integration**
- Seamless service discovery using Docker service names
- Works with Docker Compose networking

### 4. **Testing**
- Easy to point to mock file service for testing
- Can override per test environment

### 5. **Maintenance**
- Single source of truth for service URLs
- Easier to update when infrastructure changes

## Testing Different Configurations

### Test with Local File Service
```properties
file.service.url=http://localhost:8086
```

### Test with Remote File Service
```properties
file.service.url=https://files.paykesho.com
```

### Test with Docker Service
```properties
file.service.url=http://file-service:8086
```

## Troubleshooting

### Issue: Connection Refused
**Check**: Is the file service URL correct?
```bash
# Test connectivity
curl http://localhost:8086/actuator/health
```

### Issue: Wrong URL Being Used
**Check**: Property precedence
1. Command line arguments (highest)
2. System properties
3. Environment variables
4. application.properties
5. Default value (lowest)

**Debug**:
```java
@PostConstruct
public void init() {
    log.info("File service URL configured as: {}", fileServiceUrl);
}
```

### Issue: Service Not Found in Docker
**Check**: Docker network configuration
```bash
# Verify service is running
docker ps | grep file-service

# Check network connectivity
docker exec backoffice ping file-service
```

## Best Practices

### 1. Always Use Default Values
```java
@Value("${file.service.url:http://localhost:8086}")
```
This ensures the application starts even if the property is missing.

### 2. Document Configuration
Keep this document updated when adding new services or changing URLs.

### 3. Use Environment-Specific Profiles
```properties
# application-dev.properties
file.service.url=http://localhost:8086

# application-prod.properties
file.service.url=http://file-service:8086
```

### 4. Validate Configuration on Startup
Add health checks to ensure the file service is reachable:
```java
@PostConstruct
public void validateFileService() {
    try {
        restTemplate.getForEntity(fileServiceUrl + "/actuator/health", String.class);
        log.info("File service is reachable at: {}", fileServiceUrl);
    } catch (Exception e) {
        log.warn("File service not reachable at: {}", fileServiceUrl);
    }
}
```

## Related Services

Other services also use configurable URLs:

### Notification Service
```properties
notification.service.url=http://localhost:8085  # Dev
notification.service.url=http://notification-service:8085  # Docker
```

### Payment Service
```properties
payment.service.url=http://localhost:8084  # Dev
payment.service.url=http://payment-service:8084  # Docker
```

## Summary

| Module | Dev URL | Docker URL |
|--------|---------|------------|
| File Service | `http://localhost:8086` | `http://file-service:8086` |
| Notification Service | `http://localhost:8085` | `http://notification-service:8085` |
| Payment Service | `http://localhost:8084` | `http://payment-service:8084` |

## Conclusion

The file service URL is now fully configurable, making the system more flexible and easier to deploy across different environments. This follows Spring Boot best practices and makes the application more maintainable.

---

*Last Updated: October 2024*

