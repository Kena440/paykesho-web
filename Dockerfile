# Build stage
FROM maven:3.8.4-openjdk-15-slim AS build
WORKDIR /app

# Copy the parent pom files
COPY pom.xml .
COPY webparent/pom.xml webparent/
COPY commons/pom.xml commons/

# Download all dependencies
RUN mvn dependency:go-offline -B

# Copy the source code
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:15-slim
WORKDIR /app

# Copy the built artifacts from the build stage
COPY --from=build /app/webparent/*/target/*.jar ./app.jar

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE="prod"

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 