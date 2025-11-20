# Build stage
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app

# Copy gradle configuration files first (better caching)
COPY build.gradle settings.gradle ./
COPY gradle.properties* ./

# Copy all module build.gradle files to cache dependencies
COPY common/build.gradle ./common/
COPY domain/build.gradle ./domain/
COPY certificate-manager/build.gradle ./certificate-manager/
COPY ai-assistant/build.gradle ./ai-assistant/
COPY api/build.gradle ./api/

# Download dependencies (this layer will be cached unless build files change)
RUN gradle build --no-daemon -x test -x compileJava -x compileTestJava || return 0

# Now copy all source code
COPY common/src ./common/src
COPY domain/src ./domain/src
COPY certificate-manager/src ./certificate-manager/src
COPY ai-assistant/src ./ai-assistant/src
COPY api/src ./api/src

# Build the application
RUN gradle :api:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/api/build/libs/*.jar app.jar

# Set timezone
ENV TZ=Asia/Seoul

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Xmx2048m", "-Xms1024m", "-jar", "app.jar"]
