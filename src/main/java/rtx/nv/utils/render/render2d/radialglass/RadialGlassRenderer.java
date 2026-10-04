package rtx.nv.utils.render.render2d.radialglass;
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
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;
import rtx.nv.utils.render.render2d.radialglass.BuiltRadialGlass;
import rtx.nv.utils.render.render2d.radialglass.RadialGlassRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class RadialGlassRenderer
implements AutoCloseable {
    private static final int MAX_SECTORS = 48;
    private static final int PARAMS_PER_SECTOR = 10;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 7680;
    private static volatile RadialGlassRenderer instance;
    public static final RenderPipeline RADIAL_GLASS_PIPELINE;
    private final List<BuiltRadialGlass> preparedSectors = new ArrayList<BuiltRadialGlass>(32);
    private final List<BlurCapture> preparedCaptures = new ArrayList<BlurCapture>(32);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private RadialGlassRenderer() {
    }

    static {
        RADIAL_GLASS_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(RadialGlassRenderer.id("pipeline/radial_glass")).withVertexShader(RadialGlassRenderer.id("ui/radialglass/radialglass")).withFragmentShader(RadialGlassRenderer.id("ui/radialglass/radialglass")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("RadialGlassParamsArray", UniformType.UNIFORM_BUFFER).withUniform("PaletteParams", UniformType.UNIFORM_BUFFER).build();
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
    public static RadialGlassRenderer getInstance() {
        RadialGlassRenderer radialGlassRenderer = instance;
        if (radialGlassRenderer != null) return radialGlassRenderer;
        Class<RadialGlassRenderer> clazz = RadialGlassRenderer.class;
        synchronized (RadialGlassRenderer.class) {
            radialGlassRenderer = instance;
            if (radialGlassRenderer != null) return radialGlassRenderer;
            instance = radialGlassRenderer = new RadialGlassRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return radialGlassRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedSectors.clear();
        this.preparedCaptures.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltRadialGlass builtRadialGlass) {
        this.submit(this.activeGraphics, builtRadialGlass);
    }

    public void submit(DrawContext drawContext, BuiltRadialGlass builtRadialGlass) {
        if (drawContext == null || builtRadialGlass == null || !builtRadialGlass.visible()) {
            return;
        }
        try {
            BuiltRadialGlass builtRadialGlass2 = this.normalize(builtRadialGlass);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            float f = builtRadialGlass2.extent();
            BlurCapture blurCapture = new BlurCapture();
            BlurFramebuffer.getInstance().requestCapture(drawContext, new BuiltBlur(builtRadialGlass2.centerX() - f, builtRadialGlass2.centerY() - f, f * 2.0f, f * 2.0f, f, f, f, f, 1.0f, builtRadialGlass2.blurRadius(), -1), blurCapture);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new RadialGlassRenderState(matrix3x2f, builtRadialGlass2, ScissorUtil.current(), blurCapture));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltRadialGlass normalize(BuiltRadialGlass builtRadialGlass) {
        float f = Math.max(builtRadialGlass.outerRadius(), 0.0f);
        float f2 = RadialGlassRenderer.clamp(builtRadialGlass.innerRadius(), 0.0f, f);
        float f3 = RadialGlassRenderer.clamp(builtRadialGlass.halfAngle(), 0.0f, 1.45f);
        float f4 = (f - f2) * 0.5f;
        float f5 = (f2 + f4) * (float)Math.tan(Math.min(f3, 1.3f));
        float f6 = RadialGlassRenderer.clamp(builtRadialGlass.corner(), 0.0f, Math.max(0.0f, Math.min(f4, f5) - 0.1f));
        float f7 = Float.isFinite(builtRadialGlass.blurRadius()) ? RadialGlassRenderer.clamp(builtRadialGlass.blurRadius(), 0.1f, 64.0f) : 0.1f;
        float f8 = Float.isFinite(builtRadialGlass.colorOffset()) ? builtRadialGlass.colorOffset() : 0.0f;
        f8 -= (float)Math.floor(f8);
        return new BuiltRadialGlass(builtRadialGlass.centerX(), builtRadialGlass.centerY(), f2, f, builtRadialGlass.midAngle(), f3, f6, Math.max(builtRadialGlass.feather(), 0.2f), builtRadialGlass.color(), builtRadialGlass.secondColor(), f8, RadialGlassRenderer.clamp(builtRadialGlass.globalAlpha(), 0.0f, 1.0f), Math.max(builtRadialGlass.fresnelPower(), 0.001f), builtRadialGlass.fresnelColor(), RadialGlassRenderer.clamp(builtRadialGlass.baseAlpha(), 0.0f, 1.0f), builtRadialGlass.fresnelInvert(), RadialGlassRenderer.clamp(builtRadialGlass.fresnelMix(), 0.0f, 1.0f), builtRadialGlass.distortStrength(), f7, builtRadialGlass.highlightColor(), RadialGlassRenderer.clamp(builtRadialGlass.highlight(), 0.0f, 1.0f), builtRadialGlass.z());
    }

    int reserve(BuiltRadialGlass builtRadialGlass, BlurCapture blurCapture) {
        int n = this.preparedSectors.size();
        if (n == 48) {
            return -1;
        }
        this.preparedSectors.add(builtRadialGlass);
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
        RadialGlassRenderer radialGlassRenderer = instance;
        if (radialGlassRenderer != null) {
            radialGlassRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedSectors.clear();
        this.preparedCaptures.clear();
        this.paramsDirty = false;
    }

    public boolean isRadialGlassPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == RADIAL_GLASS_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedSectors.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedSectors);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void bindParams(RenderPass renderPass) {
        GpuBuffer gpuBuffer;
        if (renderPass == null || this.preparedSectors.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer2 = this.ensureParamsBuffer();
        if (gpuBuffer2 != null) {
            renderPass.setUniform("RadialGlassParamsArray", gpuBuffer2);
        }
        if ((gpuBuffer = ClientPalette.buffer()) != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 7680L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_radial_glass_params", 136, 7680L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltRadialGlass> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(7680);
        for (int i = 0; i < list.size(); ++i) {
            BlurCapture blurCapture;
            BuiltRadialGlass builtRadialGlass = list.get(i);
            int n = i * 10 * 4 * 4;
            byteBuffer.putFloat(n, builtRadialGlass.outerRadius());
            byteBuffer.putFloat(n + 4, builtRadialGlass.innerRadius());
            byteBuffer.putFloat(n + 8, builtRadialGlass.midAngle());
            byteBuffer.putFloat(n + 12, builtRadialGlass.halfAngle());
            byteBuffer.putFloat(n + 16, builtRadialGlass.extent());
            byteBuffer.putFloat(n + 20, builtRadialGlass.corner());
            byteBuffer.putFloat(n + 24, builtRadialGlass.feather());
            byteBuffer.putFloat(n + 28, builtRadialGlass.highlight());
            byteBuffer.putFloat(n + 32, builtRadialGlass.globalAlpha());
            byteBuffer.putFloat(n + 36, builtRadialGlass.fresnelPower());
            byteBuffer.putFloat(n + 40, builtRadialGlass.baseAlpha());
            byteBuffer.putFloat(n + 44, builtRadialGlass.fresnelMix());
            RadialGlassRenderer.putColor(byteBuffer, n + 48, builtRadialGlass.fresnelColor());
            byteBuffer.putFloat(n + 64, builtRadialGlass.fresnelInvert() ? 1.0f : 0.0f);
            byteBuffer.putFloat(n + 68, builtRadialGlass.distortStrength());
            byteBuffer.putFloat(n + 72, builtRadialGlass.z());
            byteBuffer.putFloat(n + 76, builtRadialGlass.colorOffset());
            RadialGlassRenderer.putColor(byteBuffer, n + 80, builtRadialGlass.color());
            RadialGlassRenderer.putColor(byteBuffer, n + 96, builtRadialGlass.secondColor());
            BlurCapture blurCapture2 = blurCapture = i < this.preparedCaptures.size() ? this.preparedCaptures.get(i) : null;
            if (blurCapture != null) {
                byteBuffer.putFloat(n + 112, blurCapture.regionX);
                byteBuffer.putFloat(n + 116, blurCapture.regionY);
                byteBuffer.putFloat(n + 120, blurCapture.regionW);
                byteBuffer.putFloat(n + 124, blurCapture.regionH);
            }
            RadialGlassRenderer.putColor(byteBuffer, n + 128, builtRadialGlass.highlightColor());
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedSectors);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_radial_glass_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

