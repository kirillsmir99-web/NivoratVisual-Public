package rtx.nv.utils.render.render2d;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.lwjgl.system.MemoryStack;

public final class ClientSplits {
    public static final int MAX_SPLITS = 16;
    private static final int VEC4_PER_SPLIT = 3;
    private static final int FLOATS_PER_VEC4 = 4;
    private static final int FLOATS = 192;
    private static final int UNIFORM_BYTES = 768;
    private static final float[] data = new float[192];
    private static int activeCount;
    private static GpuBuffer buffer;

    private ClientSplits() {
    }

    public static void reset() {
        activeCount = 0;
        Arrays.fill(data, 0.0f);
    }

    public static void update() {
        GpuBuffer gpuBuffer = ClientSplits.ensureBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(768);
            for (int i = 0; i < 192; ++i) {
                byteBuffer.putFloat(i * 4, data[i]);
            }
            byteBuffer.position(0);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, 768L), byteBuffer);
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    public static int add(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10) {
        if (activeCount >= 16) {
            return 0;
        }
        int n = activeCount++;
        int n2 = n * 3 * 4;
        ClientSplits.data[n2] = f;
        ClientSplits.data[n2 + 1] = f2;
        ClientSplits.data[n2 + 2] = f3;
        ClientSplits.data[n2 + 3] = f4;
        ClientSplits.data[n2 + 4] = f5;
        ClientSplits.data[n2 + 5] = f6;
        ClientSplits.data[n2 + 6] = f7;
        ClientSplits.data[n2 + 7] = f8;
        ClientSplits.data[n2 + 8] = f9;
        ClientSplits.data[n2 + 9] = f10;
        return n + 1;
    }

    public static GpuBuffer buffer() {
        return ClientSplits.ensureBuffer();
    }

    public static void closeBuffer() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    private static GpuBuffer ensureBuffer() {
        if (buffer != null && !buffer.isClosed() && buffer.size() >= 768L) {
            return buffer;
        }
        ClientSplits.closeBuffer();
        try {
            buffer = RenderSystem.getDevice().createBuffer(() -> "nv_client_splits", 136, 768L);
            return buffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }
}

