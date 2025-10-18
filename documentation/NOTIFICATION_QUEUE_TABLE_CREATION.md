# Notification Queue Table Creation

## Issue

The `notification_queue` table doesn't exist in your VPS database yet.

---

## ✅ **Solution: 2 Options**

### **Option 1: Auto-Create (Recommended)** ⭐

Since you have `spring.jpa.hibernate.ddl-auto=update` configured, the table will be **automatically created** when you restart any service that uses this entity.

**Which services use NotificationQueue:**
- Notification Service (port 8085)
- Backoffice (port 8080) - for security audit
- Possibly client-portal, organization-portal, merchant-portal

**Just restart:**
```bash
cd deploy
docker-compose restart
```

**Hibernate will automatically run:**
```sql
CREATE TABLE notification_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME,
    notification_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(128),
    subject VARCHAR(255),
    content TEXT,
    template_name VARCHAR(100),
    template_variables TEXT,
    priority INT NOT NULL,
    retry_count INT NOT NULL,
    max_retries INT NOT NULL,
    scheduled_for DATETIME,
    sent_at DATETIME,
    processing_started_at DATETIME,
    error_message TEXT,
    metadata TEXT,
    entity_reference VARCHAR(100),
    entity_type VARCHAR(50),
    -- Indexes
    INDEX idx_notification_status (status),
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_recipient (recipient_email)
) ENGINE=InnoDB;
```

**Check logs after restart:**
```bash
grep "create table notification_queue" logs/*.log
```

---

### **Option 2: Manual Creation** 

If you prefer to create it manually (or auto-update is disabled on VPS):

```bash
mysql -u root -p paykesho_db < migrations/scripts/create_notification_queue_table.sql
```

---

## 🔍 **Verify Table Exists**

**Check if table exists:**
```sql
SHOW TABLES LIKE 'notification_queue';
```

**If exists:**
```
+---------------------------+
| notification_queue        |
+---------------------------+
```

**If doesn't exist:**
```
Empty set
```

**See table structure:**
```sql
DESCRIBE notification_queue;
```

---

## 🚀 **What to Do**

### **For Development/Local:**
```bash
# Just restart - table auto-creates
docker-compose restart
```

### **For VPS/Production:**

**Option A: Let auto-create happen (safer)**
```bash
# Restart services
docker-compose restart

# Check if created
mysql -u root -p paykesho_db -e "SHOW TABLES LIKE 'notification_queue';"
```

**Option B: Create manually first**
```bash
# Upload migration script to VPS
scp migrations/scripts/create_notification_queue_table.sql user@vps:/path/

# SSH to VPS
ssh user@vps

# Run migration
mysql -u root -p paykesho_db < create_notification_queue_table.sql

# Restart services
docker-compose restart
```

---

## ⚠️ **Why Table Might Not Exist**

1. **NotificationQueue entity is new**
   - Recently created
   - Services haven't restarted since creation

2. **Service not started yet**
   - Notification service may not be running
   - Table only created when service starts

3. **Entity not scanned**
   - Check `@EntityScan` includes the package
   - Should be: `@EntityScan({"org.commons.entity", ...})`

---

## 🔧 **Verify Entity Scanning**

Check `BackOfficeApplication.java`:
```java
@SpringBootApplication
@EnableScheduling
@EntityScan({"org.commons.entity", "org.paykesho.backoffice.user"})
public class BackOfficeApplication {
    ...
}
```

✅ `org.commons.entity` is included, so `NotificationQueue` should be scanned!

---

## ✅ **After Table Exists**

Then the **Security Audit** will scan it:

**Automated scan (2 AM):**
- Scans notification_queue for XSS
- Detects malicious content/subject/recipient
- Saves findings to security_audits table
- Shows in dashboard

**Manual scan:**
- Dashboard → "Run Security Scan Now"
- Includes notification_queue

---

## 📋 **Quick Fix Steps**

### **Step 1: Create Table**

Choose one:
```bash
# A. Auto-create (restart services)
docker-compose restart

# B. Manual create (run migration)
mysql -u root -p paykesho_db < migrations/scripts/create_notification_queue_table.sql
```

### **Step 2: Verify**
```sql
DESCRIBE notification_queue;
```

### **Step 3: Run Security Scan**
```
Backoffice → Security → Security Audit → Run Scan Now
```

### **Step 4: Check for Issues**
```sql
SELECT * FROM security_audits WHERE table_name = 'notification_queue';
```

---

## 🎯 **Expected Outcome**

After restart:
- ✅ `notification_queue` table exists
- ✅ Validation active for new notifications
- ✅ Security scan includes notification_queue
- ✅ Dashboard shows notification_queue findings
- ✅ Can delete malicious queued notifications

---

## 📞 **Files Created**

1. ✅ `migrations/scripts/create_notification_queue_table.sql` - Manual creation script
2. ✅ `documentation/NOTIFICATION_QUEUE_TABLE_CREATION.md` - This guide

**Already updated:**
- ✅ `NotificationQueue.java` - Validation added
- ✅ `SecurityValidator.java` - Validation method added
- ✅ `SecurityAuditService.java` - Scanning added
- ✅ `cleanup_all_malicious_data.sql` - Updated with table existence check

---

## 🎉 **Summary**

**Problem:** `notification_queue` table doesn't exist on VPS

**Solution:** 
1. Restart services (auto-creates)
2. OR run migration script manually

**Why:** Entity is new, table will be created on next restart

**Status:** Migration script ready, auto-create enabled

**Next:** Just restart! ✅

---

*Created: October 18, 2025*
*Status: Ready to deploy*

