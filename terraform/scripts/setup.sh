#!/bin/bash
set -euo pipefail

sleep 15

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y nginx certbot python3-certbot-nginx openssl ufw

ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

systemctl enable nginx
systemctl start nginx

mkdir -p /etc/nginx/certs

# DuckDNS dynamic update (optional)
if [ -n "${duckdns_domain}" ] && [ -n "${duckdns_token}" ]; then
  curl -s "https://www.duckdns.org/update?domains=${duckdns_domain}&token=${duckdns_token}&ip=" >/dev/null || true
fi

# Generate Let's Encrypt certificate when a DuckDNS domain is provided
if [ -n "${duckdns_domain}" ] && [ -n "${letsencrypt_email}" ]; then
  certbot --nginx -d "${duckdns_domain}" --non-interactive --agree-tos -m "${letsencrypt_email}" --redirect || true
fi

cat > /etc/nginx/sites-available/quarkus <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name ${duckdns_domain};

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Forwarded-Proto http;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF

ln -sf /etc/nginx/sites-available/quarkus /etc/nginx/sites-enabled/quarkus
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

mkdir -p /home/ubuntu/app
chown ubuntu:ubuntu /home/ubuntu/app
