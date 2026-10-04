"""NV social service. One worker: ordered WS rooms and SQLite social identities.

Installation credentials authenticate social sessions, not Minecraft accounts or
commercial entitlements. Never trust a client-supplied role, UID or sender name.
"""
import asyncio
from collections import OrderedDict
from contextlib import asynccontextmanager, suppress
from dataclasses import dataclass, field
import hashlib
import json
import math
import os
from pathlib import Path
import re
import secrets
import sqlite3
import time
import uuid

from fastapi import FastAPI, HTTPException, Request, WebSocket, WebSocketDisconnect

VERSION = "nv-social-2"
MAX_BODY = 65536
MAX_PEERS = 256
NAME = re.compile(r"[A-Za-z0-9_]{3,16}\Z")
db = None
tokens = {}
presence = {}
parties = {}
invites = {}
peers = {"party": {}, "gui": {}, "pets": {}, "emotions": {}}
states = {"gui": {}, "pets": {}, "emotions": {}}
ip_buckets = OrderedDict()


class Bucket:
    def __init__(self, rate, burst):
        self.rate, self.burst, self.value, self.at = rate, burst, float(burst), time.monotonic()

    def take(self):
        now = time.monotonic()
        self.value = min(self.burst, self.value + (now-self.at)*self.rate)
        self.at = now
        if self.value < 1:
            return False
        self.value -= 1
        return True


def digest(value):
    return hashlib.sha256(value.encode()).hexdigest()


def clean(value, length=160):
    return "".join(c for c in str(value or "") if c >= " " and c != "\x7f" and c != "§")[:length]


def finite(value, maximum=30000000):
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value) or abs(value) > maximum:
        raise ValueError("Invalid coordinate")
    return value


def bounded(value, depth=0):
    if depth > 8:
        raise ValueError("State nesting limit")
    if isinstance(value, dict):
        if len(value) > 128:
            raise ValueError("State field limit")
        return {clean(k, 32): bounded(v, depth+1) for k, v in value.items()}
    if isinstance(value, list):
        if len(value) > 128:
            raise ValueError("State array limit")
        return [bounded(v, depth+1) for v in value]
    if isinstance(value, str):
        return clean(value)
    if isinstance(value, bool) or value is None:
        return value
    return finite(value, 1e15)


def parse(raw):
    if len(raw.encode("utf-8")) > MAX_BODY:
        raise ValueError("Message size limit")
    obj = json.loads(raw, parse_constant=lambda _: (_ for _ in ()).throw(ValueError("Non-finite JSON")))
    if not isinstance(obj, dict):
        raise ValueError("Object required")
    return obj


def save_parties():
    db.execute("INSERT OR REPLACE INTO state VALUES ('parties', ?)", (json.dumps(parties),))
    db.commit()


@asynccontextmanager
async def lifespan(app):
    global db, parties
    path = Path(os.environ.get("NV_DB", "state/nv-social.sqlite3"))
    path.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(path)
    db.execute("PRAGMA journal_mode=WAL")
    db.executescript("""
      CREATE TABLE IF NOT EXISTS identities(id TEXT PRIMARY KEY, secret TEXT NOT NULL, name TEXT NOT NULL);
      CREATE UNIQUE INDEX IF NOT EXISTS unique_nickname ON identities(lower(name));
      CREATE TABLE IF NOT EXISTS messages(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, sender TEXT, receiver TEXT, user TEXT, recipient TEXT, prefix TEXT, text TEXT);
      CREATE TABLE IF NOT EXISTS state(key TEXT PRIMARY KEY, value TEXT);
    """)
    row = db.execute("SELECT value FROM state WHERE key='parties'").fetchone()
    parties = json.loads(row[0]) if row else {}
    db.commit()
    maintenance = asyncio.create_task(expire())
    try:
        yield
    finally:
        maintenance.cancel()
        with suppress(asyncio.CancelledError):
            await maintenance
        for room in peers.values():
            for peer in list(room.values()):
                await peer.close(1001)
        save_parties()
        db.close()
        db = None
        tokens.clear()
        presence.clear()
        invites.clear()
        for room in peers.values():
            room.clear()
        for room in states.values():
            room.clear()


app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)


