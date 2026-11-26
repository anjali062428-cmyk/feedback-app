# Stage 1: Build the application with Maven and Java 17
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies/plugins if any
COPY pom.xml .

# Copy source code and resources
COPY src ./src

# Build the package (creates feedback-app.jar)
RUN mvn -B clean package

# Stage 2: Minimal Java 17 JRE runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/feedback-app.jar feedback-app.jar

# Expose HTTP port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "feedback-app.jar"]
