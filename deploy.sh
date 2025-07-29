#!/bin/bash

# Exit script immediately if a command fails
set -e

# Load environment variables
source .env

# --- Build Backend ---
echo "--- Building backend application... ---"
cd backend
./mvnw clean package -DskipTests
cd ..

# --- Build Frontend ---
echo "--- Building frontend application... ---"
cd frontend
npm install
VITE_API_URL="http://$REMOTE_SSH_HOST" npm run build
cd ..


# --- Prepare Remote Directories ---
echo "--- Setting permissions on $REMOTE_SSH_HOST... ---"
# This command now prepares both backend and frontend directories
FIX_PERMS_COMMAND="echo '$REMOTE_SSH_PASSWORD' | sudo -S sh -c ' \
  mkdir -p /opt/pdds-app && chown $REMOTE_SSH_USERNAME:$REMOTE_SSH_USERNAME /opt/pdds-app; \
  mkdir -p /var/www/html/pdds-app && chown $REMOTE_SSH_USERNAME:$REMOTE_SSH_USERNAME /var/www/html/pdds-app'"
sshpass -p "$REMOTE_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no "$REMOTE_SSH_USERNAME@$REMOTE_SSH_HOST" "$FIX_PERMS_COMMAND"


# --- Deploy Backend ---
echo "--- Deploying backend to $REMOTE_SSH_HOST... ---"
sshpass -p "$REMOTE_SSH_PASSWORD" scp -o StrictHostKeyChecking=no ./backend/target/backend-*.jar "$REMOTE_SSH_USERNAME@$REMOTE_SSH_HOST:/opt/pdds-app/backend.jar"


# --- Deploy Frontend ---
echo "--- Deploying frontend to $REMOTE_SSH_HOST... ---"
# The -r flag recursively copies the contents of the 'dist' directory
sshpass -p "$REMOTE_SSH_PASSWORD" scp -r -o StrictHostKeyChecking=no ./frontend/dist/* "$REMOTE_SSH_USERNAME@$REMOTE_SSH_HOST:/var/www/html/pdds-app/"


# --- Restart Backend Service ---
echo "--- Restarting backend service... ---"
REMOTE_COMMAND="echo '$REMOTE_SSH_PASSWORD' | sudo -S bash -c 'systemctl daemon-reload && systemctl restart backend.service'"
sshpass -p "$REMOTE_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no "$REMOTE_SSH_USERNAME@$REMOTE_SSH_HOST" "$REMOTE_COMMAND"

# --- Copy scripts ---
echo "--- Deploying scripts to $REMOTE_SSH_HOST... ---"
sshpass -p "$REMOTE_SSH_PASSWORD" rm -rf ~/scripts
sshpass -p "$REMOTE_SSH_PASSWORD" scp -r -o StrictHostKeyChecking=no ./scripts/* "$REMOTE_SSH_USERNAME@$REMOTE_SSH_HOST:~/scripts"

echo "--- Deployment finished successfully! ---"
