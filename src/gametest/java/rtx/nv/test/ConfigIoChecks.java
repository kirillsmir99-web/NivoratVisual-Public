package rtx.nv.test;

import rtx.nv.utils.storage.AtomicFiles;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.*;

final class ConfigIoChecks {
    static void verify() {
        Path directory = null;
        ExecutorService writers = Executors.newFixedThreadPool(2);
        try {
            directory = Files.createTempDirectory("nv-config-regression-");
            Path target = directory.resolve("config.json");
            String first = "{\"writer\":1,\"data\":\""+"A".repeat(8192)+"\"}";
            String second = "{\"writer\":2,\"data\":\""+"Б".repeat(8192)+"\"}";
            AtomicFiles.writeUtf8(target,first);
            Future<?> one = writers.submit(() -> repeat(target,first));
            Future<?> two = writers.submit(() -> repeat(target,second));
            one.get(10,TimeUnit.SECONDS); two.get(10,TimeUnit.SECONDS);
            String result = Files.readString(target);
            if (!result.equals(first) && !result.equals(second)) throw new AssertionError("Concurrent save produced partial content");
            if (System.getProperty("os.name").startsWith("Windows")) {
                @SuppressWarnings({"rawtypes","unchecked"})
                OpenOption denyDelete = (OpenOption)Enum.valueOf((Class)Class.forName("com.sun.nio.file.ExtendedOpenOption"),"NOSHARE_DELETE");
                FileChannel reader = FileChannel.open(target,Set.of(StandardOpenOption.READ,denyDelete));
                Future<?> release = writers.submit(() -> {
                    try { Thread.sleep(25); reader.close(); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                });
                try { AtomicFiles.writeUtf8(target,first); }
                finally { reader.close(); }
                release.get(5,TimeUnit.SECONDS);
                if (!Files.readString(target).equals(first)) throw new AssertionError("Transient Windows lock was not recovered");
            }
            try (var children = Files.list(directory)) {
                if (children.anyMatch(p -> p.toString().endsWith(".tmp"))) throw new AssertionError("Temporary config leaked");
            }
            rtx.nv.NV.LOGGER.info("[NV-TEST] Concurrent UTF-8 atomic saves and transient Windows file lock passed");
        } catch (Exception failure) { throw new AssertionError("Config IO regression",failure); }
        finally {
            writers.shutdownNow();
            if (directory != null) {
                try (var children = Files.list(directory)) { for (Path path : children.toList()) Files.deleteIfExists(path); }
                catch (Exception ignored) {}
                try { Files.deleteIfExists(directory); } catch (Exception ignored) {}
            }
        }
    }
    private static void repeat(Path path,String content) {
        try { for (int i=0;i<25;++i) AtomicFiles.writeUtf8(path,content); }
        catch (Exception failure) { throw new CompletionException(failure); }
    }
}
