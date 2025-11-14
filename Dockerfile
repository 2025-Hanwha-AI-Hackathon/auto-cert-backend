# Build stage
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app

# Copy gradle configuration files
COPY build.gradle settings.gradle ./

# Copy all module source code
COPY common ./common
COPY domain ./domain
COPY certificate-manager ./certificate-manager
COPY api ./api

# Build the application using Docker image's gradle (no wrapper download needed)
RUN gradle :api:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from build stage (use wildcard to match any version)
COPY --from=build /app/api/build/libs/*.jar app.jar

# Set timezone (can be overridden by TZ environment variable)
ENV TZ=Asia/Seoul

# Expose port (Railway will override with PORT env var)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Xmx1024m", "-Xms512m", "-jar", "app.jar"]
