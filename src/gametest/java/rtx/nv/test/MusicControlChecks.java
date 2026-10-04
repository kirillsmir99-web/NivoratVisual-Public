package rtx.nv.test;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import rtx.nv.api.music.MusicManager;

final class MusicControlChecks {
    static void verify() {
        HttpServer server = null;
        var client = MusicManager.get().getClient();
        int originalPort = rtx.nv.api.music.MusicHelperManager.get().getPort();
        if (originalPort <= 0) {
            originalPort = rtx.nv.api.music.MusicHelperManager.get().start();
        }
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                if (originalPort <= 0) throw new AssertionError("Bundled Windows media helper did not start");
            var connection = (java.net.HttpURLConnection) java.net.URI.create("http://127.0.0.1:" + originalPort + "/health").toURL().openConnection();
            connection.setConnectTimeout(1500); connection.setReadTimeout(1500);
            try (var in = connection.getInputStream()) {
                if (!new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("nv-media-1"))
                    throw new AssertionError("Bundled helper version mismatch");
            } finally { connection.disconnect(); }
            rtx.nv.NV.LOGGER.info("[NV-TEST] Bundled NV Media Windows helper started successfully");
        }
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var count = new AtomicInteger();
            var received = new CountDownLatch(1);
            var payload = new AtomicReference<>("{\"isActive\":false}");
            var preferred = new AtomicReference<String>();
            server.createContext("/now-playing", exchange -> {
                byte[] body = payload.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var out = exchange.getResponseBody()) { out.write(body); }
            });
            server.createContext("/source", exchange -> {
                preferred.set(exchange.getRequestURI().getQuery());
                exchange.sendResponseHeaders(204, -1); exchange.close();
            });
            server.createContext("/control/play-pause", exchange -> {
                count.incrementAndGet(); received.countDown();
                exchange.sendResponseHeaders(204, -1); exchange.close();
            });
            server.start();
            client.start(server.getAddress().getPort());
            client.sendPlayPause();
            if (!received.await(3, TimeUnit.SECONDS)) throw new AssertionError("Play command missing");
            // A toggle must stay single through the poll interval; two commands recreate the reported pause.
            Thread.sleep(1100);
            if (count.get() != 1) throw new AssertionError("Play emitted " + count.get() + " HTTP toggles");
            rtx.nv.NV.LOGGER.info("[NV-TEST] Play sent exactly one command across 1.1 seconds");
            for (String filter : java.util.List.of("auto", "yandex", "vk", "spotify", "browser")) {
                if (!client.setPreferredSource(filter).get(3, TimeUnit.SECONDS) || !("filter=" + filter).equals(preferred.get()))
                    throw new AssertionError("Source filter was not sent: " + filter);
            }
            payload.set("{\"isActive\":true,\"title\":\"Yesterday\",\"artist\":\"Леонид Агутин\",\"sourceId\":\"Spotify.exe\",\"status\":\"playing\"}");
            long deadline = System.currentTimeMillis() + 3000;
            while (!client.getState().isActive() && System.currentTimeMillis() < deadline) Thread.sleep(50);
            if (!client.getState().isActive() || !client.getState().title().equals("Yesterday"))
                throw new AssertionError("A valid song was rejected by source filters");
            payload.set("{\"isActive\":false}");
            Thread.sleep(550);
            if (!client.getState().isActive() || !client.getState().isPaused())
                throw new AssertionError("Transient media gap erased or extrapolated the current song");
            Thread.sleep(1700);
            if (client.getState().isActive()) throw new AssertionError("Expired media gap retained a stale song");
            payload.set("{\"isActive\":true,\"title\":\"Next song\",\"artist\":\"Artist\",\"sourceId\":\"Chrome.exe\",\"status\":\"paused\",\"positionMs\":10000,\"durationMs\":60000}");
            deadline = System.currentTimeMillis() + 3000;
            while (!client.getState().isActive() && System.currentTimeMillis() < deadline) Thread.sleep(50);
            long frozen = client.getState().currentPositionMs();
            Thread.sleep(350);
            if (!client.getState().isPaused() || client.getState().currentPositionMs() != frozen)
                throw new AssertionError("Paused timeline kept advancing");
            rtx.nv.NV.LOGGER.info("[NV-TEST] Media gap grace, stale expiry, browser metadata and pause clock passed");
            payload.set("{\"isActive\":true,\"title\":\"Yesterday\",\"artist\":\"Artist\",\"sourceId\":\"Telegram.exe\",\"status\":\"playing\"}");
            deadline = System.currentTimeMillis() + 3000;
            while (client.getState().isActive() && System.currentTimeMillis() < deadline) Thread.sleep(50);
            if (client.getState().isActive()) throw new AssertionError("Messenger session accepted as music");
            rtx.nv.NV.LOGGER.info("[NV-TEST] Five source filters delivered; valid songs retained and messenger excluded");
        } catch (Exception e) { throw new AssertionError(e); }
        finally { client.stop(); if (server != null) server.stop(0); if (originalPort > 0) client.start(originalPort); }
    }
}
