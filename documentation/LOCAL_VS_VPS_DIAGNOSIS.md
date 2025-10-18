# 🔍 Local vs VPS Diagnosis

**Status**: ✅ Local works | ❌ VPS doesn't work

**Root Cause**: VPS application code is missing UUID implementation updates.

---

## 🎯 **Quick Diagnosis**

### **Step 1: Check VPS Application Version**

SSH into your VPS and check if the UUID code exists:

```bash
# SSH into VPS
ssh user@your-vps-ip

# Check if MerchantService has UUID methods
grep -n "getMerchantByUuid" /path/to/your/app/merchants/service/MerchantService.java
```

**Expected**: Should show lines with `getMerchantByUuid` method
**If empty**: VPS code is missing UUID implementation

### **Step 2: Check VPS Template**

```bash
# Check if merchants template uses UUID
grep -n "merchant.uuid" /path/to/your/app/templates/merchants/merchants.html
```

**Expected**: Should show lines with `${merchant.uuid}`
**If empty**: VPS template is missing UUID implementation

### **Step 3: Check VPS Entity**

```bash
# Check if IdBasedEntity exists on VPS
ls -la /path/to/your/app/commons/entity/IdBasedEntity.java
```

**Expected**: File should exist and contain UUID field
**If missing**: VPS is missing the base entity updates

---

## 🚀 **Quick Fixes to Try**

### **Fix 1: Simple Restart (Most Common)**
```bash
# SSH into VPS
ssh user@your-vps-ip

# Restart application
sudo systemctl restart paykesho-backoffice
# or
sudo service paykesho-backoffice restart
```

### **Fix 2: Clear Application Cache**
```bash
# SSH into VPS
ssh user@your-vps-ip

# Clear application cache
rm -rf /path/to/your/app/target/
rm -rf /path/to/your/app/webparent/*/target/

# Restart application
sudo systemctl restart paykesho-backoffice
```

### **Fix 3: Check File Timestamps**
```bash
# SSH into VPS
ssh user@your-vps-ip

# Check when files were last modified
ls -la /path/to/your/app/merchants/service/MerchantService.java
ls -la /path/to/your/app/templates/merchants/merchants.html

# Compare with your local files
```

---

## 🔧 **Deployment Methods**

### **Method 1: Git Pull (Recommended)**
```bash
# SSH into VPS
ssh user@your-vps-ip
cd /path/to/paykesho-web
git pull origin main
sudo systemctl restart paykesho-backoffice
```

### **Method 2: Manual File Upload**
Upload these files from local to VPS:
- `MerchantService.java`
- `MerchantController.java`
- `merchants.html`
- `IdBasedEntity.java`

### **Method 3: Docker Container Update**
```bash
# SSH into VPS
ssh user@your-vps-ip

# Stop and restart container
docker stop paykesho-backoffice
docker start paykesho-backoffice
```

---

## 📋 **Verification Steps**

After deployment, verify on VPS:

### **1. Check Application Logs**
```bash
# SSH into VPS
ssh user@your-vps-ip

# Check application logs for errors
tail -f /var/log/paykesho-backoffice/application.log
```

### **2. Test Direct URL**
Try accessing a merchant directly:
```
https://backoffice.paykesho.com/merchants/view/001fca58-abe7-4b2c-9d8e-123456789abc
```

### **3. Check Browser Network Tab**
1. Open merchants page
2. Open browser DevTools (F12)
3. Go to Network tab
4. Look for requests to `/merchants/view/`
5. Check if URLs contain UUIDs

---

## 🎯 **Most Likely Solutions**

### **Solution 1: Application Restart (90% success rate)**
```bash
ssh user@your-vps-ip
sudo systemctl restart paykesho-backoffice
```

### **Solution 2: Code Deployment (if restart doesn't work)**
```bash
ssh user@your-vps-ip
cd /path/to/paykesho-web
git pull origin main
sudo systemctl restart paykesho-backoffice
```

### **Solution 3: Cache Clear (if still not working)**
```bash
ssh user@your-vps-ip
rm -rf /path/to/your/app/target/
sudo systemctl restart paykesho-backoffice
```

---

## 🔍 **Debug Commands**

If nothing works, run these debug commands:

```bash
# SSH into VPS
ssh user@your-vps-ip

# Check application status
sudo systemctl status paykesho-backoffice

# Check if application is running
ps aux | grep java

# Check application logs
tail -f /var/log/paykesho-backoffice/application.log

# Test application response
curl -I http://localhost:8080/merchants
```

---

## 📞 **Next Steps**

1. **Try Solution 1 first** (restart application)
2. **If that doesn't work**, try Solution 2 (code deployment)
3. **Check the verification steps** to confirm it's working
4. **Report back** with what you find

---

**Most Common Issue**: VPS application cache needs to be cleared or application needs restart.

**Last Updated**: October 18, 2025
