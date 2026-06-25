# C. Production Deployment Guide

Deploying to a VPS (Ubuntu 22.04 / Debian 12) from scratch.

---

## Infrastructure requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| CPU | 2 vCPU | 4 vCPU |
| RAM | 2 GB | 4 GB |
| Disk | 20 GB | 50 GB SSD |
| OS | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| Open ports | 22, 80, 443 | 22, 80, 443 |

---

## 1. Prepare the VPS

```bash
# Update system
sudo apt-get update && sudo apt-get upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# Install Docker Compose
sudo apt-get install -y docker-compose-plugin

# Verify
docker --version
docker compose version
```

---

## 2. Create deployment user

```bash
sudo adduser placement-deploy
sudo usermod -aG docker placement-deploy
```

---

## 3. Set up firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

---

## 4. Clone and configure

```bash
su - placement-deploy
git clone <repository-url> /opt/placement-platform
cd /opt/placement-platform/backend

# Generate JWT keys
bash scripts/generate-jwt-keys.sh   # or PowerShell on Windows runner

# Copy and fill environment template
cp .env.prod.template .env.prod
nano .env.prod
```

Required `.env.prod` values for production:

```bash
# Database
DB_URL=jdbc:postgresql://postgres:5432/placement_prod
DB_USERNAME=placement_user
DB_PASSWORD=<very-strong-random-password>

# JWT — paste PEM file contents, replace newlines with \n
JWT_PRIVATE_KEY_PEM=<PKCS8 RSA private key PEM>
JWT_PUBLIC_KEY_PEM=<X.509 RSA public key PEM>
JWT_ACCESS_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=604800000
JWT_ISSUER=placement-platform
JWT_AUDIENCE=placement-api

# CORS
CORS_ALLOWED_ORIGINS=https://your-domain.com

# File uploads
FILE_UPLOAD_DIR=/data/uploads
FILE_MAX_SIZE_BYTES=10485760
FILE_SCAN_ENABLED=true

# ClamAV
CLAMAV_HOST=clamav
CLAMAV_PORT=3310
CLAMAV_TIMEOUT_MS=30000

# Server
SERVER_PORT=8080
```

---

## 5. Set up SSL certificates (Let's Encrypt)

```bash
sudo apt-get install -y certbot
sudo certbot certonly --standalone -d your-domain.com

# Certificates are at:
# /etc/letsencrypt/live/your-domain.com/fullchain.pem
# /etc/letsencrypt/live/your-domain.com/privkey.pem

# Copy to nginx certs directory
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem /opt/placement-platform/nginx/certs/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem /opt/placement-platform/nginx/certs/key.pem
sudo chown -R placement-deploy:placement-deploy /opt/placement-platform/nginx/certs/
```

---

## 6. Build and deploy

```bash
cd /opt/placement-platform/backend

# Build JAR
.m2/apache-maven-3.9.6/bin/mvn clean package -DskipTests

# Start production stack
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

---

## 7. Verify deployment

```bash
# All containers up?
docker compose -f docker-compose.prod.yml ps

# Health check via HTTPS
curl https://your-domain.com/actuator/health

# App logs
docker compose -f docker-compose.prod.yml logs -f app

# PostgreSQL health
docker compose -f docker-compose.prod.yml exec postgres pg_isready -U placement_user
```

Expected health response:
```json
{"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

---

## 8. Database backup (run after deploy)

```bash
bash scripts/backup.sh
```

Or schedule via cron:
```bash
# Daily backup at 2 AM
echo "0 2 * * * placement-deploy /opt/placement-platform/backend/scripts/backup.sh >> /var/log/placement-backup.log 2>&1" | sudo crontab -u placement-deploy -
```

---

## 9. Enable cert auto-renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Add cron for renewal + nginx reload
echo "0 3 * * * root certbot renew --quiet && docker compose -f /opt/placement-platform/backend/docker-compose.prod.yml exec nginx nginx -s reload" | sudo tee /etc/cron.d/certbot-renew
```

---

## 10. Set up log rotation

```bash
sudo tee /etc/logrotate.d/placement-platform > /dev/null <<'EOF'
/var/log/placement-*.log {
    daily
    rotate 14
    compress
    missingok
    notifempty
}
EOF
```

---

## 11. Update deployment

```bash
cd /opt/placement-platform
git pull origin main

cd backend
.m2/apache-maven-3.9.6/bin/mvn clean package -DskipTests

# Rolling restart (zero downtime)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build app
```

---

## 12. Security hardening checklist

- [ ] Firewall allows only 22, 80, 443
- [ ] SSH key authentication only (no password)
- [ ] `.env.prod` has `chmod 600` permissions
- [ ] DB password is at least 32 random characters
- [ ] JWT RSA key is 2048+ bits
- [ ] HSTS enabled in production (`security.headers.hsts-enabled=true`)
- [ ] CORS allows only your domain
- [ ] Backup tested and verified
- [ ] Log rotation configured
- [ ] Cert auto-renewal tested

---

## Rollback procedure

```bash
# Get the last working image tag
docker images placement-platform

# Roll back to specific version
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-build
# OR tag and redeploy:
docker tag placement-platform:previous placement-platform:latest
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d app
```
