# Auto Database Update Guide

## ✅ **Already Configured and Working!**

Your Paykesho system is **already set up** for automatic database schema updates. When you add new entities or fields, the database automatically creates the tables and columns!

---

## 🎯 **What You Have**

### **Configuration (All Services)** ✅

All your services are configured with:
```properties
spring.jpa.hibernate.ddl-auto=update
```

This means:
- ✅ **New tables** auto-created when you add `@Entity` classes
- ✅ **New columns** auto-created when you add fields to entities
- ✅ **Indexes** auto-created from annotations
- ✅ **Relationships** auto-created (@ManyToOne, @OneToMany, etc.)
- ✅ **No manual SQL** needed for schema changes

**Services with auto-update:**
- ✅ Backoffice (port 8080)
- ✅ Client Portal (port 8081)
- ✅ Merchant Portal (port 8082)
- ✅ Organization Portal (port 8083)
- ✅ File Service (port 8086)
- ✅ Payment Service (port 8087)
- ✅ Notification Service (port 8085)

---

## 🚀 **How It Works**

### **Example 1: Adding a New Entity**

**Step 1:** Create entity class
```java
@Entity
@Table(name = "security_audits")
public class SecurityAudit extends IdBasedEntity {
    @Column(name = "scan_date", nullable = false)
    private Date scanDate;
    
    @Column(name = "table_name", length = 50)
    private String tableName;
    
    // ... more fields
}
```

**Step 2:** Restart service
```bash
docker-compose restart backoffice
```

**Step 3:** Check database
```sql
SHOW TABLES;
-- New table 'security_audits' appears automatically! ✅

DESCRIBE security_audits;
-- All columns created with correct types! ✅
```

**You did nothing manually! The table was auto-created!** 🎉

---

### **Example 2: Adding a New Field**

**Step 1:** Add field to existing entity
```java
@Entity
public class Client extends IdBasedEntity {
    // Existing fields...
    
    // NEW FIELD - just add it!
    @Column(name = "loyalty_points")
    private Integer loyaltyPoints;  // ← New field
}
```

**Step 2:** Restart service
```bash
docker-compose restart client-portal
```

**Step 3:** Check database
```sql
DESCRIBE clients;
-- New column 'loyalty_points' appears! ✅
```

**No migration script needed! Column auto-added!** 🎉

---

### **Example 3: Adding Validation**

**Step 1:** Add validation to existing field
```java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 245)
@Pattern(regexp = "^[a-zA-Z\\s'-]+$")  // ← New validation
private String name;
```

**Step 2:** Restart service
```bash
docker-compose restart
```

**Step 3:** Database remains same (validation is application-level)
```sql
-- No database change needed
-- Validation happens in Java before saving
```

**Validation works immediately! No DB changes!** 🎉

---

## 🔧 **DDL Auto Mode Options**

### **Current Setting: `update`** ✅ RECOMMENDED

```properties
spring.jpa.hibernate.ddl-auto=update
```

**What it does:**
- ✅ Creates new tables
- ✅ Adds new columns
- ✅ Updates indexes
- ❌ Never deletes columns (safe!)
- ❌ Never deletes tables (safe!)

**Best for:** Development and Production

---

### **Other Options (For Reference)**

#### **`create`** ⚠️ DANGEROUS
```properties
spring.jpa.hibernate.ddl-auto=create
```
- Creates schema at startup
- **DROPS ALL TABLES** first
- **LOSES ALL DATA**
- **NEVER use in production!**

