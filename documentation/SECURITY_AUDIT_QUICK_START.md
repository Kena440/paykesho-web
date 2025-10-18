# Security Audit Dashboard - Quick Start

## 🚀 **Get Started in 5 Minutes**

Perfect! You now have a **Security Audit Dashboard** in your backoffice where admins can view and act on malicious data!

---

## ⚡ **Quick Setup**

### **Step 1: Create Database Table** (1 minute)

```bash
mysql -u root -p paykesho_db < migrations/scripts/create_security_audits_table.sql
```

### **Step 2: Enable Scheduling** (1 minute)

**Edit:** `webparent/backoffice/src/main/java/org/paykesho/backoffice/BackofficeApplication.java`

**Add `@EnableScheduling`:**
```java
@SpringBootApplication
@EnableScheduling  // ← Add this line
public class BackofficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackofficeApplication.class, args);
    }
}
```

### **Step 3: Configure** (30 seconds)

**Add to:** `webparent/backoffice/src/main/resources/application.properties`

```properties
# Enable scheduled security scans
spring.task.scheduling.enabled=true
```

### **Step 4: Restart Backoffice** (2 minutes)

```bash
cd deploy
docker-compose restart backoffice
```

### **Step 5: Access Dashboard** (30 seconds)

Open: `http://localhost:8080/security-audit`

Or: Backoffice → Security → Security Audit Dashboard

---

## 🎯 **What You Get**

### **✅ Automated Daily Scans**
- Runs every day at 2 AM
- Scans all 10 entities
- Saves findings to database
- No manual work needed!

### **✅ Visual Dashboard**
- See all security threats in one place
- Color-coded by severity (CRITICAL, HIGH, MEDIUM, LOW)
- Real-time status updates

### **✅ Admin Actions**
- **Delete** malicious records with one click
- **Mark as false positive** for legitimate data
- All actions logged and auditable

### **✅ Detection Types**
- XSS attacks (script tags)
- SQL injection patterns
- Path traversal attempts
- URL spam in addresses
- Invalid data formats

---

## 📊 **Dashboard Features**

### **Main View**
```
+------------------------------------------+
| 🔴 PENDING SECURITY ISSUES: 5           |
| Malicious data detected in database      |
+------------------------------------------+
| [Run Security Scan Now]                  |
+------------------------------------------+

Table: Security Findings
┌──────────┬─────────┬───────────┬──────────┬─────────┐
│ SEVERITY │ TABLE   │ RECORD ID │ THREAT   │ ACTIONS │
├──────────┼─────────┼───────────┼──────────┼─────────┤
│ CRITICAL │ clients │ 6         │ XSS      │ Delete  │
│ HIGH     │ clients │ 4         │ URL SPAM │ Delete  │
└──────────┴─────────┴───────────┴──────────┴─────────┘
```

### **Actions Per Finding**
- 🗑️ **Delete** - Remove malicious record permanently
- ❌ **False Positive** - Mark as legitimate data

---

## 🔍 **Example: Handling an XSS Attack**

### **1. Dashboard Shows:**
```
Severity: CRITICAL
Table: clients
Record ID: 6
Threat: XSS_ATTACK
Field: name
Value: <script>alert('XSS')</script>John
Description: XSS pattern detected in client name
```

### **2. Admin Actions:**

**Option A: Delete (if malicious)**
1. Click "Delete" button
2. Confirm in modal
3. Enter notes: "Confirmed XSS attack"
4. Click "Yes, Delete Record"
5. ✅ Record deleted from database
6. ✅ Audit marked as RESOLVED

**Option B: False Positive (if legitimate)**
1. Click "False Positive"
2. Enter reason: "Legitimate test data"
3. Click "Mark as False Positive"
4. ✅ Record kept in database
5. ✅ Audit marked as FALSE_POSITIVE

---

## 📅 **Schedule**

### **Automatic Scan**
- **Daily at 2:00 AM** - Fully automated
- Scans all tables
- No action required

