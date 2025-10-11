# Docker File Mapping Verification for VPS Deployment

## Current Configuration Analysis

### ✅ File Service Configuration

#### Docker Compose Setup
```yaml
file-service:
  build: ./file-service
  volumes:
    - ./file-service/application.properties:/app/application.properties:ro
    - file_uploads:/app/uploads  # ✅ Volume mapped correctly
  environment:
    FILE_STORAGE_PATH: ${FILE_STORAGE_PATH:-/app/uploads}
```

#### Volume Definition
```yaml
volumes:
  file_uploads:  # ✅ Named volume for persistence
```

#### Application Properties (Deploy)
```properties
file.storage.upload-dir=/app/uploads  # ✅ Matches volume mount
file.storage.base-url=http://file-service:8086  # ✅ Correct internal URL
```

### ✅ File Mapping Alignment

| Component | Path/URL | Status |
|-----------|----------|--------|
| **Docker Volume** | `file_uploads` → `/app/uploads` | ✅ Correct |
| **File Service Config** | `file.storage.upload-dir=/app/uploads` | ✅ Matches |
| **File Service Base URL** | `http://file-service:8086` | ✅ Correct |
| **Volume Persistence** | Named volume (persists) | ✅ Good |

## File Storage Structure in Container

```
/app/
├── application.properties (mounted from host)
├── file-service.jar
└── uploads/ (Docker volume: file_uploads)
    ├── logo/
    │   ├── uuid_file1.png
    │   └── uuid_file2.jpg
    ├── payslip/
    │   ├── uuid_doc1.pdf
    │   └── uuid_doc2.pdf
    └── photo/
        ├── uuid_photo1.jpg
        └── uuid_photo2.png
```

## Service URL Configuration

### Development (Local)
```properties
# backoffice & client-portal
file.service.url=http://localhost:8086
```

### Production (Docker)
```properties
# backoffice
file.service.url=http://file-service:8086  ✅

# client-portal, merchant-portal, organization-portal
file:
  service:
    url: http://file-service:8086  ✅
```

## ✅ Everything is Aligned!

### 1. Volume Mapping ✅
- **Docker Volume**: `file_uploads`
- **Mount Point**: `/app/uploads`
- **Service Expects**: `/app/uploads`
- **Result**: ✅ **ALIGNED**

### 2. Service Discovery ✅
- **File Service Name**: `file-service`
- **Other Services Point To**: `http://file-service:8086`
- **Docker Network**: `paykesho-network`
- **Result**: ✅ **ALIGNED**

### 3. File Access ✅
- **Upload URL**: `http://file-service:8086/uploads-api`
- **Access URL**: `http://file-service:8086/access-api`
- **Serve URL**: `http://file-service:8086/uploads/{type}/{filename}`
- **Result**: ✅ **ALIGNED**

## Deployment Flow

### 1. File Upload
```
User → Backoffice (Docker) → http://file-service:8086/uploads-api
                                      ↓
                            File saved to /app/uploads
                                      ↓
                            Persisted in file_uploads volume
```

### 2. File Access
```
User → Backoffice (Docker) → http://file-service:8086/access-api
                                      ↓
                            Returns: http://file-service:8086/uploads/logo/uuid_file.png
                                      ↓
                            Browser requests file from file-service
                                      ↓
                            File served from /app/uploads volume
```

## ⚠️ Potential Issues & Solutions

### Issue 1: External File Access from Browser
**Problem**: Browsers can't access `http://file-service:8086` (internal Docker network)

**Solution**: Files need to be served through reverse proxy (HAProxy/Nginx)

**Fix Required**: Update HAProxy configuration

```haproxy
# Add to haproxy.cfg
frontend file_frontend
    bind *:8086
    default_backend file_backend

backend file_backend
    balance roundrobin
    server file1 file-service:8086 check
```

### Issue 2: File URLs in Response
**Problem**: File URLs contain internal Docker hostname

**Current**:
```json
{
  "file_url": "http://file-service:8086/uploads/logo/uuid_file.png"
}
```

**Solution**: Update `file.storage.base-url` for external access

**Fix Required in deploy/file-service/application.properties**:
```properties
# Change from:
file.storage.base-url=http://file-service:8086

# To (for production):
file.storage.base-url=https://yourdomain.com
# OR
file.storage.base-url=http://your-vps-ip:8086
```

### Issue 3: Direct Browser Access to Files
**Problem**: Users can't access files directly from external network

