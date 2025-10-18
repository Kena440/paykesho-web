

# Security Audit Dashboard Guide

## Overview

The Security Audit Dashboard is an **automated security monitoring system** that scans your database for malicious content and provides a **visual interface** for admins to review and take action.

---

## 🎯 **How It Works**

### **Automated Scanning**
1. **Daily at 2 AM** - Automated scan runs
2. **Scans 10 entities** - All user-facing tables checked
3. **Saves to database** - Findings stored in `security_audits` table
4. **Admin notification** - Alerts logged for review

### **Detection Types**
- ✅ **XSS Attacks** - Script tags, JavaScript in text fields
- ✅ **SQL Injection** - Dangerous SQL patterns
- ✅ **Path Traversal** - `../../../` in file paths
- ✅ **URL Spam** - URLs in address fields
- ✅ **Invalid Formats** - NRC, TPIN format violations

---

## 🚀 **Setup**

### **1. Create Database Table**

```bash
mysql -u root -p paykesho_db < migrations/scripts/create_security_audits_table.sql
```

This creates the `security_audits` table to store findings.

### **2. Enable Scheduling in Backoffice**

**Add to `BackofficeApplication.java`:**

```java
@SpringBootApplication
@EnableScheduling  // ← Add this annotation
public class BackofficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackofficeApplication.class, args);
    }
}
```

### **3. Configure in application.properties**

```properties
# Enable Spring Scheduling
spring.task.scheduling.enabled=true

# Security Audit runs daily at 2 AM automatically
# Cron: 0 0 2 * * ? = Every day at 2:00 AM
```

### **4. Restart Backoffice Service**

```bash
cd deploy
docker-compose restart backoffice
```

---

## 📊 **Using the Dashboard**

### **Access the Dashboard**

URL: `http://your-domain/security-audit`

**Navigation:** Backoffice → Security → Security Audit

### **Dashboard Features**

#### **1. Pending Issues Count**
- Red alert shows total pending issues
- Updates automatically after each scan

#### **2. Security Findings Table**
Displays all pending issues with:
- **Severity:** CRITICAL, HIGH, MEDIUM, LOW
- **Scan Date:** When detected
- **Table & Record ID:** Location of malicious data
- **Threat Type:** What kind of attack
- **Field Name:** Which field contains malicious data
- **Suspicious Value:** Preview of malicious content
- **Description:** What the issue is

#### **3. Actions Available**

**Delete Record:**
- Permanently removes malicious record from database
- Requires confirmation
- Requires action notes (reason for deletion)
- Cannot be undone!

**Mark as False Positive:**
- Keeps the record but marks finding as incorrect
- Requires reason
- Useful for legitimate data that triggered detection

**Status Tracking:**
- PENDING - Needs review
- IN_PROGRESS - Being investigated
- RESOLVED - Record deleted
- FALSE_POSITIVE - Marked as safe
- REVIEWED - Reviewed by admin

---

## 🔍 **Example Workflow**

### **Scenario: XSS Attack Detected**

**1. Dashboard Alert:**
```
Severity: CRITICAL
Table: clients
Record ID: 6
Threat Type: XSS_ATTACK
Field: name
Suspicious Value: <sCRiPt/sRC=//l01.co/1/></sCrIpT>Nocal
Description: XSS pattern detected in client name field
```

**2. Admin Actions:**

**Option A: Delete (Malicious)**
1. Click "Delete" button
2. Review details in modal
3. Enter action notes: "Confirmed XSS attack - malicious script tag"
4. Click "Yes, Delete Record"
5. Record removed from `clients` table
6. Audit marked as RESOLVED

**Option B: False Positive (Legitimate)**
1. Click "False Positive" button
2. Enter reason: "Legitimate business name containing special characters"
3. Click "Mark as False Positive"
4. Record kept in database
5. Audit marked as FALSE_POSITIVE

---

## 📅 **Automated Schedule**

### **Daily Scan (2 AM)**

The system automatically scans:
- ✅ clients
- ✅ announcements (CRITICAL - public facing)
- ✅ notifications
- ✅ file_uploads
- ✅ users (admin accounts)
- ✅ organization_admins
- ✅ merchant_admins
- ✅ organizations
- ✅ merchants
- ✅ employee_records

### **Manual Scan**

Click **"Run Security Scan Now"** button to run immediately.

---

## 🛡️ **Security Severity Levels**

### **CRITICAL** 🔴
- Announcements with XSS (public facing)
- Notifications with XSS (sent to users)
- Path traversal in file uploads
- XSS in admin user accounts

**Action:** Delete immediately

### **HIGH** 🟠
- URLs in client addresses (spam)
- XSS in admin tables
- SQL injection patterns

**Action:** Delete after review

### **MEDIUM** 🟡
- XSS in organizations/merchants
- Invalid NRC formats
- Invalid data patterns

