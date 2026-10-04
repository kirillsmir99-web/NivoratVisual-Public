package rtx.nv.utils.render.others;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;

public final class FullscreenQuadBuffer {
    private static final int VERTEX_COUNT = 6;
    private static final int BUFFER_SIZE = 6 * VertexFormats.POSITION.getVertexSize();
    private static GpuBuffer buffer;

    private FullscreenQuadBuffer() {
    }

    public static void close() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    public static GpuBuffer getOrCreate() {
        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        if (gpuDevice == null) {
            return null;
        }
        if (buffer != null && !buffer.isClosed()) {
            return buffer;
        }
        try (BufferAllocator bufferAllocator = new BufferAllocator(BUFFER_SIZE);){
            GpuBuffer gpuBuffer;
            block13: {
                BuiltBuffer builtBuffer = FullscreenQuadBuffer.buildFullscreenQuad(bufferAllocator);
                try {
                    gpuBuffer = buffer = gpuDevice.createBuffer(() -> "nv:fullscreen_quad", 32, builtBuffer.getBuffer());
                    if (builtBuffer == null) break block13;
                }
                catch (Throwable throwable) {
                    if (builtBuffer != null) {
                        try {
                            builtBuffer.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                builtBuffer.close();
            }
            return gpuBuffer;
        }
    }

    private static BuiltBuffer buildFullscreenQuad(BufferAllocator bufferAllocator) {
        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION);
        bufferBuilder.vertex(-1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(-1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(-1.0f, 1.0f, 0.0f);
        return bufferBuilder.end();
    }
}