### **Manual Scan**
- Click **"Run Security Scan Now"** anytime
- Useful after bulk imports
- Immediate results

---

## 🎨 **Dashboard UI Preview**

```
╔══════════════════════════════════════════════╗
║ 🛡️ SECURITY AUDIT DASHBOARD                 ║
╠══════════════════════════════════════════════╣
║                                              ║
║  ⚠️ PENDING SECURITY ISSUES: 5               ║
║  Issues Require Attention                   ║
║                                              ║
║  [🔄 Run Security Scan Now]                  ║
║                                              ║
╠══════════════════════════════════════════════╣
║  📋 SECURITY FINDINGS                        ║
╠══════════════════════════════════════════════╣
║  CRITICAL  2024-10-18  clients  6   XSS      ║
║  [Delete] [False Positive]                   ║
║                                              ║
║  HIGH      2024-10-18  clients  4   URL      ║
║  [Delete] [False Positive]                   ║
╚══════════════════════════════════════════════╝
```

---

## ✅ **Verification**

### **Test the Setup**

1. **Access Dashboard:**
   ```
   http://localhost:8080/security-audit
   ```

2. **Run Manual Scan:**
   ```
   Click "Run Security Scan Now"
   ```

3. **Check Results:**
   ```
   Should find IDs 4 and 6 in clients table (if still there)
   ```

4. **Test Delete:**
   ```
   Click Delete on one finding
   Enter notes
   Confirm deletion
   ```

5. **Verify in Database:**
   ```sql
   SELECT * FROM security_audits;
   ```

---

## 📁 **Files Created**

### **Java Classes (Backoffice)**
```
webparent/backoffice/src/main/java/org/paykesho/backoffice/security/
├── controller/
│   └── SecurityAuditController.java    ← Dashboard controller
├── service/
│   ├── SecurityAuditService.java       ← Scanning logic
│   └── SecurityActionService.java      ← Action handling (delete/mark)
└── repository/
    └── SecurityAuditRepository.java    ← Database access
```

### **Entities (Commons)**
```
commons/src/main/java/org/commons/
├── entity/
│   └── SecurityAudit.java              ← Audit findings entity
└── enums/
    ├── SecurityThreatType.java         ← Threat types
    └── SecurityAuditStatus.java        ← Status types
```

### **UI (Backoffice)**
```
webparent/backoffice/src/main/resources/templates/security/
└── audit_dashboard.html                ← Dashboard UI
```

### **Database**
```
migrations/scripts/
└── create_security_audits_table.sql    ← Table creation script
```

---

## 🔧 **Troubleshooting**

### **Dashboard not accessible?**
```bash
# Check if @EnableScheduling is added
# Check if spring.task.scheduling.enabled=true
# Restart backoffice service
```

### **Scans not running?**
```bash
# Check logs
grep "Starting daily security audit" logs/backoffice.log

# Verify cron expression
# 0 0 2 * * ? = Every day at 2:00 AM
```

### **Table doesn't exist?**
```bash
# Run migration script
mysql -u root -p paykesho_db < migrations/scripts/create_security_audits_table.sql
```

---

## 📞 **Need Help?**

**Documentation:**
- `SECURITY_AUDIT_DASHBOARD.md` - Full guide
- `SECURITY_AUDIT_QUICK_START.md` - This file

**Check:**
- Database table: `security_audits`
- Logs: `logs/backoffice.log`
- Dashboard URL: `/security-audit`

---

## 🎉 **You're Done!**

Your system now has:
- ✅ Automated daily security scans
- ✅ Visual dashboard for review
- ✅ One-click actions (delete/mark)
- ✅ Audit trail of all actions
- ✅ Severity-based prioritization

**Next Steps:**
1. Access dashboard
2. Run first scan
3. Review findings
4. Delete malicious records
5. Check daily at 2 AM for automatic scans

---

*Setup Time: ~5 minutes*
*Last Updated: October 18, 2025*

