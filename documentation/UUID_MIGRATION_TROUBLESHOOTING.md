# UUID Database Migration - Troubleshooting Guide

## 🚨 Error 1175: Safe Update Mode

### Error Message:
```
Error Code: 1175. You are using safe update mode and you tried to update a table 
without a WHERE that uses a KEY column. To disable safe mode, toggle the option 
in Preferences -> SQL Editor and reconnect.
```

### What This Means:
MySQL Workbench's "safe update mode" prevents UPDATE statements that don't use a key column in the WHERE clause. This is a safety feature to prevent accidentally updating all rows.

---

## ✅ **SOLUTION: Use the Safe Mode Compatible Script**

### Option 1: Use the Safe Mode Script (Recommended)
```bash
# Use this script - it handles safe mode automatically
mysql -u username -p database_name < documentation/add_uuid_columns_safe_mode.sql
```

**What this script does:**
1. ✅ Temporarily disables safe mode
2. ✅ Runs all updates with `WHERE id > 0` (uses primary key)
3. ✅ Re-enables safe mode when done
4. ✅ Safe and production-ready

### Option 2: Disable Safe Mode in MySQL Workbench
If you're running directly in MySQL Workbench:

**Steps:**
1. Edit → Preferences (or MySQL Workbench → Preferences on Mac)
2. SQL Editor tab
3. Uncheck "Safe Updates" (rejects UPDATEs and DELETEs with no restrictions)
4. Click OK
5. Close and reconnect to database
6. Run the script

**Then re-enable it after migration:**
1. Edit → Preferences
2. SQL Editor tab
3. Check "Safe Updates"
4. Click OK

### Option 3: Disable Safe Mode in Session
Add this at the beginning of your script:
```sql
SET SQL_SAFE_UPDATES = 0;
-- ... your migration statements ...
SET SQL_SAFE_UPDATES = 1;
```

---

## 📋 **Which Script to Use?**

| Script | Safe Mode Compatible | Idempotent | Recommended |
|--------|---------------------|------------|-------------|
| `add_uuid_columns.sql` | ❌ No | ❌ No | ❌ No |
| `add_uuid_columns_safe.sql` | ❌ No | ✅ Yes | 🟡 If safe mode disabled |
| `add_uuid_columns_safe_mode.sql` | ✅ Yes | ✅ Yes | ✅ **YES - Use This!** |

---

## 🔍 **Understanding the Fix**

### Original UPDATE (Causes Error):
```sql
-- This fails in safe mode
UPDATE users SET uuid = UUID() WHERE uuid IS NULL;
```
**Problem**: `WHERE uuid IS NULL` doesn't use a KEY column

### Fixed UPDATE (Works in Safe Mode):
```sql
-- This works!
UPDATE users SET uuid = UUID() WHERE uuid IS NULL AND id > 0;
```
**Solution**: Added `AND id > 0` which uses the primary KEY column

---

## ✅ **Verification After Migration**

### Check UUIDs Were Generated:
```sql
-- Should return 0 (no NULL UUIDs)
SELECT COUNT(*) FROM users WHERE uuid IS NULL;
SELECT COUNT(*) FROM merchants WHERE uuid IS NULL;
SELECT COUNT(*) FROM clients WHERE uuid IS NULL;
```

### Check UUID Format:
```sql
-- Sample UUIDs to verify format
SELECT id, uuid, email FROM users LIMIT 5;
SELECT id, uuid, name FROM merchants LIMIT 5;
```

**Expected Output:**
```
+----+--------------------------------------+------------------+
| id | uuid                                 | email            |
+----+--------------------------------------+------------------+
|  1 | 550e8400-e29b-41d4-a716-446655440000 | admin@email.com  |
|  2 | 7c9e6679-7425-40de-944b-e07fc1f90ae7 | user@email.com   |
+----+--------------------------------------+------------------+
```

### Check Indexes Created:
```sql
-- Should show uuid indexes
SHOW INDEX FROM users WHERE Key_name LIKE '%uuid%';
SHOW INDEX FROM merchants WHERE Key_name LIKE '%uuid%';
```

---

## 🛠️ **Other Common Errors**

