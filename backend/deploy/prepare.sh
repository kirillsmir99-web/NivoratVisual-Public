#!/bin/sh
set -eu
cd /opt/nv-backend
backup=/opt/nv-backend/backups/20260930-social-v2
if [ -e "$backup" ]; then echo 'Backup already exists; refusing to overwrite it'; exit 1; fi
install -d -m 700 "$backup"
cp -a server.py "$backup/server.py"
cp -a /etc/systemd/system/nv-backend.service "$backup/nv-backend.service"
cp -a /etc/nginx/conf.d/virion-license.conf "$backup/virion-license.conf"
curl --fail --max-time 5 'http://127.0.0.1:8080/api/messages?since=0' > "$backup/legacy-irc-private.json"
chmod 600 "$backup/legacy-irc-private.json"
python3 -m venv /opt/nv-backend/venv
/opt/nv-backend/venv/bin/python3 -m pip install -r /opt/nv-backend/staged/requirements.txt httpx==0.28.1 > "$backup/venv-install.log" 2>&1
getent passwd nv-social > /dev/null || useradd --system --home-dir /var/lib/nv-backend --shell /usr/sbin/nologin nv-social
systemd-run --unit=nv-social-staging --property=User=nv-social --property=Group=nv-social --property=StateDirectory=nv-social-staging --property=StateDirectoryMode=0700 --property=WorkingDirectory=/opt/nv-backend/staged --setenv=NV_DB=/var/lib/nv-social-staging/nv-social.sqlite3 --setenv=NV_PORT=18818 /opt/nv-backend/venv/bin/python3 /opt/nv-backend/staged/server.py
echo 'Backup saved; isolated loopback staging started'
