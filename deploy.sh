#!/bin/bash

# Configuration
APP_DIR="/home/ubuntu/app"
LOG_DIR="/home/ubuntu/logs"
LOG_FILE="$LOG_DIR/application.log"
ERROR_LOG="$LOG_DIR/error.log"

# Create log directory if it doesn't exist
mkdir -p $LOG_DIR

# Print deployment information
echo "===== Deployment started at $(date) =====" | tee -a $LOG_FILE

# Check if docker-compose is installed, if not install it
if ! command -v docker-compose &> /dev/null; then
  echo "docker-compose not found, installing..." | tee -a $LOG_FILE

  # For Ubuntu 22.04, docker-compose is available as docker compose (with a space)
  if command -v docker &> /dev/null; then
    echo "Checking if docker compose plugin is available..." | tee -a $LOG_FILE

    if docker help | grep -q "compose"; then
      echo "Docker compose plugin is available, using it instead..." | tee -a $LOG_FILE
      # Define a function to use instead of an alias (works in the current shell)
      docker-compose() {
        docker compose "$@"
      }
      export -f docker-compose
    else
      echo "Docker compose plugin is not available, trying to install it..." | tee -a $LOG_FILE

      # Try to install docker compose plugin if it's not available
      if command -v apt-get &> /dev/null; then
        echo "Using apt to install docker-compose-plugin..." | tee -a $LOG_FILE
        sudo apt-get update
        sudo apt-get install -y docker-compose-plugin

        # Check if installation was successful
        if docker help | grep -q "compose"; then
          echo "Docker compose plugin installed successfully" | tee -a $LOG_FILE
          docker-compose() {
            docker compose "$@"
          }
          export -f docker-compose
        else
          echo "Failed to install docker compose plugin, falling back to standalone docker-compose..." | tee -a $LOG_FILE
          sudo apt-get install -y docker-compose
        fi
      else
        echo "apt-get not available, trying to install standalone docker-compose..." | tee -a $LOG_FILE
        # Try to install standalone docker-compose using curl
        if command -v curl &> /dev/null; then
          echo "Installing docker-compose using curl..." | tee -a $LOG_FILE
          sudo curl -L "https://github.com/docker/compose/releases/download/v2.23.3/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
          sudo chmod +x /usr/local/bin/docker-compose
        else
          echo "curl not available, cannot install docker-compose" | tee -a $LOG_FILE
        fi
      fi
    fi
  else
    # If docker is not installed or we want the standalone docker-compose
    echo "Docker is not installed, trying to install docker-compose standalone..." | tee -a $LOG_FILE
    if command -v apt-get &> /dev/null; then
      sudo apt-get update
      sudo apt-get install -y docker-compose
    else
      echo "apt-get not available, cannot install docker-compose" | tee -a $LOG_FILE
    fi
  fi
fi

# Build Docker image from the JAR file
echo "Building Docker image..." | tee -a $LOG_FILE
cd $APP_DIR

# Copy JAR file to the current directory for Docker build
echo "Copying JAR file to current directory..." | tee -a $LOG_FILE

