# VRP Planner Infrastructure

This directory contains the nginx configuration and deployment scripts for the VRP Planner application.

## Files

- `nginx.conf` - Nginx server configuration
- `deploy.sh` - Automated deployment script
- `README.md` - This file

## Quick Deployment

1. **Run the deployment script:**
   ```bash
   sudo ./deploy.sh
   ```

2. **Build and deploy the frontend:**
   ```bash
   cd ../frontend
   npm run build
   sudo cp -r dist/* /var/www/vrp-planner-frontend/
   ```

3. **Start the backend:**
   ```bash
   cd ../backend
   ./mvnw spring-boot:run
   ```

4. **Access the application:**
   - URL: http://200.16.7.180
   - API: http://200.16.7.180/api/
   - WebSocket: ws://200.16.7.180/ws/

## Manual Setup

If you prefer to set up nginx manually:

1. **Install nginx:**
   ```bash
   sudo apt update
   sudo apt install nginx
   ```

2. **Copy the configuration:**
   ```bash
   sudo cp nginx.conf /etc/nginx/sites-available/vrp-planner
   sudo ln -s /etc/nginx/sites-available/vrp-planner /etc/nginx/sites-enabled/
   sudo rm /etc/nginx/sites-enabled/default
   ```

3. **Test and reload:**
   ```bash
   sudo nginx -t
   sudo systemctl reload nginx
   ```

## Configuration Details

### Frontend
- Served from `/var/www/vrp-planner-frontend/`
- Single Page Application (SPA) routing
- Static asset caching enabled

### Backend API
- Proxied from `/api/` to `http://localhost:8080/api/`
- CORS headers configured
- Preflight request handling

### WebSocket
- Proxied from `/ws/` to `http://localhost:8080/ws/`
- WebSocket upgrade handling
- Long timeout for real-time communication

### Security
- Security headers enabled
- XSS protection
- Content type sniffing prevention
- Frame options

### Performance
- Gzip compression enabled
- Static asset caching
- Optimized for web applications

## Troubleshooting

### Check nginx status:
```bash
sudo systemctl status nginx
```

### View logs:
```bash
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Test configuration:
```bash
sudo nginx -t
```

### Restart nginx:
```bash
sudo systemctl restart nginx
```

## Firewall

Make sure port 80 is open:
```bash
sudo ufw allow 80
sudo ufw status
``` 