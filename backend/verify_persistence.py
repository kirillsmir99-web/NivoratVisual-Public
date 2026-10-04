"""Restart an isolated temporary service; never touch the deployed database."""
import asyncio
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import tempfile

import httpx
from websockets.asyncio.client import connect
from verify_protocol import receive


async def verify():
    with tempfile.TemporaryDirectory(prefix="nv-restart-") as directory:
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]
        credentials = {"installation": "a57da1e2-6743-45dd-ab34-e87e0f571fd9", "secret": secrets.token_hex(32), "name": "NVpersist"}
        env = {**os.environ, "NV_PORT": str(port), "NV_DB": str(Path(directory)/"test.sqlite3")}
        with open(Path(directory)/"service.log", "wb") as log:
            def start():
                return subprocess.Popen([sys.executable, str(Path(__file__).with_name("server.py"))], env=env, stdout=log, stderr=log)
            async def ready(http):
                for _ in range(100):
                    try:
                        if (await http.get("/health")).status_code == 200:
                            return
                    except httpx.TransportError:
                        pass
                    await asyncio.sleep(.05)
                raise AssertionError("Temporary service did not become ready")
            process = start()
            try:
                async with httpx.AsyncClient(base_url=f"http://127.0.0.1:{port}", timeout=5) as http:
                    await ready(http)
                    session = (await http.post("/api/session", json=credentials)).json()
                    old_headers = {"Authorization": "Bearer "+session["token"]}
                    response = await http.post("/api/send", headers=old_headers, json={"text": "restart persistence fixture"})
                    assert response.status_code == 200
                    async with connect(f"ws://127.0.0.1:{port}/party", additional_headers=old_headers) as ws:
                        await ws.send(json.dumps({"type": "hello"}))
                        await receive(ws, "welcome")
                        await ws.send(json.dumps({"type": "create", "name": "Restart fixture"}))
                        original = (await receive(ws, "party"))["party"]
                    process.terminate()
                    await asyncio.to_thread(process.wait, 10)
                    process = start()
                    await ready(http)
                    assert (await http.get("/api/messages", headers=old_headers)).status_code == 401
                    response = await http.post("/api/session", json=credentials)
                    assert response.status_code == 200
                    new_headers = {"Authorization": "Bearer "+response.json()["token"]}
                    messages = (await http.get("/api/messages", headers=new_headers)).json()["messages"]
                    assert any(m["text"]=="restart persistence fixture" for m in messages)
                    async with connect(f"ws://127.0.0.1:{port}/party", additional_headers=new_headers) as ws:
                        await ws.send(json.dumps({"type": "hello"}))
                        recovered = (await receive(ws, "welcome"))["party"]
                        assert recovered["name"] == original["name"] and recovered["members"] == original["members"] and recovered["leader"] == "NVpersist"
            finally:
                if process.poll() is None:
                    process.terminate()
                    await asyncio.to_thread(process.wait, 10)
    print(json.dumps({"status":"passed", "checks":["identity retained after restart", "old token rejected and installation reauthorized", "IRC history retained", "Party membership and leader retained"]}))


if __name__ == "__main__":
    asyncio.run(verify())
