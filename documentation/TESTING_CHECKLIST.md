# Testing Checklist After JAR Optimization

## ✅ SAFE - No Breaking Changes Expected

### 1. AWS S3 SDK Removal
**Status**: ✅ **SAFE**
- **Finding**: Only found in commented-out code (APIController.java lines 260-261)
- **No actual imports or usage** anywhere in the codebase
- **No AmazonS3Util class** exists
- **No S3 configuration** in properties/yml files

### 2. OpenCV Removal
**Status**: ✅ **SAFE**
- **Finding**: Zero usage found in any Java files
- **No imports** of opencv/openpnp packages
- **No image processing** code using OpenCV
- All "opencv" grep matches were false positives (word "format", etc.)

### 3. File Upload System
**Status**: ✅ **WORKING** (Uses external file service API)
- Uses REST API to `https://files.paykesho.com/uploads-api`
- Not dependent on AWS S3
- Files uploaded via `FileUploadService` to remote service
- Local temp files cleaned up after upload

### 4. Apache POI (Excel Export)
**Status**: ✅ **WORKING** (Still in backoffice POM)
- Used in: `UserExcelExporter.java` (backoffice only)
- Still declared in `backoffice/pom.xml`
- No changes made to POI dependencies

### 5. OpenPDF (PDF Export)
**Status**: ✅ **WORKING** (Still in backoffice POM)
- Used in: `UserPDFExporter.java` (backoffice only)
- Still declared in `backoffice/pom.xml`
- No changes made to OpenPDF dependencies

## 🧪 Testing Recommendations

### Priority 1: Critical Features (Must Test)

#### 1.1 File Uploads
- [ ] **Backoffice**: Upload user photo
- [ ] **Backoffice**: Upload organization logo/documents
- [ ] **Backoffice**: Upload merchant logo/documents
- [ ] **Client Portal**: Upload voucher payslip (if active)
- [ ] **API**: Test secure file upload endpoint `/api/files/upload`

**Test locations**:
```
webparent/backoffice/src/main/java/org/paykesho/backoffice/files/controller/SecureFileUploadController.java
webparent/backoffice/src/main/java/org/paykesho/backoffice/files/service/FileUploadService.java
```

#### 1.2 Export Functions (Backoffice)
- [ ] **Excel Export**: Export users to Excel
- [ ] **PDF Export**: Export users to PDF
- [ ] **CSV Export**: Export users to CSV (if available)

**Test locations**:
```
webparent/backoffice/src/main/java/org/paykesho/backoffice/user/fileHandlers/UserExcelExporter.java
webparent/backoffice/src/main/java/org/paykesho/backoffice/user/fileHandlers/UserPDFExporter.java
```

#### 1.3 Basic Application Startup
- [ ] **Backoffice**: Starts successfully on configured port
- [ ] **Client Portal**: Starts successfully on configured port
- [ ] **Merchant Portal**: Starts successfully on configured port
- [ ] **Organization Portal**: Starts successfully on configured port
- [ ] **Payment Service**: Starts successfully on configured port
- [ ] **Notification Service**: Starts successfully on configured port
- [ ] **File Service**: Starts successfully on configured port

### Priority 2: Functionality Testing

#### 2.1 User Management (Backoffice)
- [ ] Create new user
- [ ] Edit user details
- [ ] Upload user photo
- [ ] Delete user
- [ ] Export users list

#### 2.2 Organization Management
- [ ] Create organization
- [ ] Upload organization logo
- [ ] View organization details
- [ ] Organization GPS tracking (if used)

#### 2.3 Merchant Management
- [ ] Create merchant
- [ ] Upload merchant logo/photos
- [ ] View merchant details

#### 2.4 Client Features
- [ ] Client registration
- [ ] Client login
- [ ] Voucher request
- [ ] View transactions

### Priority 3: Integration Testing

#### 3.1 Email Notifications
- [ ] Password reset emails
- [ ] Verification emails
- [ ] Transaction notifications
- [ ] Voucher notifications

#### 3.2 Payment Processing
- [ ] MoMo merchant payments
- [ ] Card payments
- [ ] Payment settlements

