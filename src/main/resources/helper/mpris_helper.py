#!/usr/bin/env python3
import http.server
import json
import os
import subprocess
import sys
import tempfile

PORT = 38472

def get_mpris_data():
    try:
        proc = subprocess.run([
            "playerctl", "metadata",
            "--format", "{{status}}:::{{artist}}:::{{title}}:::{{album}}:::{{position}}:::{{mpris:length}}:::{{mpris:artUrl}}"
        ], capture_output=True, text=True, timeout=1.0)
        if proc.returncode != 0 or not proc.stdout.strip():
            return {"isActive": False, "status": "stopped"}
        parts = proc.stdout.strip().split(":::")
        if len(parts) < 3:
            return {"isActive": False, "status": "stopped"}
        status_raw = parts[0].strip().lower()
        status = "playing" if status_raw == "playing" else ("paused" if status_raw == "paused" else "stopped")
        artist = parts[1].strip()
        title = parts[2].strip()
        album = parts[3].strip() if len(parts) > 3 else ""
        pos_ms = 0
        if len(parts) > 4 and parts[4].strip().isdigit():
            pos_ms = max(0, int(parts[4].strip()) // 1000)
        dur_ms = 0
        if len(parts) > 5 and parts[5].strip().isdigit():
            dur_ms = max(0, int(parts[5].strip()) // 1000)
        art_url = parts[6].strip() if len(parts) > 6 else ""
        cover_hash = hex(abs(hash(art_url)))[2:] if art_url else None
        data = {
            "isActive": bool(title or artist),
            "title": title,
            "artist": artist,
            "album": album,
            "status": status,
            "positionMs": pos_ms,
            "durationMs": dur_ms,
            "platform": "player",
            "platformName": "Linux Media"
        }
        if cover_hash:
            data["coverHash"] = cover_hash
        return data
    except (subprocess.SubprocessError, OSError) as err:
        sys.stderr.write(f"MPRIS query error: {err}\n")
        return {"isActive": False, "status": "stopped"}

class MprisHandler(http.server.BaseHTTPRequestHandler):
    ACTIONS = {
        "/control/play-pause": ["playerctl", "play-pause"],
        "/control/next": ["playerctl", "next"],
        "/control/previous": ["playerctl", "previous"],
    }

    def log_message(self, format, *args):
        pass

    def do_GET(self):
        if self.path == "/now-playing":
            data = get_mpris_data()
            resp = json.dumps(data).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Content-Length", str(len(resp)))
            self.end_headers()
            self.wfile.write(resp)
        elif self.path == "/cover":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            resp = b"{}"
            self.send_header("Content-Length", str(len(resp)))
            self.end_headers()
            self.wfile.write(resp)
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        cmd = self.ACTIONS.get(self.path)
        if cmd:
            subprocess.run(cmd, capture_output=True)
            self.send_response(200)
            self.end_headers()
            return
        if self.path == "/shutdown":
            self.send_response(200)
            self.end_headers()
            sys.exit(0)
        self.send_response(404)
        self.end_headers()

def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else PORT
    tmp_dir = tempfile.gettempdir()
    port_file = os.path.join(tmp_dir, "yandexmusichud_port.txt")
    pid_file = os.path.join(tmp_dir, "yandexmusichud_pid.txt")
    try:
        server = http.server.HTTPServer(("127.0.0.1", port), MprisHandler)
    except OSError:
        server = http.server.HTTPServer(("127.0.0.1", 0), MprisHandler)
        port = server.server_port
    with open(port_file, "w", encoding="utf-8") as f:
        f.write(str(port))
    with open(pid_file, "w", encoding="utf-8") as f:
        f.write(str(os.getpid()))
    try:
        server.serve_forever()
    finally:
        for cleanup_path in (port_file, pid_file):
            try:
                if os.path.exists(cleanup_path):
                    os.remove(cleanup_path)
            except OSError as err:
                sys.stderr.write(f"Cleanup error for {cleanup_path}: {err}\n")

if __name__ == "__main__":
    main()
