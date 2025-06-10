# Paykesho Web Application

## Docker Deployment Instructions

### Prerequisites
- Docker
- Docker Compose
- Git

### Environment Setup
1. Create a `.env` file in the root directory with the following variables:
```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=paykesho
MYSQL_USER=paykesho_user
MYSQL_PASSWORD=your_password

# Java Options
JAVA_OPTS=-Xmx512m -Xms256m

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

### Building and Running
1. Clone the repository:
```bash
git clone <repository-url>
cd paykesho-web
```

2. Build and start the services:
```bash
docker-compose up -d
```

This will start the following services:
- MySQL database (port 3306)
- Backoffice (port 8080)
- Client Portal (port 8081)
- Merchant Portal (port 8082)
- Payment Service (port 8083)
- Notification Service (port 8084)
- File Service (port 8085)

### Accessing the Services
- Backoffice: http://localhost:8080
- Client Portal: http://localhost:8081
- Merchant Portal: http://localhost:8082
- Payment Service: http://localhost:8083
- Notification Service: http://localhost:8084
- File Service: http://localhost:8085

### Stopping the Services
```bash
docker-compose down
```

To remove all data including the database volume:
```bash
docker-compose down -v
```

### Logs
To view logs for all services:
```bash
docker-compose logs -f
```

To view logs for a specific service:
```bash
docker-compose logs -f [service-name]
```

### Troubleshooting
1. If services fail to start, check the logs:
```bash
docker-compose logs -f [service-name]
```

2. Ensure all required ports are available on your system.

3. If you need to rebuild the services:
```bash
docker-compose build --no-cache
```

4. To restart a specific service:
```bash
docker-compose restart [service-name]
``` 


# Build all JAR files
mvn clean package -DskipTests

# Create deployment directory
mkdir -p deploy/{backoffice,client-portal,merchant-portal,payment-service,notification-service,file-service}

# Copy JAR files to deployment directories
cp webparent/backoffice/target/backoffice-1.0-SNAPSHOT.jar deploy/backoffice/
cp webparent/client-portal/target/client-portal-1.0-SNAPSHOT.jar deploy/client-portal/
cp webparent/merchant-portal/target/merchant-portal-1.0-SNAPSHOT.jar deploy/merchant-portal/
cp webparent/payment-service/target/payment-service-1.0-SNAPSHOT.jar deploy/payment-service/
cp webparent/notification-service/target/notification-service-1.0-SNAPSHOT.jar deploy/notification-service/
cp webparent/file-service/target/file-service-1.0-SNAPSHOT.jar deploy/file-service/

# Copy the deploy directory to your server
scp -r deploy/ user@your-server:/path/to/deployment/

# SSH into your server
ssh user@your-server

# Navigate to deployment directory
cd /path/to/deployment

# Create .env file
nano .env
# Add the environment variables as shown above

# Start the services
docker-compose up -d