#### 3.3 File Access
- [ ] View uploaded images/documents
- [ ] Download files
- [ ] File access permissions

## 🔍 What Changed in POMs

### Parent POM (`webparent/pom.xml`)
```xml
REMOVED:
- OpenCV dependency (72 MB)

CHANGED:
- spring-boot-devtools marked as <optional>true</optional>
```

### All Portal Modules
```xml
REMOVED:
- AWS S3 SDK dependencies
- AWS BOM dependency management

CHANGED:
- spring-boot-devtools marked as <optional>true</optional>
```

## ⚠️ Potential Issues (Low Risk)

### 1. Development Tools
**Issue**: `spring-boot-devtools` now marked as optional
**Impact**: Should not affect production builds, but verify hot-reload in development
**Test**: Start app in dev mode and verify auto-reload works

### 2. Transitive Dependencies
**Issue**: Removing AWS SDK might remove shared transitive dependencies
**Impact**: Very unlikely - AWS SDK dependencies are isolated
**Test**: Check application logs for any ClassNotFoundException

### 3. Classpath Changes
**Issue**: Smaller JARs might affect classpath scanning
**Impact**: Highly unlikely - only removed unused libraries
**Test**: Verify all Spring beans initialize correctly

## 📝 Testing Script

### Quick Smoke Test
```bash
# 1. Clean build
mvn clean install

# 2. Check JAR sizes (should be ~50-60% smaller)
dir deploy\backoffice\backoffice-1.0-SNAPSHOT.jar
dir deploy\client-portal\client-portal-1.0-SNAPSHOT.jar
dir deploy\payment-service\payment-service-1.0-SNAPSHOT.jar

# 3. Start each service
cd deploy
docker-compose up -d

# 4. Check logs for errors
docker-compose logs -f backoffice
docker-compose logs -f client-portal
# ... etc
```

### Manual Test Checklist
1. ✅ Login to Backoffice
2. ✅ Create a test user
3. ✅ Upload user photo
4. ✅ Export users to Excel
5. ✅ Export users to PDF
6. ✅ Upload organization logo
7. ✅ Upload merchant document
8. ✅ Test client portal login
9. ✅ Test password reset flow
10. ✅ Test file download/view

## 🎯 Success Criteria

### Must Pass:
- ✅ All services start without errors
- ✅ No ClassNotFoundException in logs
- ✅ File uploads work correctly
- ✅ Excel/PDF exports work correctly
- ✅ User authentication works
- ✅ Basic CRUD operations work

### Nice to Have:
- ✅ Application startup is faster (smaller JARs)
- ✅ Memory usage is slightly lower
- ✅ Deployment is faster (smaller artifact transfer)

## 🐛 If Issues Found

### Symptoms & Solutions

**Symptom**: `ClassNotFoundException` for AWS classes
**Solution**: Check if any Commons module uses AWS (unlikely based on search)

**Symptom**: File upload fails
**Solution**: Verify file-service is running and accessible at `https://files.paykesho.com`

**Symptom**: Excel export fails
**Solution**: Verify Apache POI is in backoffice JAR: `jar -tf backoffice.jar | findstr poi`

**Symptom**: PDF export fails
**Solution**: Verify OpenPDF is in backoffice JAR: `jar -tf backoffice.jar | findstr openpdf`

**Symptom**: Application won't start
**Solution**: Check logs for dependency conflicts, verify Maven build completed successfully

## 📊 Expected Results

### Before Optimization:
- backoffice: 167 MB
- client-portal: 148 MB
- payment-service: 130 MB

### After Optimization (Expected):
- backoffice: ~87 MB (**-80 MB, 48% reduction**)
- client-portal: ~68 MB (**-80 MB, 54% reduction**)
- payment-service: ~50 MB (**-80 MB, 62% reduction**)

## ✅ Conclusion

**Risk Level**: 🟢 **LOW**

All removed dependencies (OpenCV, AWS S3 SDK) were confirmed unused. The only actively used large libraries (Apache POI, OpenPDF) remain in the backoffice module where they're needed.

**Recommended Testing Time**: 30-60 minutes
**Critical Tests**: File uploads, Excel/PDF exports, Application startup
**Rollback Plan**: Revert POM changes via git if issues found