#### **`create-drop`** ⚠️ VERY DANGEROUS
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```
- Creates schema at startup
- **DROPS ALL TABLES** at shutdown
- **FOR TESTING ONLY**

#### **`validate`** ✋ STRICT
```properties
spring.jpa.hibernate.ddl-auto=validate
```
- Validates schema matches entities
- **Does NOT create/update** anything
- Throws error if mismatch
- Good for production (with manual migrations)

#### **`none`** 🚫 MANUAL
```properties
spring.jpa.hibernate.ddl-auto=none
```
- Does nothing
- Requires manual migrations
- Most conservative

---

## 🎯 **What Happens When You Add SecurityAudit Entity**

### **Before Restart:**
```sql
mysql> SHOW TABLES;
+-------------------------+
| Tables_in_paykesho_db   |
+-------------------------+
| clients                 |
| users                   |
| merchants               |
| ...                     |
+-------------------------+
```

### **You Add SecurityAudit.java:**
```java
@Entity
@Table(name = "security_audits")
public class SecurityAudit extends IdBasedEntity {
    // fields defined here
}
```

### **Restart Backoffice:**
```bash
docker-compose restart backoffice
```

### **Hibernate Detects New Entity:**
```
Hibernate: 
    create table security_audits (
       id integer not null auto_increment,
       uuid varchar(36),
       scan_date datetime not null,
       table_name varchar(50) not null,
       record_id integer not null,
       threat_type varchar(50) not null,
       field_name varchar(50),
       suspicious_value varchar(1000),
       description varchar(500),
       status varchar(20) not null,
       reviewed_by integer,
       reviewed_date datetime,
       action_taken varchar(500),
       severity varchar(20),
       primary key (id)
    ) engine=InnoDB
```

### **After Restart:**
```sql
mysql> SHOW TABLES;
+-------------------------+
| Tables_in_paykesho_db   |
+-------------------------+
| clients                 |
| users                   |
| merchants               |
| security_audits         | ← NEW TABLE! ✅
| ...                     |
+-------------------------+

mysql> DESCRIBE security_audits;
-- All columns created! ✅
```

**You didn't write any SQL! Hibernate did it automatically!** 🎉

---

## 📋 **What Gets Auto-Updated**

### ✅ **Automatically Created:**

1. **Tables**
   ```java
   @Entity
   @Table(name = "my_new_table")
   public class MyEntity { }
   ```
   → `my_new_table` created

2. **Columns**
   ```java
   @Column(name = "new_field", length = 100)
   private String newField;
   ```
   → Column `new_field VARCHAR(100)` added

3. **Primary Keys**
   ```java
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer id;
   ```
   → Primary key with auto-increment

4. **Foreign Keys**
   ```java
   @ManyToOne
   @JoinColumn(name = "organization_id")
   private Organization organization;
   ```
   → Foreign key constraint created

5. **Indexes**
   ```java
   @Column(unique = true)
   private String email;
   ```
   → Unique index created

6. **Enums**
   ```java
   @Enumerated(EnumType.STRING)
   private SecurityThreatType threatType;
   ```
   → VARCHAR column created

---

### ❌ **NOT Automatically Done:**

1. **Column Deletion**
   - If you remove a field, column stays in DB
   - **Safe!** No data loss

2. **Column Rename**
   - Hibernate creates new column
   - Old column stays (data preserved)
   - Need manual migration to move data

3. **Table Deletion**
   - If you remove entity, table stays
   - **Safe!** No data loss

4. **Data Migration**
   - New columns are NULL
   - Need to populate with data manually

---

## 🎉 **Real Example: SecurityAudit**

### **What Happened:**

1. **I created `SecurityAudit.java`** in commons
2. **You'll restart backoffice**
3. **Hibernate auto-creates `security_audits` table** ✅
4. **All columns auto-created** ✅
5. **Indexes auto-created** ✅
6. **Ready to use immediately!** ✅

**You don't need to run `create_security_audits_table.sql` manually!**

But I provided it as:
- Documentation (shows table structure)
- Backup option (if auto-create fails)
- Production option (if you prefer manual control)

---

## 🔄 **How Auto-Update Works**

### **Application Startup Flow:**

```
1. Spring Boot starts
   ↓
2. Hibernate scans for @Entity classes
   ↓
3. Compares entities with database schema
   ↓
4. Finds differences:
   - New entity → CREATE TABLE
   - New field → ALTER TABLE ADD COLUMN
   - New index → CREATE INDEX
   ↓
5. Executes SQL automatically
   ↓
6. Logs DDL statements (if show-sql=true)
   ↓
