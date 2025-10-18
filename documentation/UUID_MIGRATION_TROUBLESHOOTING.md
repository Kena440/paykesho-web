# 🚨 UUID Migration Troubleshooting Guide

**Issue**: Merchant view URLs showing as `/merchants/view/` (without UUID) and returning 404 errors.

**Root Cause**: Database migration not yet executed - UUID columns don't exist in database.

---

## 🔍 **Diagnosis Steps**

### 1. Check Database Schema

Run this SQL query to check if UUID columns exist:

```sql
-- Check if UUID columns exist in merchants table
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'merchants'
AND COLUMN_NAME = 'uuid';
```

**Expected Result**:
- If UUID column exists: You'll see a row with `uuid` column
- If UUID column doesn't exist: Empty result set

### 2. Check Existing Data

```sql
-- Check if merchants have UUID values
SELECT id, uuid, name FROM merchants LIMIT 5;
```

**Expected Results**:
- If migration run: UUID values like `550e8400-e29b-41d4-a716-446655440000`
- If migration not run: `uuid` column doesn't exist (error) or all NULL values

---

## ✅ **Solution: Run Database Migration**

### Step 1: Backup Database (CRITICAL)

```bash
# Create backup before migration
mysqldump -u username -p database_name > backup_pre_uuid_$(date +%Y%m%d).sql
```

### Step 2: Run UUID Migration

```bash
# Navigate to project directory
cd D:\KAEDIGITAL\IdeaProjects\paykesho-web

# Run the safe migration script
mysql -u username -p database_name < migrations/scripts/add_uuid_columns_safe.sql
```

### Step 3: Verify Migration

```sql
-- Verify UUID columns were added
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND COLUMN_NAME = 'uuid'
ORDER BY TABLE_NAME;

-- Check UUID generation for merchants
SELECT id, uuid, name FROM merchants LIMIT 5;
```

**Expected Results**:
- All tables should have `uuid` column
- All existing records should have UUID values generated

### Step 4: Test Application

1. Restart your application server
2. Go to `/merchants` page
3. Hover over merchant names - should show URLs like `/merchants/view/550e8400-e29b-41d4-a716-446655440000`
4. Click on merchant - should work without 404 error

---

## 🔧 **Alternative: Quick Fix for Testing**

If you need to test immediately without full migration:

### Option 1: Add UUID to One Table Only

```sql
-- Add UUID column to merchants table only (for testing)
ALTER TABLE merchants ADD COLUMN uuid VARCHAR(36) UNIQUE;
UPDATE merchants SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE merchants MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
CREATE INDEX idx_merchants_uuid ON merchants(uuid);
```

### Option 2: Manual UUID Assignment

```sql
-- Manually assign UUIDs to existing merchants
UPDATE merchants SET uuid = '550e8400-e29b-41d4-a716-446655440000' WHERE id = 1;
UPDATE merchants SET uuid = '7c9e6679-7425-40de-944b-e07fc1f90ae7' WHERE id = 2;
-- ... continue for all merchants
```

---

## 🚨 **Common Issues & Solutions**

### Issue 1: "Column 'uuid' doesn't exist"

**Cause**: Migration not run

**Solution**: Run the migration script

### Issue 2: "Duplicate entry for key 'uuid'"

**Cause**: UUID generation created duplicates

**Solution**:
```sql
-- Regenerate UUIDs to fix duplicates
UPDATE merchants SET uuid = UUID();
```

### Issue 3: "Data truncated for column 'uuid'"

**Cause**: UUID format issue

**Solution**:
```sql
-- Ensure UUID column is VARCHAR(36)
ALTER TABLE merchants MODIFY COLUMN uuid VARCHAR(36);
```

### Issue 4: Still getting 404 after migration

**Cause**: Application cache or restart needed

**Solution**:
1. Restart application server
2. Clear browser cache
3. Check application logs for errors

---

## 📋 **Migration Checklist**

### Before Migration:
- [ ] Database backup created
- [ ] Application stopped (optional but recommended)
- [ ] Migration script reviewed

### During Migration:
- [ ] Migration script executed successfully
- [ ] No errors in MySQL output
- [ ] UUID columns created
- [ ] UUID values populated for existing records

### After Migration:
- [ ] Application restarted
- [ ] Database queries tested
- [ ] Frontend URLs working
- [ ] No 404 errors on merchant view

---

## 🔍 **Verification Commands**

### Check Migration Status:
```sql
-- Count tables with UUID columns
SELECT COUNT(*) as tables_with_uuid
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND COLUMN_NAME = 'uuid';
```

### Check UUID Generation:
```sql
-- Sample UUID values
SELECT id, uuid, name FROM merchants LIMIT 3;
```

### Test Application:
```bash
# Check if application starts without errors
tail -f application.log

# Test a specific merchant URL (replace with actual UUID)
curl http://localhost:8080/merchants/view/550e8400-e29b-41d4-a716-446655440000
```

---

## 📞 **Support**

If issues persist after migration:

1. **Check Application Logs**: Look for UUID-related errors
2. **Verify Database Connection**: Ensure application can connect to updated database
3. **Clear Cache**: Clear any application or browser cache
4. **Restart Services**: Restart database and application services

---

## 🎯 **Expected Results After Fix**

### Before Fix:
```
URL: /merchants/view/
Error: 404 Not Found
```

### After Fix:
```
URL: /merchants/view/550e8400-e29b-41d4-a716-446655440000
Result: Merchant details page loads successfully
```

---

**Last Updated**: October 18, 2025  
**Status**: Ready for immediate execution