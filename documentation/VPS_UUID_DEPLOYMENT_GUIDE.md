# 🚀 VPS UUID Deployment Guide

**Issue**: UUID implementation works locally but not on VPS server.

**Root Cause**: VPS deployment is missing the latest code changes or database migrations.

---

## 🎯 **Solution Overview**

You need to deploy the updated code and run database migrations on your VPS server.

---

## 📋 **Deployment Checklist**

### **Step 1: Deploy Updated Code to VPS**

#### Option A: Git Pull (if using Git)
```bash
# SSH into your VPS
ssh user@your-vps-ip

# Navigate to your application directory
cd /path/to/your/paykesho-web

# Pull latest changes
git pull origin main

# Build the application
mvn clean package -DskipTests
```

#### Option B: Manual File Upload
If not using Git, you need to upload these updated files to your VPS:

**Backend Files to Update:**
- `commons/src/main/java/org/commons/entity/IdBasedEntity.java`
- `webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/service/MerchantService.java`
- `webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/repository/MerchantRepository.java`
- All other UUID-related service and repository files

**Frontend Files to Update:**
- `webparent/backoffice/src/main/resources/templates/merchants/merchants.html`
- All other template files with UUID links

### **Step 2: Run Database Migration on VPS**

```bash
# SSH into your VPS
ssh user@your-vps-ip

# Navigate to your application directory
cd /path/to/your/paykesho-web

# Run the UUID migration script on VPS database
mysql -u your_vps_db_user -p your_vps_db_name < migrations/scripts/add_uuid_columns_safe.sql
```

### **Step 3: Restart Application on VPS**

```bash
# Stop your application server
sudo systemctl stop paykesho-backoffice
# or
sudo service paykesho-backoffice stop
# or kill the Java process

# Start your application server
sudo systemctl start paykesho-backoffice
# or
sudo service paykesho-backoffice start
# or restart your Java application
```

### **Step 4: Verify Deployment**

1. **Check VPS database**:
```sql
-- SSH into VPS and check database
mysql -u your_vps_db_user -p your_vps_db_name
SELECT id, uuid, name FROM merchants LIMIT 3;
```

2. **Test VPS application**:
- Go to `https://backoffice.paykesho.com/merchants`
- Check if URLs show UUIDs
- Test clicking on merchant links

---

## 🔍 **VPS-Specific Issues to Check**

### **Issue 1: Database Connection**
Make sure your VPS application is connecting to the same database where you ran the migration:

```bash
# Check application configuration
cat /path/to/your/app/application.properties | grep database
```

### **Issue 2: File Permissions**
Make sure the application can read the updated files:

```bash
# Check file permissions
ls -la /path/to/your/app/webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/service/MerchantService.java
```

### **Issue 3: Application Cache**
VPS applications often have persistent caches:

```bash
# Clear application cache
rm -rf /path/to/your/app/target/
rm -rf /path/to/your/app/webparent/backoffice/target/

# Restart application
sudo systemctl restart paykesho-backoffice
```

---

## 🚨 **Quick VPS Fix Commands**

If you have SSH access to your VPS, run these commands:

```bash
# 1. SSH into VPS
ssh user@your-vps-ip

# 2. Navigate to app directory
cd /path/to/paykesho-web

# 3. Pull latest code (if using Git)
git pull origin main

# 4. Run database migration
mysql -u db_user -p db_name < migrations/scripts/add_uuid_columns_safe.sql

# 5. Restart application
sudo systemctl restart paykesho-backoffice

# 6. Check logs
tail -f /var/log/paykesho-backoffice/application.log
```

---

## 📊 **VPS Database Migration**

### **Check Current VPS Database State**

```sql
-- Check if UUID columns exist on VPS
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'your_vps_database_name'
AND COLUMN_NAME = 'uuid'
AND TABLE_NAME = 'merchants';
```

### **Expected Results**
- **If empty result**: UUID columns don't exist - need to run migration
- **If shows uuid column**: UUID columns exist - check if they have values

### **Check UUID Values on VPS**

```sql
-- Check UUID values on VPS
SELECT id, uuid, name FROM merchants LIMIT 3;
```

**Expected**: Should show UUID values like `001fca58-abe7-4b2c-9d8e-123456789abc`

**If NULL**: UUID values not populated - need to run migration

---

## 🔧 **Alternative: Manual VPS Database Update**

If the migration script doesn't work, manually add UUIDs to VPS database:

```sql
-- Add UUID column to merchants table
ALTER TABLE merchants ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Generate UUIDs for existing records
UPDATE merchants SET uuid = UUID() WHERE uuid IS NULL;

-- Make UUID column NOT NULL
ALTER TABLE merchants MODIFY COLUMN uuid VARCHAR(36) NOT NULL;

-- Add index for performance
CREATE INDEX idx_merchants_uuid ON merchants(uuid);

-- Verify
SELECT id, uuid, name FROM merchants LIMIT 3;
```

---

## 📞 **VPS Deployment Support**

### **Common VPS Issues**

1. **Permission Denied**: Use `sudo` for system commands
2. **Database Connection Failed**: Check VPS database credentials
3. **Application Won't Start**: Check Java version and memory settings
4. **Files Not Updated**: Clear application cache and restart

### **VPS Log Locations**

```bash
# Application logs
tail -f /var/log/paykesho-backoffice/application.log

# System logs
tail -f /var/log/syslog

# Database logs
tail -f /var/log/mysql/error.log
```

---

## ✅ **Verification Steps**

After deployment, verify:

1. **VPS Database**:
```sql
SELECT COUNT(*) as merchants_with_uuid FROM merchants WHERE uuid IS NOT NULL;
```

2. **VPS Application**:
- Go to merchants page
- URLs should show UUIDs
- Clicking should work without 404

3. **Application Logs**:
```bash
tail -f /var/log/paykesho-backoffice/application.log | grep -i uuid
```

---

## 🎯 **Expected Results**

### **Before VPS Deployment**:
```
URL: https://backoffice.paykesho.com/merchants/view/
Error: 404 Not Found
```

### **After VPS Deployment**:
```
URL: https://backoffice.paykesho.com/merchants/view/001fca58-abe7-4b2c-9d8e-123456789abc
Result: Merchant details page loads successfully
```

---

**Last Updated**: October 18, 2025  
**Status**: Ready for VPS deployment
