# filepath: /sparqle-demo/sparqle-demo/backend/Dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the entire source code and resources
COPY target/order-link-*.jar /app/app.jar

# Set the command to run the Spring Boot application
CMD ["java", "-jar", "/app/app.jar"]

EXPOSE 8080