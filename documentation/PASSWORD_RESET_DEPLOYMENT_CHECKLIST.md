# Password Reset Fix - Deployment Checklist

## Pre-Deployment Verification

### Code Review
- [x] All 12 files modified correctly
- [x] CAPTCHA validation added to all portal controllers
- [x] Idempotency exclusions added to all interceptors
- [x] WebMvc configurations updated
- [x] Documentation created

### Linter Status
✅ **No Critical Errors**
- Minor warnings about missing annotations (pre-existing)
- Minor warnings about unused imports (non-breaking)
- All warnings are non-critical and do not affect functionality

## Deployment Steps

### 1. Build All Modules
```bash
# From project root
mvn clean package -DskipTests

# Or build specific modules:
cd webparent/backoffice && mvn clean package
cd webparent/client-portal && mvn clean package
cd webparent/merchant-portal && mvn clean package
cd webparent/organization-portal && mvn clean package
cd webparent/payment-service && mvn clean package
cd webparent/notification-service && mvn clean package
```

### 2. Backup Current Deployment
```bash
# Backup current JARs
cp deploy/backoffice/backoffice-1.0-SNAPSHOT.jar deploy/backoffice/backoffice-1.0-SNAPSHOT.jar.backup
cp deploy/client-portal/client-portal-1.0-SNAPSHOT.jar deploy/client-portal/client-portal-1.0-SNAPSHOT.jar.backup
cp deploy/merchant-portal/merchant-portal-1.0-SNAPSHOT.jar deploy/merchant-portal/merchant-portal-1.0-SNAPSHOT.jar.backup
cp deploy/organization-portal/organization-portal-1.0-SNAPSHOT.jar deploy/organization-portal/organization-portal-1.0-SNAPSHOT.jar.backup
cp deploy/payment-service/payment-service-1.0-SNAPSHOT.jar deploy/payment-service/payment-service-1.0-SNAPSHOT.jar.backup
cp deploy/notification-service/notification-service-1.0-SNAPSHOT.jar deploy/notification-service/notification-service-1.0-SNAPSHOT.jar.backup
```

### 3. Copy New JARs
```bash
# Copy newly built JARs to deploy directory
cp webparent/backoffice/target/backoffice-1.0-SNAPSHOT.jar deploy/backoffice/
cp webparent/client-portal/target/client-portal-1.0-SNAPSHOT.jar deploy/client-portal/
cp webparent/merchant-portal/target/merchant-portal-1.0-SNAPSHOT.jar deploy/merchant-portal/
cp webparent/organization-portal/target/organization-portal-1.0-SNAPSHOT.jar deploy/organization-portal/
cp webparent/payment-service/target/payment-service-1.0-SNAPSHOT.jar deploy/payment-service/
cp webparent/notification-service/target/notification-service-1.0-SNAPSHOT.jar deploy/notification-service/
```

### 4. Restart Services
```bash
# Using docker-compose
cd deploy
docker-compose down
docker-compose up -d

# Or using the restart script (Windows)
./restart-services.ps1
```

### 5. Verify Services Are Running
```bash
# Check service status
docker-compose ps

# Check logs for each service
docker-compose logs backoffice
docker-compose logs client-portal
docker-compose logs merchant-portal
docker-compose logs organization-portal
docker-compose logs payment-service
docker-compose logs notification-service
```

## Post-Deployment Testing

### Test Matrix

