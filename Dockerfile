# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Install Maven for the build stage
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

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
FROM eclipse-temurin:17-jdk-jammy
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