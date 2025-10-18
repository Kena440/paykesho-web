# Database Migrations

This folder contains database migration scripts for the Paykesho platform.

## 📁 Structure

```
migrations/
└── scripts/
    ├── run_all_uuid_migrations.sql             - ⭐ ALL-IN-ONE comprehensive migration (RECOMMENDED)
    ├── add_uuid_columns.sql                    - Full UUID migration
    ├── add_uuid_columns_safe_mode.sql          - Safe mode compatible version
    ├── add_user_uuid_to_file_uploads.sql       - File uploads UUID support
    ├── populate_uuids_existing_data.sql        - Populate UUIDs with progress tracking
    └── simple_populate_uuids.sql               - Simple UUID population
```

---

## 🚀 **Quick Start - UUID Migration**

### **⭐ RECOMMENDED: Run All Migrations (One Command):**
```bash
mysql -u username -p database_name < migrations/scripts/run_all_uuid_migrations.sql
```
This runs everything: adds UUID columns to 30 tables, populates data, adds file_uploads support.

### **Alternative: If UUID Columns Don't Exist:**
```bash
mysql -u username -p database_name < migrations/scripts/add_uuid_columns_safe_mode.sql
mysql -u username -p database_name < migrations/scripts/add_user_uuid_to_file_uploads.sql
```

### **If UUID Columns Exist But Empty:**
```bash
mysql -u username -p database_name < migrations/scripts/simple_populate_uuids.sql
```

---

## 📋 **Migration Scripts**

### 0. `run_all_uuid_migrations.sql` ⭐⭐⭐
- **Purpose**: Complete UUID migration in one script
- **Includes**: 
  - Adds UUID to 30 main tables
  - Populates UUIDs for existing data
  - Adds user_uuid to file_uploads table
  - Creates all necessary indexes
- **Safe Mode**: ✅ Yes (handles automatically)
- **Idempotent**: ✅ Yes (safe to run multiple times)
- **Use When**: **ALWAYS - This is the recommended script for production!**
- **Recommended**: ⭐⭐⭐ **BEST CHOICE**

### 1. `add_uuid_columns.sql`
- **Purpose**: Add UUID columns to all tables
- **Safe Mode**: ❌ No
- **Idempotent**: ❌ No
- **Use When**: First time only, safe mode disabled

### 2. `add_uuid_columns_safe_mode.sql` ⭐
- **Purpose**: Add UUID columns + populate data
- **Safe Mode**: ✅ Yes (handles automatically)
- **Idempotent**: ✅ Yes (checks before adding)
- **Use When**: First time, safe mode enabled
- **Recommended**: Yes for production

### 3. `populate_uuids_existing_data.sql`
- **Purpose**: Only populate UUIDs (columns must exist)
- **Safe Mode**: ✅ Yes
- **Progress Tracking**: ✅ Yes (shows count per table)
- **Use When**: Columns exist but data missing

### 4. `simple_populate_uuids.sql` ⭐
- **Purpose**: Populate UUIDs for existing data
- **Safe Mode**: ✅ Yes
- **Simple**: ✅ Very straightforward
- **Use When**: After columns added, need to populate
- **Recommended**: Yes for populating existing data

### 5. `add_user_uuid_to_file_uploads.sql`
- **Purpose**: Add user_uuid column to file_uploads table
- **Safe Mode**: ✅ Yes
- **Idempotent**: ✅ Yes
- **Use When**: After main UUID migration, for file upload UUID support
- **Note**: Included in `run_all_uuid_migrations.sql`

---

## 🎯 **Recommended Workflow**

### ⭐ Best Practice - Use the Comprehensive Script:
```bash
# This handles EVERYTHING (recommended for all scenarios)
mysql -u username -p database_name < migrations/scripts/run_all_uuid_migrations.sql
```

### Alternative: Step-by-Step Approach

#### New Database (No UUID Columns):
```bash
# Use the all-in-one script
mysql -u username -p database_name < migrations/scripts/add_uuid_columns_safe_mode.sql
mysql -u username -p database_name < migrations/scripts/add_user_uuid_to_file_uploads.sql
```

#### Existing Database (Columns Added, Need Population):
```bash
# Use the simple populate script
mysql -u username -p database_name < migrations/scripts/simple_populate_uuids.sql
```

---

## ✅ **Verification**

After running migration:

```sql
-- Check all tables have uuid column
SELECT COUNT(DISTINCT TABLE_NAME) 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND COLUMN_NAME = 'uuid';
-- Should return 30

-- Check no NULL uuids
SELECT COUNT(*) FROM users WHERE uuid IS NULL;
-- Should return 0

-- See sample UUIDs
SELECT id, uuid, email FROM users LIMIT 3;
```

---

## 📚 **Documentation**

For complete implementation guide, see:
- `documentation/README_UUID_SECURITY.md` - Start here
- `documentation/UUID_DATABASE_MIGRATION_HOW_TO.md` - Database guide
- `documentation/POST_MIGRATION_CHECKLIST.md` - What to test

---

## ⚠️ **Important Notes**

### Before Running Any Migration:
1. **BACKUP YOUR DATABASE!**
   ```bash
   mysqldump -u username -p database_name > backup_$(date +%Y%m%d).sql
   ```

2. **Test on development first**
3. **Verify backup can be restored**
4. **Then run on production**

### Safe Mode:
- All scripts in this folder handle MySQL safe mode
- No need to disable safe mode manually
- Scripts temporarily disable and re-enable automatically

---

## 🔒 **Security**

These migrations add UUID support for enhanced security:
- Prevents ID enumeration attacks
- Reduces IDOR vulnerabilities
- Obscures system scale
- Follows OWASP best practices

---

**For questions, refer to documentation folder or the comprehensive guides!**

