# 🔧 Docker Networking Fix for VPS Database Connection

**Issue**: Docker containers couldn't connect to VPS host database due to `host.docker.internal` resolution failure.

**Error**: `java.net.UnknownHostException: host.docker.internal`

## ✅ **Solution Applied**

### **1. Removed MySQL Container**
- ✅ Removed `mysql` service from `docker-compose.yml`
- ✅ Removed MySQL-related volumes and networks
- ✅ Simplified configuration since we're using VPS host database directly

### **2. Updated Docker Networking**
- ✅ Changed all services to use `network_mode: host`
- ✅ This allows containers to access VPS host database directly via `localhost:3306`

### **3. Updated Database Connections**
- ✅ Changed all database URLs from `host.docker.internal:3306` to `localhost:3306`
- ✅ Updated both `docker-compose.yml` and all `application.properties` files

## 📋 **Changes Made**

### **docker-compose.yml**
```yaml
# Before
services:
  mysql:
    image: mysql:8.0
    # ... MySQL configuration
  
  backoffice:
    build: ./backoffice
    # ... service configuration
    networks:
      - paykesho-network

# After
services:
  backoffice:
    build: ./backoffice
    network_mode: host
    # ... service configuration
```

### **Application Properties**
```properties
# Before
spring.datasource.url=jdbc:mysql://host.docker.internal:3306/paykesho_db

# After
spring.datasource.url=jdbc:mysql://localhost:3306/paykesho_db
```

## 🎯 **How This Fixes the Issue**

### **Before (Failed)**
```
Docker Container → host.docker.internal:3306 → ❌ Unknown Host
```

### **After (Working)**
```
Docker Container (host network) → localhost:3306 → ✅ VPS Database
```

## 🚀 **Benefits**

1. **✅ Direct Database Access**: Containers can now connect to VPS host database
2. **✅ Simplified Configuration**: No need for Docker networks or MySQL container
3. **✅ Better Performance**: Direct connection without Docker networking overhead
4. **✅ UUID Support**: Containers now access the same database with UUIDs

## 📋 **Services Updated**

All services now use host networking and connect to `localhost:3306/paykesho_db`:

- ✅ **backoffice** (port 8080)
- ✅ **client-portal** (port 8081)
- ✅ **merchant-portal** (port 8082)
- ✅ **organization-portal** (port 8083)
- ✅ **payment-service** (port 8084)
- ✅ **notification-service** (port 8085)
- ✅ **file-service** (port 8086)

## 🔄 **Deployment Steps**

1. **Upload Updated Configuration**:
```bash
scp -r deploy/ user@your-vps-ip:/path/to/your/deploy/
```

2. **Restart Services**:
```bash
ssh user@your-vps-ip
cd /path/to/your/deploy/
docker-compose down
docker-compose up -d
```

3. **Verify Connection**:
```bash
docker-compose logs backoffice
# Should show successful database connection
```

## ✅ **Expected Results**

- ✅ No more `UnknownHostException` errors
- ✅ Successful database connections
- ✅ UUID-enabled merchant URLs working
- ✅ All services running on host network ports

---

**Last Updated**: October 18, 2025  
**Status**: Ready for VPS deployment