@app.middleware("http")
async def throttle(request: Request, call_next):
    # Nginx supplies the real address; Uvicorn trusts forwarded headers only from loopback.
    address = request.client.host if request.client else "unknown"
    bucket = ip_buckets.get(address)
    if bucket is None:
        if len(ip_buckets) >= 4096:
            ip_buckets.popitem(last=False)
        bucket = ip_buckets[address] = Bucket(30, 120)
    ip_buckets.move_to_end(address)
    if not bucket.take():
        from fastapi.responses import JSONResponse
        return JSONResponse({"detail": "Request rate limit"}, 429)
    response = await call_next(request)
    response.headers["Cache-Control"] = "no-store"
    return response


async def body(request):
    data = bytearray()
    async for chunk in request.stream():
        data.extend(chunk)
        if len(data) > MAX_BODY:
            raise HTTPException(413, "Request size limit")
    try:
        return parse(data.decode("utf-8"))
    except (ValueError, UnicodeError, RecursionError):
        raise HTTPException(400, "Invalid JSON") from None


def authenticate(connection):
    header = connection.headers.get("authorization", "")
    token = header[7:] if header.startswith("Bearer ") else ""
    session = tokens.get(digest(token)) if token else None
    if not session or session["expires"] < time.time():
        raise HTTPException(401, "Social session required")
    return session


@app.get("/")
@app.get("/health")
async def health():
    return {"status": "ok", "version": VERSION, "voice": False}


@app.post("/api/session")
async def session(request: Request):
    obj = await body(request)
    try:
        installation = str(uuid.UUID(str(obj.get("installation", ""))))
    except ValueError:
        raise HTTPException(400, "Invalid installation") from None
    secret, name = obj.get("secret", ""), obj.get("name", "")
    if not isinstance(secret, str) or not re.fullmatch(r"[a-f0-9]{64}", secret) or not isinstance(name, str) or not NAME.fullmatch(name):
        raise HTTPException(400, "Invalid credentials or nickname")
    row = db.execute("SELECT secret,name FROM identities WHERE id=?", (installation,)).fetchone()
    if row and not secrets.compare_digest(row[0], digest(secret)):
        raise HTTPException(403, "Installation credential mismatch")
    if not row and db.execute("SELECT count(*) FROM identities").fetchone()[0] >= 10000:
        raise HTTPException(503, "Registration capacity reached")
    try:
        db.execute("INSERT INTO identities VALUES (?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name", (installation, digest(secret), name))
        db.commit()
    except sqlite3.IntegrityError:
        db.rollback()
        raise HTTPException(409, "Nickname reserved by another installation") from None
    # One renewable token per installation. An expired token cannot keep a WS alive forever.
    for key, value in list(tokens.items()):
        if value["id"] == installation:
            del tokens[key]
    token = secrets.token_urlsafe(32)
    tokens[digest(token)] = {"id": installation, "name": name, "expires": time.time()+86400,
                             "send_bucket": Bucket(1, 5)}
    return {"token": token, "id": installation, "name": name, "expiresIn": 86400}


@app.get("/api/presence")
async def online_users(request: Request):
    user = authenticate(request)
    presence[user["id"]] = (user["name"], time.monotonic())
    return {"online": [name for name, at in presence.values() if time.monotonic()-at < 15]}


@app.get("/api/messages")
async def messages(request: Request, since: int = 0):
    user = authenticate(request)
    presence[user["id"]] = (user["name"], time.monotonic())
    rows = db.execute("SELECT id,ts,user,recipient,prefix,text FROM messages WHERE id>? AND (receiver='' OR sender=? OR receiver=?) ORDER BY id LIMIT 128", (max(0, since), user["id"], user["id"])).fetchall()
    latest = db.execute("SELECT coalesce(max(id),0) FROM messages").fetchone()[0]
    return {"messages": [dict(zip(("id", "ts", "user", "to", "prefix", "text"), row)) for row in rows],
            "latest": latest, "online": sum(time.monotonic()-at < 15 for _, at in presence.values())}


