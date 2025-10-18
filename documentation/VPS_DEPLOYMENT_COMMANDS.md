# 🚀 VPS Deployment Commands

Since `docker system prune` didn't work on your VPS, here are alternative deployment methods:

## 🎯 **Method 1: Manual File Upload**

### Step 1: Prepare Files for Upload

You need to upload these key files to your VPS:

**Backend Files:**
```
commons/src/main/java/org/commons/entity/IdBasedEntity.java
webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/service/MerchantService.java
webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/repository/MerchantRepository.java
webparent/backoffice/src/main/java/org/paykesho/backoffice/merchants/controller/MerchantController.java
```

**Frontend Files:**
```
webparent/backoffice/src/main/resources/templates/merchants/merchants.html
webparent/backoffice/src/main/resources/templates/organizations/organizations.html
webparent/backoffice/src/main/resources/templates/users/users.html
webparent/backoffice/src/main/resources/templates/clients/clients.html
webparent/backoffice/src/main/resources/templates/vouchers/vouchers.html
```

### Step 2: Upload to VPS

```bash
# Upload files using SCP (replace with your VPS details)
scp -r local/path/to/files user@your-vps-ip:/path/to/vps/app/
```

## 🎯 **Method 2: Git Pull (Recommended)**

```bash
# SSH into your VPS
ssh user@your-vps-ip

# Navigate to your application directory
cd /path/to/paykesho-web

# Pull latest changes
git pull origin main

# Build the application
mvn clean package -DskipTests

# Restart the application
sudo systemctl restart paykesho-backoffice
```

## 🎯 **Method 3: Docker Container Update**

```bash
# SSH into your VPS
ssh user@your-vps-ip

# Stop the current container
docker stop paykesho-backoffice

# Remove the old container
docker rm paykesho-backoffice

# Pull latest image (if using Docker Hub)
docker pull your-registry/paykesho-backoffice:latest

# Or rebuild from local code
docker build -t paykesho-backoffice .

# Start new container
docker run -d --name paykesho-backoffice -p 8080:8080 paykesho-backoffice
```

## 🎯 **Method 4: Application Server Restart**

```bash
# SSH into your VPS
ssh user@your-vps-ip

# Stop application
sudo systemctl stop paykesho-backoffice
# or
sudo service paykesho-backoffice stop

# Clear application cache
rm -rf /path/to/app/target/
rm -rf /path/to/app/webparent/*/target/

# Start application
sudo systemctl start paykesho-backoffice
# or
sudo service paykesho-backoffice start
```

## 🔍 **VPS Status Check**

After deployment, verify:

```bash
# Check if application is running
sudo systemctl status paykesho-backoffice

# Check application logs
tail -f /var/log/paykesho-backoffice/application.log

# Test the application
curl http://localhost:8080/merchants
```

## 📋 **Quick VPS Fix**

If you have SSH access, try this quick fix:

```bash
# 1. SSH into VPS
ssh user@your-vps-ip

# 2. Navigate to app directory
cd /path/to/paykesho-web

# 3. Restart application (simplest fix)
sudo systemctl restart paykesho-backoffice

# 4. Check if it worked
curl -I http://localhost:8080/merchants
```

## 🚨 **Emergency Fix**

If nothing else works, try this:

```bash
# SSH into VPS
ssh user@your-vps-ip

# Kill all Java processes
sudo pkill -f java

# Clear all caches
sudo rm -rf /tmp/*
sudo rm -rf /path/to/app/target/

# Restart everything
sudo reboot
```

---

**Which method would you like to try first?**
