#!/bin/bash

# Configuration
MATCH_PORT="4567"
BACKEND_DIR="java-server"
SDK_DIR="stripe-terminal-react-native"
APP_DIR="stripe-terminal-react-native/example-app"
LOCAL_IP="10.0.0.237"

echo "=================================================="
echo "      Stripe Terminal E2E Automation Setup        "
echo "=================================================="

# 1. Kill any process on port 4567
echo "[1/5] Checking for rogue server instances on port $MATCH_PORT..."
PID=$(lsof -ti :$MATCH_PORT)
if [ -n "$PID" ]; then
  echo "Found process $PID using port $MATCH_PORT. Killing it..."
  kill -9 $PID
else
  echo "Port $MATCH_PORT is free."
fi

# 2. Start Java Backend
echo "[2/5] Starting Java Backend..."
cd $BACKEND_DIR
# Build and run in background
./mvnw clean package -DskipTests > /dev/null 2>&1
echo "Backend build complete. Starting server..."
./mvnw spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend started with PID $BACKEND_PID. Logs redirected to backend.log."
cd ..

# Wait for server to be responsive
echo "Waiting for backend to be ready..."
RETRIES=0
while ! curl -s "http://localhost:$MATCH_PORT/connection_token" > /dev/null; do
  sleep 2
  RETRIES=$((RETRIES+1))
  if [ $RETRIES -ge 30 ]; then
    echo "Backend failed to start in 60 seconds. Check backend.log."
    exit 1
  fi
  echo -n "."
done
echo " Backend is UP!"

# 3. Setup React Native SDK
echo "[3/5] Setting up React Native SDK..."
cd $SDK_DIR
if ! command -v yarn &> /dev/null; then
    echo "Yarn not found. Attempting to use npm to install dependencies..."
    npm install
else
    yarn bootstrap
fi
cd ..

# 4. Setup & Launch Example App
echo "[4/5] Setting up Example App..."
cd $APP_DIR

# Ensure .env is correct
echo "API_URL=http://$LOCAL_IP:$MATCH_PORT" > .env
echo "Updated .env with local server IP."

if ! command -v yarn &> /dev/null; then
    echo "Yarn not found. Using npm..."
    npm install
else
    yarn install
fi

# Clean Android Build
echo "Cleaning Android build..."
cd android
./gradlew clean > /dev/null 2>&1
cd ..

echo "Launching Android App..."
if ! command -v yarn &> /dev/null; then
    npm run android
else
    yarn android
fi

echo "=================================================="
echo "      Setup Complete! App should launch soon.     "
echo "=================================================="