@app.post("/api/send")
async def send_message(request: Request):
    user = authenticate(request)
    if not user["send_bucket"].take():
        raise HTTPException(429, "Message rate limit")
    obj = await body(request)
    text, target = clean(obj.get("text"), 400).strip(), clean(obj.get("to"), 16)
    if not text:
        raise HTTPException(400, "Empty message")
    receiver = ""
    if target:
        row = db.execute("SELECT id,name FROM identities WHERE lower(name)=lower(?)", (target,)).fetchone()
        if not row:
            raise HTTPException(404, "Recipient not registered")
        receiver, target = row
    prefix = clean(obj.get("prefix"), 24)
    # Prefixes are cosmetics, never authorization. Admin-like prefix IDs are rejected.
    if prefix.lower() not in ("", "none", "prism", "vector", "sunset", "magma", "garnet", "rime",
                              "phoenix", "malachite", "nocturne", "grin", "quartz", "amber", "ether",
                              "nebula", "chimera", "eclipse", "ember", "cardinal", "sakura", "blaze"):
        prefix = ""
    cursor = db.execute("INSERT INTO messages(ts,sender,receiver,user,recipient,prefix,text) VALUES (?,?,?,?,?,?,?)", (int(time.time()*1000), user["id"], receiver, user["name"], target, prefix, text))
    message_id = cursor.lastrowid
    db.execute("DELETE FROM messages WHERE id < ?", (message_id-1000,))
    db.commit()
    return {"id": message_id, "ok": True}


@dataclass(eq=False)
class Peer:
    ws: WebSocket
    identity: dict
    channel: str
    queue: asyncio.Queue = field(default_factory=lambda: asyncio.Queue(maxsize=64))
    bucket: Bucket = field(default_factory=lambda: Bucket(20, 60))
    task: object = None
    closed: bool = False
    world: str = ""
    updated_at: float = field(default_factory=time.monotonic)

    def emit(self, obj):
        if self.closed:
            return
        try:
            self.queue.put_nowait(json.dumps(obj, ensure_ascii=False, allow_nan=False, separators=(",", ":")))
        except asyncio.QueueFull:
            asyncio.create_task(self.close(1013))

    async def writer(self):
        try:
            while True:
                await asyncio.wait_for(self.ws.send_text(await self.queue.get()), 5)
        except (Exception, asyncio.CancelledError):
            await self.close(1013)

    async def close(self, code=1000):
        if self.closed:
            return
        self.closed = True
        if self.task and self.task is not asyncio.current_task():
            self.task.cancel()
        with suppress(Exception):
            await asyncio.wait_for(self.ws.close(code), 2)


def party_for(uid):
    return next(((pid, p) for pid, p in parties.items() if uid in p["members"]), (None, None))


def snapshot(p):
    if not p:
        return None
    rows = {uid: db.execute("SELECT name FROM identities WHERE id=?", (uid,)).fetchone() for uid in p["members"]}
    names = {uid: row[0] if row else "Player" for uid, row in rows.items()}
    return {"name": p["name"], "leader": names.get(p["leader"], "Player"), "max": 10,
            "members": [{"name": names[uid], "leader": uid == p["leader"], "online": uid in peers["party"]} for uid in p["members"]]}


def publish_party(p):
    if p:
        payload = {"type": "party", "party": snapshot(p)}
        for uid in p["members"]:
            peer = peers["party"].get(uid)
            if peer:
                peer.emit(payload)


def leave(uid):
    pid, p = party_for(uid)
    if p:
        p["members"].remove(uid)
        if not p["members"]:
            del parties[pid]
        else:
            if p["leader"] == uid:
                p["leader"] = next((m for m in p["members"] if m in peers["party"]), p["members"][0])
            publish_party(p)
        save_parties()
    if uid in peers["party"]:
        peers["party"][uid].emit({"type": "party", "party": None})


def notice(peer, text):
    peer.emit({"type": "error", "message": text})