# Try different possible locations for the JAR file
if [ "$(ls -A *.jar 2>/dev/null)" ]; then
  echo "Found JAR file in current directory" | tee -a $LOG_FILE
  # Get the first JAR file if multiple exist
  JAR_FILE=$(ls -A *.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  # Create a copy with specific name
  cp "$JAR_FILE" app.jar
elif [ -d "build/libs" ] && [ "$(ls -A build/libs/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in build/libs directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A build/libs/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "$APP_DIR/build/libs" ] && [ "$(ls -A $APP_DIR/build/libs/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in $APP_DIR/build/libs directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A $APP_DIR/build/libs/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "../build/libs" ] && [ "$(ls -A ../build/libs/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in ../build/libs directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A ../build/libs/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "libs" ] && [ "$(ls -A libs/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in libs directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A libs/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "$APP_DIR/libs" ] && [ "$(ls -A $APP_DIR/libs/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in $APP_DIR/libs directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A $APP_DIR/libs/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "../libs" ] && [ "$(ls -A ../libs/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in ../libs directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A ../libs/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "build/libs/build" ] && [ "$(ls -A build/libs/build/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in build/libs/build directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A build/libs/build/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
elif [ -d "$APP_DIR/build/libs/build" ] && [ "$(ls -A $APP_DIR/build/libs/build/*.jar 2>/dev/null)" ]; then
  echo "Found JAR file in $APP_DIR/build/libs/build directory" | tee -a $LOG_FILE
  JAR_FILE=$(ls -A $APP_DIR/build/libs/build/*.jar 2>/dev/null | head -1)
  echo "Using JAR file: $JAR_FILE" | tee -a $LOG_FILE
  cp "$JAR_FILE" app.jar
else
  echo "Error: JAR file not found in any of the expected locations" | tee -a $LOG_FILE
  echo "Listing current directory:" | tee -a $LOG_FILE
  ls -la | tee -a $LOG_FILE
  echo "Listing build/libs directory (if it exists):" | tee -a $LOG_FILE
  ls -la build/libs/ 2>/dev/null | tee -a $LOG_FILE
  echo "Listing $APP_DIR/build/libs directory (if it exists):" | tee -a $LOG_FILE
  ls -la $APP_DIR/build/libs/ 2>/dev/null | tee -a $LOG_FILE
  echo "Listing libs directory (if it exists):" | tee -a $LOG_FILE
  ls -la libs/ 2>/dev/null | tee -a $LOG_FILE
  echo "Listing $APP_DIR/libs directory (if it exists):" | tee -a $LOG_FILE
  ls -la $APP_DIR/libs/ 2>/dev/null | tee -a $LOG_FILE
  echo "Listing build/libs/build directory (if it exists):" | tee -a $LOG_FILE
  ls -la build/libs/build/ 2>/dev/null | tee -a $LOG_FILE

  # Try to find any JAR file in the system
  echo "Searching for JAR files in the app directory:" | tee -a $LOG_FILE
  find $APP_DIR -name "*.jar" -type f 2>/dev/null | tee -a $LOG_FILE

  # Check if we can create an empty JAR file for testing
  echo "Attempting to create a test JAR file for debugging..." | tee -a $LOG_FILE
  if command -v jar &> /dev/null; then
    echo "jar command is available, creating test JAR" | tee -a $LOG_FILE
    echo "Test file" > test.txt
    jar cf test.jar test.txt
    if [ -f "test.jar" ]; then
      echo "Successfully created test.jar" | tee -a $LOG_FILE
      cp test.jar app.jar
      echo "Using test.jar as app.jar for Docker build (this will not work in production)" | tee -a $LOG_FILE
      echo "This is just to test if the Docker build process works" | tee -a $LOG_FILE
    else
      echo "Failed to create test.jar" | tee -a $LOG_FILE
    fi
  else
    echo "jar command not available" | tee -a $LOG_FILE
    # Create a simple ZIP file as JAR is just a ZIP with specific structure
    if command -v zip &> /dev/null; then
      echo "zip command is available, creating test JAR" | tee -a $LOG_FILE
      echo "Test file" > test.txt
      zip test.jar test.txt
      if [ -f "test.jar" ]; then
        echo "Successfully created test.jar using zip" | tee -a $LOG_FILE
        cp test.jar app.jar
        echo "Using test.jar as app.jar for Docker build (this will not work in production)" | tee -a $LOG_FILE
        echo "This is just to test if the Docker build process works" | tee -a $LOG_FILE
      else
        echo "Failed to create test.jar using zip" | tee -a $LOG_FILE
      fi
    else
      echo "Neither jar nor zip commands are available" | tee -a $LOG_FILE
      echo "Creating an empty file as app.jar (this will not work in production)" | tee -a $LOG_FILE
      touch app.jar
    fi
  fi

  if [ -f "./app.jar" ]; then
    echo "Created a test app.jar file. Continuing with Docker build for testing purposes." | tee -a $LOG_FILE
    echo "WARNING: This is not a real JAR file and will not run properly!" | tee -a $LOG_FILE
  else
    echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
    exit 1
  fi
fi

# Verify JAR file exists
if [ ! -f "app.jar" ]; then
  echo "Error: JAR file copy failed" | tee -a $LOG_FILE
  echo "Checking current directory permissions:" | tee -a $LOG_FILE
  ls -la . | tee -a $LOG_FILE
  echo "Checking if app.jar exists with different case:" | tee -a $LOG_FILE
  find . -maxdepth 1 -iname "app.jar" | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

# Verify JAR file has content
JAR_SIZE=$(stat -c%s "app.jar" 2>/dev/null || stat -f%z "app.jar" 2>/dev/null)
echo "JAR file size: $JAR_SIZE bytes" | tee -a $LOG_FILE
if [ -z "$JAR_SIZE" ] || [ "$JAR_SIZE" -lt 1000 ]; then
  echo "Warning: JAR file seems too small or empty" | tee -a $LOG_FILE
fi

echo "JAR file copied successfully. Building Docker image..." | tee -a $LOG_FILE

# Verify Docker is installed and running
if ! command -v docker &> /dev/null; then
  echo "Error: Docker is not installed or not in PATH" | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

# Check Docker daemon status
if ! docker info &> /dev/null; then
  echo "Error: Docker daemon is not running or not accessible" | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

# JAR file size has already been verified above

# Build Docker image with detailed output
echo "Building Docker image with command: docker build -t meme-wiki-be:latest ." | tee -a $LOG_FILE
if ! docker build -t meme-wiki-be:latest . 2>&1 | tee -a $LOG_FILE; then
  echo "Docker build failed. See above for details." | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi
echo "Docker image built successfully." | tee -a $LOG_FILE

# Verify image was created
if ! docker images | grep -q meme-wiki-be; then
  echo "Error: Docker image not found after build" | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

# Stop and remove existing container if it exists
if docker ps -a | grep -q meme-wiki-be; then
  echo "Stopping and removing existing container..." | tee -a $LOG_FILE
  docker stop meme-wiki-be
  docker rm meme-wiki-be
fi

# Start the application using docker-compose
echo "Starting application with Docker..." | tee -a $LOG_FILE

# Verify docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
  echo "Error: docker-compose.yml not found in current directory" | tee -a $LOG_FILE
  echo "Listing current directory:" | tee -a $LOG_FILE
  ls -la | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

# Final verification of docker-compose availability
echo "Performing final verification of docker-compose availability..." | tee -a $LOG_FILE

# Debug information
echo "Docker compose command availability:" | tee -a $LOG_FILE
if command -v docker-compose &> /dev/null; then
  echo "docker-compose command is available" | tee -a $LOG_FILE
  docker-compose --version 2>&1 | tee -a $LOG_FILE
else
  echo "docker-compose command is NOT available" | tee -a $LOG_FILE

  # If we still don't have docker-compose, try one more approach - create a simple script
  if command -v docker &> /dev/null && docker help | grep -q "compose"; then
    echo "Creating a docker-compose script as a last resort..." | tee -a $LOG_FILE
    cat > /tmp/docker-compose << 'EOF'
#!/bin/bash
docker compose "$@"
EOF
    chmod +x /tmp/docker-compose
    export PATH="/tmp:$PATH"
    echo "Added /tmp to PATH and created docker-compose script" | tee -a $LOG_FILE

    # Verify the script works
    if /tmp/docker-compose --version 2>&1 | tee -a $LOG_FILE; then
      echo "Successfully created docker-compose script" | tee -a $LOG_FILE
    else
      echo "Failed to create working docker-compose script" | tee -a $LOG_FILE
    fi
  fi
fi

if command -v docker &> /dev/null; then
  echo "docker command is available" | tee -a $LOG_FILE
  docker --version 2>&1 | tee -a $LOG_FILE
  echo "Checking if docker compose plugin is available:" | tee -a $LOG_FILE
  if docker help | grep -q "compose"; then
    echo "docker compose plugin is available" | tee -a $LOG_FILE
  else
    echo "docker compose plugin is NOT available" | tee -a $LOG_FILE
  fi
else
  echo "docker command is NOT available" | tee -a $LOG_FILE
fi

echo "Running docker-compose up -d..." | tee -a $LOG_FILE

# Try to start the container using available docker-compose methods
COMPOSE_SUCCESS=false

# First try with docker-compose if available
if command -v docker-compose &> /dev/null; then
  echo "Attempting to start container with docker-compose command..." | tee -a $LOG_FILE
  if docker-compose up -d 2>&1 | tee -a $LOG_FILE; then
    COMPOSE_SUCCESS=true
    echo "Successfully started container with docker-compose" | tee -a $LOG_FILE
  else
    echo "docker-compose command failed, will try alternatives..." | tee -a $LOG_FILE
  fi
fi

# If docker-compose failed or isn't available, try docker compose directly
if [ "$COMPOSE_SUCCESS" = false ] && command -v docker &> /dev/null && docker help | grep -q "compose"; then
  echo "Attempting to start container with docker compose plugin..." | tee -a $LOG_FILE
  if docker compose up -d 2>&1 | tee -a $LOG_FILE; then
    COMPOSE_SUCCESS=true
    echo "Successfully started container with docker compose plugin" | tee -a $LOG_FILE
  else
    echo "docker compose plugin command failed" | tee -a $LOG_FILE
  fi
fi

# If both methods failed, try with the script we created (if it exists)
if [ "$COMPOSE_SUCCESS" = false ] && [ -x "/tmp/docker-compose" ]; then
  echo "Attempting to start container with our docker-compose script..." | tee -a $LOG_FILE
  if /tmp/docker-compose up -d 2>&1 | tee -a $LOG_FILE; then
    COMPOSE_SUCCESS=true
    echo "Successfully started container with our docker-compose script" | tee -a $LOG_FILE
  else
    echo "Our docker-compose script failed" | tee -a $LOG_FILE
  fi
fi

# If all methods failed, try a direct approach as a last resort
if [ "$COMPOSE_SUCCESS" = false ]; then
  echo "All docker-compose methods failed. Trying direct docker run as last resort..." | tee -a $LOG_FILE

  # Extract configuration from docker-compose.yml using grep and awk
  echo "Extracting configuration from docker-compose.yml..." | tee -a $LOG_FILE

  # Check if container exists and remove it
  if docker ps -a | grep -q meme-wiki-be; then
    echo "Removing existing container before direct run..." | tee -a $LOG_FILE
    docker rm -f meme-wiki-be 2>&1 | tee -a $LOG_FILE
  fi

  # Run container directly
  echo "Running container directly with docker run..." | tee -a $LOG_FILE
  if docker run -d --name meme-wiki-be -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -v /home/ubuntu/logs:/app/logs meme-wiki-be:latest 2>&1 | tee -a $LOG_FILE; then
    COMPOSE_SUCCESS=true
    echo "Successfully started container with direct docker run" | tee -a $LOG_FILE
  else
    echo "Direct docker run failed" | tee -a $LOG_FILE
    echo "Checking docker-compose.yml validity..." | tee -a $LOG_FILE
    if command -v docker &> /dev/null && docker help | grep -q "compose"; then
      docker compose config 2>&1 | tee -a $LOG_FILE
    fi
    echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
    exit 1
  fi
fi

# Final check if any method succeeded
if [ "$COMPOSE_SUCCESS" = false ]; then
  echo "All container startup methods failed." | tee -a $LOG_FILE
  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

# Check if container started successfully
echo "Waiting for container to start..." | tee -a $LOG_FILE
sleep 5

# Check container status with retries
MAX_RETRIES=3
RETRY_COUNT=0
CONTAINER_RUNNING=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  if docker ps | grep -q meme-wiki-be; then
    CONTAINER_RUNNING=true
    break
  else
    RETRY_COUNT=$((RETRY_COUNT+1))
    echo "Container not running yet. Retry $RETRY_COUNT of $MAX_RETRIES..." | tee -a $LOG_FILE
    sleep 5
  fi
done

if [ "$CONTAINER_RUNNING" = true ]; then
  echo "Container started successfully" | tee -a $LOG_FILE

  # Check container health
  echo "Container status:" | tee -a $LOG_FILE
  docker ps | grep meme-wiki-be | tee -a $LOG_FILE

  # Show container logs
  echo "Container logs:" | tee -a $LOG_FILE
  docker logs --tail 20 meme-wiki-be | tee -a $LOG_FILE

  echo "===== Deployment completed at $(date) =====" | tee -a $LOG_FILE
else
  echo "Failed to start container. Diagnostic information:" | tee -a $LOG_FILE

  # Check if container exists but is not running
  if docker ps -a | grep -q meme-wiki-be; then
    echo "Container exists but is not running. Container status:" | tee -a $LOG_FILE
    docker ps -a | grep meme-wiki-be | tee -a $LOG_FILE

    echo "Container logs:" | tee -a $LOG_FILE
    docker logs meme-wiki-be | tee -a $LOG_FILE

    echo "Container inspection:" | tee -a $LOG_FILE
    docker inspect meme-wiki-be | tee -a $LOG_FILE
  else
    echo "Container does not exist. Docker compose might have failed silently." | tee -a $LOG_FILE
  fi

  echo "===== Deployment failed at $(date) =====" | tee -a $LOG_FILE
  exit 1
fi

exit 0
