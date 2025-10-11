# UUID Database Migration - How To Guide

## 🚨 Error: "Duplicate column name 'uuid'"

If you got this error, it means some tables already have the UUID column. This is fixable! Follow the steps below.

---

## ✅ Solution: Use the Safe Migration Script

I've created **3 scripts** for you:

### 1. **`check_uuid_columns.sql`** - Diagnostic Script
Run this FIRST to see which tables already have UUID columns.

```sql
-- Shows which tables have uuid column
-- Shows how many NULL vs populated UUIDs
```

**How to run:**
```bash
mysql -u username -p database_name < check_uuid_columns.sql
```

### 2. **`add_uuid_columns_safe.sql`** - SAFE Migration Script ✅
This is the **IDEMPOTENT** version that:
- ✅ Checks if column exists before adding
- ✅ Only updates NULL UUIDs (preserves existing ones)
- ✅ Safe to run multiple times
- ✅ Won't error on duplicate columns

**How to run:**
```bash
mysql -u username -p database_name < add_uuid_columns_safe.sql
```

### 3. **`add_uuid_columns.sql`** - Original Script
The original simple script. **Don't use this if you've already run it once.**

---

## 📋 Step-by-Step Instructions

### Step 1: Check Current State
```bash
# See which tables already have UUID columns
mysql -u username -p database_name < check_uuid_columns.sql
```

**Expected Output:**
```
+-------------------+-------------+-------------+-------------+------------+
| TABLE_NAME        | COLUMN_NAME | COLUMN_TYPE | IS_NULLABLE | COLUMN_KEY |
+-------------------+-------------+-------------+-------------+------------+
| users             | uuid        | varchar(36) | YES         | UNI        |
| merchants         | uuid        | varchar(36) | YES         | UNI        |
+-------------------+-------------+-------------+-------------+------------+
```

### Step 2: Run Safe Migration
```bash
# This is safe to run even if some tables have uuid column
mysql -u username -p database_name < add_uuid_columns_safe.sql
```

**What this does:**
1. ✅ Checks each table for existing `uuid` column
2. ✅ Only adds column if it doesn't exist
3. ✅ Generates UUIDs for records that don't have one
4. ✅ Makes columns NOT NULL
5. ✅ Creates indexes (if not exist)

### Step 3: Verify Success
Run the check script again to confirm:
```bash
mysql -u username -p database_name < check_uuid_columns.sql
```

**Expected:**
- All 30 tables should have `uuid` column
- All records should have non-NULL UUIDs
- Indexes created

---

## 🔧 How the Safe Script Works

### Phase 1: Check Before Adding Column
```sql
-- Check if column exists
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'uuid');

-- Only add if doesn't exist
SET @sqlstmt := IF(@exist > 0, 
    'SELECT ''Column uuid already exists''',  -- Skip if exists
    'ALTER TABLE users ADD COLUMN uuid VARCHAR(36) UNIQUE');  -- Add if not
    
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
```

### Phase 2: Only Update NULL UUIDs
```sql
-- This only generates UUIDs for records that don't have one
-- Preserves existing UUIDs!
UPDATE users SET uuid = UUID() WHERE uuid IS NULL;
```

### Phase 3: Make NOT NULL
```sql
-- Only runs if all records have UUIDs
ALTER TABLE users MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
```

### Phase 4: Create Indexes Safely
```sql
-- IF NOT EXISTS prevents errors
CREATE INDEX IF NOT EXISTS idx_users_uuid ON users(uuid);
```

---

## 🔍 Troubleshooting

### Problem: Some tables have UUID, some don't
**Solution:** Run `add_uuid_columns_safe.sql` - it handles mixed states

### Problem: Some records have NULL UUIDs
**Solution:** The safe script automatically generates UUIDs for NULL records

### Problem: Want to start fresh
**Solution:** Drop UUID columns first, then run migration

```sql
-- ⚠️ WARNING: This removes all UUIDs! Backup first!
ALTER TABLE users DROP COLUMN uuid;
ALTER TABLE merchants DROP COLUMN uuid;
-- ... repeat for all tables

-- Then run the safe migration script
```

### Problem: Want to see which records are missing UUIDs
```sql
SELECT COUNT(*) FROM users WHERE uuid IS NULL;
SELECT COUNT(*) FROM merchants WHERE uuid IS NULL;
-- etc.
```

---

## 📊 Expected Results

After successful migration:

| Table | Has UUID Column | All Records Have UUID | Index Created |
|-------|----------------|---------------------|---------------|
| users | ✅ | ✅ | ✅ |
| merchants | ✅ | ✅ | ✅ |
| organizations | ✅ | ✅ | ✅ |
| clients | ✅ | ✅ | ✅ |
| vouchers | ✅ | ✅ | ✅ |
| transactions | ✅ | ✅ | ✅ |
| settlements | ✅ | ✅ | ✅ |
| ... | ✅ | ✅ | ✅ |
| **Total: 30 tables** | ✅ | ✅ | ✅ |

