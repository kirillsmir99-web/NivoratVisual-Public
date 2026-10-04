package rtx.nv.utils.net;

import java.io.IOException;
import java.io.InputStream;

public final class BoundedInput {
    private BoundedInput() {}

    public static byte[] read(InputStream input, int maximumBytes) throws IOException {
        if (maximumBytes < 0 || maximumBytes == Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid response byte limit");
        byte[] bytes = input.readNBytes(maximumBytes + 1);
        if (bytes.length > maximumBytes) throw new IOException("Response exceeds byte limit");
        return bytes;
    }
}
