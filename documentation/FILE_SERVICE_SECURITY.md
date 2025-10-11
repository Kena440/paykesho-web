# File Service Security Guide

## Overview

The file-service implements multiple layers of security to protect against common vulnerabilities and abuse patterns. This document outlines the security features and best practices.

## Security Layers

### 1. Rate Limiting

#### Purpose
Prevents abuse and DoS attacks by limiting the number of requests from a single IP address.

#### Configuration

**Development (Default: Disabled)**
```properties
paykesho.security.rate-limiting.enabled=false
```

**Production (Recommended: Enabled)**
```properties
paykesho.security.rate-limiting.enabled=true
```

#### Limits

| Endpoint Type | Limit | Time Window |
|--------------|-------|-------------|
| Upload API (`/uploads-api`) | 20 requests | 60 seconds |
| General API | 100 requests | 60 seconds |

#### IP Detection
The system detects client IP using:
1. `X-Forwarded-For` header (preferred for proxied requests)
2. `X-Real-IP` header (fallback)
3. Direct remote address (last resort)

#### Response
When rate limit is exceeded:
```json
HTTP/1.1 429 Too Many Requests
{
  "error": "Rate limit exceeded. Please try again later."
}
```

### 2. Input Validation

#### File Upload Validation

**File Size**
- Maximum: 5MB (development) / 10MB (production)
- Configurable via `file.storage.max-file-size`

**Allowed File Types**
- `image/jpeg`
- `image/jpg`
- `image/png`
- `application/pdf`

**Filename Security**
- Path traversal prevention (`..` sequences blocked)
- Special character sanitization
- UUID prefix for uniqueness

**Example Validation Error**
```json
{
  "error": "File size exceeds maximum allowed size of 5242880 bytes"
}
```

### 3. Security Headers

The service automatically adds security headers to all responses:

#### HTTP Strict Transport Security (HSTS)
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
```
- Forces HTTPS connections
- 1-year duration
- Applies to all subdomains

#### X-Frame-Options
```
X-Frame-Options: DENY
```
- Prevents clickjacking attacks
- Blocks embedding in iframes

#### X-Content-Type-Options
```
X-Content-Type-Options: nosniff
```
- Prevents MIME-type sniffing
- Enforces declared content types

#### X-XSS-Protection
```
X-XSS-Protection: 1; mode=block
```
- Enables browser XSS filter
- Blocks page rendering on XSS detection

#### Content Security Policy
```
Content-Security-Policy: default-src 'self'
```
- Restricts resource loading to same origin
- Prevents XSS and data injection attacks

### 4. Stateless Session Management

```java
SessionCreationPolicy.STATELESS
```

- No server-side sessions
- Suitable for microservices
- Reduces memory footprint
- Improves horizontal scalability

### 5. CSRF Protection

CSRF is disabled for REST API endpoints since the service is stateless and doesn't use session cookies.

```java
.csrf(csrf -> csrf.disable())
```

**Note**: Re-enable CSRF if adding session-based authentication.

## Access Control

### Public Endpoints
These endpoints are accessible without authentication:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/uploads-api` | POST | Upload files |
| `/access-api` | POST | Get file access URLs |
| `/uploads/**` | GET | Serve static files |
| `/actuator/health` | GET | Health checks |

