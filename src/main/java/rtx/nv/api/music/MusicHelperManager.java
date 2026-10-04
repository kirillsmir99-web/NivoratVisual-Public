package rtx.nv.api.music;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.loader.api.FabricLoader;

public final class MusicHelperManager {
    private static final MusicHelperManager INSTANCE = new MusicHelperManager();
    private static final String HELPER_EXE_NAME = "NvMediaHelper.exe";
    private static final String PORT_FILE_NAME = "nv_media_port.txt";

    private volatile Process helperProcess;
    private volatile int port = -1;
    private volatile boolean started = false;

    // Cross-platform fallbacks
    private HttpServer localServer;
    private ScheduledExecutorService localPoller;
    private volatile String cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";

    public static MusicHelperManager get() {
        return INSTANCE;
    }

    public int getPort() {
        return this.port;
    }

    public boolean checkPortAlive(int p) {
        if (p <= 0) return false;
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", p), 180);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isStarted() {
        return this.started;
    }

    public synchronized int start() {
        if (this.started && this.checkPortAlive(this.port)) return this.port;
        this.stop();
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) return this.startWindowsHelper();
        if (os.contains("linux")) return this.startLinuxBridge();
        if (os.contains("mac")) return this.startMacBridge();
        return -1;
    }

    private int startWindowsHelper() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("nv").resolve("helper");
            Files.createDirectories(configDir);
            Path exePath = configDir.resolve(HELPER_EXE_NAME);

            this.extractHelperIfNeeded(exePath);

            ProcessBuilder pb = new ProcessBuilder(exePath.toString());
            pb.directory(configDir.toFile());
            pb.redirectErrorStream(true);
            Path logFile = configDir.resolve("helper.log");
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));

            Files.deleteIfExists(configDir.resolve(PORT_FILE_NAME));
            this.helperProcess = pb.start();
            this.port = this.waitForPortFile(8000L);

            if (this.port > 0) {
                this.started = true;
                return this.port;
            } else {
                this.stop();
                return -1;
            }
        } catch (Exception e) {
            this.stop();
            return -1;
        }
    }

    private void extractHelperIfNeeded(Path target) throws IOException {
        try (InputStream in = this.getClass().getResourceAsStream("/helper/" + HELPER_EXE_NAME)) {
            if (in == null) {
                if (Files.exists(target)) return;
                throw new IOException("Helper executable resource not found");
            }
            byte[] bundled = in.readAllBytes();
            if (Files.exists(target) && java.util.Arrays.equals(Files.readAllBytes(target), bundled)) return;
            Path temporary = Files.createTempFile(target.getParent(), "helper-", ".tmp");
            try {
                Files.write(temporary, bundled);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private int readPortFromTempFile() {
        Path helperDir = FabricLoader.getInstance().getConfigDir().resolve("nv").resolve("helper");
        Path configPort = helperDir.resolve(PORT_FILE_NAME);
        try {
            if (Files.exists(configPort)) {
                int p = Integer.parseInt(Files.readString(configPort).trim());
                if (p > 0) return p;
            }
        } catch (Exception ignored) {
        }

        try {
            Path logFile = helperDir.resolve("helper.log");
            if (Files.exists(logFile)) {
                String logContent = Files.readString(logFile);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("port\\s+(\\d+)").matcher(logContent);
                while (m.find()) {
                    int p = Integer.parseInt(m.group(1));
                    if (p > 0) return p;
                }
            }
        } catch (Exception ignored) {
        }

        return -1;
    }

    private int waitForPortFile(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            int p = this.readPortFromTempFile();
            if (p > 0 && checkPortAlive(p)) {
                return p;
            }
            try {
                Thread.sleep(150L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
            if (this.helperProcess != null && !this.helperProcess.isAlive()) {
                return -1;
            }
        }
        return -1;
    }

    private int startLinuxBridge() {
        try {
            this.localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int p = this.localServer.getAddress().getPort();
            this.setupBridgeRoutes();
            this.localServer.start();
            this.localPoller = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NV-LinuxMprisPoller");
                t.setDaemon(true);
                return t;
            });
            this.localPoller.scheduleWithFixedDelay(this::pollLinuxMpris, 0L, 300L, TimeUnit.MILLISECONDS);
            this.port = p;
            this.started = true;
            return p;
        } catch (Exception e) {
            return -1;
        }
    }

    private int startMacBridge() {
        try {
            this.localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int p = this.localServer.getAddress().getPort();
            this.setupBridgeRoutes();
            this.localServer.start();
            this.port = p;
            this.started = true;
            return p;
        } catch (Exception e) {
            return -1;
        }
    }

    private void setupBridgeRoutes() {
        this.localServer.createContext("/now-playing", exchange -> {
            byte[] bytes = this.cachedNowPlayingJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        this.localServer.createContext("/control/play-pause", exchange -> {
            this.runCommand("playerctl", "play-pause");
            exchange.sendResponseHeaders(200, -1L);
            exchange.close();
        });
        this.localServer.createContext("/control/next", exchange -> {
            this.runCommand("playerctl", "next");
            exchange.sendResponseHeaders(200, -1L);
            exchange.close();
        });
        this.localServer.createContext("/control/previous", exchange -> {
            this.runCommand("playerctl", "previous");
            exchange.sendResponseHeaders(200, -1L);
            exchange.close();
        });
        this.localServer.createContext("/cover", exchange -> {
            byte[] empty = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, empty.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(empty);
            }
        });
    }

    private void pollLinuxMpris() {
        try {
            List<String> cmd = List.of("playerctl", "metadata", "--format",
                "{{status}}:::{{artist}}:::{{title}}:::{{album}}:::{{position}}:::{{mpris:length}}");
            Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String output;
            try (InputStream in = proc.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            if (proc.waitFor(300L, TimeUnit.MILLISECONDS) && proc.exitValue() == 0 && output.contains(":::")) {
                String[] parts = output.split(":::", -1);
                String status = parts.length > 0 ? parts[0].toLowerCase() : "stopped";
                String artist = parts.length > 1 ? parts[1] : "";
                String title = parts.length > 2 ? parts[2] : "";
                String album = parts.length > 3 ? parts[3] : "";
                long pos = parts.length > 4 ? tryParseLong(parts[4]) / 1000L : 0L;
                long len = parts.length > 5 ? tryParseLong(parts[5]) / 1000L : 0L;
                boolean active = !title.isEmpty() || !artist.isEmpty();
                this.cachedNowPlayingJson = String.format(
                    "{\"isActive\":%b,\"title\":\"%s\",\"artist\":\"%s\",\"album\":\"%s\",\"status\":\"%s\",\"positionMs\":%d,\"durationMs\":%d}",
                    active, escape(title), escape(artist), escape(album), status, pos, len
                );
            } else {
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
            }
        } catch (Exception ignored) {
        }
    }

    private void runCommand(String... cmd) {
        try {
            new ProcessBuilder(cmd).start();
        } catch (Exception ignored) {
        }
    }

    private static long tryParseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public synchronized void stop() {
        this.started = false;
        if (this.localPoller != null) {
            this.localPoller.shutdownNow();
            this.localPoller = null;
        }
        if (this.localServer != null) {
            try {
                this.localServer.stop(0);
            } catch (Exception ignored) {
            }
            this.localServer = null;
        }

        // Stop only the process owned by this client instance.
        if (this.port > 0 && this.helperProcess != null) {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1L)).build();
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + this.port + "/shutdown"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(1L))
                    .build();
                client.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        }

        if (this.helperProcess != null && this.helperProcess.isAlive()) {
            try {
                if (!this.helperProcess.waitFor(750L, TimeUnit.MILLISECONDS)) this.helperProcess.destroyForcibly();
            } catch (InterruptedException interrupted) {
                this.helperProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        this.helperProcess = null;
        this.port = -1;
    }

}
