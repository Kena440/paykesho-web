#!/bin/bash

# Create directories for each service
mkdir -p {backoffice,client-portal,merchant-portal,organization-portal,payment-service,notification-service,file-service}

# Copy Dockerfile to each service directory
for dir in backoffice client-portal merchant-portal organization-portal payment-service notification-service file-service; do
    cp Dockerfile $dir/
done

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    cat > .env << EOF
# MySQL Configuration
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24)
MYSQL_DATABASE=paykesho
MYSQL_USER=paykesho_user
MYSQL_PASSWORD=$(openssl rand -base64 24)

# Java Options
JAVA_OPTS=-Xmx512m -Xms256m

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
EOF
fi

echo "Setup complete! Now you can:"
echo "1. Copy your JAR files to their respective directories"
echo "2. Run 'docker-compose up -d' to start the services"