### Error: "Duplicate column name 'uuid'"
**Solution**: Some tables already have the UUID column. Use `add_uuid_columns_safe_mode.sql` - it checks before adding.

### Error: "Table 'xyz' doesn't exist"
**Solution**: That table doesn't exist in your database. Comment out those lines in the script.

### Error: "Data truncated for column 'uuid'"
**Solution**: UUID format issue. Ensure MySQL's `UUID()` function is available.

### Warning: "1287 - Setting user variables within expressions is deprecated"
**Solution**: This is just a warning in MySQL 8.0+. The script still works fine.

---

## 📊 **Progress Tracking**

### While Migration is Running:
Open another terminal/connection and run:

```sql
-- Check how many tables have uuid column
SELECT COUNT(DISTINCT TABLE_NAME) as tables_with_uuid
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'uuid';

-- Should increase from 0 to 30 as migration progresses
```

### Monitor Specific Table:
```sql
-- Check if users table has UUIDs
SELECT 
    COUNT(*) as total_rows,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) as has_uuids
FROM users;
```

---

## 🎯 **Quick Commands**

### Check Safe Mode Status:
```sql
SHOW VARIABLES LIKE 'sql_safe_updates';
```

### Temporarily Disable Safe Mode:
```sql
SET SQL_SAFE_UPDATES = 0;
```

### Re-enable Safe Mode:
```sql
SET SQL_SAFE_UPDATES = 1;
```

### Run Migration (Command Line):
```bash
# Best method - bypasses Workbench safe mode
mysql -u username -p database_name < documentation/add_uuid_columns_safe_mode.sql
```

---

## ✅ **Recommended Approach**

### Step 1: Backup Database
```bash
mysqldump -u username -p database_name > backup_before_uuid.sql
```

### Step 2: Run Safe Mode Compatible Script
```bash
# This handles safe mode automatically
mysql -u username -p database_name < documentation/add_uuid_columns_safe_mode.sql
```

### Step 3: Verify Success
```bash
# Check all tables have UUIDs
mysql -u username -p database_name < documentation/check_uuid_columns.sql
```

### Step 4: Test Application
- Start application
- Verify UUID-based URLs work
- Check logs for any errors

---

## 🆘 **If Migration Fails Midway**

### Safe to Rerun:
The script is **idempotent** - safe to run multiple times:
- ✅ Checks if column exists before adding
- ✅ Only updates NULL UUIDs (preserves existing)
- ✅ Won't duplicate indexes
- ✅ Safe to rerun if interrupted

### Rollback (if needed):
```sql
-- Only if you need to start over
ALTER TABLE users DROP COLUMN uuid;
ALTER TABLE merchants DROP COLUMN uuid;
-- ... etc for all tables

-- Then run migration again
```

---

## 📞 **Need Help?**

### Check These First:
1. **Is safe mode enabled?**
   ```sql
   SHOW VARIABLES LIKE 'sql_safe_updates';
   ```

2. **Does table exist?**
   ```sql
   SHOW TABLES LIKE 'table_name';
   ```

3. **Does column already exist?**
   ```sql
   SHOW COLUMNS FROM users LIKE 'uuid';
   ```

### Solutions:
- ✅ Use `add_uuid_columns_safe_mode.sql` (handles all edge cases)
- ✅ Run from command line (bypasses Workbench restrictions)
- ✅ Check `check_uuid_columns.sql` for current state

---

## ✅ **Success Criteria**

After successful migration:
- [x] All 30 tables have `uuid` column
- [x] All existing records have UUIDs (no NULLs)
- [x] UUID columns are NOT NULL
- [x] All indexes created
- [x] Safe mode re-enabled
- [x] Application starts without errors
- [x] UUID-based URLs work

---

## 🎯 **Quick Fix Commands**

### If stuck with safe mode error:
```bash
# Just use this script - it handles everything:
mysql -u username -p database_name < documentation/add_uuid_columns_safe_mode.sql
```

**This script:**
- ✅ Disables safe mode automatically
- ✅ Runs all updates with proper WHERE clauses
- ✅ Re-enables safe mode when done
- ✅ Handles edge cases
- ✅ Provides success message

---

**Problem solved! Use `add_uuid_columns_safe_mode.sql` for hassle-free migration!** ✅

