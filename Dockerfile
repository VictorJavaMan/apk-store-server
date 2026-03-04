# Build stage
FROM gradle:8.8-jdk17 AS build
WORKDIR /app

# Copy gradle files first for caching
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy source code
COPY server server
COPY shared shared

# Build the application
RUN chmod +x gradlew && ./gradlew :server:buildFatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/server/build/libs/*-all.jar app.jar

# Copy web files
COPY server/web web

# Create directories for data persistence
RUN mkdir -p data uploads

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
