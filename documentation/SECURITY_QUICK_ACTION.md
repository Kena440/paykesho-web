# Security Quick Action Guide

## 🚨 IMMEDIATE ACTIONS REQUIRED

You have **malicious data** in your database. Follow these steps immediately:

---

## Step 1: Backup Your Database (5 minutes)

```bash
# Linux/Mac
mysqldump -u root -p paykesho_db > paykesho_backup_$(date +%Y%m%d_%H%M%S).sql

# Windows
mysqldump -u root -p paykesho_db > paykesho_backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%.sql
```

---

## Step 2: Identify Malicious Records (2 minutes)

```sql
-- Connect to database
mysql -u root -p paykesho_db

-- View the malicious records
SELECT id, name, email, address, nrc, created_time 
FROM clients 
WHERE id IN (4, 6);
```

**Expected Output:**
- ID 4: Spam with URL in address field (`https://buyviagraonlinet.com/`)
- ID 6: XSS attack with `<script>` tag in name field

---

## Step 3: Delete Malicious Records (1 minute)

```sql
-- Delete the two malicious records
DELETE FROM clients WHERE id = 4;  -- Spam record
DELETE FROM clients WHERE id = 6;  -- XSS attack

-- Verify deletion
SELECT COUNT(*) FROM clients WHERE id IN (4, 6);
-- Should return 0
```

---

## Step 4: Run Full Security Scan (5 minutes)

```bash
# Run the comprehensive cleanup script
mysql -u root -p paykesho_db < migrations/scripts/cleanup_malicious_data.sql
```

This will:
- Scan for all XSS patterns
- Find URLs in address fields
- Identify invalid NRC formats
- Detect HTML tags in data
- Show suspicious email patterns

---

## Step 5: Rebuild Application (10 minutes)

The security fixes are already implemented in the code. Just rebuild:

```bash
# Navigate to project root
cd D:\KAEDIGITAL\IdeaProjects\paykesho-web

# Build the project (Maven will be invoked automatically)
# The new validation rules will be compiled
```

---

## Step 6: Restart Services (5 minutes)

```bash
# Using Docker (if deployed)
cd deploy
docker-compose restart

# Or restart individual services
docker-compose restart backoffice
docker-compose restart client-portal
docker-compose restart organization-portal
```

---

## What Has Been Fixed

### ✅ Input Validation
- All client fields now have strict validation rules
- Invalid data will be rejected automatically

### ✅ XSS Protection
- HTML tags are blocked
- Script tags are detected and removed
- JavaScript protocols are sanitized

### ✅ SQL Injection Protection
- Input patterns are validated
- Dangerous SQL keywords are detected
- All queries use parameterized statements

### ✅ Field-Specific Validation
- **Name**: Letters, spaces, hyphens, apostrophes only
- **Email**: Valid email format required
- **Address**: No URLs allowed, alphanumeric only
- **NRC**: Must match format `XXXXXX/XX/X`
- **TPIN**: Exactly 10 digits
- **Phone**: Valid phone number format

---

## Verification Tests

### Test 1: Try to Register with XSS

**Input:**
```
Name: <script>alert('test')</script>John
```

**Expected Result:**
❌ Rejected with error message: "Name can only contain letters, spaces, hyphens and apostrophes"

### Test 2: Try to Register with URL in Address

**Input:**
```
Address: https://spam.com
```

**Expected Result:**
❌ Rejected with error message: "Address contains invalid characters"

### Test 3: Try to Register with Invalid NRC

**Input:**
```
NRC: MiguelHah
```

**Expected Result:**
❌ Rejected with error message: "NRC must be in format XXXXXX/XX/X"

### Test 4: Try to Register with Valid Data

**Input:**
```
Name: John Doe
Email: john@example.com
Address: Plot 123, Main Street, Lusaka
NRC: 364205/10/1
TPIN: 1003060012
Phone: 0971684296
```

**Expected Result:**
✅ Registration successful

---

## Monitoring

### Check Logs Daily

```bash
# Search for security alerts
grep -r "SECURITY ALERT" webparent/*/logs/
grep -r "SECURITY VIOLATION" webparent/*/logs/

# Windows PowerShell
Select-String -Path "webparent\*\logs\*" -Pattern "SECURITY"
```

### Check Database Weekly

```sql
-- Run this query weekly to find suspicious data
SELECT id, name, email, address, created_time 
FROM clients 
WHERE name REGEXP '<[^>]*>|javascript:'
   OR address REGEXP 'https?://'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$'
ORDER BY created_time DESC;
```

---

## Security Checklist

- [x] Input validation added to Client entity
- [x] Input sanitizer created
- [x] Security validator implemented
- [x] Controllers updated with @Valid annotation
- [x] Service layer validates all inputs
- [x] Malicious data cleanup script created
- [x] Security documentation created
- [ ] Database backed up ← **DO THIS NOW**
- [ ] Malicious records deleted ← **DO THIS NOW**
- [ ] Application rebuilt ← **DO THIS NOW**
- [ ] Services restarted ← **DO THIS NOW**

---

## Files Created/Modified

### New Security Files
1. `commons/src/main/java/org/commons/security/InputSanitizer.java`
2. `commons/src/main/java/org/commons/security/SecurityValidator.java`
3. `migrations/scripts/cleanup_malicious_data.sql`
4. `documentation/SECURITY_GUIDE.md`
5. `documentation/SECURITY_QUICK_ACTION.md` (this file)

### Modified Files
1. `commons/src/main/java/org/commons/entity/client/Client.java` - Added validation annotations
2. `webparent/client-portal/src/main/java/org/paykesho/clientportal/clients/service/ClientService.java` - Added security validation
3. `webparent/backoffice/src/main/java/org/paykesho/backoffice/client/service/ClientService.java` - Added security validation
4. `webparent/client-portal/src/main/java/org/paykesho/clientportal/clients/controller/ClientController.java` - Added @Valid
5. `webparent/backoffice/src/main/java/org/paykesho/backoffice/client/controller/ClientController.java` - Added @Valid

---

## Need Help?

1. **Read the full guide:** `documentation/SECURITY_GUIDE.md`
2. **Check logs:** `webparent/*/logs/application.log`
3. **Review validation rules:** `commons/src/main/java/org/commons/entity/client/Client.java`
4. **Test sanitization:** `commons/src/main/java/org/commons/security/InputSanitizer.java`

---

## Summary

**Threat Level:** 🔴 HIGH (XSS attack detected)

**Action Required:** 🚨 IMMEDIATE

**Estimated Time:** ⏱️ 30 minutes

**Priority:** 1. Backup → 2. Delete malicious data → 3. Rebuild → 4. Restart

---

*Your system is now protected, but you must complete the cleanup steps above!*