**Solution 1: HAProxy Routing** (Recommended)
```haproxy
# In haproxy.cfg - Add backend for file-service
backend file-service
    balance roundrobin
    option httpchk GET /actuator/health
    http-check expect status 200
    server file1 file-service:8086 check inter 2000ms rise 2 fall 3
```

**Solution 2: Nginx Reverse Proxy**
```nginx
location /files/ {
    proxy_pass http://file-service:8086/uploads/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## 🔧 Required Updates for VPS Deployment

### 1. Update File Service Base URL

**File**: `deploy/file-service/application.properties`

```properties
# Change this based on your setup:

# Option A: If using HAProxy on port 80/443
file.storage.base-url=https://yourdomain.com

# Option B: If exposing file-service port
file.storage.base-url=http://your-vps-ip:8086

# Option C: If using subdomain
file.storage.base-url=https://files.yourdomain.com
```

### 2. Update HAProxy Configuration (Recommended)

**File**: `deploy/haproxy.cfg`

Add file-service backend:
```haproxy
# File Service Backend (around line 133)
backend file-service
    balance roundrobin
    option httpchk GET /actuator/health
    http-check expect status 200
    server file1 file-service:8086 check inter 2000ms rise 2 fall 3

# Add routing rule in frontend
frontend paykesho_frontend
    # ... existing rules ...
    
    # Route file service requests
    acl is_file_service path_beg /uploads-api /access-api /uploads/
    use_backend file-service if is_file_service
```

### 3. Ensure Docker Network Connectivity

**File**: `deploy/docker-compose.yml`

Verify all services are on the same network:
```yaml
services:
  backoffice:
    networks:
      - paykesho-network  # ✅

  client-portal:
    networks:
      - paykesho-network  # ✅

  file-service:
    networks:
      - paykesho-network  # ✅
```

## 🧪 Testing After Deployment

### Test 1: Internal Service Discovery
```bash
# SSH into backoffice container
docker exec -it backoffice bash

# Test file-service connectivity
curl http://file-service:8086/actuator/health

# Expected: {"status":"UP"}
```

### Test 2: File Upload
```bash
# From host or external
curl -X POST http://your-vps-ip/uploads-api \
  -F "file=@test.pdf" \
  -F "file_type=photo" \
  -F "user_id=1" \
  -F "description=Test upload"
```

### Test 3: File Access
```bash
# Test access API
curl -X POST http://your-vps-ip/access-api \
  -H "Content-Type: application/json" \
  -d '{"file_id": 1}'
```

### Test 4: Volume Persistence
```bash
# Check if files persist after restart
docker-compose down
docker-compose up -d

# Files should still be there
docker exec file-service ls -la /app/uploads/
```

## 📋 Pre-Deployment Checklist

- [ ] Update `file.storage.base-url` in deploy config
- [ ] Update HAProxy configuration for file-service routing
- [ ] Verify all services have correct `file.service.url`
- [ ] Test Docker network connectivity
- [ ] Backup existing files (if any)
- [ ] Test file upload/download flow
- [ ] Verify volume persistence
- [ ] Check file permissions in volume
- [ ] Monitor disk space on VPS
- [ ] Set up log rotation for file-service

## 🎯 Summary

### Current Status
✅ **Volume Mapping**: Correctly configured
✅ **Internal URLs**: All services point to correct endpoints
✅ **Persistence**: Named volume ensures data survives restarts
⚠️ **External Access**: Needs HAProxy/Nginx configuration
⚠️ **Base URL**: Should be updated for production domain

### What Works Out of the Box
- ✅ File uploads from internal services
- ✅ File storage in Docker volume
- ✅ Data persistence across restarts
- ✅ Service-to-service communication

### What Needs Configuration
- ⚠️ External file access via browser
- ⚠️ Production domain/IP in base URL
- ⚠️ HAProxy routing for file endpoints
- ⚠️ SSL/HTTPS setup (optional but recommended)

## 🚀 Recommended Production Setup

```properties
# deploy/file-service/application.properties
file.storage.base-url=https://yourdomain.com
```

```haproxy
# deploy/haproxy.cfg
frontend paykesho_frontend
    bind *:443 ssl crt /etc/ssl/certs/yourdomain.pem
    
    acl is_file_upload path_beg /uploads-api /access-api
    acl is_file_serve path_beg /uploads/
    
    use_backend file-service if is_file_upload or is_file_serve

backend file-service
    balance roundrobin
    option httpchk GET /actuator/health
    server file1 file-service:8086 check
```

---

**Conclusion**: The file mappings are **correctly aligned** for Docker deployment. You just need to update the base URL and configure external access via HAProxy for complete functionality on your VPS.

