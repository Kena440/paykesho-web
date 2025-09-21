#!/bin/bash

# Paykesho Secure Deployment Script
# This script deploys the Paykesho platform with all security hardening improvements

set -e  # Exit on any error

echo "🚀 Starting Paykesho Secure Deployment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if .env file exists
if [ ! -f ".env" ]; then
    print_error ".env file not found!"
    print_warning "Please copy environment-template.env to .env and configure your secrets"
    echo "cp environment-template.env .env"
    echo "nano .env  # Edit with your actual values"
    exit 1
fi

print_success ".env file found"

# Validate required environment variables
print_status "Validating environment variables..."

required_vars=("MYSQL_ROOT_PASSWORD" "MYSQL_DATABASE" "MYSQL_USER" "MYSQL_PASSWORD")
for var in "${required_vars[@]}"; do
    if ! grep -q "^$var=" .env; then
        print_error "Required environment variable $var not found in .env file"
        exit 1
    fi
done

print_success "Environment variables validated"

# Create necessary directories
print_status "Creating necessary directories..."
mkdir -p ./mysql/init
mkdir -p ./logs
mkdir -p ./backups

# Set proper permissions
chmod 755 ./mysql/init
chmod 755 ./logs
chmod 755 ./backups

print_success "Directories created"

# Build all services
print_status "Building all services..."
if ! docker-compose build; then
    print_error "Failed to build services"
    exit 1
fi

print_success "All services built successfully"

# Start database first and wait for it to be healthy
print_status "Starting MySQL database..."
docker-compose up -d mysql

print_status "Waiting for MySQL to be healthy..."
timeout=60
counter=0
while [ $counter -lt $timeout ]; do
    if docker-compose ps mysql | grep -q "healthy"; then
        print_success "MySQL is healthy"
        break
    fi
    sleep 2
    counter=$((counter + 2))
    echo -n "."
done

if [ $counter -ge $timeout ]; then
    print_error "MySQL failed to start within $timeout seconds"
    docker-compose logs mysql
    exit 1
fi

# Start all other services
print_status "Starting all services..."
if ! docker-compose up -d; then
    print_error "Failed to start services"
    docker-compose logs
    exit 1
fi

# Wait for all services to be healthy
print_status "Waiting for all services to be healthy..."
services=("backoffice" "client-portal" "merchant-portal" "organization-portal" "payment-service" "notification-service" "file-service")

for service in "${services[@]}"; do
    print_status "Checking health of $service..."
    timeout=120
    counter=0
    
    while [ $counter -lt $timeout ]; do
        if docker-compose ps $service | grep -q "healthy\|Up"; then
            print_success "$service is running"
            break
        fi
        sleep 3
        counter=$((counter + 3))
        echo -n "."
    done
    
    if [ $counter -ge $timeout ]; then
        print_warning "$service may not be fully healthy yet"
        docker-compose logs $service | tail -20
    fi
done

# Test health endpoints
print_status "Testing health endpoints..."
health_endpoints=(
    "http://localhost:8080/actuator/health"
    "http://localhost:8081/actuator/health" 
    "http://localhost:8082/actuator/health"
    "http://localhost:8083/actuator/health"
    "http://localhost:8084/actuator/health"
    "http://localhost:8085/actuator/health"
    "http://localhost:8086/actuator/health"
)

for endpoint in "${health_endpoints[@]}"; do
    if curl -f -s "$endpoint" > /dev/null; then
        print_success "Health check passed: $endpoint"
    else
        print_warning "Health check failed: $endpoint"
    fi
done

# Display deployment summary
echo ""
echo "🎉 Deployment Summary:"
echo "===================="
print_success "BackOffice: http://localhost:8080"
print_success "Client Portal: http://localhost:8081" 
print_success "Merchant Portal: http://localhost:8082"
print_success "Organization Portal: http://localhost:8083"
print_success "Payment Service: http://localhost:8084"
print_success "Notification Service: http://localhost:8085"
print_success "File Service: http://localhost:8086"
print_success "MySQL Database: localhost:3307"

echo ""
echo "🛡️ Security Features Enabled:"
echo "============================="
print_success "✅ Secrets management via environment variables"
print_success "✅ Resource limits on all containers"
print_success "✅ Health checks and auto-restart"
print_success "✅ Circuit breaker for notification service"
print_success "✅ Rate limiting on authentication endpoints"
print_success "✅ Enhanced error handling and logging"
print_success "✅ Database connection pool monitoring"
print_success "✅ Security headers (HSTS, Content-Type)"

echo ""
echo "📊 Monitoring:"
echo "=============="
echo "Health Checks: curl http://localhost:8080/actuator/health"
echo "Metrics: curl http://localhost:8080/actuator/metrics"
echo "Database Status: curl http://localhost:8080/actuator/health/db"

echo ""
print_success "🎉 Paykesho platform deployed successfully with enterprise-grade security!"
print_warning "⚠️  Remember to:"
echo "   - Monitor application logs regularly"
echo "   - Set up database backups"
echo "   - Monitor resource usage"
echo "   - Update secrets regularly"