**Action:** Review and fix or delete

### **LOW** 🔵
- Minor format violations
- Non-critical pattern matches

**Action:** Review and update validation rules

---

## 📈 **Monitoring**

### **Check Logs**

```bash
# View security audit logs
grep "SECURITY AUDIT" logs/backoffice.log

# View deletions
grep "Deleted malicious record" logs/backoffice.log

# Count findings
grep -c "SECURITY:" logs/backoffice.log
```

### **Database Queries**

```sql
-- Count pending issues
SELECT COUNT(*) FROM security_audits WHERE status = 'PENDING';

-- Pending by table
SELECT table_name, COUNT(*) as count 
FROM security_audits 
WHERE status = 'PENDING' 
GROUP BY table_name;

-- Recent scans
SELECT * FROM security_audits 
ORDER BY scan_date DESC 
LIMIT 20;

-- Critical issues
SELECT * FROM security_audits 
WHERE severity = 'CRITICAL' AND status = 'PENDING';
```

---

## ⚡ **Quick Actions**

### **Run Manual Scan**
```
Dashboard → Click "Run Security Scan Now"
```

### **Delete All Critical Issues**
```
1. Filter by CRITICAL severity
2. Review each carefully
3. Delete confirmed malicious records
4. Leave false positives
```

### **Weekly Review Process**
```
Monday:
1. Check pending count
2. Review critical issues
3. Delete confirmed threats
4. Mark false positives

Weekly Report:
- Total scans this week
- Issues found
- Issues resolved
- Trends
```

---

## 🔧 **Customization**

### **Change Scan Schedule**

Edit `SecurityAuditService.java`:

```java
// Daily at 2 AM
@Scheduled(cron = "0 0 2 * * ?")

// Every 6 hours
@Scheduled(cron = "0 0 */6 * * ?")

// Weekly on Sunday at 3 AM
@Scheduled(cron = "0 0 3 ? * SUN")
```

### **Add New Detection Pattern**

Edit scan methods in `SecurityAuditService.java` to add new threat patterns.

---

## 🎨 **Dashboard UI**

### **Color Coding**
- 🔴 Red Badge = CRITICAL
- 🟠 Orange Badge = HIGH
- 🟡 Yellow Badge = MEDIUM
- 🔵 Blue Badge = LOW

### **Icons**
- 🛡️ Shield = Security Dashboard
- ⚠️ Warning = Security Alert
- 🗑️ Trash = Delete Action
- ❌ X = False Positive
- ✓ Check = Resolved

---

## 📋 **Best Practices**

### **1. Daily Review**
- Check dashboard every morning
- Review overnight scan results
- Act on critical issues immediately

### **2. Document Actions**
- Always provide detailed action notes
- Explain why deleting or marking as false positive
- Maintain audit trail

### **3. False Positive Tuning**
- Track common false positives
- Update detection rules
- Reduce noise over time

### **4. Team Training**
- Train all admins on dashboard use
- Document decision-making process
- Regular security meetings

### **5. Backup Before Bulk Delete**
- Always backup before mass deletions
- Test restore procedures
- Keep 30+ days of backups

---

## ❓ **FAQ**

**Q: How often does the scan run?**
A: Automatically daily at 2 AM, or manually anytime.

**Q: Can I undo a deletion?**
A: No, deletions are permanent. Always review carefully and maintain backups.

**Q: What if I delete a legitimate record by mistake?**
A: Restore from backup. This is why action notes are required.

**Q: How long are audit records kept?**
A: Indefinitely. You can archive old resolved audits periodically.

**Q: Can I export the findings?**
A: Query the `security_audits` table and export to CSV.

**Q: What if scan detects too many false positives?**
A: Adjust detection patterns in `SecurityAuditService.java` or mark them as false positives.

---

## 📞 **Support**

**Files:**
- `SecurityAuditService.java` - Scanning logic
- `SecurityActionService.java` - Action handling
- `SecurityAuditController.java` - Dashboard controller
- `audit_dashboard.html` - UI template
- `security_audits` table - Database storage

**Tables:**
- `security_audits` - Audit findings
- All scanned tables (clients, announcements, etc.)

---

## ✅ **Setup Checklist**

- [ ] Database table created (`create_security_audits_table.sql`)
- [ ] @EnableScheduling added to BackofficeApplication
- [ ] spring.task.scheduling.enabled=true in properties
- [ ] Backoffice service restarted
- [ ] Dashboard accessible at /security-audit
- [ ] Manual scan tested
- [ ] Actions tested (delete, false positive)
- [ ] Daily scan scheduled (2 AM)
- [ ] Team trained on dashboard use
- [ ] Backup procedures in place

---

*Last Updated: October 18, 2025*
*Version: 1.0*

