# SSL Certificate Setup for HAProxy

This guide explains how to set up SSL certificates for your Paykesho domains with HAProxy.

## Prerequisites

- Domain names pointing to your server's IP address
- Server with Docker and Docker Compose installed
- Root or sudo access to the server

## Option 1: Let's Encrypt (Recommended - Free)

### 1. Install Certbot

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install certbot

# CentOS/RHEL
sudo yum install certbot

# Or use snap (works on most distributions)
sudo snap install --classic certbot
```

### 2. Generate Certificates

```bash
# Create SSL directory
mkdir -p deploy/ssl

# Generate certificates for all domains
sudo certbot certonly --standalone \
  -d backoffice.paykesho.com \
  -d merchant.paykesho.com \
  -d organization.paykesho.com \
  -d client.paykesho.com \
  -d api.paykesho.com \
  -d notifications.paykesho.com \
  -d files.paykesho.com

# Copy certificates to deploy/ssl directory
sudo cp /etc/letsencrypt/live/backoffice.paykesho.com/fullchain.pem deploy/ssl/
sudo cp /etc/letsencrypt/live/backoffice.paykesho.com/privkey.pem deploy/ssl/

# Combine certificate and private key for HAProxy
sudo cat /etc/letsencrypt/live/backoffice.paykesho.com/fullchain.pem \
        /etc/letsencrypt/live/backoffice.paykesho.com/privkey.pem \
        > deploy/ssl/paykesho.pem

# Set proper permissions
sudo chown $USER:$USER deploy/ssl/paykesho.pem
chmod 600 deploy/ssl/paykesho.pem
```

### 3. Auto-renewal Setup

```bash
# Create renewal script
cat > deploy/renew-ssl.sh << 'EOF'
#!/bin/bash
# Renew certificates
certbot renew --quiet

# Update HAProxy certificate
cat /etc/letsencrypt/live/backoffice.paykesho.com/fullchain.pem \
    /etc/letsencrypt/live/backoffice.paykesho.com/privkey.pem \
    > /path/to/your/deploy/ssl/paykesho.pem

# Reload HAProxy
docker-compose -f /path/to/your/deploy/docker-compose.yml exec haproxy haproxy -f /usr/local/etc/haproxy/haproxy.cfg -c
docker-compose -f /path/to/your/deploy/docker-compose.yml exec haproxy kill -USR2 1
EOF

chmod +x deploy/renew-ssl.sh

# Add to crontab for automatic renewal
(crontab -l 2>/dev/null; echo "0 12 * * * /path/to/your/deploy/renew-ssl.sh") | crontab -
```

## Option 2: Self-Signed Certificates (Development Only)

⚠️ **Warning**: Only use for development/testing. Browsers will show security warnings.

```bash
# Create SSL directory
mkdir -p deploy/ssl

# Generate self-signed certificate
openssl req -x509 -newkey rsa:4096 -keyout deploy/ssl/privkey.pem \
  -out deploy/ssl/fullchain.pem -days 365 -nodes \
  -subj "/C=ZM/ST=Lusaka/L=Lusaka/O=Paykesho/CN=paykesho.com"

# Combine for HAProxy
cat deploy/ssl/fullchain.pem deploy/ssl/privkey.pem > deploy/ssl/paykesho.pem

# Set permissions
chmod 600 deploy/ssl/paykesho.pem
```

## Option 3: Commercial SSL Certificate

If you have a commercial SSL certificate:

```bash
# Create SSL directory
mkdir -p deploy/ssl

# Copy your certificate files
cp your-certificate.crt deploy/ssl/fullchain.pem
cp your-private-key.key deploy/ssl/privkey.pem

# Combine for HAProxy
cat deploy/ssl/fullchain.pem deploy/ssl/privkey.pem > deploy/ssl/paykesho.pem

# Set permissions
chmod 600 deploy/ssl/paykesho.pem
```

## Verification

After setting up SSL certificates:

1. **Check certificate validity:**
   ```bash
   openssl x509 -in deploy/ssl/paykesho.pem -text -noout
   ```

2. **Test HAProxy configuration:**
   ```bash
   docker-compose -f deploy/docker-compose.yml exec haproxy haproxy -f /usr/local/etc/haproxy/haproxy.cfg -c
   ```

3. **Access your domains:**
    - https://backoffice.paykesho.com
    - https://merchant.paykesho.com
    - https://organization.paykesho.com
    - https://client.paykesho.com
    - https://api.paykesho.com
    - https://notifications.paykesho.com
    - https://files.paykesho.com

## Troubleshooting

### Certificate Issues

- Ensure domain names are correctly configured in DNS
- Check certificate file permissions (should be 600)
- Verify certificate is not expired

### HAProxy Issues

- Check HAProxy logs: `docker-compose logs haproxy`
- Test configuration: `docker-compose exec haproxy haproxy -f /usr/local/etc/haproxy/haproxy.cfg -c`
- Verify SSL certificate path in haproxy.cfg

### Domain Issues

- Ensure all domains point to your server's IP address
- Check DNS propagation: `nslookup your-domain.com`
- Verify firewall allows ports 80 and 443

## Security Notes

1. **Never commit private keys** to version control
2. **Use strong passwords** for private keys
3. **Regularly update certificates** before expiration
4. **Monitor certificate expiration** dates
5. **Use HSTS headers** (already configured in HAProxy)
6. **Implement proper backup** of certificates and keys

