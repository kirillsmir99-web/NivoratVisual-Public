package rtx.nv.api.mods.accountswitcher.ias;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.nv.api.mods.accountswitcher.ias.config.IASConfig;
import rtx.nv.api.mods.accountswitcher.ias.config.IASStorage;
import rtx.nv.api.mods.accountswitcher.ias.utils.Holder;

public final class IAS {
    public static final String CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";
    public static final Duration TIMEOUT = Duration.ofSeconds(Long.getLong("ias.timeout", 15L));
    public static final String USER_AGENT = "NV-Account-Switcher/1.0.0";
    private static final Logger LOGGER = LoggerFactory.getLogger("IAS");
    private static ScheduledExecutorService executor;
    private static Path gameDirectory;
    private static Path configDirectory;
    private static boolean disabled;

    private IAS() {
        throw new AssertionError("No instances.");
    }

    static {
        disabled = false;
    }

    public static void close() {
        LOGGER.info("IAS: Closing IAS...");
        try {
            ScheduledExecutorService scheduledExecutorService = executor;
            if (scheduledExecutorService != null) {
                LOGGER.info("IAS: Shutting down IAS executor...");
                scheduledExecutorService.shutdown();
                if (scheduledExecutorService.awaitTermination(30L, TimeUnit.SECONDS)) {
                    LOGGER.info("IAS: IAS executor shut down.");
                } else {
                    LOGGER.warn("IAS: Unable to shutdown IAS executor. Shutting down forcefully...");
                    scheduledExecutorService.shutdownNow();
                    if (scheduledExecutorService.awaitTermination(30L, TimeUnit.SECONDS)) {
                        LOGGER.info("IAS: IAS executor shut down forcefully.");
                    } else {
                        LOGGER.error("IAS: Unable to shutdown IAS executor forcefully.");
                    }
                }
            }
        }
        catch (InterruptedException interruptedException) {
            LOGGER.error("IAS: IAS executor interrupted while shutting down. Shutting down forcefully...", (Throwable)interruptedException);
            ScheduledExecutorService scheduledExecutorService = executor;
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdownNow();
            }
            Thread.currentThread().interrupt();
        }
        executor = null;
        if (gameDirectory != null) {
            try {
                IAS.disclaimersStorage();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        LOGGER.info("IAS: IAS has been unloaded.");
    }

    public static void init(Path path, Path path2) {
        LOGGER.info("IAS: Initializing IAS...");
        gameDirectory = path;
        configDirectory = path2;
        try {
            IAS.disclaimersStorage();
        }
        catch (Throwable throwable) {
            LOGGER.error("IAS: Unable to write disclaimers.", throwable);
        }
        try {
            IAS.loadConfig();
        }
        catch (Throwable throwable) {
            LOGGER.error("IAS: Unable to load IAS config.", throwable);
        }
        try {
            IAS.loadStorage();
        }
        catch (Throwable throwable) {
            LOGGER.error("IAS: Unable to load IAS storage.", throwable);
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "IAS"));
        LOGGER.info("IAS: IAS has been loaded.");
    }

    public static ScheduledExecutorService executor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "IAS"));
        }
        return executor;
    }

    public static void loadConfig() {
        IASConfig.load(configDirectory());
    }

    public static void saveStorage() {
        IASStorage.save(gameDirectory());
    }

    public static Path configDirectory() {
        if (configDirectory == null) {
            try {
                rtx.nv.api.mods.accountswitcher.IasService.ensureInitialized();
            } catch (Throwable ignored) {
            }
        }
        Path path = configDirectory;
        if (path == null) {
            try {
                path = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
            } catch (Throwable ignored) {
                path = Path.of("config");
            }
        }
        return path;
    }

    public static Path gameDirectory() {
        if (gameDirectory == null) {
            try {
                rtx.nv.api.mods.accountswitcher.IasService.ensureInitialized();
            } catch (Throwable ignored) {
            }
        }
        Path path = gameDirectory;
        if (path == null) {
            try {
                path = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            } catch (Throwable ignored) {
                path = Path.of(".");
            }
        }
        return path;
    }

    public static void gameDisclaimerShownStorage() {
        IASStorage.gameDisclaimerShown(gameDirectory());
    }

    public static void saveConfig() {
        IASConfig.save(configDirectory());
    }

    public static boolean disabled() {
        return disabled;
    }

    public static void loadStorage() {
        IASStorage.load(gameDirectory);
    }

    public static void disclaimersStorage() {
        IASStorage.disclaimers(gameDirectory);
    }
}