def party_command(peer, obj):
    uid, name = peer.identity["id"], peer.identity["name"]
    command = obj.get("type")
    pid, p = party_for(uid)
    if command in ("hello", "info"):
        peer.emit({"type": "welcome" if command == "hello" else "party", "party": snapshot(p), "info": command == "info"})
    elif command == "create":
        if p:
            return notice(peer, "Вы уже состоите в группе")
        if len(parties) >= 1024:
            return notice(peer, "Лимит групп")
        p = {"name": clean(obj.get("name"), 32) or name+"'s Party", "leader": uid, "members": [uid]}
        parties[str(uuid.uuid4())] = p
        save_parties()
        publish_party(p)
    elif command == "invite":
        target_name = clean(obj.get("target", obj.get("to")), 16)
        target = next((c for c in peers["party"].values() if c.identity["name"].lower() == target_name.lower()), None)
        if not target or target is peer:
            return notice(peer, "Игрок недоступен")
        if not p:
            if len(parties) >= 1024:
                return notice(peer, "Лимит групп")
            pid = str(uuid.uuid4())
            p = parties[pid] = {"name": name+"'s Party", "leader": uid, "members": [uid]}
            save_parties()
            publish_party(p)
        if p["leader"] != uid or len(p["members"]) >= 10 or len(invites) >= 256:
            return notice(peer, "Приглашение недоступно")
        iid = secrets.token_urlsafe(18)
        invites[iid] = {"from": uid, "recipient": target.identity["id"], "party": pid, "expires": time.monotonic()+60}
        target.emit({"type": "invite", "invite": iid, "from": name, "party": p["name"], "expires": 60000})
    elif command == "invite_response":
        iid = clean(obj.get("invite"), 64)
        invite = invites.get(iid)
        if not invite or invite["recipient"] != uid or invite["expires"] < time.monotonic():
            return notice(peer, "Приглашение недействительно")
        dest = parties.get(invite["party"])
        del invites[iid]
        if obj.get("accept") is not True:
            return
        if not dest or dest["leader"] != invite["from"] or len(dest["members"]) >= 10:
            return notice(peer, "Группа недоступна")
        if uid in dest["members"]:
            return
        leave(uid)
        dest["members"].append(uid)
        save_parties()
        publish_party(dest)
    elif command == "leave":
        leave(uid)
    elif command in ("kick", "disband"):
        if not p or p["leader"] != uid:
            return notice(peer, "Только лидер может выполнить команду")
        if command == "kick":
            target = clean(obj.get("target"), 16).lower()
            victim = next((m for m in p["members"] if (db.execute("SELECT name FROM identities WHERE id=?", (m,)).fetchone() or [""])[0].lower() == target), None)
            if victim and victim != uid:
                leave(victim)
        else:
            members = p["members"][:]
            del parties[pid]
            for member in members:
                if member in peers["party"]:
                    peers["party"][member].emit({"type": "party", "party": None})
            save_parties()
    elif command == "color":
        peer.identity["color"] = int(finite(obj.get("color", 0), 2**32)) & 0xffffffff
        peer.identity["rainbow"] = obj.get("rainbow") is True
    elif command in ("chat", "marker", "spit") and p:
        out = {"type": command, "from": name}
        if command == "chat":
            out["text"] = clean(obj.get("text"), 400)
            if not out["text"]:
                return
        else:
            out.update({k: finite(obj.get(k, 0)) for k in ("x", "y", "z")})
            out["dim"] = clean(obj.get("dim"), 96)
            out["color"] = peer.identity.get("color", 0)
            out["rainbow"] = peer.identity.get("rainbow", False)
            if command == "marker":
                out["id"] = str(uuid.uuid4())
                out["ttl"] = max(1000, min(300000, int(finite(obj.get("ttl", 8000)))))
            else:
                out.update({k: finite(obj.get(k, 0), 16) for k in ("dx", "dy", "dz")})
        for member in p["members"]:
            if member in peers["party"]:
                peers["party"][member].emit(out)
    else:
        notice(peer, "Неизвестная или недоступная команда")


