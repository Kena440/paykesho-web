# Paykesho Web Application Configuration

This directory contains Docker configuration and application properties for the Paykesho web application modules.

## Structure

```
deploy/
├── docker-compose.yml              # Main Docker Compose configuration
├── setup.sh                        # Setup script
├── Dockerfile                      # Base Dockerfile
├── backoffice/
│   └── application.properties      # Backoffice module configuration
├── client-portal/
│   └── application.yml             # Client portal module configuration
├── merchant-portal/
│   └── application.yml             # Merchant portal module configuration
├── organization-portal/
│   └── application.yml             # Organization portal module configuration
├── payment-service/
│   └── application.properties      # Payment service module configuration
├── notification-service/
│   └── application.properties      # Notification service module configuration
├── file-service/
│   └── application.properties      # File service module configuration
└── mysql/
    └── init/                       # MySQL initialization scripts
```

## Configuration Files

Each module has its own configuration file that can be easily modified without rebuilding Docker images:

### Backoffice (`backoffice/application.properties`)

- Server port: 8080
- Database configuration
- Security settings (OAuth2)
- External service URLs
- File upload settings
- Cache configuration

### Client Portal (`client-portal/application.yml`)

- Server port: 8081
- Database configuration
- OAuth2 client settings
- External service URLs
- Session management

### Merchant Portal (`merchant-portal/application.yml`)

- Server port: 8082
- Database configuration
- Merchant-specific settings
- Settlement processing
- Transaction limits

### Organization Portal (`organization-portal/application.yml`)

- Server port: 8083
- Database configuration
- Organization-specific settings
- Payroll processing
- Employee management

### Payment Service (`payment-service/application.properties`)

- Server port: 8084
- Payment gateway configurations
- Processing settings
- Security settings
- Transaction limits

### Notification Service (`notification-service/application.properties`)

- Server port: 8085
- Email configuration (SMTP)
- SMS configuration (Africa's Talking)
- Push notification settings
- Rate limiting

### File Service (`file-service/application.properties`)

- Server port: 8086
- File storage configuration
- AWS S3 settings
- File processing settings
- Upload limits

## Usage

### 1. Modify Configuration

To change application settings, simply edit the corresponding configuration file:

```bash
# Edit backoffice configuration
nano backoffice/application.properties

# Edit client portal configuration
nano client-portal/application.yml
```

### 2. Apply Changes

After modifying configuration files, restart the affected services:

**On Windows:**

```powershell
# Restart all services with new configuration
.\restart-services.ps1

# Or manually:
docker-compose down
docker-compose up -d
```

**On Linux/Mac:**

```bash
# Restart specific service
docker-compose restart backoffice

# Restart all services
docker-compose restart

# Or completely restart with new configuration:
docker-compose down
docker-compose up -d
```

### 3. Environment Variables

The configuration files use Docker service names for inter-service communication:

- `mysql` - Database service
- `notification-service` - Notification service (port 8085)
- `payment-service` - Payment service (port 8084)
- `file-service` - File service (port 8086)

**Important:** The configuration files in the deploy directory override the default application properties and use
Docker service names instead of localhost for inter-service communication.

### 4. Production Deployment

For production deployment, update the following in configuration files:

1. **Database credentials** - Change default passwords
2. **API keys** - Replace placeholder values with actual keys
3. **External URLs** - Update to production domain names
4. **Security settings** - Configure proper OAuth2 credentials

### 5. Environment-Specific Configuration

You can create environment-specific configuration files:

```bash
# Development
cp backoffice/application.properties backoffice/application-dev.properties

# Production
cp backoffice/application.properties backoffice/application-prod.properties
```

Then update the `SPRING_PROFILES_ACTIVE` environment variable in `docker-compose.yml`.

## Security Notes

1. **Never commit sensitive data** like API keys or passwords to version control
2. **Use environment variables** for sensitive configuration in production
3. **Regularly rotate** API keys and passwords
4. **Use secrets management** for production deployments

## Troubleshooting

### Configuration Not Applied

- Ensure the volume mount path is correct
- Check if the service is using the correct profile
- Verify file permissions

### Service Communication Issues

- Verify service names in configuration files
- Check network connectivity between containers
- Ensure ports are correctly mapped

### Database Connection Issues

- Verify MySQL service is running
- Check database credentials
- Ensure database initialization is complete

## HAProxy Load Balancer

This deployment includes HAProxy for load balancing and SSL termination. HAProxy routes traffic from your domains to the
appropriate microservices:

- `backoffice.paykesho.com` → Backoffice Service (port 8080)
- `merchant.paykesho.com` → Merchant Portal (port 8082)
- `organization.paykesho.com` → Organization Portal (port 8083)
- `client.paykesho.com` → Client Portal (port 8081)
- `api.paykesho.com` → Payment Service (port 8084)
- `notifications.paykesho.com` → Notification Service (port 8085)
- `files.paykesho.com` → File Service (port 8086)

### Quick Deployment

```bash
# Run the deployment script
./deploy.sh

# Or manually deploy
docker-compose up -d
```

### SSL Setup

See `SSL_SETUP.md` for detailed SSL certificate configuration instructions.

### HAProxy Statistics

Access HAProxy statistics at: `http://your-server:8404/stats`

## Support

For issues related to configuration or deployment, check:

1. Docker logs: `docker-compose logs [service-name]`
2. Application logs inside containers
3. Network connectivity between services
4. HAProxy configuration: `./deploy.sh validate`
5. SSL certificate setup: `./deploy.sh ssl`