7. Application ready with updated schema
```

### **What You See in Logs:**

```
Hibernate: create table security_audits (...)
Hibernate: alter table security_audits add index idx_status (status)
Hibernate: alter table security_audits add index idx_severity (severity)
```

---

## ⚙️ **Enhanced Configuration**

### **What I Added:**

```properties
# Auto-create missing tables and columns
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Scheduled Tasks (for Security Audit)
spring.task.scheduling.enabled=true
spring.task.scheduling.pool.size=5
```

**Benefits:**
- Better performance (batch inserts)
- Consistent naming (matches your @Column names exactly)
- Scheduled tasks enabled (for security scans)

---

## 🧪 **Testing Auto-Update**

### **Test 1: Add New Field**

**Add to `Client.java`:**
```java
@Column(name = "test_field")
private String testField;
```

**Restart:**
```bash
docker-compose restart client-portal
```

**Verify:**
```sql
DESCRIBE clients;
-- Should see 'test_field' column! ✅
```

**Remove field and restart to clean up.**

---

### **Test 2: SecurityAudit Auto-Creation**

**Current state:**
```sql
SHOW TABLES LIKE 'security_audits';
-- May return empty
```

**Restart backoffice:**
```bash
docker-compose restart backoffice
```

**Check again:**
```sql
SHOW TABLES LIKE 'security_audits';
-- Should show security_audits table! ✅

DESCRIBE security_audits;
-- All columns from SecurityAudit.java created! ✅
```

---

## ⚠️ **Best Practices**

### **Development (Current Setup)** ✅

```properties
spring.jpa.hibernate.ddl-auto=update  # Safe auto-update
spring.jpa.show-sql=true              # See SQL statements
```

**Perfect for:**
- Adding new features
- Quick iterations
- Testing new entities

### **Production (Also Configured)** ✅

```properties
spring.jpa.hibernate.ddl-auto=update  # Still safe
spring.jpa.show-sql=false             # Hide SQL (performance)
```

**Safe because `update` mode:**
- Never deletes data
- Never drops tables
- Only adds new structures

### **Alternative Production (Conservative)**

```properties
spring.jpa.hibernate.ddl-auto=validate  # No auto-update
# Use manual migrations instead
```

**When to use:**
- Very large production systems
- High-traffic applications
- When you want full control
- Regulatory requirements

---

## 📊 **What Auto-Updates**

| Action | Auto-Update Behavior |
|--------|---------------------|
| **Add new @Entity** | ✅ CREATE TABLE automatically |
| **Add new @Column field** | ✅ ALTER TABLE ADD COLUMN |
| **Add @ManyToOne** | ✅ CREATE foreign key |
| **Add unique=true** | ✅ CREATE unique index |
| **Add validation (@NotNull)** | ❌ No DB change (app-level) |
| **Remove field** | ❌ Column stays (safe!) |
| **Remove entity** | ❌ Table stays (safe!) |
| **Rename field** | ⚠️ Creates new column (old stays) |
| **Change column type** | ⚠️ May not update (manual SQL needed) |

---

## 🎓 **Common Scenarios**

### **Scenario 1: I Added SecurityAudit Entity**

**What happens automatically:**
```
1. Restart backoffice
2. Hibernate sees new @Entity
3. Creates 'security_audits' table
4. Adds all columns from entity
5. Creates indexes (idx_status, idx_severity, etc.)
6. Done! ✅
```

**Manual SQL needed:** NONE

---

### **Scenario 2: I Add "description" to FileUpload**

**What happens automatically:**
```
1. Add field to FileUpload.java:
   @Column(name = "description")
   private String description;

2. Restart any service using FileUpload
3. Hibernate detects new field
4. Runs: ALTER TABLE file_uploads ADD COLUMN description VARCHAR(255)
5. Done! ✅
```

**Manual SQL needed:** NONE

---

### **Scenario 3: I Want to Rename a Column**

**Problem:** Auto-update creates NEW column, doesn't rename

**Solution - Option A (Auto + Manual):**
```java
// 1. Add new field
@Column(name = "new_name")
private String newName;

// 2. Restart (new column created)
// 3. Run manual SQL to copy data:
UPDATE clients SET new_name = old_name;