### Protected Endpoints
These require authentication:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/actuator/**` | GET | Management endpoints |

## Best Practices

### 1. Production Deployment

```properties
# Enable security features
paykesho.security.rate-limiting.enabled=true

# Use environment-specific URLs
file.storage.base-url=https://files.yourdomain.com

# Restrict actuator exposure
management.endpoints.web.exposure.include=health,info
```

### 2. Reverse Proxy Configuration

When using nginx or HAProxy, ensure headers are forwarded:

**Nginx**
```nginx
location /file-service/ {
    proxy_pass http://file-service:8086/;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

**HAProxy**
```
backend file-service
    option forwardfor
    http-request set-header X-Forwarded-Proto https if { ssl_fc }
    server file1 file-service:8086 check
```

### 3. File Storage Security

#### Directory Permissions
```bash
# Uploads directory should have restricted permissions
chmod 750 /app/uploads
chown app-user:app-group /app/uploads
```

#### Storage Isolation
- Store files outside web root
- Use UUID-based filenames
- Separate directories by file type

### 4. Database Security

```properties
# Use secure credentials
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Enable SSL for database connections (if supported)
spring.datasource.url=jdbc:mysql://mysql:3306/paykesho_db?useSSL=true
```

### 5. Logging and Monitoring

```properties
# Log security events
logging.level.org.paykesho.fileservice.security=INFO

# Monitor rate limiting
logging.level.org.paykesho.fileservice.security.RateLimitingFilter=WARN
```

## Threat Mitigation

### 1. Denial of Service (DoS)
**Mitigation**: Rate limiting per IP address
```
Max 20 uploads/minute per IP
Max 100 requests/minute per IP
```

### 2. Path Traversal
**Mitigation**: Filename validation and sanitization
```java
if (filename != null && filename.contains("..")) {
    throw new IllegalArgumentException("Filename contains invalid path sequence");
}
```

### 3. File Bomb Attacks
**Mitigation**: File size limits
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### 4. Malicious File Upload
**Mitigation**: Content type validation
```java
ALLOWED_CONTENT_TYPES = ["image/jpeg", "image/png", "application/pdf"]
```

### 5. MIME Confusion
**Mitigation**: Strict content type checking and nosniff header
```
X-Content-Type-Options: nosniff
```

## Security Testing

### 1. Rate Limiting Test
```bash
# Test upload endpoint rate limit
for i in {1..25}; do
  curl -X POST http://localhost:8086/uploads-api \
    -F "file=@test.pdf" \
    -F "file_type=photo" \
    -F "user_id=1"
done

# Should get 429 response after 20 requests
```

### 2. File Validation Test
```bash
# Test large file rejection
dd if=/dev/zero of=largefile.bin bs=1M count=20
curl -X POST http://localhost:8086/uploads-api \
  -F "file=@largefile.bin" \
  -F "file_type=photo" \
  -F "user_id=1"

# Should get 400 response
```

### 3. Path Traversal Test
```bash
# Test path traversal prevention
curl -X POST http://localhost:8086/uploads-api \
  -F "file=@test.pdf" \
  -F "file_type=../../etc/passwd" \
  -F "user_id=1"

# Should get 400 response
```

### 4. Security Headers Test
```bash
curl -I http://localhost:8086/uploads/logo/test.png

# Verify headers present:
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
```

## Incident Response

### Detecting Abuse

Monitor logs for patterns:
```bash
# Check for rate limit violations
grep "Rate limit exceeded" /var/log/file-service/app.log

# Check for validation failures
grep "Validation error" /var/log/file-service/app.log
```

### Blocking Malicious IPs

For persistent abuse, implement IP blocking at the firewall level:
```bash
# iptables example
iptables -A INPUT -s <malicious-ip> -j DROP
```

Or use HAProxy ACLs:
```
acl blocked_ips src 192.168.1.100 192.168.1.101
http-request deny if blocked_ips
```

## Future Security Enhancements

1. **Virus Scanning**
   - Integrate ClamAV for file scanning
   - Scan files before storage

2. **JWT Authentication**
   - Add token-based authentication
   - Validate requests from other services

3. **File Encryption**
   - Encrypt files at rest
   - Use encryption keys from vault

4. **Audit Logging**
   - Log all file operations
   - Track user actions

5. **Advanced Rate Limiting**
   - Per-user rate limits
   - Sliding window algorithm
   - Redis-based distributed limiting

## Compliance

### GDPR Considerations
- Files contain personal data
- Implement data retention policies
- Provide file deletion capabilities
- Log access for audit trails

### PCI DSS
- If storing payment-related documents
- Ensure encryption in transit (HTTPS)
- Implement access controls
- Regular security audits

## Support and Reporting

To report security vulnerabilities:
1. **DO NOT** create public issues
2. Contact security team directly
3. Provide detailed reproduction steps
4. Allow time for patching before disclosure

## Conclusion

The file-service implements defense-in-depth security with multiple layers of protection. Regular security audits and updates are recommended to maintain a strong security posture.

