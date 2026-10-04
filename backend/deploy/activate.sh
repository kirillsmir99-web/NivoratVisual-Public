#!/bin/sh
set -eu
backup=/opt/nv-backend/backups/20260930-social-v2
config=/etc/nginx/conf.d/virion-license.conf
test -f "$backup/server.py"
expected=3a87689fe609f44d69cd04e60645de10017bcc0c7c79a0e3aff731a0aff0981e
actual=$(sha256sum "$config" | cut -d ' ' -f 1)
test "$expected" = "$actual" || { echo 'Nginx config changed; refusing to overwrite'; exit 1; }
rollback() {
    install -m 644 "$backup/server.py" /opt/nv-backend/server.py
    install -m 644 "$backup/nv-backend.service" /etc/systemd/system/nv-backend.service
    install -m 644 "$backup/virion-license.conf" "$config"
    systemctl daemon-reload
    systemctl restart nv-backend.service
    nginx -t && systemctl reload nginx.service
}
trap 'rollback' HUP INT TERM
python3 - <<'PY'
from pathlib import Path
p=Path('/etc/nginx/conf.d/virion-license.conf')
s=p.read_text()
anchor='    location = /health {'
assert s.count(anchor)==1
p.write_text(s.replace(anchor, '    include /etc/nginx/snippets/nv-social.conf;\n\n'+anchor))
PY
install -m 644 /opt/nv-backend/staged/nv-social.conf /etc/nginx/snippets/nv-social.conf
if ! nginx -t; then rollback; exit 1; fi
install -m 644 /opt/nv-backend/staged/server.py /opt/nv-backend/server.py
install -m 644 /opt/nv-backend/staged/nv-backend.service /etc/systemd/system/nv-backend.service
systemctl daemon-reload
if ! systemctl restart nv-backend.service; then rollback; exit 1; fi
systemctl reload nginx.service
ready=false
for attempt in 1 2 3 4 5; do
    if curl --silent --fail --max-time 3 http://127.0.0.1:8080/health | grep -q 'nv-social-2'; then ready=true; break; fi
    sleep 1
done
if [ "$ready" != true ]; then rollback; exit 1; fi
systemctl stop nv-social-staging.service
trap - HUP INT TERM
sha256sum /opt/nv-backend/server.py /etc/systemd/system/nv-backend.service /etc/nginx/snippets/nv-social.conf
systemctl show nv-backend.service -p ActiveState -p SubState -p User -p MainPID
curl --silent --show-error --fail --max-time 10 https://virion.185-56-162-195.sslip.io/nv/health
