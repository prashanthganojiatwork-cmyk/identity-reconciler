# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and build files first for better caching
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the fat JAR
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Add health check using wget (available in the JRE image)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/ui/input || exit 1

# Set JVM memory flags appropriate for free-tier EC2 (t2.micro)
# Activate the prod Spring profile
ENTRYPOINT ["java", "-Xmx384m", "-Xms128m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
