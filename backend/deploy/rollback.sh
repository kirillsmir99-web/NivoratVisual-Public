#!/bin/sh
set -eu
backup=/opt/nv-backend/backups/20260930-social-v2
test -f "$backup/server.py"
# The current database remains in /var/lib/nv-backend for a later repair.
install -m 644 "$backup/server.py" /opt/nv-backend/server.py
install -m 644 "$backup/nv-backend.service" /etc/systemd/system/nv-backend.service
install -m 644 "$backup/virion-license.conf" /etc/nginx/conf.d/virion-license.conf
nginx -t
systemctl daemon-reload
systemctl restart nv-backend.service
systemctl reload nginx.service