def sync_command(peer, obj):
    channel, uid = peer.channel, peer.identity["id"]
    if obj.get("t") not in ("h", "u"):
        raise ValueError("Unknown state command")
    state = bounded(obj)
    state.update({"i": uid, "n": peer.identity["name"], "p": peer.identity["name"], "ro": "Guest", "av": ""})
    world = clean(obj.get("w"), 64)
    old_world = peer.world
    peer.world = world
    peer.updated_at = time.monotonic()
    for axis in ("x", "y", "z", "ax", "ay", "az"):
        if axis in state:
            state[axis] = finite(state[axis])
    if channel == "pets":
        if state.get("v") not in ("MASCOT", "SPIRIT", "DRAGON", "MOTH", "FROG", "ROBOT", "OWL"):
            raise ValueError("Unknown pet")
        state["s"] = max(.05, min(4, finite(state.get("s", 1), 1e5)))
    if channel == "emotions":
        state["v"] = max(.05, min(4, finite(state.get("v", 1), 1e5)))
        state["s"] = max(0, min(int(time.time()*1000)+60000, int(finite(state.get("s", 0), 1e15))))
    if old_world != world:
        for other in peers[channel].values():
            if other is not peer and other.world == old_world:
                other.emit({"t": "d", "r": [uid], "p": []})
        # Clear the receiver's previous world without requiring a protocol extension.
        peer.emit({"t": "d", "r": list(states[channel]), "p": []})
    states[channel][uid] = state
    if obj.get("t") == "h" or old_world != world:
        peer.emit({"t": "d" if channel == "emotions" else "s", "p": [s for i, s in states[channel].items() if i != uid and s.get("w") == world], "r": []})
    for other in peers[channel].values():
        if other is not peer and other.world == world:
            other.emit({"t": "d", "p": [state], "r": []})


@app.websocket("/party")
@app.websocket("/sync/{channel}")
async def socket(ws: WebSocket, channel: str = "party"):
    if channel not in peers:
        return await ws.close(1008)
    try:
        user = authenticate(ws)
    except HTTPException:
        return await ws.close(1008)
    if sum(len(p) for p in peers.values()) >= MAX_PEERS:
        return await ws.close(1013)
    await ws.accept()
    peer = Peer(ws, user, channel)
    uid = user["id"]
    old = peers[channel].get(uid)
    peers[channel][uid] = peer
    if old:
        await old.close(1000)
    peer.task = asyncio.create_task(peer.writer())
    if channel == "party":
        publish_party(party_for(uid)[1])
    try:
        while True:
            # Uvicorn ping/pong detects dead transports; a quiet Party is still valid.
            frame = await ws.receive()
            if frame["type"] == "websocket.disconnect":
                break
            if user["expires"] < time.time():
                await peer.close(1008)
                break
            if not peer.bucket.take():
                await peer.close(1008)
                break
            if frame.get("bytes") is not None:
                # Voice controls remain unavailable until a real capture/playback engine exists.
                notice(peer, "Голосовая связь пока недоступна")
                continue
            try:
                obj = parse(frame.get("text", ""))
                if channel == "party":
                    party_command(peer, obj)
                else:
                    sync_command(peer, obj)
            except (ValueError, TypeError, OverflowError, RecursionError):
                notice(peer, "Некорректное сообщение")
    except (WebSocketDisconnect, asyncio.TimeoutError):
        pass
    finally:
        if peers[channel].get(uid) is peer:
            del peers[channel][uid]
            if channel == "party":
                _, party = party_for(uid)
                if party and party["leader"] == uid:
                    party["leader"] = next((m for m in party["members"] if m in peers["party"]), uid)
                    save_parties()
                publish_party(party)
            else:
                states[channel].pop(uid, None)
                for other in peers[channel].values():
                    if other.world == peer.world:
                        other.emit({"t": "d", "r": [uid], "p": []})
        await peer.close()


async def expire():
    while True:
        await asyncio.sleep(5)
        now = time.monotonic()
        for key, value in list(invites.items()):
            if value["expires"] < now:
                del invites[key]
        for key, (_, at) in list(presence.items()):
            if now-at >= 15:
                del presence[key]
        for key, value in list(tokens.items()):
            if value["expires"] < time.time():
                del tokens[key]
        for room in peers.values():
            for peer in list(room.values()):
                if peer.identity["expires"] < time.time():
                    await peer.close(1008)
        # A half-open or quiet cosmetic sender must not leave a frozen ghost in the world.
        for channel, room in states.items():
            for uid in list(room):
                peer = peers[channel].get(uid)
                if peer is not None and now-peer.updated_at > 10:
                    del room[uid]
                    for other in peers[channel].values():
                        if other is not peer and other.world == peer.world:
                            other.emit({"t": "d", "r": [uid], "p": []})


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=int(os.environ.get("NV_PORT", "8080")),
                workers=1, ws_max_size=MAX_BODY, ws_max_queue=8, limit_concurrency=320,
                timeout_keep_alive=5, access_log=False, forwarded_allow_ips="127.0.0.1")
