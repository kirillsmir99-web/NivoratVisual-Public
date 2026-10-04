#!/bin/sh
set -eu
cd /opt/nv-backend
revision=$(date -u +%Y%m%dT%H%M%SZ)
backup=/opt/nv-backend/backups/update-$revision
install -d -m 700 "$backup"
cp -a server.py "$backup/server.py"
if [ -f /var/lib/nv-backend/nv-social.sqlite3 ]; then
    python3 - "$backup" <<'PY'
import sqlite3,sys
with sqlite3.connect('/var/lib/nv-backend/nv-social.sqlite3') as source:
    with sqlite3.connect(sys.argv[1]+'/nv-social.sqlite3') as target: source.backup(target)
PY
fi
install -m 644 staged/server.py server.py.part
mv server.py.part server.py
if ! systemctl restart nv-backend.service; then
    install -m 644 "$backup/server.py" server.py
    systemctl restart nv-backend.service
    exit 1
fi
healthy=false
for attempt in 1 2 3 4 5; do
    if curl --fail --silent --max-time 2 http://127.0.0.1:8080/health >/dev/null; then
        healthy=true
        break
    fi
    sleep 1
done
if [ "$healthy" != true ]; then
    install -m 644 "$backup/server.py" server.py
    systemctl restart nv-backend.service
    echo "Health check failed; previous source restored, SQLite retained" >&2
    exit 1
fi
sha256sum server.py
echo "Previous source and SQLite snapshot: $backup"
