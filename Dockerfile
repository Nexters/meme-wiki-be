FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the specific JAR file
COPY app.jar app.jar

# Verify the JAR file exists and has content
RUN ls -la app.jar && \
    if [ ! -s app.jar ]; then echo "Warning: app.jar is empty"; fi

EXPOSE 8080

# Use exec form of ENTRYPOINT for proper signal handling
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
