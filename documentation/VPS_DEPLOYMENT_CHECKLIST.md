# VPS Deployment Checklist - Security Updates

## 🎯 **Deploying Security Updates to VPS**

This guide helps you deploy all security updates to your VPS safely.

---

## 📋 **Pre-Deployment Checklist**

### **1. Backup VPS Database** (CRITICAL!)

```bash
# SSH to VPS
ssh user@your-vps

# Backup database
mysqldump -u root -p paykesho_db > ~/backups/paykesho_backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup
ls -lh ~/backups/paykesho_backup_*.sql
```

### **2. Upload Migration Scripts**

```bash
# From your local machine
scp migrations/scripts/verify_all_tables.sql user@vps:/tmp/
scp migrations/scripts/create_missing_tables.sql user@vps:/tmp/
scp migrations/scripts/create_all_missing_tables_vps.sql user@vps:/tmp/
```

---

## 🚀 **Deployment Steps**

### **Step 1: Check Current State** (2 min)

```bash
# SSH to VPS
ssh user@your-vps

# Check which tables exist
mysql -u root -p paykesho_db < /tmp/verify_all_tables.sql
```

**Expected output:**
```
Table Name          | Status
--------------------|-------------
clients             | ✓ EXISTS
users               | ✓ EXISTS
security_audits     | ✗ MISSING  ← Need to create
notification_queue  | ✗ MISSING  ← Need to create
announcements       | ✓ EXISTS
...
```

---

### **Step 2: Create Missing Tables** (1 min)

```bash
# Run the master creation script
mysql -u root -p paykesho_db < /tmp/create_all_missing_tables_vps.sql
```

**What it does:**
- ✅ Creates `security_audits` table (if missing)
- ✅ Creates `notification_queue` table (if missing)
- ✅ Adds missing UUID columns to existing tables
- ✅ Adds `description` column to `file_uploads` (if missing)
- ✅ Adds `user_uuid` column to `file_uploads` (if missing)
- ✅ Creates all necessary indexes
- ✅ Safe to run multiple times (IF NOT EXISTS)

**Expected output:**
```
Creating security_audits table... ✓
Creating notification_queue table... ✓
Adding missing columns... ✓
All missing tables created successfully!
```

---

### **Step 3: Verify Tables Created** (1 min)

```bash
# Verify security_audits
mysql -u root -p paykesho_db -e "DESCRIBE security_audits;"

# Verify notification_queue
mysql -u root -p paykesho_db -e "DESCRIBE notification_queue;"

# Count all tables
mysql -u root -p paykesho_db -e "SELECT COUNT(*) as total_tables FROM information_schema.tables WHERE table_schema = 'paykesho_db';"
```

---

### **Step 4: Deploy Code** (5 min)

```bash
# Pull latest code
cd /path/to/paykesho-web
git pull origin master

# Or upload compiled JARs
# scp webparent/backoffice/target/*.jar user@vps:/path/to/deploy/
```

---

### **Step 5: Restart Services** (2 min)

```bash
# Restart all services
cd deploy
docker-compose restart

# Or restart individually
docker-compose restart backoffice
docker-compose restart client-portal
docker-compose restart organization-portal
docker-compose restart merchant-portal
docker-compose restart notification-service
```

---

### **Step 6: Verify Security Features Active** (3 min)

**A. Check Hibernate Auto-Created/Verified Tables:**
```bash
# Check logs for table creation
docker-compose logs backoffice | grep "create table\|alter table"
```

**B. Access Security Dashboard:**
```
http://your-vps-ip:8080/security-audit
```

**C. Run Security Scan:**
```
Dashboard → Click "Run Security Scan Now"
```

**D. Check for Malicious Data:**
```
Should detect clients ID 4 and 6 (if still there)
```

---

### **Step 7: Clean Malicious Data** (5 min)

**Option A: Via Dashboard (Recommended)**
```
1. Security Dashboard → Shows findings
2. Click "Delete" on each malicious record
3. Enter action notes
4. Confirm deletion
```

**Option B: Via SQL**
```bash
# Upload cleanup script
scp migrations/scripts/cleanup_all_malicious_data.sql user@vps:/tmp/

# Run scan
mysql -u root -p paykesho_db < /tmp/cleanup_all_malicious_data.sql

# Review results, then uncomment DELETE statements in script and re-run
```

---

## 📊 **What Gets Created**

### **New Tables:**

**1. security_audits**
- Stores security scan findings
- Used by Security Audit Dashboard
- Allows admins to review/delete malicious data

