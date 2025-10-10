# Paykesho Configuration Guide

## 🎉 **Great News!**

The service communication is now working perfectly! The backoffice service can successfully connect to the notification
service using Docker service names.

## 🔧 **Configuration Updates Needed**

### **1. Email Configuration (Notification Service)**

Update `deploy/notification-service/application.properties`:

```properties
# Replace with your actual Gmail credentials
spring.mail.username=your-actual-email@gmail.com
spring.mail.password=your-actual-app-password
```

**To get Gmail App Password:**

1. Enable 2-Factor Authentication on your Gmail account
2. Go to Google Account Settings → Security → App Passwords
3. Generate an app password for "Mail"
4. Use this password in the configuration

### **2. SMS Configuration (Notification Service)**

Update `deploy/notification-service/application.properties`:

```properties
# Replace with your actual SMS Zambia credentials
sms.provider.smszambia.username=your-actual-username
sms.provider.smszambia.password=your-actual-password
sms.provider.smszambia.sender.id=PAYKESHO
```

### **3. OAuth2 Configuration (All Portal Services)**

Update the OAuth2 client IDs and secrets in each service configuration:

**Backoffice:**

```properties
spring.security.oauth2.client.registration.google.client-id=your-actual-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-actual-google-client-secret
spring.security.oauth2.client.registration.facebook.client-id=your-actual-facebook-client-id
spring.security.oauth2.client.registration.facebook.client-secret=your-actual-facebook-client-secret
```

**Client Portal, Merchant Portal, Organization Portal:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-actual-google-client-id
            client-secret: your-actual-google-client-secret
          facebook:
            client-id: your-actual-facebook-client-id
            client-secret: your-actual-facebook-client-secret
```

### **4. Payment Gateway Configuration (Payment Service)**

Update `deploy/payment-service/application.properties`:

```properties
# Replace with your actual payment gateway credentials
payment.gateway.mpesa.api.key=your-actual-mpesa-api-key
payment.gateway.mpesa.api.secret=your-actual-mpesa-api-secret
payment.gateway.card.api.key=your-actual-card-api-key
payment.gateway.card.api.secret=your-actual-card-api-secret
```

## 🚀 **How to Apply Changes**

After updating the configuration files:

**On Windows:**

```powershell
.\restart-services.ps1
```

**On Linux/Mac:**

```bash
docker-compose down
docker-compose up -d
```

## 📊 **Current Status**

✅ **Working:**

- Service communication between all modules
- Database connections
- Docker container networking
- Configuration file mounting

✅ **Configured:**

- Email credentials (Gmail) - ✅ Updated with real credentials
- SMS credentials (SMS Zambia) - ✅ Updated with real credentials

⚠️ **Needs Configuration:**

- OAuth2 credentials (Google, Facebook)
- Payment gateway credentials

## 🔍 **Testing the Configuration**

1. **Test Email Service:**
   ```bash
   curl -X POST http://localhost:8085/api/notifications/send \
     -H "Content-Type: application/json" \
     -d '{"recipient":"test@example.com","message":"Test email","notificationType":"email"}'
   ```

2. **Test SMS Service:**
   ```bash
   curl -X POST http://localhost:8085/api/notifications/send \
     -H "Content-Type: application/json" \
     -d '{"recipient":"0971684296","message":"Test SMS","notificationType":"sms"}'
   ```

3. **Check Service Logs:**
   ```bash
   docker-compose logs -f notification-service
   ```

## 🛡️ **Security Notes**

1. **Never commit real credentials** to version control
2. **Use environment variables** for production deployments
3. **Regularly rotate** API keys and passwords
4. **Use secrets management** for production environments

## 📞 **Support**

If you encounter issues:

1. Check Docker logs: `docker-compose logs [service-name]`
2. Verify configuration file syntax
3. Ensure all required credentials are provided
4. Test individual services independently
