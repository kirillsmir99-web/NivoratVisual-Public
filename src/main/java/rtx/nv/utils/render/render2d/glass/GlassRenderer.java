package rtx.nv.utils.render.render2d.glass;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.ClientSplits;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.utils.render.render2d.glass.GlassRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class GlassRenderer
implements AutoCloseable {
    private static final int MAX_GLASSES = 320;
    private static final int PARAMS_PER_GLASS = 12;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 61440;
    private static volatile GlassRenderer instance;
    private static float frameTimeSeconds = 0.0f;
    private static float accumulatedEdgeTime = 0.0f;
    private static long lastEdgeFrameNs = 0L;
    public static final RenderPipeline GLASS_PIPELINE;
    private final List<BuiltGlass> preparedGlasses = new ArrayList<BuiltGlass>(64);
    private final List<BlurCapture> preparedCaptures = new ArrayList<BlurCapture>(64);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private GlassRenderer() {
    }

    static {
        GLASS_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlassRenderer.id("pipeline/glass")).withVertexShader(GlassRenderer.id("ui/glass/glass")).withFragmentShader(GlassRenderer.id("ui/glass/glass")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("GlassParamsArray", UniformType.UNIFORM_BUFFER).withUniform("PaletteParams", UniformType.UNIFORM_BUFFER).withUniform("SplitParams", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static GlassRenderer getInstance() {
        GlassRenderer glassRenderer = instance;
        if (glassRenderer != null) return glassRenderer;
        Class<GlassRenderer> clazz = GlassRenderer.class;
        synchronized (GlassRenderer.class) {
            glassRenderer = instance;
            if (glassRenderer != null) return glassRenderer;
            instance = glassRenderer = new GlassRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return glassRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedGlasses.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltGlass builtGlass) {
        this.submit(this.activeGraphics, builtGlass);
    }

    private void submit(DrawContext drawContext, BuiltGlass builtGlass) {
        if (drawContext == null || builtGlass == null || !builtGlass.visible()) {
            return;
        }
        try {
            InterfaceModule iface = InterfaceModule.getInstance();
            float material = iface == null ? 0.0f : iface.getStyleTransition();
            if (!builtGlass.mosaicEnabled() && material > 0.001f) {
                boolean allowMosaic = true;
                if (iface != null && iface.isMosaicOnBackgroundOnly()) {
                    allowMosaic = builtGlass.width() >= 250.0f && builtGlass.height() >= 180.0f;
                }
                if (allowMosaic) {
                    builtGlass = builtGlass.withMosaic(true, material, iface.mosaicScale.getFloat(), iface.mosaicSpeed.getFloat(), iface.mosaicSeam.getFloat(), iface.mosaicBevel.getFloat(), iface.mosaicCellGlow.getFloat(), iface.mosaicMorph.getFloat(), iface.textReadability.getFloat());
                }
            }
            BuiltGlass builtGlass2 = this.normalize(builtGlass);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            BlurCapture blurCapture = new BlurCapture();
            BlurFramebuffer.getInstance().requestCapture(drawContext, new BuiltBlur(builtGlass2.x(), builtGlass2.y(), builtGlass2.width(), builtGlass2.height(), builtGlass2.radiusTopLeft(), builtGlass2.radiusTopRight(), builtGlass2.radiusBottomRight(), builtGlass2.radiusBottomLeft(), 1.0f, builtGlass2.blurRadius(), -1), blurCapture);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new GlassRenderState(matrix3x2f, builtGlass2, ScissorUtil.current(), blurCapture));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltGlass normalize(BuiltGlass builtGlass) {
        float f = Math.max(0.0f, Math.min(builtGlass.width(), builtGlass.height()) * 0.5f);
        float f2 = GlassRenderer.clamp(builtGlass.radiusTopLeft(), 0.0f, f);
        float f3 = GlassRenderer.clamp(builtGlass.radiusTopRight(), 0.0f, f);
        float f4 = GlassRenderer.clamp(builtGlass.radiusBottomRight(), 0.0f, f);
        float f5 = GlassRenderer.clamp(builtGlass.radiusBottomLeft(), 0.0f, f);
        float material = builtGlass.mosaicEnabled() ? builtGlass.styleTransition() : 0.0f;
        f2 += (Math.min(f2, rtx.nv.api.ui.UiTokens.MOSAIC_RADIUS) - f2) * material;
        f3 += (Math.min(f3, rtx.nv.api.ui.UiTokens.MOSAIC_RADIUS) - f3) * material;
        f4 += (Math.min(f4, rtx.nv.api.ui.UiTokens.MOSAIC_RADIUS) - f4) * material;
        f5 += (Math.min(f5, rtx.nv.api.ui.UiTokens.MOSAIC_RADIUS) - f5) * material;
        float f6 = GlassRenderer.clamp(builtGlass.globalAlpha(), 0.0f, 1.0f);
        float f7 = Math.max(builtGlass.fresnelPower(), 0.001f);
        float f8 = GlassRenderer.clamp(builtGlass.baseAlpha(), 0.0f, 1.0f);
        float f9 = GlassRenderer.clamp(builtGlass.fresnelMix(), 0.0f, 1.0f);
        float f10 = Math.max(builtGlass.squirt(), 0.001f);
        float f11 = Float.isFinite(builtGlass.blurRadius()) ? GlassRenderer.clamp(builtGlass.blurRadius(), 0.1f, 64.0f) : 0.1f;
        float f12 = Float.isFinite(builtGlass.colorOffset()) ? builtGlass.colorOffset() : 0.0f;
        f12 -= (float)Math.floor(f12);
        if (f2 == builtGlass.radiusTopLeft() && f3 == builtGlass.radiusTopRight() && f4 == builtGlass.radiusBottomRight() && f5 == builtGlass.radiusBottomLeft() && f6 == builtGlass.globalAlpha() && f7 == builtGlass.fresnelPower() && f8 == builtGlass.baseAlpha() && f9 == builtGlass.fresnelMix() && f10 == builtGlass.squirt() && f11 == builtGlass.blurRadius() && f12 == builtGlass.colorOffset()) {
            return builtGlass;
        }
        return new BuiltGlass(builtGlass.x(), builtGlass.y(), builtGlass.width(), builtGlass.height(), f2, f3, f4, f5, builtGlass.color(), f6, f7, builtGlass.fresnelColor(), f8, builtGlass.fresnelInvert(), f9, builtGlass.distortStrength(), f10, builtGlass.z(), f11, builtGlass.secondColor(), f12, builtGlass.splitIndex(), builtGlass.paletteSlot(), builtGlass.mosaicEnabled(), builtGlass.mosaicScale(), builtGlass.mosaicSpeed(), builtGlass.mosaicSeam(), builtGlass.mosaicBevel(), builtGlass.mosaicCellGlow(), builtGlass.mosaicMorph(), builtGlass.textReadability(), builtGlass.styleTransition(), builtGlass.liveEdgeEnabled(), builtGlass.liveEdgeActivation(), builtGlass.liveEdgeIntensity(), builtGlass.liveEdgeSize(), builtGlass.liveEdgeDensity(), builtGlass.liveEdgeMotion(), builtGlass.liveEdgeSpeed(), builtGlass.liveEdgeGlow(), builtGlass.liveEdgeSeed(), builtGlass.liveEdgeProfile());
    }

    int reserve(BuiltGlass builtGlass, BlurCapture blurCapture) {
        int n = this.preparedGlasses.size();
        if (n == MAX_GLASSES) {
            return -1;
        }
        this.preparedGlasses.add(builtGlass);
        this.preparedCaptures.add(blurCapture);
        this.paramsDirty = true;
        return n;
    }

    private static void putColor(ByteBuffer byteBuffer, int n, int n2) {
        byteBuffer.putFloat(n, (float)(n2 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 4, (float)(n2 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 8, (float)(n2 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 12, (float)(n2 >>> 24 & 0xFF) / 255.0f);
    }

    public static void closeInstance() {
        GlassRenderer glassRenderer = instance;
        if (glassRenderer != null) {
            glassRenderer.close();
            instance = null;
        }
    }

    public void updateEdgeTime() {
        long now = System.nanoTime();
        if (lastEdgeFrameNs > 0L) {
            float dt = Math.min(0.1f, (float)(now - lastEdgeFrameNs) / 1.0E9f);
            InterfaceModule iface = InterfaceModule.getInstance();
            boolean motionOn = iface == null || iface.waveEdgeMotion.getValue();
            float speed = iface != null ? iface.waveEdgeSpeed.getFloat() : 0.5f;
            if (motionOn && speed > 0.001f) {
                accumulatedEdgeTime = (accumulatedEdgeTime + dt * (speed * 3.5f)) % 10000.0f;
            }
        }
        lastEdgeFrameNs = now;
        frameTimeSeconds = (float)(System.currentTimeMillis() % 36000000L) * 0.001f;
    }

    public void beginGuiFrame() {
        rtx.nv.api.ui.theme.ThemeManager.beginFrame();
        this.preparedGlasses.clear();
        this.preparedCaptures.clear();
        this.paramsDirty = false;
        this.updateEdgeTime();
    }

    public boolean isGlassPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == GLASS_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedGlasses.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedGlasses);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
        this.updateEdgeTime();
    }

    public void draw(DrawContext drawContext, BuiltGlass builtGlass) {
        this.beginFrame(drawContext);
        this.enqueue(builtGlass);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        GpuBuffer gpuBuffer;
        GpuBuffer gpuBuffer2;
        if (renderPass == null || this.preparedGlasses.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer3 = this.ensureParamsBuffer();
        if (gpuBuffer3 != null) {
            renderPass.setUniform("GlassParamsArray", gpuBuffer3);
        }
        if ((gpuBuffer2 = ClientPalette.buffer()) != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer2);
        }
        if ((gpuBuffer = ClientSplits.buffer()) != null) {
            renderPass.setUniform("SplitParams", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= (long)UNIFORM_BYTES) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glass_params", 136, (long)UNIFORM_BYTES);
            return this.paramsBuffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private void closeParamsBuffer() {
        if (this.paramsBuffer != null) {
            this.paramsBuffer.close();
            this.paramsBuffer = null;
        }
    }

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltGlass> list) {
        int n = Math.max(1, list.size()) * PARAMS_PER_GLASS * FLOATS_PER_PARAM * 4;
        ByteBuffer byteBuffer = memoryStack.calloc(n);
        for (int i = 0; i < list.size(); ++i) {
            BlurCapture blurCapture;
            BuiltGlass builtGlass = list.get(i);
            int n2 = i * PARAMS_PER_GLASS * FLOATS_PER_PARAM * 4;
            byteBuffer.putFloat(n2, builtGlass.radiusTopLeft());
            byteBuffer.putFloat(n2 + 4, builtGlass.radiusTopRight());
            byteBuffer.putFloat(n2 + 8, builtGlass.radiusBottomRight());
            byteBuffer.putFloat(n2 + 12, builtGlass.radiusBottomLeft());
            byteBuffer.putFloat(n2 + 16, builtGlass.width());
            byteBuffer.putFloat(n2 + 20, builtGlass.height());
            byteBuffer.putFloat(n2 + 24, builtGlass.splitIndex());
            byteBuffer.putFloat(n2 + 28, Math.max(builtGlass.squirt(), 0.001f));
            byteBuffer.putFloat(n2 + 32, builtGlass.globalAlpha());
            byteBuffer.putFloat(n2 + 36, builtGlass.fresnelPower());
            byteBuffer.putFloat(n2 + 40, builtGlass.baseAlpha());
            byteBuffer.putFloat(n2 + 44, builtGlass.fresnelMix());
            GlassRenderer.putColor(byteBuffer, n2 + 48, builtGlass.fresnelColor());
            byteBuffer.putFloat(n2 + 64, builtGlass.fresnelInvert() ? 1.0f : 0.0f);
            byteBuffer.putFloat(n2 + 68, builtGlass.distortStrength());
            byteBuffer.putFloat(n2 + 72, builtGlass.paletteSlot() > 0 ? 10.0f + (float)builtGlass.paletteSlot() : builtGlass.z());
            byteBuffer.putFloat(n2 + 76, builtGlass.colorOffset());
            GlassRenderer.putColor(byteBuffer, n2 + 80, builtGlass.color());
            GlassRenderer.putColor(byteBuffer, n2 + 96, builtGlass.secondColor());
            BlurCapture blurCapture2 = blurCapture = i < this.preparedCaptures.size() ? this.preparedCaptures.get(i) : null;
            if (blurCapture != null) {
                byteBuffer.putFloat(n2 + 112, blurCapture.regionX);
                byteBuffer.putFloat(n2 + 116, blurCapture.regionY);
                byteBuffer.putFloat(n2 + 120, blurCapture.regionW);
                byteBuffer.putFloat(n2 + 124, blurCapture.regionH);
            }
            byteBuffer.putFloat(n2 + 128, builtGlass.mosaicEnabled() ? builtGlass.styleTransition() : 0.0f);
            byteBuffer.putFloat(n2 + 132, builtGlass.mosaicScale());
            byteBuffer.putFloat(n2 + 136, builtGlass.mosaicSpeed());
            byteBuffer.putFloat(n2 + 140, builtGlass.mosaicSeam());
            byteBuffer.putFloat(n2 + 144, builtGlass.mosaicBevel());
            byteBuffer.putFloat(n2 + 148, builtGlass.mosaicCellGlow());
            byteBuffer.putFloat(n2 + 152, frameTimeSeconds);
            byteBuffer.putFloat(n2 + 156, builtGlass.mosaicMorph() + (float) Math.floor(builtGlass.textReadability() * 10.0f) * 10.0f);
            byteBuffer.putFloat(n2 + 160, builtGlass.liveEdgeEnabled() ? builtGlass.liveEdgeActivation() : 0.0f);
            byteBuffer.putFloat(n2 + 164, builtGlass.liveEdgeIntensity());
            byteBuffer.putFloat(n2 + 168, builtGlass.liveEdgeSize());
            byteBuffer.putFloat(n2 + 172, builtGlass.liveEdgeDensity());
            byteBuffer.putFloat(n2 + 176, accumulatedEdgeTime);
            byteBuffer.putFloat(n2 + 180, builtGlass.liveEdgeGlow());
            byteBuffer.putFloat(n2 + 184, builtGlass.liveEdgeSeed());
            byteBuffer.putFloat(n2 + 188, (float) builtGlass.liveEdgeProfile());
        }
        byteBuffer.position(0);
        return byteBuffer;
    }

    private GpuBuffer ensureParamsBuffer() {
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.prepareBuffers();
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedGlasses);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glass_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

