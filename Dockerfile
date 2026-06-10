# Stage 1: Build the Spring Boot application using Maven Wrapper
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the maven wrapper files and project configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Fix line endings for linux execution and ensure execution permissions
RUN tr -d '\r' < mvnw > mvnw.linux && mv mvnw.linux mvnw && chmod +x mvnw

# Copy the source code
COPY src ./src

# Build the application using the wrapper
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled .jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 9090

# Command to execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]