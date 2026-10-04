import asyncio
import json
import time
import uuid
from typing import Dict, List, Set, Optional
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Request, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

app = FastAPI(title="NV Backend (IRC & Party)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# -------------------------------------------------------------
# IRC REST API
# -------------------------------------------------------------
messages: List[dict] = []
msg_id_counter = 0
active_users: Dict[str, float] = {}

@app.get("/")
async def root():
    return {"status": "ok", "service": "NV Backend"}

@app.get("/api/messages")
async def get_messages(since: int = 0, me: str = "Player"):
    now = time.time()
    if me:
        active_users[me.strip().lower()] = now
    
    cutoff = now - 15.0
    for u in [usr for usr, t in list(active_users.items()) if t < cutoff]:
        active_users.pop(u, None)

    filtered = [m for m in messages if m["id"] > since]
    latest = messages[-1]["id"] if messages else 0
    return {
        "online": max(1, len(active_users)),
        "latest": latest,
        "messages": filtered
    }

@app.post("/api/send")
async def send_message(req: Request):
    global msg_id_counter
    try:
        data = await req.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON")

    text = (data.get("text") or "").strip()
    user = (data.get("user") or "Player").strip()
    prefix = (data.get("prefix") or "").strip()
    to = (data.get("to") or "").strip()
    uid = data.get("uid", 0)

    if not text:
        raise HTTPException(status_code=400, detail="Сообщение не может быть пустым")
    if len(text) > 400:
        raise HTTPException(status_code=400, detail="Сообщение слишком длинное")

    msg_id_counter += 1
    msg = {
        "id": msg_id_counter,
        "ts": int(time.time() * 1000),
        "user": user,
        "prefix": prefix,
        "text": text,
        "to": to,
        "uid": uid
    }
    messages.append(msg)
    if len(messages) > 1000:
        messages.pop(0)

    now = time.time()
    active_users[user.lower()] = now
    return {"ok": True, "id": msg_id_counter}


# -------------------------------------------------------------
# Party WebSocket Manager
# -------------------------------------------------------------
class PartyManager:
    def __init__(self):
        self.clients: Dict[str, dict] = {}
        self.parties: Dict[str, dict] = {}
        self.invites: Dict[str, dict] = {}

    def get_party_snapshot(self, party_id: Optional[str]) -> dict:
        if not party_id or party_id not in self.parties:
            return {"name": None, "leader": None, "max": 10, "members": []}
        p = self.parties[party_id]
        leader_info = self.clients.get(p["leader_id"])
        leader_name = leader_info["name"] if leader_info else "?"
        members_list = []
        for mid in p["members"]:
            c = self.clients.get(mid)
            if c:
                members_list.append({
                    "name": c["name"],
                    "leader": (mid == p["leader_id"]),
                    "online": True
                })
        return {
            "name": p["name"],
            "leader": leader_name,
            "max": 10,
            "members": members_list
        }

    async def broadcast_party(self, party_id: Optional[str], data: dict, exclude_cid: Optional[str] = None):
        if not party_id or party_id not in self.parties:
            return
        p = self.parties[party_id]
        for mid in list(p["members"]):
            if mid == exclude_cid:
                continue
            c = self.clients.get(mid)
            if c and c.get("ws"):
                try:
                    await c["ws"].send_text(json.dumps(data, ensure_ascii=False))
                except Exception:
                    pass

    async def broadcast_party_bytes(self, party_id: Optional[str], raw_bytes: bytes, exclude_cid: Optional[str] = None):
        if not party_id or party_id not in self.parties:
            return
        p = self.parties[party_id]
        for mid in list(p["members"]):
            if mid == exclude_cid:
                continue
            c = self.clients.get(mid)
            if c and c.get("ws"):
                try:
                    await c["ws"].send_bytes(raw_bytes)
                except Exception:
                    pass

    async def remove_client(self, client_entry: dict):
        cid = client_entry.get("id")
        if cid in self.clients:
            del self.clients[cid]
        pid = client_entry.get("party_id")
        if pid and pid in self.parties:
            p = self.parties[pid]
            p["members"].discard(cid)
            if not p["members"]:
                del self.parties[pid]
            else:
                if p["leader_id"] == cid:
                    p["leader_id"] = next(iter(p["members"]))
                snap = self.get_party_snapshot(pid)
                await self.broadcast_party(pid, {"type": "party", "party": snap})
                await self.broadcast_party(pid, {
                    "type": "notice",
                    "level": "warn",
                    "message": f"{client_entry.get('name', 'Игрок')} вышел из сети"
                })

pm = PartyManager()

async def handle_party_text(ws: WebSocket, client: dict, data: dict):
    msg_type = data.get("type", "")

    if msg_type == "hello":
        old_id = client.get("id")
        cid = data.get("id") or old_id
        name = data.get("name") or "Player"
        client["id"] = cid
        client["name"] = name
        client["uuid"] = data.get("uuid", "")
        if old_id != cid and old_id in pm.clients:
            del pm.clients[old_id]
        pm.clients[cid] = client

        pid = client.get("party_id")
        if pid:
            snap = pm.get_party_snapshot(pid)
            await ws.send_text(json.dumps({"type": "party", "party": snap}, ensure_ascii=False))

    elif msg_type == "info":
        pid = client.get("party_id")
        snap = pm.get_party_snapshot(pid)
        await ws.send_text(json.dumps({"type": "party", "party": snap, "info": True}, ensure_ascii=False))

    elif msg_type == "invite":
        target_name = (data.get("to") or "").strip().lower()
        target_cid = None
        for c_id, c_data in pm.clients.items():
            if c_data["name"].strip().lower() == target_name:
                target_cid = c_id
                break

        if not target_cid:
            await ws.send_text(json.dumps({
                "type": "notice",
                "level": "warn",
                "message": f"Игрок {data.get('to')} не в сети"
            }, ensure_ascii=False))
            return

        pid = client.get("party_id")
        if not pid:
            pid = str(uuid.uuid4())
            client["party_id"] = pid
            pm.parties[pid] = {
                "name": f"Пати {client['name']}",
                "leader_id": client["id"],
                "members": {client["id"]}
            }
            snap = pm.get_party_snapshot(pid)
            await ws.send_text(json.dumps({"type": "party", "party": snap}, ensure_ascii=False))

        inv_id = str(uuid.uuid4())
        pm.invites[inv_id] = {
            "from_id": client["id"],
            "party_id": pid,
            "expires": time.time() + 60.0
        }
        target_ws = pm.clients[target_cid]["ws"]
        await target_ws.send_text(json.dumps({
            "type": "invite",
            "invite": inv_id,
            "from": client["name"],
            "party": pm.parties[pid]["name"],
            "expires": 60000
        }, ensure_ascii=False))
        await ws.send_text(json.dumps({
            "type": "notice",
            "level": "info",
            "message": f"Приглашение отправлено игроку {pm.clients[target_cid]['name']}"
        }, ensure_ascii=False))

    elif msg_type == "invite_response":
        inv_id = data.get("invite")
        accept = bool(data.get("accept", False))
        inv = pm.invites.pop(inv_id, None)
        if inv and accept and time.time() < inv["expires"]:
            pid = inv["party_id"]
            if pid in pm.parties:
                old_pid = client.get("party_id")
                if old_pid and old_pid in pm.parties:
                    pm.parties[old_pid]["members"].discard(client["id"])

                client["party_id"] = pid
                pm.parties[pid]["members"].add(client["id"])
                snap = pm.get_party_snapshot(pid)
                await pm.broadcast_party(pid, {"type": "party", "party": snap})
                await pm.broadcast_party(pid, {
                    "type": "notice",
                    "level": "info",
                    "message": f"{client['name']} вступил в пати"
                })

    elif msg_type == "chat":
        pid = client.get("party_id")
        text = data.get("text", "")
        if pid and text:
            await pm.broadcast_party(pid, {
                "type": "chat",
                "from": client["name"],
                "text": text
            })

    elif msg_type in ("marker", "marker_remove", "spit", "color"):
        pid = client.get("party_id")
        if pid:
            await pm.broadcast_party(pid, data, exclude_cid=client.get("id"))

    elif msg_type == "leave":
        pid = client.get("party_id")
        if pid and pid in pm.parties:
            pm.parties[pid]["members"].discard(client["id"])
            client["party_id"] = None
            empty_snap = {"name": None, "leader": None, "max": 10, "members": []}
            await ws.send_text(json.dumps({"type": "party", "party": empty_snap}, ensure_ascii=False))
            if not pm.parties[pid]["members"]:
                del pm.parties[pid]
            else:
                snap = pm.get_party_snapshot(pid)
                await pm.broadcast_party(pid, {"type": "party", "party": snap})
                await pm.broadcast_party(pid, {
                    "type": "notice",
                    "level": "info",
                    "message": f"{client['name']} покинул пати"
                })

async def handle_party_bytes(client: dict, raw_bytes: bytes):
    pid = client.get("party_id")
    if pid:
        await pm.broadcast_party_bytes(pid, raw_bytes, exclude_cid=client.get("id"))

@app.websocket("/party")
@app.websocket("/")
async def party_ws_endpoint(ws: WebSocket):
    await ws.accept()
    cid = str(uuid.uuid4())
    client_entry = {"id": cid, "name": "Player", "ws": ws, "party_id": None}
    pm.clients[cid] = client_entry
    try:
        while True:
            msg = await ws.receive()
            if msg.get("type") == "websocket.disconnect":
                break
            if "text" in msg and msg["text"]:
                try:
                    data = json.loads(msg["text"])
                    await handle_party_text(ws, client_entry, data)
                except Exception:
                    pass
            elif "bytes" in msg and msg["bytes"]:
                await handle_party_bytes(client_entry, msg["bytes"])
    except WebSocketDisconnect:
        pass
    except Exception:
        pass
    finally:
        await pm.remove_client(client_entry)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080, log_level="info")
