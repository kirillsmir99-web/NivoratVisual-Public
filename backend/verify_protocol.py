"""Black box protocol checks against an isolated local or staged NV service.

Prints results only. Generated installation secrets and bearer tokens stay in RAM.
"""
import asyncio
from contextlib import AsyncExitStack
import json
import secrets
import sys
import uuid

import httpx
from websockets.asyncio.client import connect


async def verify(base):
    ws_base = base.replace("https://", "wss://").replace("http://", "ws://")
    passed = []
    async with httpx.AsyncClient(base_url=base, timeout=10) as http:
        assert (await http.get("/health")).json()["version"] == "nv-social-2"
        assert (await http.get("/api/messages")).status_code == 401
        users = []
        for _ in range(3):
            credentials = {"installation": str(uuid.uuid4()), "secret": secrets.token_hex(32), "name": "NVtest"+secrets.token_hex(4)}
            response = await http.post("/api/session", json=credentials)
            assert response.status_code == 200, response.status_code
            user = {**credentials, **response.json()}
            user["headers"] = {"Authorization": "Bearer "+user["token"]}
            users.append(user)
        a, b, c = users
        bad = {k: a[k] for k in ("installation", "secret", "name")}
        bad["secret"] = secrets.token_hex(32)
        assert (await http.post("/api/session", json=bad)).status_code == 403
        assert (await http.post("/api/session", json={"installation": str(uuid.uuid4()), "secret": secrets.token_hex(32), "name": a["name"]})).status_code == 409
        passed.append("session ownership and nickname reservation")
        private = "private-"+secrets.token_hex(5)
        public = "public-"+secrets.token_hex(5)
        sent = await http.post("/api/send", headers=a["headers"], json={"user": c["name"], "text": private, "to": b["name"], "prefix": "admin"})
        assert sent.status_code == 200
        assert (await http.post("/api/send", headers=a["headers"], json={"text": public})).status_code == 200
        for user in users:
            data = (await http.get("/api/messages", headers=user["headers"], params={"me": b["name"]})).json()["messages"]
            assert any(m["text"] == public for m in data)
            pm = [m for m in data if m["text"] == private]
            assert bool(pm) == (user is a or user is b)
            if pm:
                assert pm[0]["user"] == a["name"] and pm[0]["prefix"] != "admin"
        assert (await http.post("/api/send", headers=a["headers"], content=b"x"*65537)).status_code == 413
        passed.append("private-message isolation, sender binding and HTTP size limit")
        for user in users:
            assert (await http.get("/api/presence", headers=user["headers"])).status_code == 200
        online = (await http.get("/api/presence", headers=c["headers"])).json()["online"]
        assert all(u["name"] in online for u in users)
        passed.append("authenticated presence")
        async with AsyncExitStack() as stack:
            sockets = []
            for user in users:
                ws = await stack.enter_async_context(connect(ws_base+"/party", additional_headers=user["headers"], open_timeout=10))
                sockets.append(ws)
                await ws.send(json.dumps({"type": "hello", "id": a["installation"], "name": "forged"}))
                await receive(ws, "welcome")
            wa, wb, wc = sockets
            await wa.send(json.dumps({"type": "create", "name": "NV protocol test"}))
            assert (await receive(wa, "party"))["party"]["leader"] == a["name"]
            await wa.send(json.dumps({"type": "invite", "target": b["name"]}))
            invite = await receive(wb, "invite")
            await wc.send(json.dumps({"type": "invite_response", "invite": invite["invite"], "accept": True}))
            await receive(wc, "error")
            await wb.send(json.dumps({"type": "invite_response", "invite": invite["invite"], "accept": True}))
            assert len((await receive(wb, "party", lambda x: bool(x["party"])))["party"]["members"]) == 2
            await receive(wa, "party")
            await wb.send(json.dumps({"type": "disband"}))
            await receive(wb, "error")
            await wb.send(json.dumps({"type": "marker", "x": 1, "y": 2, "z": 3, "dim": "minecraft:overworld", "from": c["name"], "ttl": 8000}))
            assert (await receive(wa, "marker"))["from"] == b["name"]
            await wa.send(json.dumps({"type": "kick", "target": b["name"]}))
            assert (await receive(wb, "party"))["party"] is None
            await receive(wa, "party")
            await wa.send(json.dumps({"type": "invite", "target": b["name"]}))
            invite = await receive(wb, "invite")
            await wb.send(json.dumps({"type": "invite_response", "invite": invite["invite"], "accept": True}))
            await receive(wb, "party", lambda x: bool(x["party"]))
            await receive(wa, "party")
            replacement = await stack.enter_async_context(connect(ws_base+"/party", additional_headers=b["headers"]))
            await replacement.send(json.dumps({"type": "hello"}))
            await receive(replacement, "welcome")
            await wa.close()
            transferred = await receive(replacement, "party", lambda x: x["party"] and x["party"]["leader"] == b["name"])
            assert transferred["party"]["leader"] == b["name"]
            await replacement.send(json.dumps({"type": "disband"}))
            assert (await receive(replacement, "party"))["party"] is None
            passed.append("Party create/invite/recipient ownership/kick/reconnect/leader transfer/disband")
        for channel in ("gui", "pets", "emotions"):
            async with AsyncExitStack() as stack:
                wa = await stack.enter_async_context(connect(ws_base+"/sync/"+channel, additional_headers=a["headers"]))
                wb = await stack.enter_async_context(connect(ws_base+"/sync/"+channel, additional_headers=b["headers"]))
                wc = await stack.enter_async_context(connect(ws_base+"/sync/"+channel, additional_headers=c["headers"]))
                state = {"t": "h", "i": c["installation"], "n": "forged", "w": "test-world", "o": True, "v": "MASCOT" if channel == "pets" else 1, "s": 1, "e": "WAVE", "x": 1, "y": 2, "z": 3}
                await wa.send(json.dumps(state))
                await receive(wa, "s" if channel != "emotions" else "d", sync=True)
                await wb.send(json.dumps({**state, "i": b["installation"]}))
                initial = await receive(wb, "s" if channel != "emotions" else "d", lambda x: bool(x.get("p")), True)
                assert initial["p"][0]["i"] == a["installation"] and initial["p"][0]["n"] == a["name"]
                await receive(wa, "d", lambda x: bool(x.get("p")), True)
                await wc.send(json.dumps({**state, "w": "other-world"}))
                other = await receive(wc, "s" if channel != "emotions" else "d", lambda x: x.get("p") == [], True)
                assert not other["p"]
                await wb.close()
                removed = await receive(wa, "d", lambda x: b["installation"] in x.get("r", []), True)
                assert b["installation"] in removed["r"]
                passed.append(channel+" identity binding, world separation and disconnect cleanup")
    print(json.dumps({"status": "passed", "checks": passed}, ensure_ascii=False))


async def receive(ws, kind, predicate=lambda x: True, sync=False):
    async def wait():
        for _ in range(100):
            obj = json.loads(await ws.recv())
            if obj.get("t" if sync else "type") == kind and predicate(obj):
                return obj
        raise AssertionError("Too many unrelated frames")
    return await asyncio.wait_for(wait(), 10)


if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(verify(sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:18818"))
