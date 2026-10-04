package rtx.nv.utils.render.render2d.shape;
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
import rtx.nv.utils.render.render2d.ClientSplits;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;
import rtx.nv.utils.render.render2d.shape.BuiltShape;
import rtx.nv.utils.render.render2d.shape.ShapeRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class ShapeRenderer
implements AutoCloseable {
    static final int MAX_SHAPES = 16;
    static final int MAX_SPANS = 64;
    static final int PARAMS_PER_SHAPE = 73;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 18688;
    private static volatile ShapeRenderer instance;
    public static final RenderPipeline SHAPE_PIPELINE;
    private final List<BuiltShape> preparedShapes = new ArrayList<BuiltShape>(32);
    private final List<BlurCapture> preparedCaptures = new ArrayList<BlurCapture>(32);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private ShapeRenderer() {
    }

    static {
        SHAPE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ShapeRenderer.id("pipeline/shape")).withVertexShader(ShapeRenderer.id("ui/shape/shape")).withFragmentShader(ShapeRenderer.id("ui/shape/shape")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ShapeParamsArray", UniformType.UNIFORM_BUFFER).withUniform("PaletteParams", UniformType.UNIFORM_BUFFER).withUniform("SplitParams", UniformType.UNIFORM_BUFFER).build();
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
    public static ShapeRenderer getInstance() {
        ShapeRenderer shapeRenderer = instance;
        if (shapeRenderer != null) return shapeRenderer;
        Class<ShapeRenderer> clazz = ShapeRenderer.class;
        synchronized (ShapeRenderer.class) {
            shapeRenderer = instance;
            if (shapeRenderer != null) return shapeRenderer;
            instance = shapeRenderer = new ShapeRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return shapeRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedShapes.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltShape builtShape) {
        this.submit(this.activeGraphics, builtShape);
    }

    public void submit(DrawContext drawContext, BuiltShape builtShape) {
        if (drawContext == null || builtShape == null || !builtShape.visible()) {
            return;
        }
        try {
            BuiltShape builtShape2 = this.normalize(builtShape);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            BlurCapture blurCapture = new BlurCapture();
            BlurFramebuffer.getInstance().requestCapture(drawContext, new BuiltBlur(builtShape2.x(), builtShape2.y(), builtShape2.width(), builtShape2.height(), builtShape2.radiusTopLeft(), builtShape2.radiusTopRight(), builtShape2.radiusBottomRight(), builtShape2.radiusBottomLeft(), 1.0f, builtShape2.blurRadius(), -1), blurCapture);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new ShapeRenderState(matrix3x2f, builtShape2, ScissorUtil.current(), blurCapture));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltShape normalize(BuiltShape builtShape) {
        float f = Math.max(0.0f, Math.min(builtShape.width(), builtShape.height()) * 0.5f);
        float f2 = ShapeRenderer.clamp(builtShape.radiusTopLeft(), 0.0f, f);
        float f3 = ShapeRenderer.clamp(builtShape.radiusTopRight(), 0.0f, f);
        float f4 = ShapeRenderer.clamp(builtShape.radiusBottomRight(), 0.0f, f);
        float f5 = ShapeRenderer.clamp(builtShape.radiusBottomLeft(), 0.0f, f);
        float f6 = ShapeRenderer.clamp(builtShape.globalAlpha(), 0.0f, 1.0f);
        float f7 = Math.max(builtShape.fresnelPower(), 0.001f);
        float f8 = ShapeRenderer.clamp(builtShape.baseAlpha(), 0.0f, 1.0f);
        float f9 = ShapeRenderer.clamp(builtShape.fresnelMix(), 0.0f, 1.0f);
        float f10 = Math.max(builtShape.squirt(), 0.001f);
        float f11 = Float.isFinite(builtShape.blurRadius()) ? ShapeRenderer.clamp(builtShape.blurRadius(), 0.1f, 64.0f) : 0.1f;
        float f12 = Float.isFinite(builtShape.colorOffset()) ? builtShape.colorOffset() : 0.0f;
        f12 -= (float)Math.floor(f12);
        if (f2 == builtShape.radiusTopLeft() && f3 == builtShape.radiusTopRight() && f4 == builtShape.radiusBottomRight() && f5 == builtShape.radiusBottomLeft() && f6 == builtShape.globalAlpha() && f7 == builtShape.fresnelPower() && f8 == builtShape.baseAlpha() && f9 == builtShape.fresnelMix() && f10 == builtShape.squirt() && f11 == builtShape.blurRadius() && f12 == builtShape.colorOffset()) {
            return builtShape;
        }
        return new BuiltShape(builtShape.x(), builtShape.y(), builtShape.width(), builtShape.height(), f2, f3, f4, f5, builtShape.color(), f6, f7, builtShape.fresnelColor(), f8, builtShape.fresnelInvert(), f9, builtShape.distortStrength(), f10, builtShape.z(), builtShape.secondColor(), f12, builtShape.splitIndex(), builtShape.spans(), builtShape.spanCount(), builtShape.innerRadius(), builtShape.leftAligned(), builtShape.bottomAnchored(), f11);
    }

    int reserve(BuiltShape builtShape, BlurCapture blurCapture) {
        int n = this.preparedShapes.size();
        if (n == 16) {
            return -1;
        }
        this.preparedShapes.add(builtShape);
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
        ShapeRenderer shapeRenderer = instance;
        if (shapeRenderer != null) {
            shapeRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedShapes.clear();
        this.preparedCaptures.clear();
        this.paramsDirty = false;
    }

    public boolean isShapePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == SHAPE_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedShapes.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedShapes);
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

    public void draw(DrawContext drawContext, BuiltShape builtShape) {
        this.beginFrame(drawContext);
        this.enqueue(builtShape);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        GpuBuffer gpuBuffer;
        GpuBuffer gpuBuffer2;
        if (renderPass == null || this.preparedShapes.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer3 = this.ensureParamsBuffer();
        if (gpuBuffer3 != null) {
            renderPass.setUniform("ShapeParamsArray", gpuBuffer3);
        }
        if ((gpuBuffer2 = ClientPalette.buffer()) != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer2);
        }
        if ((gpuBuffer = ClientSplits.buffer()) != null) {
            renderPass.setUniform("SplitParams", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 18688L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_shape_params", 136, 18688L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltShape> list) {
        int n = Math.max(1, list.size()) * 73 * 4 * 4;
        ByteBuffer byteBuffer = memoryStack.calloc(n);
        for (int i = 0; i < list.size(); ++i) {
            BlurCapture blurCapture;
            BuiltShape builtShape = list.get(i);
            int n2 = i * 73 * 4 * 4;
            byteBuffer.putFloat(n2, builtShape.radiusTopLeft());
            byteBuffer.putFloat(n2 + 4, builtShape.radiusTopRight());
            byteBuffer.putFloat(n2 + 8, builtShape.radiusBottomRight());
            byteBuffer.putFloat(n2 + 12, builtShape.radiusBottomLeft());
            byteBuffer.putFloat(n2 + 16, builtShape.width());
            byteBuffer.putFloat(n2 + 20, builtShape.height());
            byteBuffer.putFloat(n2 + 24, ShapeRenderer.shapeFresnelScale(builtShape));
            byteBuffer.putFloat(n2 + 28, Math.max(builtShape.squirt(), 0.001f));
            byteBuffer.putFloat(n2 + 32, builtShape.globalAlpha());
            byteBuffer.putFloat(n2 + 36, builtShape.fresnelPower());
            byteBuffer.putFloat(n2 + 40, builtShape.baseAlpha());
            byteBuffer.putFloat(n2 + 44, builtShape.fresnelMix());
            ShapeRenderer.putColor(byteBuffer, n2 + 48, builtShape.fresnelColor());
            byteBuffer.putFloat(n2 + 48, builtShape.splitIndex());
            byteBuffer.putFloat(n2 + 64, builtShape.fresnelInvert() ? 1.0f : 0.0f);
            byteBuffer.putFloat(n2 + 68, builtShape.distortStrength());
            byteBuffer.putFloat(n2 + 72, builtShape.z());
            byteBuffer.putFloat(n2 + 76, builtShape.colorOffset());
            ShapeRenderer.putColor(byteBuffer, n2 + 80, builtShape.color());
            ShapeRenderer.putColor(byteBuffer, n2 + 96, builtShape.secondColor());
            BlurCapture blurCapture2 = blurCapture = i < this.preparedCaptures.size() ? this.preparedCaptures.get(i) : null;
            if (blurCapture != null) {
                byteBuffer.putFloat(n2 + 112, blurCapture.regionX);
                byteBuffer.putFloat(n2 + 116, blurCapture.regionY);
                byteBuffer.putFloat(n2 + 120, blurCapture.regionW);
                byteBuffer.putFloat(n2 + 124, blurCapture.regionH);
            }
            int n3 = builtShape.spanCount();
            byteBuffer.putFloat(n2 + 128, n3);
            byteBuffer.putFloat(n2 + 132, builtShape.innerRadius());
            byteBuffer.putFloat(n2 + 136, builtShape.leftAligned());
            byteBuffer.putFloat(n2 + 140, builtShape.bottomAnchored());
            float[] fArray = builtShape.spans();
            int n4 = Math.min(n3, 64);
            for (int j = 0; j < n4; ++j) {
                int n5 = j * 4;
                byteBuffer.putFloat(n2 + 144 + j * 16, fArray[n5]);
                byteBuffer.putFloat(n2 + 144 + j * 16 + 4, fArray[n5 + 1]);
                byteBuffer.putFloat(n2 + 144 + j * 16 + 8, fArray[n5 + 2]);
                byteBuffer.putFloat(n2 + 144 + j * 16 + 12, fArray[n5 + 3]);
            }
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedShapes);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_shape_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }

    private static float shapeFresnelScale(BuiltShape builtShape) {
        int n = Math.min(builtShape.spanCount(), 64);
        float[] fArray = builtShape.spans();
        if (n <= 0 || fArray == null) {
            return 0.0f;
        }
        float f = Float.MAX_VALUE;
        for (int i = 0; i < n; ++i) {
            float f2 = fArray[i * 4 + 3];
            float f3 = fArray[i * 4 + 2];
            float f4 = f2 - f3;
            if (!(f4 > 0.001f)) continue;
            f = Math.min(f, f4);
        }
        if (f == Float.MAX_VALUE) {
            return 0.0f;
        }
        return Math.max(f * 0.5f, 1.5f);
    }
}

