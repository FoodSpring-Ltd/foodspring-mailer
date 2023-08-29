# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the packaged Spring Boot JAR file into the container at /app
COPY target/foodspring-mailer-0.0.1-SNAPSHOT.jar /app/app.jar

# Specify the command to run your application
CMD ["java", "-jar", "app.jar"]