---

## 🎯 How UUIDs Are Generated

### For New Records (Automatic):
```java
// In your Java entity (IdBasedEntity)
@PrePersist
public void generateUuid() {
    if (this.uuid == null) {
        this.uuid = UUID.randomUUID().toString();
    }
}
```
✅ Every new record automatically gets a UUID when saved

### For Existing Records (Database Script):
```sql
UPDATE users SET uuid = UUID() WHERE uuid IS NULL;
```
✅ MySQL's `UUID()` function generates unique IDs for existing records

### UUID Format:
```
550e8400-e29b-41d4-a716-446655440000
```
- 36 characters (including hyphens)
- Globally unique
- Random (version 4 UUID)
- Impossible to guess or enumerate

---

## ⚠️ Important Notes

### 1. UUIDs Are Generated Once
Once a record has a UUID, it **never changes**. The UUID is:
- ✅ Set once on creation
- ✅ Immutable (cannot be updated)
- ✅ Unique across all records
- ✅ Globally unique (safe for distributed systems)

### 2. Existing IDs Stay the Same
```sql
-- Integer IDs don't change
id = 1, uuid = "550e8400-e29b-41d4-a716-446655440000"
id = 2, uuid = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
id = 3, uuid = "c5e9c6c1-2f45-4a8f-9c3d-5b6a8e7f9d3e"
```
- ✅ ID remains primary key
- ✅ Foreign keys still use ID
- ✅ UUID only for public URLs

### 3. Safe to Run Multiple Times
The safe script is **idempotent**:
- ✅ Won't error if column exists
- ✅ Won't overwrite existing UUIDs
- ✅ Only fills in missing UUIDs
- ✅ Safe for production

### 4. Performance Impact
UUIDs are fast with proper indexes:
- ✅ Indexes created automatically
- ✅ VARCHAR(36) with index is very fast
- ✅ Minimal performance impact
- ✅ Query time: <10ms typical

---

## 🚀 After Migration

Once migration is complete, you can:

### 1. Use UUIDs in Repositories
```java
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUuid(String uuid);
}
```

### 2. Use UUIDs in Services
```java
public User getUserByUuid(String uuid) {
    return userRepository.findByUuid(uuid)
        .orElseThrow(() -> new PaykeshoError("User not found"));
}
```

### 3. Use UUIDs in Controllers
```java
@GetMapping("/users/edit/{uuid}")
public String editUser(@PathVariable String uuid) {
    User user = userService.getUserByUuid(uuid);
    // ...
}
```

### 4. Use UUIDs in Templates
```html
<a th:href="@{'/users/edit/' + ${user.uuid}}">Edit</a>
```

---

## 📝 Quick Reference

| Script | Purpose | Safe to Rerun? |
|--------|---------|----------------|
| `check_uuid_columns.sql` | Diagnostic check | ✅ Yes |
| `add_uuid_columns_safe.sql` | Safe migration | ✅ Yes |
| `add_uuid_columns.sql` | Simple migration | ❌ No |

---

## ✅ Verification Commands

After migration, verify with these queries:

```sql
-- Check all tables have uuid column (should return 30)
SELECT COUNT(DISTINCT TABLE_NAME) 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() 
AND COLUMN_NAME = 'uuid';

-- Check no NULL uuids in users
SELECT COUNT(*) FROM users WHERE uuid IS NULL;

-- Should return 0 if all UUIDs populated

-- Check uuid is unique
SELECT uuid, COUNT(*) 
FROM users 
GROUP BY uuid 
HAVING COUNT(*) > 1;

-- Should return empty if all unique

-- Sample UUIDs to verify format
SELECT id, uuid, email FROM users LIMIT 5;
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

---

## 🎉 Success Checklist

- [ ] Ran `check_uuid_columns.sql` to diagnose
- [ ] Backed up database before migration
- [ ] Ran `add_uuid_columns_safe.sql` successfully
- [ ] Verified all tables have UUID column (30 tables)
- [ ] Verified no NULL UUIDs remain
- [ ] Verified indexes created
- [ ] Tested application still works
- [ ] Ready to implement UUID in code

---

## 🆘 Need Help?

If you encounter any issues:

1. **Check Current State**
   ```bash
   mysql -u username -p -e "USE database_name; \
   SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS \
   WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'uuid';"
   ```

2. **See Error Logs**
   ```bash
   tail -f /var/log/mysql/error.log
   ```

3. **Rollback if Needed**
   ```sql
   -- Only if something went wrong
   ALTER TABLE table_name DROP COLUMN uuid;
   ```

---

**Next Steps:** After successful migration, refer to the UUID implementation guides to update your code to use UUIDs in URLs!

- 📖 `UUID_IMPLEMENTATION_GUIDE.md` - Complete guide
- ✅ `UUID_MIGRATION_CHECKLIST.md` - Task list
- 🔄 `UUID_BEFORE_AFTER_EXAMPLES.md` - Code examples