| Portal | Test | Expected Result | Status |
|--------|------|----------------|--------|
| **Backoffice** | Navigate to login page | Login page loads | ⬜ |
| | Click "Forgot password?" | Modal opens with CAPTCHA | ⬜ |
| | Submit without CAPTCHA | Error: "Please solve the security challenge" | ⬜ |
| | Submit with wrong CAPTCHA | Error: "Security challenge verification failed" | ⬜ |
| | Submit with correct CAPTCHA | Success message shown | ⬜ |
| | Check email | Password reset email received | ⬜ |
| **Client Portal** | Navigate to login page | Login page loads | ⬜ |
| | Click "Forgot password?" | Modal opens with CAPTCHA | ⬜ |
| | Submit without CAPTCHA | Error: "Please solve the security challenge" | ⬜ |
| | Submit with wrong CAPTCHA | Error: "Security challenge verification failed" | ⬜ |
| | Submit with correct CAPTCHA | Success message shown | ⬜ |
| | Check email | Password reset email received | ⬜ |
| **Merchant Portal** | Navigate to login page | Login page loads | ⬜ |
| | Click "Forgot password?" | Modal opens with CAPTCHA | ⬜ |
| | Submit without CAPTCHA | Error: "Please solve the security challenge" | ⬜ |
| | Submit with wrong CAPTCHA | Error: "Security challenge verification failed" | ⬜ |
| | Submit with correct CAPTCHA | Success message shown | ⬜ |
| | Check email | Password reset email received | ⬜ |
| **Organization Portal** | Navigate to login page | Login page loads | ⬜ |
| | Click "Forgot password?" | Modal opens with CAPTCHA | ⬜ |
| | Submit without CAPTCHA | Error: "Please solve the security challenge" | ⬜ |
| | Submit with wrong CAPTCHA | Error: "Security challenge verification failed" | ⬜ |
| | Submit with correct CAPTCHA | Success message shown | ⬜ |
| | Check email | Password reset email received | ⬜ |

### Database Verification

```sql
-- Check that no new HTML responses are being stored in idempotency records
SELECT 
    id,
    endpoint,
    http_method,
    LENGTH(response_body) as response_size,
    status,
    created_at
FROM idempotency_records
WHERE endpoint LIKE '%forgot-password%'
ORDER BY created_at DESC
LIMIT 10;

-- Expected: No new records after deployment (or only small JSON responses, not HTML)
```

### Log Verification

Check for these log messages:
```
✅ "CAPTCHA fields are empty for session" - Validation working
✅ "CAPTCHA verification failed for session" - Verification working
✅ "Generated CAPTCHA challenge for session" - CAPTCHA generation working
✅ "Password reset email sent to existing user" - Email sending working
❌ "Marked idempotency record as completed" for /forgot-password - Should NOT appear
```

## Performance Monitoring

### Metrics to Watch

1. **Database**
   - Idempotency table size should decrease over time
   - No large HTML responses being stored

2. **Application Logs**
   - No errors related to CAPTCHA loading
   - No errors related to password reset flow

3. **User Experience**
   - CAPTCHA loads within 1 second
   - Form submission completes within 2 seconds
   - Email delivery within 1 minute

## Rollback Procedure

If critical issues are found:

```bash
# Stop services
docker-compose down

# Restore backup JARs
cp deploy/backoffice/backoffice-1.0-SNAPSHOT.jar.backup deploy/backoffice/backoffice-1.0-SNAPSHOT.jar
cp deploy/client-portal/client-portal-1.0-SNAPSHOT.jar.backup deploy/client-portal/client-portal-1.0-SNAPSHOT.jar
cp deploy/merchant-portal/merchant-portal-1.0-SNAPSHOT.jar.backup deploy/merchant-portal/merchant-portal-1.0-SNAPSHOT.jar
cp deploy/organization-portal/organization-portal-1.0-SNAPSHOT.jar.backup deploy/organization-portal/organization-portal-1.0-SNAPSHOT.jar
cp deploy/payment-service/payment-service-1.0-SNAPSHOT.jar.backup deploy/payment-service/payment-service-1.0-SNAPSHOT.jar
cp deploy/notification-service/notification-service-1.0-SNAPSHOT.jar.backup deploy/notification-service/notification-service-1.0-SNAPSHOT.jar

# Restart services
docker-compose up -d
```

## Success Criteria

- [x] All 12 files successfully modified
- [ ] All services build without errors
- [ ] All services start without errors
- [ ] All portal password reset forms working
- [ ] CAPTCHA validation working correctly
- [ ] No HTML responses in idempotency table
- [ ] All emails being sent successfully
- [ ] No performance degradation
- [ ] No new errors in logs

## Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| QA Lead | | | |
| DevOps | | | |
| Product Owner | | | |

## Notes

_Add any deployment notes or observations here_

---

**Fix Version:** 1.0  
**Deployment Date:** ___________  
**Deployment By:** ___________  

