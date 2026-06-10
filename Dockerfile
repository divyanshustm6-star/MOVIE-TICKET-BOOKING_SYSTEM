# Stage 1: Build the Spring Boot application using Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the source code and configuration files
COPY pom.xml .
COPY src ./src

# Build the application and skip tests to speed up the process
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled .jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 9090

# Command to execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]