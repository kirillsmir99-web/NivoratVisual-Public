package rtx.nv.api.music.lyrics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LiveCaptionServer {
    public static final int PORT = 38473;
    private final LyricsManager lyricsManager;
    private HttpServer server;

    public LiveCaptionServer(LyricsManager lyricsManager) {
        this.lyricsManager = lyricsManager;
    }

    public void start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            this.server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "NV-LiveCaptionServer");
                t.setDaemon(true);
                return t;
            }));
            this.server.createContext("/captions/push", new PushHandler());
            this.server.createContext("/captions/transcript", new TranscriptHandler());
            this.server.createContext("/captions/status", new StatusHandler());
            this.server.start();
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        if (this.server != null) {
            try {
                this.server.stop(0);
            } catch (Exception ignored) {
            }
            this.server = null;
        }
    }

    private static void sendCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        sendCors(exchange);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private class PushHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LiveCaptionServer.sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1L);
            } else if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                LiveCaptionServer.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            } else {
                try (InputStream is = exchange.getRequestBody()) {
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    String text = json.has("text") ? json.get("text").getAsString() : "";
                    long duration = json.has("durationMs") ? json.get("durationMs").getAsLong() : 3500L;
                    if (!text.isEmpty()) {
                        LiveCaptionServer.this.lyricsManager.pushLiveCaption(text, duration);
                        LiveCaptionServer.sendResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        LiveCaptionServer.sendResponse(exchange, 400, "{\"error\":\"Empty text\"}");
                    }
                } catch (Exception e) {
                    LiveCaptionServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    private static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LiveCaptionServer.sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1L);
            } else {
                LiveCaptionServer.sendResponse(exchange, 200, "{\"status\":\"ok\",\"service\":\"NV-LiveCaptionServer\",\"port\":38473}");
            }
        }
    }

    private class TranscriptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LiveCaptionServer.sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1L);
            } else if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                LiveCaptionServer.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            } else {
                try (InputStream is = exchange.getRequestBody()) {
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    String title = json.has("videoTitle") ? json.get("videoTitle").getAsString() : "";
                    List<LyricLine> lines = new ArrayList<>();
                    if (json.has("lines") && json.get("lines").isJsonArray()) {
                        for (JsonElement el : json.getAsJsonArray("lines")) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                long start = obj.has("startMs") ? obj.get("startMs").getAsLong() : 0L;
                                long end = obj.has("endMs") ? obj.get("endMs").getAsLong() : start + 3500L;
                                String text = obj.has("text") ? obj.get("text").getAsString() : "";
                                if (!text.trim().isEmpty()) {
                                    lines.add(new LyricLine(start, end, text.trim(), null));
                                }
                            }
                        }
                    }

                    if (!lines.isEmpty()) {
                        LiveCaptionServer.this.lyricsManager.pushTranscript(title, lines);
                        LiveCaptionServer.sendResponse(exchange, 200, "{\"success\":true,\"count\":" + lines.size() + "}");
                    } else {
                        LiveCaptionServer.sendResponse(exchange, 400, "{\"error\":\"No lines provided\"}");
                    }
                } catch (Exception e) {
                    LiveCaptionServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }
}
