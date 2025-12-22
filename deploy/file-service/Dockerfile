FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy the JAR file
COPY *.jar app.jar

# Create uploads directory structure with proper permissions
RUN mkdir -p /app/uploads/logo /app/uploads/payslip /app/uploads/photo && \
    chmod -R 755 /app/uploads

# Create log directories with proper permissions
RUN mkdir -p /logs/file-service && \
    chmod -R 755 /logs

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE="prod"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 