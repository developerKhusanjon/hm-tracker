FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file
COPY target/scala-3.3.1/hm-tracker-assembly-0.1.0-SNAPSHOT.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV DB_URL="jdbc:postgresql://postgres:5432/hm_tracker"
ENV DB_USER="postgres"
ENV DB_PASSWORD="password"

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]