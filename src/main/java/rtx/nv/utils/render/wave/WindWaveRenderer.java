package rtx.nv.utils.render.wave;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import rtx.nv.NV;
import rtx.nv.api.modules.impl.Visuals.Ambience;

public final class WindWaveRenderer {
    private static final int UNIFORM_SIZE = 16;
    private static final double TIME_WRAP = 3600.0;
    private static RenderPipeline solidPipeline;
    private static RenderPipeline cutoutPipeline;
    private static GpuBuffer uniformBuffer;
    private static boolean disabledAfterError;
    private static boolean active;
    private static long lastNanos;
    private static double phase;

    private WindWaveRenderer() {
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static void init() {
        if (solidPipeline == null || cutoutPipeline == null) {
            RenderPipeline renderPipeline = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.TERRAIN_SNIPPET}).withLocation(WindWaveRenderer.id("pipeline/wave_solid_terrain")).withVertexShader(WindWaveRenderer.id("core/terrain_wave")).withUniform("WaveParams", UniformType.UNIFORM_BUFFER).build();
            RenderPipeline renderPipeline2 = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.TERRAIN_SNIPPET}).withLocation(WindWaveRenderer.id("pipeline/wave_cutout_terrain")).withVertexShader(WindWaveRenderer.id("core/terrain_wave")).withUniform("WaveParams", UniformType.UNIFORM_BUFFER).withShaderDefine("ALPHA_CUTOUT", 0.5f).build();
            if (!RenderSystem.getDevice().precompilePipeline(renderPipeline).isValid() || !RenderSystem.getDevice().precompilePipeline(renderPipeline2).isValid()) {
                disabledAfterError = true;
                NV.LOGGER.warn("WindWave disabled: terrain_wave shader failed to compile");
                return;
            }
            solidPipeline = RenderPipelines.register((RenderPipeline)renderPipeline);
            cutoutPipeline = RenderPipelines.register((RenderPipeline)renderPipeline2);
        }
        if (uniformBuffer == null || uniformBuffer.isClosed()) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_wave_params", 136, 16L);
        }
    }

    public static RenderPipeline substitute(RenderPipeline renderPipeline) {
        if (!active || uniformBuffer == null || uniformBuffer.isClosed()) {
            return renderPipeline;
        }
        if (renderPipeline == RenderPipelines.SOLID_TERRAIN && solidPipeline != null) {
            return solidPipeline;
        }
        if (renderPipeline == RenderPipelines.CUTOUT_TERRAIN && cutoutPipeline != null) {
            return cutoutPipeline;
        }
        return renderPipeline;
    }

    public static void beginFrame() {
        Ambience ambience = Ambience.getInstance();
        if (disabledAfterError || ambience == null || !ambience.isWindActive() || rtx.nv.utils.render.post.handsflame.IrisShaderCompat.isShaderPackInUse()) {
            active = false;
            lastNanos = 0L;
            return;
        }
        try {
            WindWaveRenderer.init();
            if (disabledAfterError) {
                active = false;
                return;
            }
            long l = System.nanoTime();
            float f = lastNanos == 0L ? 0.016f : Math.min((float)(l - lastNanos) * 1.0E-9f, 0.1f);
            lastNanos = l;
            double d = 0.5 + (double)ambience.getWindSpeed() * 2.0;
            phase = (phase + (double)f * d) % 3600.0;
            try (MemoryStack memoryStack = MemoryStack.stackPush();){
                ByteBuffer byteBuffer = memoryStack.calloc(16);
                byteBuffer.putFloat(0, (float)phase);
                byteBuffer.putFloat(4, ambience.getWindGrassStrength());
                byteBuffer.putFloat(8, ambience.getWindLeavesStrength());
                byteBuffer.putFloat(12, ambience.hasWindGusts() ? 1.0f : 0.0f);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(0L, 16L), byteBuffer);
            }
            active = true;
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            active = false;
            WindWaveRenderer.closeUniform();
            NV.LOGGER.warn("WindWave disabled after error", throwable);
        }
    }

    public static void bindParams(RenderPass renderPass) {
        if (uniformBuffer != null && !uniformBuffer.isClosed()) {
            renderPass.setUniform("WaveParams", uniformBuffer);
        }
    }

    private static void closeUniform() {
        if (uniformBuffer != null) {
            try {
                if (!uniformBuffer.isClosed()) {
                    uniformBuffer.close();
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            uniformBuffer = null;
        }
    }
}