// 4. Remove old field
// 5. Restart (old column stays but unused)
```

**Solution - Option B (Manual Migration):**
```sql
-- Just rename in database
ALTER TABLE clients CHANGE old_name new_name VARCHAR(255);
```

---

## 🆕 **Adding New Security Features (Example)**

### **Scenario: Add IP Tracking to SecurityAudit**

**Step 1:** Add field to entity
```java
@Entity
public class SecurityAudit extends IdBasedEntity {
    // Existing fields...
    
    // NEW FIELD - just add it!
    @Column(name = "ip_address", length = 45)
    private String ipAddress;  // ← Auto-creates column on restart
}
```

**Step 2:** Restart
```bash
docker-compose restart backoffice
```

**Step 3:** Hibernate auto-runs
```sql
ALTER TABLE security_audits ADD COLUMN ip_address VARCHAR(45);
```

**Step 4:** Use immediately
```java
audit.setIpAddress("192.168.1.1");
securityAuditRepository.save(audit);  // Works! ✅
```

**That's it! No manual SQL!**

---

## 📝 **Migration Script Still Useful?**

### **When to Use Manual Scripts:**

Even with auto-update, manual scripts are useful for:

1. **Data Migration**
   ```sql
   -- Populate new column with default values
   UPDATE clients SET loyalty_points = 0 WHERE loyalty_points IS NULL;
   ```

2. **Complex Changes**
   ```sql
   -- Rename column
   ALTER TABLE clients CHANGE old_name new_name VARCHAR(255);
   
   -- Change column type
   ALTER TABLE clients MODIFY COLUMN price DECIMAL(10,2);
   ```

3. **Data Cleanup**
   ```sql
   -- Our security cleanup scripts
   DELETE FROM clients WHERE name REGEXP '<script>';
   ```

4. **Production Control**
   ```sql
   -- Review and approve changes before applying
   -- Run during maintenance window
   ```

---

## ✅ **Your Current Setup**

### **Development:**
```properties
spring.jpa.hibernate.ddl-auto=update  ✅ Enabled
spring.jpa.show-sql=true              ✅ See SQL in logs
```

**Result:**
- Add entity → Table auto-created
- Add field → Column auto-created
- See SQL in console
- Fast development

### **Production:**
```properties
spring.jpa.hibernate.ddl-auto=update  ✅ Enabled
spring.jpa.show-sql=false             ✅ Hidden for performance
```

**Result:**
- Safe auto-updates
- No data loss
- Performance optimized
- Production-ready

---

## 🎉 **You're Already Set Up!**

### **For SecurityAudit:**
- ✅ Entity created in commons
- ✅ Auto-update enabled in backoffice
- ✅ On restart, table auto-creates
- ✅ No manual SQL needed!

### **Just Restart:**
```bash
docker-compose restart backoffice
```

**And the `security_audits` table will be created automatically!**

---

## 🚀 **Quick Reference**

### **Add New Entity:**
```
1. Create @Entity class
2. Restart service
3. Table created automatically ✅
```

### **Add New Field:**
```
1. Add @Column field to entity
2. Restart service
3. Column added automatically ✅
```

### **Add Validation:**
```
1. Add @NotBlank, @Pattern, etc.
2. Restart service
3. Validation works (no DB change needed) ✅
```

### **Check What Changed:**
```sql
SHOW CREATE TABLE table_name;
DESCRIBE table_name;
```

---

## 📞 **Summary**

**Q: Do tables/columns auto-create?**
✅ **YES!** Already configured with `spring.jpa.hibernate.ddl-auto=update`

**Q: Do I need manual SQL?**
❌ **NO!** For new tables/columns, Hibernate does it automatically

**Q: Is it safe?**
✅ **YES!** Update mode never deletes data or columns

**Q: Do I need migration scripts?**
⚠️ **Optional** - Useful for data migration and complex changes, but not required for schema changes

**Q: What about SecurityAudit?**
✅ **Auto-creates on restart!** Just restart backoffice and the table appears!

---

**You're already fully configured for auto-database updates!** 🎉

Just restart your services and new entities/fields appear automatically in the database!
