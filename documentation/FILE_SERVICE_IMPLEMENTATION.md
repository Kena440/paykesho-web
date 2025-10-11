# File Service Implementation Guide

## Overview

The file-service module has been fully implemented as a Spring Boot microservice to replace the PHP-based external file service (`https://files.paykesho.com`). This provides better integration, security, and maintainability within the Paykesho ecosystem.

## Architecture

### Components

1. **Entity Layer** (`commons` module)
   - `FileUpload` - Enhanced entity for file metadata storage
   - Includes fields: id, fileName, filePath, fileType, userId, description, fileSize, contentType, createdAt, logMessage

2. **DTOs** (`commons` module)
   - `FileUploadResponse` - Response for file upload operations
   - `FileAccessResponse` - Response for file access requests
   - `FileAccessRequest` - Request for file access

3. **Repository Layer**
   - `FileUploadRepository` - JPA repository for database operations

4. **Service Layer**
   - `FileStorageService` - Core service handling file storage operations
   - Validates files (size, type, security)
   - Manages file storage in organized directories
   - Generates secure file URLs

5. **Controller Layer**
   - `FileUploadController` - Handles `/uploads-api` endpoint
   - `FileAccessController` - Handles `/access-api` endpoint
   - `FileServeController` - Serves static files via `/uploads/{fileType}/{filename}`

6. **Configuration**
   - `FileStorageProperties` - Configurable file storage settings
   - Support for different environments (local, docker)

## Security Features

### 1. Rate Limiting
- **Upload endpoints**: Max 20 requests/minute per IP
- **General endpoints**: Max 100 requests/minute per IP
- **Configurable**: Can be enabled/disabled via configuration
- **Smart detection**: Uses X-Forwarded-For and X-Real-IP headers

### 2. Security Headers
- **HSTS**: HTTP Strict Transport Security enabled
- **XSS Protection**: Cross-site scripting prevention
- **Frame Options**: Clickjacking protection (DENY)
- **Content Security Policy**: Restricts resource loading

### 3. Stateless Authentication
- No session management
- Suitable for microservice architecture
- Public access to file operations (rate-limited)
- Protected actuator endpoints

### 4. Input Validation
- File size limits
- Content type validation
- Path traversal prevention
- Filename sanitization

## Key Features

### 1. File Upload
**Endpoint:** `POST /uploads-api`

**Request Parameters:**
- `file` (MultipartFile) - The file to upload
- `file_type` (String) - Type: logo, payslip, photo
- `user_id` (Integer) - User ID performing the upload
- `description` (String, optional) - File description

**Response:**
```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "file_id": 123,
  "file_name": "document.pdf"
}
```

**Validations:**
- File size: Maximum 5MB (configurable)
- Allowed types: JPEG, PNG, PDF
- Security: Path traversal prevention
- Filename sanitization

### 2. File Access
**Endpoint:** `POST /access-api`

**Request Body:**
```json
{
  "file_id": 123
}
```

**Response:**
```json
{
  "file_name": "document.pdf",
  "file_url": "http://file-service:8086/uploads/logo/uuid_document.pdf"
}
```

### 3. File Serving
**Endpoint:** `GET /uploads/{fileType}/{filename}`

- Serves files directly with appropriate content types
- Supports inline viewing for images and PDFs
- Secure file access with validation

## Security Configuration

### Production Deployment
Enable rate limiting in production by setting environment variable:
```bash
RATE_LIMITING_ENABLED=true
```

### Development
Rate limiting is disabled by default for easier testing:
```properties
paykesho.security.rate-limiting.enabled=false
```

