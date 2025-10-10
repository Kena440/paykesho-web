# HAProxy Deployment Guide for Paykesho

This guide explains how to deploy your Paykesho microservices with HAProxy load balancer on a Linux server.

## Overview

HAProxy will route traffic from your domains to the appropriate microservices:

- `backoffice.paykesho.com` → Backoffice Service (port 8080)
- `merchant.paykesho.com` → Merchant Portal (port 8082)
- `organization.paykesho.com` → Organization Portal (port 8083)
- `client.paykesho.com` → Client Portal (port 8081)
- `api.paykesho.com` → Payment Service (port 8084)
- `notifications.paykesho.com` → Notification Service (port 8085)
- `files.paykesho.com` → File Service (port 8086)

## Prerequisites

### Server Requirements

- Linux server (Ubuntu 20.04+ recommended)
- Minimum 4GB RAM, 2 CPU cores
- 50GB+ disk space
- Docker and Docker Compose installed
- Domain names pointing to server IP

### Domain Configuration

Ensure your DNS records point to your server's IP address:

```
A    backoffice.paykesho.com     → YOUR_SERVER_IP
A    merchant.paykesho.com       → YOUR_SERVER_IP
A    organization.paykesho.com   → YOUR_SERVER_IP
A    client.paykesho.com         → YOUR_SERVER_IP
A    api.paykesho.com            → YOUR_SERVER_IP
A    notifications.paykesho.com  → YOUR_SERVER_IP
A    files.paykesho.com          → YOUR_SERVER_IP
```

## Deployment Steps

### 1. Server Setup

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Logout and login again to apply docker group changes
```

### 2. Upload Project Files

```bash
# Create project directory
mkdir -p /opt/paykesho
cd /opt/paykesho

# Upload your deploy directory to the server
# You can use scp, rsync, or git clone
scp -r /path/to/your/deploy user@your-server:/opt/paykesho/
```

### 3. SSL Certificate Setup

Follow the instructions in `SSL_SETUP.md` to configure SSL certificates.

### 4. Configure Environment

```bash
cd /opt/paykesho/deploy

# Update configuration files with production values
# Edit each service's configuration file:
nano backoffice/application.properties
nano client-portal/application.yml
nano merchant-portal/application.yml
nano organization-portal/application.yml
nano payment-service/application.properties
nano notification-service/application.properties
nano file-service/application.properties
```

### 5. Deploy Services

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f
```

### 6. Verify Deployment

```bash
# Check HAProxy status
curl -I http://your-server-ip:8404/stats

# Test domain routing
curl -I https://backoffice.paykesho.com
curl -I https://merchant.paykesho.com
curl -I https://organization.paykesho.com
```

## Monitoring and Management

### HAProxy Statistics

Access HAProxy statistics at: `http://your-server-ip:8404/stats`

### Service Management

```bash
# Restart specific service
docker-compose restart backoffice

# Restart all services
docker-compose restart

# View service logs
docker-compose logs -f haproxy
docker-compose logs -f backoffice
docker-compose logs -f merchant-portal

# Scale services (if needed)
docker-compose up -d --scale merchant-portal=2
```

### Health Checks

HAProxy automatically performs health checks on all services. You can monitor health status in the statistics page.

## Security Configuration

### Firewall Setup

```bash
# Install UFW
sudo apt install ufw

# Configure firewall
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### SSL/TLS Security

The HAProxy configuration includes:

- TLS 1.2+ only
- Strong cipher suites
- HSTS headers
- Security headers (X-Frame-Options, X-Content-Type-Options, etc.)

## Load Balancing

### Current Configuration

- Round-robin load balancing
- Health checks every 2 seconds
- Automatic failover

### Scaling Services

To add more instances of a service:

1. **Update docker-compose.yml:**
   ```yaml
   merchant-portal:
     # ... existing configuration ...
     deploy:
       replicas: 2
   ```

2. **Update HAProxy configuration:**
   ```bash
   # Add more servers in haproxy.cfg
   server merchant1 merchant-portal:8082 check inter 2000ms rise 2 fall 3
   server merchant2 merchant-portal:8082 check inter 2000ms rise 2 fall 3
   ```

3. **Restart services:**
   ```bash
   docker-compose up -d --scale merchant-portal=2
   docker-compose restart haproxy
   ```

## Backup and Recovery

### Database Backup

```bash
# Create backup script
cat > backup-db.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/paykesho/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

docker-compose exec mysql mysqldump -u paykesho_user -p***REMOVED*** paykesho > $BACKUP_DIR/paykesho_$DATE.sql

# Keep only last 7 days of backups
find $BACKUP_DIR -name "paykesho_*.sql" -mtime +7 -delete
EOF

chmod +x backup-db.sh

# Add to crontab
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/paykesho/backup-db.sh") | crontab -
```

### Configuration Backup

```bash
# Backup configuration files
tar -czf paykesho-config-$(date +%Y%m%d).tar.gz deploy/
```

## Troubleshooting

### Common Issues

1. **Services not starting:**
   ```bash
   docker-compose logs [service-name]
   docker-compose ps
   ```

2. **HAProxy configuration errors:**
   ```bash
   docker-compose exec haproxy haproxy -f /usr/local/etc/haproxy/haproxy.cfg -c
   ```

3. **SSL certificate issues:**
   ```bash
   openssl x509 -in ssl/paykesho.pem -text -noout
   ```

4. **Domain routing issues:**
   ```bash
   nslookup your-domain.com
   curl -I https://your-domain.com
   ```

### Performance Tuning

1. **Increase HAProxy timeouts** for slow services
2. **Adjust health check intervals** based on service response times
3. **Monitor resource usage** with `docker stats`
4. **Scale services** based on traffic patterns

## Maintenance

### Regular Tasks

1. **Update SSL certificates** (automated with Let's Encrypt)
2. **Monitor disk space** and clean up old logs
3. **Update Docker images** regularly
4. **Review and rotate** application logs
5. **Monitor service health** and performance

### Updates

```bash
# Update services
docker-compose pull
docker-compose up -d

# Update HAProxy configuration
# Edit haproxy.cfg and restart HAProxy
docker-compose restart haproxy
```

## Support

For issues:

1. Check service logs: `docker-compose logs [service-name]`
2. Verify HAProxy statistics: `http://your-server:8404/stats`
3. Test individual services: `curl http://localhost:8080/actuator/health`
4. Check SSL certificate validity
5. Verify DNS configuration