**2. notification_queue**
- Stores queued notifications (email/SMS)
- Protected from XSS injection
- Validated before sending

### **New Columns (if missing):**

**3. clients.uuid**
- Unique identifier for public URLs
- Better security than integer IDs

**4. file_uploads.description**
- File description field
- Validated for XSS

**5. file_uploads.user_uuid**
- Links files to users via UUID
- Better privacy/security

---

## 🔍 **Verification Commands**

### **Check Table Counts:**
```sql
SELECT 
    (SELECT COUNT(*) FROM security_audits) as security_audits,
    (SELECT COUNT(*) FROM notification_queue) as notification_queue,
    (SELECT COUNT(*) FROM clients) as clients;
```

### **Check Indexes:**
```sql
SHOW INDEX FROM security_audits;
SHOW INDEX FROM notification_queue;
```

### **Check for Missing Tables:**
```sql
-- Run verification script
SOURCE /tmp/verify_all_tables.sql;
```

---

## ⚠️ **Troubleshooting**

### **Error: Table already exists**
```
No problem! IF NOT EXISTS prevents errors.
Script is safe to run multiple times.
```

### **Error: Column already exists**
```
Script checks before adding columns.
Safe to run multiple times.
```

### **Error: Access denied**
```bash
# Make sure MySQL user has CREATE TABLE permission
GRANT CREATE, ALTER ON paykesho_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### **Error: Unknown database**
```bash
# Create database if it doesn't exist
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS paykesho_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

---

## 🎯 **Complete VPS Deployment Script**

### **One-Command Deployment:**

```bash
#!/bin/bash
# VPS Deployment Script

echo "=== Paykesho Security Update Deployment ==="

# 1. Backup
echo "Step 1: Backing up database..."
mysqldump -u root -p paykesho_db > ~/backups/paykesho_backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Create missing tables
echo "Step 2: Creating missing tables..."
mysql -u root -p paykesho_db < /tmp/create_all_missing_tables_vps.sql

# 3. Verify
echo "Step 3: Verifying tables..."
mysql -u root -p paykesho_db < /tmp/verify_all_tables.sql

# 4. Pull code
echo "Step 4: Pulling latest code..."
cd /path/to/paykesho-web
git pull origin master

# 5. Restart services
echo "Step 5: Restarting services..."
cd deploy
docker-compose restart

# 6. Wait for startup
echo "Waiting for services to start..."
sleep 30

# 7. Check logs
echo "Step 6: Checking logs..."
docker-compose logs backoffice | tail -50

echo "=== Deployment Complete ==="
echo "Access security dashboard at: http://your-vps:8080/security-audit"
```

---

## ✅ **Post-Deployment Verification**

### **1. Check Services Running:**
```bash
docker-compose ps
```

**Expected:**
```
backoffice           running   8080
client-portal        running   8081
merchant-portal      running   8082
organization-portal  running   8083
notification-service running   8085
file-service         running   8086
payment-service      running   8087
```

### **2. Check Tables Created:**
```sql
SHOW TABLES;
```

Should include:
- ✅ security_audits
- ✅ notification_queue

### **3. Access Security Dashboard:**
```
http://your-vps-ip:8080/security-audit
```

### **4. Run Security Scan:**
```
Click "Run Security Scan Now"
```

### **5. Check Logs:**
```bash
# Security audit logs
docker-compose logs backoffice | grep "SECURITY AUDIT"

# Table creation logs
docker-compose logs backoffice | grep "create table"
```

---

## 📁 **Scripts Summary**

| Script | Purpose | When to Use |
|--------|---------|-------------|
| **verify_all_tables.sql** | Check which tables exist | Before deployment |
| **create_missing_tables.sql** | Create security tables only | Quick fix |
| **create_all_missing_tables_vps.sql** | Create all missing tables + columns | **VPS deployment** ⭐ |

---

## 🎯 **Quick VPS Deployment**

```bash
# 1. Upload scripts
scp migrations/scripts/create_all_missing_tables_vps.sql user@vps:/tmp/

# 2. SSH to VPS
ssh user@vps

# 3. Backup
mysqldump -u root -p paykesho_db > backup_$(date +%Y%m%d).sql

# 4. Create tables
mysql -u root -p paykesho_db < /tmp/create_all_missing_tables_vps.sql

# 5. Restart
cd /path/to/deploy
docker-compose restart

# 6. Test
# Access: http://vps-ip:8080/security-audit
```

**Done in 5 minutes!** ✅

---

*Last Updated: October 18, 2025*
*For VPS Deployment*

