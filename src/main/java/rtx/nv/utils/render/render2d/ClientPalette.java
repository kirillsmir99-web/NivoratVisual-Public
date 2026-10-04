package rtx.nv.utils.render.render2d;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.render.render2d.GradientSweep;

public final class ClientPalette {
    public static final int MAX_STOPS = 6;
    private static final int VEC4_FLOATS = 4;
    private static final int META2_OFFSET = 112;
    private static final int UNIFORM_BYTES = 128;
    public static final int SLOT_STRIDE_BYTES = 128;
    public static final int SLOTS = 5;
    private static final int TOTAL_BYTES = 640;
    private static final long PERIOD_MS = 24000L;
    public static final long SCROLL_PERIOD_MS = 2200L;
    private static GpuBuffer buffer;
    private static final int[] displayed;
    private static final int[] fallback;
    private static int count;
    private static float phase;
    private static long lastMs;
    private static float styleId;
    private static float prevStyle;
    private static float lastStyleId;
    private static float closed;

    private ClientPalette() {
    }

    static {
        displayed = new int[]{0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF};
        fallback = new int[]{0xFFFFFF};
        count = 1;
        lastStyleId = -1.0f;
    }

    public static void update() {
        int[] nArray = ClientAccent.blendedClientPalette();
        if (nArray == null || nArray.length == 0) {
            nArray = fallback;
        }
        ClientPalette.resample(nArray, displayed);
        count = 6;
        closed = ClientAccent.closedFactor();
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        long l = System.currentTimeMillis();
        if (lastMs != 0L && interfaceModule != null && interfaceModule.clientColorMovement() && (phase = (phase + (float)(l - lastMs) / 24000.0f) % 1.0f) < 0.0f) {
            phase += 1.0f;
        }
        lastMs = l;
        float f = styleId = interfaceModule == null ? 1.0f : (float)interfaceModule.gradientStyleId();
        if (lastStyleId >= 0.0f && styleId != lastStyleId) {
            prevStyle = lastStyleId;
            GradientSweep.trigger();
        }
        lastStyleId = styleId;
        GpuBuffer gpuBuffer = ClientPalette.ensureBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(128);
            byteBuffer.putFloat(0, count);
            byteBuffer.putFloat(4, phase);
            byteBuffer.putFloat(8, styleId);
            byteBuffer.putFloat(12, GradientSweep.progress());
            byteBuffer.putFloat(112, prevStyle);
            byteBuffer.putFloat(116, closed);
            byteBuffer.putFloat(120, ClientPalette.scrollPhase());
            for (int i = 0; i < count; ++i) {
                int n = displayed[i] & 0xFFFFFF;
                int n2 = (1 + i) * 4 * 4;
                byteBuffer.putFloat(n2, (float)(n >>> 16 & 0xFF) / 255.0f);
                byteBuffer.putFloat(n2 + 4, (float)(n >>> 8 & 0xFF) / 255.0f);
                byteBuffer.putFloat(n2 + 8, (float)(n & 0xFF) / 255.0f);
                byteBuffer.putFloat(n2 + 12, 1.0f);
            }
            byteBuffer.position(0);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, 128L), byteBuffer);
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    public static int count() {
        return count;
    }

    public static GpuBuffer buffer() {
        return ClientPalette.ensureBuffer();
    }

    public static float closed() {
        return closed;
    }

    public static float phase() {
        return phase;
    }

    private static float chan(int[] nArray, int n, int n2) {
        int n3 = nArray[Math.max(0, Math.min(nArray.length - 1, n))];
        return (float)(n3 >> 16 - n2 * 8 & 0xFF) / 255.0f;
    }

    public static float scrollPhase() {
        return (float)(System.currentTimeMillis() % 2200L) / 2200.0f;
    }

    public static void closeBuffer() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    public static int[] colors() {
        return displayed;
    }

    public static int loopColor(float f) {
        int n;
        int n2;
        int[] nArray = displayed;
        int n3 = count;
        if (nArray == null || nArray.length == 0) {
            return 0xFFFFFF;
        }
        if (n3 <= 1) {
            return nArray[0] & 0xFFFFFF;
        }
        float f2 = f - (float)Math.floor(f);
        float f3 = f2 * (float)n3;
        int n4 = Math.max(0, Math.min(n3 - 1, (int)Math.floor(f3)));
        float f4 = Math.max(0.0f, Math.min(1.0f, f3 - (float)n4));
        int n5 = n4 - 1;
        if (n5 < 0) {
            n5 += n3;
        }
        if ((n2 = n4 + 1) >= n3) {
            n2 -= n3;
        }
        if ((n = n4 + 2) >= n3) {
            n -= n3;
        }
        float f5 = f4 * f4;
        float f6 = f5 * f4;
        float f7 = 0.5f - 0.5f * (float)Math.cos((float)Math.PI * 2 * f2);
        float f8 = Math.max(0.0f, Math.min(1.0f, closed));
        int n6 = 0;
        for (int i = 0; i < 3; ++i) {
            float f9 = ClientPalette.chan(nArray, n5, i);
            float f10 = ClientPalette.chan(nArray, n4, i);
            float f11 = ClientPalette.chan(nArray, n2, i);
            float f12 = ClientPalette.chan(nArray, n, i);
            float f13 = 0.5f * (2.0f * f10 + (-f9 + f11) * f4 + (2.0f * f9 - 5.0f * f10 + 4.0f * f11 - f12) * f5 + (-f9 + 3.0f * f10 - 3.0f * f11 + f12) * f6);
            float f14 = ClientPalette.rampChannel(f7, i);
            float f15 = f14 + (f13 - f14) * f8;
            int n7 = Math.round(Math.max(0.0f, Math.min(1.0f, f15)) * 255.0f);
            n6 |= n7 << 16 - i * 8;
        }
        return n6;
    }

    public static boolean isTransitioning() {
        return ClientAccent.isModeTransitioning();
    }

    public static int[] cornerColors(float f) {
        int n = Math.round(Math.max(0.0f, Math.min(1.0f, f)) * 255.0f);
        float f2 = phase;
        return new int[]{n << 24 | ClientPalette.loopColor(f2), n << 24 | ClientPalette.loopColor(f2 + 0.25f), n << 24 | ClientPalette.loopColor(f2 + 0.5f), n << 24 | ClientPalette.loopColor(f2 + 0.75f)};
    }

    public static void writeRemoteSlot(int n, int[] nArray, float f, float f2, float f3, float f4, float f5) {
        if (n <= 0 || n >= 5 || nArray == null || nArray.length == 0) {
            return;
        }
        GpuBuffer gpuBuffer = ClientPalette.ensureBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(128);
            byteBuffer.putFloat(0, 6.0f);
            byteBuffer.putFloat(4, f);
            byteBuffer.putFloat(8, f2);
            byteBuffer.putFloat(12, f4);
            byteBuffer.putFloat(112, f5);
            byteBuffer.putFloat(116, f3);
            byteBuffer.putFloat(120, ClientPalette.scrollPhase());
            for (int i = 0; i < 6; ++i) {
                int n2 = nArray[Math.min(i, nArray.length - 1)] & 0xFFFFFF;
                int n3 = (1 + i) * 4 * 4;
                byteBuffer.putFloat(n3, (float)(n2 >>> 16 & 0xFF) / 255.0f);
                byteBuffer.putFloat(n3 + 4, (float)(n2 >>> 8 & 0xFF) / 255.0f);
                byteBuffer.putFloat(n3 + 8, (float)(n2 & 0xFF) / 255.0f);
                byteBuffer.putFloat(n3 + 12, 1.0f);
            }
            byteBuffer.position(0);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice((long)(n * 128), 128L), byteBuffer);
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    public static void writeRemoteSlot(int n, int[] nArray, float f, float f2, boolean closed, float f4, float f5) {
        writeRemoteSlot(n, nArray, f, f2, closed ? 1.0f : 0.0f, f4, f5);
    }

    public static void writeRemoteSlot(int n, int[] nArray, float f, float f2, float f3) {
        ClientPalette.writeRemoteSlot(n, nArray, f, f2, f3, -1.0f, f2);
    }

    private static float rampChannel(float f, int n) {
        int[] nArray = displayed;
        int n2 = count;
        if (n2 <= 1) {
            return ClientPalette.chan(nArray, 0, n);
        }
        float f2 = Math.max(0.0f, Math.min(1.0f, f));
        float f3 = f2 * (float)(n2 - 1);
        int n3 = Math.max(0, Math.min(n2 - 1, (int)Math.floor(f3)));
        int n4 = Math.min(n3 + 1, n2 - 1);
        float f4 = Math.max(0.0f, Math.min(1.0f, f3 - (float)n3));
        f4 = f4 * f4 * (3.0f - 2.0f * f4);
        float f5 = ClientPalette.chan(nArray, n3, n);
        float f6 = ClientPalette.chan(nArray, n4, n);
        return f5 + (f6 - f5) * f4;
    }

    private static GpuBuffer ensureBuffer() {
        if (buffer != null && !buffer.isClosed() && buffer.size() >= 640L) {
            return buffer;
        }
        ClientPalette.closeBuffer();
        try {
            buffer = RenderSystem.getDevice().createBuffer(() -> "nv_client_palette", 136, 640L);
            return buffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    public static float prevStyle() {
        return prevStyle;
    }

    private static int mixRgb(int n, int n2, float f) {
        float f2 = f < 0.0f ? 0.0f : Math.min(f, 1.0f);
        int n3 = n >> 16 & 0xFF;
        int n4 = n >> 8 & 0xFF;
        int n5 = n & 0xFF;
        int n6 = n2 >> 16 & 0xFF;
        int n7 = n2 >> 8 & 0xFF;
        int n8 = n2 & 0xFF;
        int n9 = Math.round((float)n3 + (float)(n6 - n3) * f2);
        int n10 = Math.round((float)n4 + (float)(n7 - n4) * f2);
        int n11 = Math.round((float)n5 + (float)(n8 - n5) * f2);
        return n9 << 16 | n10 << 8 | n11;
    }

    private static void resample(int[] nArray, int[] nArray2) {
        if (nArray.length == 1) {
            int n = nArray[0] & 0xFFFFFF;
            for (int i = 0; i < nArray2.length; ++i) {
                nArray2[i] = n;
            }
            return;
        }
        for (int i = 0; i < nArray2.length; ++i) {
            float f = nArray2.length <= 1 ? 0.0f : (float)i / (float)(nArray2.length - 1);
            float f2 = f * (float)(nArray.length - 1);
            int n = (int)f2;
            if (n > nArray.length - 2) {
                n = nArray.length - 2;
            }
            nArray2[i] = ClientPalette.mixRgb(nArray[n], nArray[n + 1], f2 - (float)n);
        }
    }

    public static float styleId() {
        return styleId;
    }
}
