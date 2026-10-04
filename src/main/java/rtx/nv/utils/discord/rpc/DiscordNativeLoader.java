package rtx.nv.utils.discord.rpc;
import com.sun.jna.NativeLibrary;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DiscordNativeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"NV/DiscordRPC");
    private static final AtomicBoolean PREPARED = new AtomicBoolean(false);

    private DiscordNativeLoader() {
    }

    public static void prepare() {
        if (!PREPARED.compareAndSet(false, true)) {
            return;
        }
        String string = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!string.contains("win")) {
            return;
        }
        boolean bl = System.getProperty("os.arch", "").contains("64");
        String string2 = bl ? "/win32-x86-64/discord-rpc.dll" : "/win32-x86/discord-rpc.dll";
        try {
            Path path = Path.of(System.getProperty("java.io.tmpdir"), "nv-discord-rpc");
            Files.createDirectories(path, new FileAttribute[0]);
            Path path2 = path.resolve("discord-rpc.dll");
            try (InputStream inputStream = DiscordNativeLoader.class.getResourceAsStream(string2);){
                if (inputStream == null) {
                    LOGGER.debug("Discord RPC DLL resource not found: {}", (Object)string2);
                    return;
                }
                Files.copy(inputStream, path2, StandardCopyOption.REPLACE_EXISTING);
            }
            NativeLibrary.addSearchPath((String)"discord-rpc", (String)path.toAbsolutePath().toString());
        }
        catch (IOException iOException) {
            LOGGER.debug("Failed to prepare Discord RPC native library", (Throwable)iOException);
        }
    }
}

