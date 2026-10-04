package rtx.nv.utils.render.render2d.image;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.image.BuiltImage;
import rtx.nv.utils.render.render2d.image.ImageQuad;
import rtx.nv.utils.render.render2d.image.ImageRenderState;
import rtx.nv.utils.render.render2d.image.ImageTexture;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class ImageRenderer
implements AutoCloseable {
    private static final int MAX_IMAGES = 512;
    private static final int PARAMS_PER_IMAGE = 6;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 49152;
    private static volatile ImageRenderer instance;
    private static final VertexFormat IMAGE_VERTEX_FORMAT;
    public static final RenderPipeline IMAGE_PIPELINE;
    public static final RenderPipeline IMAGE_ADDITIVE_PIPELINE;
    private static boolean additiveMode;
    private final Map<Identifier, ImageRenderer.CachedTexture> textures = new HashMap<Identifier, ImageRenderer.CachedTexture>();
    private final Map<ImageRenderer.FrameBatchKey, ImageRenderState> frameBatches = new LinkedHashMap<ImageRenderer.FrameBatchKey, ImageRenderState>(32);
    private final List<ImageQuad> preparedImages = new ArrayList<ImageQuad>(128);
    private final ByteBuffer paramsUploadBuffer = ByteBuffer.allocateDirect(49152).order(ByteOrder.nativeOrder());
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty;

    private ImageRenderer() {
    }

    static {
        IMAGE_VERTEX_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).add("LineWidth", VertexFormatElement.LINE_WIDTH).build();
        IMAGE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ImageRenderer.id("pipeline/image")).withVertexShader(ImageRenderer.id("core/image")).withFragmentShader(ImageRenderer.id("core/image")).withVertexFormat(IMAGE_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER).build();
        IMAGE_ADDITIVE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ImageRenderer.id("pipeline/image_additive")).withVertexShader(ImageRenderer.id("core/image")).withFragmentShader(ImageRenderer.id("core/image")).withVertexFormat(IMAGE_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.LIGHTNING).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
        this.frameBatches.clear();
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static ImageRenderer getInstance() {
        ImageRenderer imageRenderer = instance;
        if (imageRenderer != null) return imageRenderer;
        Class<ImageRenderer> clazz = ImageRenderer.class;
        synchronized (ImageRenderer.class) {
            imageRenderer = instance;
            if (imageRenderer != null) return imageRenderer;
            instance = imageRenderer = new ImageRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return imageRenderer;
        }
    }

    @Override
    public void close() {
        this.frameBatches.clear();
        this.preparedImages.clear();
        this.textures.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltImage builtImage) {
        this.submit(this.activeGraphics, builtImage);
    }

    private void submit(DrawContext drawContext, BuiltImage builtImage) {
        float f;
        if (drawContext == null || builtImage == null || !builtImage.visible()) {
            return;
        }
        ImageTexture imageTexture = this.resolveTexture(builtImage.texture());
        if (imageTexture == null) {
            return;
        }
        float f2 = builtImage.explicitWidth() > 0.0f ? builtImage.explicitWidth() : imageTexture.drawWidth(builtImage.size());
        float f3 = f = builtImage.explicitHeight() > 0.0f ? builtImage.explicitHeight() : imageTexture.drawHeight(builtImage.size());
        if (f2 <= 0.0f || f <= 0.0f) {
            return;
        }
        float f4 = Math.max(0.0f, Math.min(f2, f) * 0.5f);
        ImageQuad imageQuad = new ImageQuad(builtImage.x(), builtImage.y(), f2, f, ImageRenderer.clamp(builtImage.radiusTL(), 0.0f, f4), ImageRenderer.clamp(builtImage.radiusTR(), 0.0f, f4), ImageRenderer.clamp(builtImage.radiusBR(), 0.0f, f4), ImageRenderer.clamp(builtImage.radiusBL(), 0.0f, f4), ImageRenderer.sanitizeSmoothness(builtImage.smoothness()), ImageRenderer.normalizeColor(builtImage.colorTopLeft()), ImageRenderer.normalizeColor(builtImage.colorTopRight()), ImageRenderer.normalizeColor(builtImage.colorBottomRight()), ImageRenderer.normalizeColor(builtImage.colorBottomLeft()), ImageRenderer.clamp(builtImage.u0(), 0.0f, 1.0f), ImageRenderer.clamp(builtImage.v0(), 0.0f, 1.0f), ImageRenderer.clamp(builtImage.u1(), 0.0f, 1.0f), ImageRenderer.clamp(builtImage.v1(), 0.0f, 1.0f), Float.isFinite(builtImage.rotationDegrees()) ? builtImage.rotationDegrees() : 0.0f, Float.isFinite(builtImage.rotationOriginX()) ? builtImage.rotationOriginX() : 0.0f, Float.isFinite(builtImage.rotationOriginY()) ? builtImage.rotationOriginY() : 0.0f);
        try {
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ScreenRect screenRect = ScissorUtil.current();
            ImageRenderer.FrameBatchKey frameBatchKey = new ImageRenderer.FrameBatchKey(guiRenderState, n, imageTexture.id(), builtImage.nearest(), ImageRenderer.PoseKey.of(matrix3x2f), screenRect, additiveMode);
            ImageRenderState imageRenderState = this.frameBatches.get(frameBatchKey);
            if (imageRenderState == null) {
                imageRenderState = new ImageRenderState(matrix3x2f, imageTexture, screenRect, builtImage.nearest(), additiveMode);
                imageRenderState.add(imageQuad);
                this.frameBatches.put(frameBatchKey, imageRenderState);
                guiRenderState.addSimpleElement((SimpleGuiElementRenderState)imageRenderState);
            } else {
                imageRenderState.add(imageQuad);
            }
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[ImageRenderer] Failed to submit image: {}", (Object)builtImage.texture(), (Object)runtimeException);
        }
    }

    int reserve(ImageQuad imageQuad) {
        int n = this.preparedImages.size();
        if (n == 512) {
            return -1;
        }
        this.preparedImages.add(imageQuad);
        this.paramsDirty = true;
        return n;
    }

    private static int normalizeColor(int n) {
        return n;
    }

    public static void setAdditive(boolean bl) {
        additiveMode = bl;
    }

    private static void putColor(ByteBuffer byteBuffer, int n, int n2) {
        byteBuffer.putFloat(n, (float)(n2 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 4, (float)(n2 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 8, (float)(n2 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 12, (float)(n2 >>> 24 & 0xFF) / 255.0f);
    }

    public static void closeInstance() {
        ImageRenderer imageRenderer = instance;
        if (imageRenderer != null) {
            imageRenderer.close();
            instance = null;
        }
    }

    public void invalidateTexture(Identifier identifier) {
        if (identifier != null) {
            this.textures.remove(identifier);
        }
    }

    public boolean isTextureResolvable(String string) {
        return this.resolveTexture(string) != null;
    }

    public void beginGuiFrame() {
        this.preparedImages.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedImages.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try {
            ByteBuffer byteBuffer = this.buildUniformData();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public boolean isImagePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == IMAGE_PIPELINE || renderPipeline == IMAGE_ADDITIVE_PIPELINE;
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedImages.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("ImageParamsArray", gpuBuffer);
        }
    }

    public void barrier() {
        this.frameBatches.clear();
    }

    private ImageTexture resolveTexture(String string) {
        Identifier identifier = this.resolveIdentifier(string);
        if (identifier == null) {
            return null;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return null;
        }
        TextureManager textureManager = minecraftClient.getTextureManager();
        if (textureManager == null) {
            return null;
        }
        AbstractTexture abstractTexture = textureManager.getTexture(identifier);
        if (abstractTexture == null || abstractTexture.getGlTextureView() == null || abstractTexture.getGlTexture() == null) {
            return null;
        }
        ImageRenderer.CachedTexture cachedTexture = this.textures.get(identifier);
        if (cachedTexture != null && cachedTexture.texture() == abstractTexture) {
            return cachedTexture.value();
        }
        int n = Math.max(1, abstractTexture.getGlTexture().getWidth(0));
        int n2 = Math.max(1, abstractTexture.getGlTexture().getHeight(0));
        TextureSetup textureSetup = TextureSetup.of((GpuTextureView)abstractTexture.getGlTextureView(), (GpuSampler)RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
        TextureSetup textureSetup2 = TextureSetup.of((GpuTextureView)abstractTexture.getGlTextureView(), (GpuSampler)RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
        ImageTexture imageTexture = new ImageTexture(identifier, textureSetup, textureSetup2, n, n2);
        this.textures.put(identifier, new ImageRenderer.CachedTexture(abstractTexture, imageTexture));
        return imageTexture;
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 49152L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_image_params", 136, 49152L);
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

    private ByteBuffer buildUniformData() {
        ByteBuffer byteBuffer = this.paramsUploadBuffer;
        byteBuffer.clear();
        for (int i = 0; i < this.preparedImages.size(); ++i) {
            ImageQuad imageQuad = this.preparedImages.get(i);
            int n = i * 6 * 4 * 4;
            byteBuffer.putFloat(n, imageQuad.radiusTL);
            byteBuffer.putFloat(n + 4, imageQuad.radiusTR);
            byteBuffer.putFloat(n + 8, imageQuad.radiusBR);
            byteBuffer.putFloat(n + 12, imageQuad.radiusBL);
            byteBuffer.putFloat(n + 16, imageQuad.width);
            byteBuffer.putFloat(n + 20, imageQuad.height);
            byteBuffer.putFloat(n + 24, imageQuad.smoothness);
            byteBuffer.putFloat(n + 28, 0.0f);
            ImageRenderer.putColor(byteBuffer, n + 32, imageQuad.colorTopLeft);
            ImageRenderer.putColor(byteBuffer, n + 48, imageQuad.colorTopRight);
            ImageRenderer.putColor(byteBuffer, n + 64, imageQuad.colorBottomRight);
            ImageRenderer.putColor(byteBuffer, n + 80, imageQuad.colorBottomLeft);
        }
        byteBuffer.limit(this.usedUniformBytes());
        byteBuffer.position(0);
        return byteBuffer;
    }

    private GpuBuffer ensureParamsBuffer() {
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.prepareBuffers();
        return !this.paramsDirty && this.paramsBuffer != null ? this.paramsBuffer : null;
    }

    private Identifier resolveIdentifier(String string) {
        int n;
        if (string == null || string.isBlank()) {
            return null;
        }
        Object object = string.trim().replace('\\', '/');
        if (((String)object).startsWith("/")) {
            object = ((String)object).substring(1);
        }
        if (((String)object).startsWith("assets/") && (n = ((String)(object = ((String)object).substring("assets/".length()))).indexOf(47)) >= 0) {
            String string2 = ((String)object).substring(0, n);
            String string3 = ((String)object).substring(n + 1);
            return Identifier.of((String)string2, (String)string3);
        }
        if (((String)object).indexOf(58) >= 0) {
            return Identifier.tryParse((String)object);
        }
        if (!((String)object).startsWith("images/")) {
            object = "images/" + (String)object;
        }
        if (!((String)object).contains(".")) {
            object = (String)object + ".png";
        }
        return Identifier.of((String)"nv", (String)object);
    }

    private int usedUniformBytes() {
        return this.preparedImages.size() * 6 * 4 * 4;
    }

    private static float sanitizeSmoothness(float f) {
        if (!Float.isFinite(f)) {
            return 0.0f;
        }
        return Math.max(0.001f, f);
    }


    public static record CachedTexture(AbstractTexture texture, ImageTexture value) {
    }
    
        public static record FrameBatchKey(GuiRenderState state, int layerSerial, Identifier texture, boolean nearest, ImageRenderer.PoseKey pose, ScreenRect scissorArea, boolean additive) {
    }
    
        public static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
        static PoseKey of(Matrix3x2f matrix3x2f) {
            return new PoseKey(matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21());
        }
    }
}

