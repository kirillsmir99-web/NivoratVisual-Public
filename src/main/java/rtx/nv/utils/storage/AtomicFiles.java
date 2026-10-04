package rtx.nv.utils.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Writes a complete document before replacing the previous copy. Temp files stay on the same filesystem. */
public final class AtomicFiles {
    private static final Object[] LOCKS = new Object[16];
    static { for (int i = 0; i < LOCKS.length; ++i) LOCKS[i] = new Object(); }
    private AtomicFiles() {}

    public static void writeUtf8(Path destination, String content) throws IOException {
        Path target = destination.toAbsolutePath().normalize();
        synchronized (LOCKS[(target.hashCode() & Integer.MAX_VALUE) % LOCKS.length]) {
            writeLocked(target, content);
        }
    }

    private static void writeLocked(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(content);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            // Windows readers and antivirus can briefly hold the old file without delete sharing.
            for (int attempt = 0; ; ++attempt) {
                try {
                    replace(temporary, target);
                    break;
                } catch (AccessDeniedException locked) {
                    if (attempt >= 4) throw locked;
                    try { Thread.sleep(5L << attempt); }
                    catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        InterruptedIOException failure = new InterruptedIOException("Config replacement interrupted");
                        failure.initCause(interrupted);
                        throw failure;
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            if (Files.exists(target)) Files.copy(target, target.resolveSibling(target.getFileName()+".bak"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