### Health Checks
Access health endpoint at:
```
GET http://localhost:8086/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

## Improvements Over PHP Version

### 1. **Better Validation**
- Type-safe enum-based file types
- Comprehensive input validation
- Security-first approach

### 2. **Enhanced Error Handling**
- Detailed logging
- Graceful error responses
- Better debugging capabilities

### 3. **Scalability**
- Connection pooling (HikariCP)
- Configurable resource limits
- Docker-ready configuration

### 4. **Maintainability**
- Clean architecture
- Separation of concerns
- Testable code structure

### 5. **Security Enhancements**
- Path traversal prevention
- File type validation
- Size restrictions
- Sanitized filenames

### 6. **Integration**
- Seamless integration with existing Paykesho modules
- Shared entities and DTOs via commons module
- Consistent error handling

## Configuration

### Local Development
**File:** `webparent/file-service/src/main/resources/application.properties`

```properties
server.port=8086
file.storage.upload-dir=uploads
file.storage.max-file-size=5242880
file.storage.base-url=http://localhost:8086
```

### Docker Deployment
**File:** `deploy/file-service/application.properties`

```properties
server.port=8086
file.storage.upload-dir=/app/uploads
file.storage.max-file-size=10485760
file.storage.base-url=http://file-service:8086
```

## Database Schema

The service uses the existing `file_uploads` table with enhanced fields:

```sql
CREATE TABLE file_uploads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_type VARCHAR(50) NOT NULL,
  user_id INT NOT NULL,
  description TEXT,
  log_message TEXT,
  created_at DATETIME,
  file_size BIGINT,
  content_type VARCHAR(100),
  remote_file_id INT
);
```

## Module Integration

### Updated Modules

1. **backoffice** - Now points to `http://localhost:8086`
2. **client-portal** - Now points to `http://localhost:8086`
3. **merchant-portal** - Configured for file-service
4. **organization-portal** - Configured for file-service

### File Upload Service Changes

Both `backoffice` and `client-portal` modules' `FileUploadService` classes have been updated to use the local file-service:

```java
// Before: https://files.paykesho.com/uploads-api
// After:  http://localhost:8086/uploads-api

// Before: https://files.paykesho.com/access-api
// After:  http://localhost:8086/access-api
```

## Running the Service

### Local Development

```bash
cd webparent/file-service
# Service will start on port 8086
# Uploads directory will be created automatically
```

### Docker Deployment

```bash
cd deploy
docker-compose up file-service
```

## File Storage Structure

```
uploads/
├── logo/
│   ├── uuid_logo1.png
│   └── uuid_logo2.jpg
├── payslip/
│   ├── uuid_payslip1.pdf
│   └── uuid_payslip2.pdf
└── photo/
    ├── uuid_photo1.jpg
    └── uuid_photo2.png
```

## API Compatibility

The Spring Boot implementation is **100% compatible** with the existing PHP API, ensuring:
- No breaking changes for existing clients
- Same request/response formats
- Same validation rules
- Same file type support

## Testing Checklist

- [x] File upload with valid file types (JPEG, PNG, PDF)
- [x] File upload with invalid file types
- [x] File size validation (exceeds 5MB)
- [x] File access by valid ID
- [x] File access by invalid ID
- [x] File serving via static URL
- [x] Multiple file types (logo, payslip, photo)
- [x] Database record creation
- [x] Directory creation on startup
- [x] Filename sanitization
- [x] Path traversal prevention

## Monitoring and Logs

### Log Levels
- `INFO` - Normal operations (file uploads, access requests)
- `WARN` - Validation failures, missing files
- `ERROR` - System errors, storage failures

### Key Log Messages
```
File stored successfully: document.pdf with ID: 123
File upload request: filename=document.pdf, fileType=logo, userId=1
File served successfully: document.pdf
```

## Future Enhancements

1. **Cloud Storage Support**
   - AWS S3 integration
   - Azure Blob Storage
   - Google Cloud Storage

2. **Image Processing**
   - Thumbnail generation
   - Image compression
   - Format conversion

3. **Advanced Security**
   - API key authentication
   - File encryption at rest
   - Virus scanning

4. **Performance**
   - CDN integration
   - Caching layer
   - Async upload processing

5. **Features**
   - File versioning
   - Bulk uploads
   - Direct download links with expiration
   - File preview generation

## Support

For issues or questions:
1. Check logs in `file-service` container
2. Verify database connectivity
3. Ensure upload directory permissions
4. Validate configuration properties

## Conclusion

The file-service is now a fully functional, production-ready microservice that provides:
- **Reliability** - Robust error handling and validation
- **Security** - Comprehensive security measures
- **Scalability** - Docker-ready with proper resource management
- **Maintainability** - Clean code and proper documentation

The service is ready for deployment and can handle all file management needs for the Paykesho platform.

