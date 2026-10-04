package rtx.nv.utils.render.renderitem;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.mixin.accessor.ItemLayerRenderStateAccessor;
import rtx.nv.mixin.accessor.ItemStackRenderStateAccessor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.effecticon.PoseKey;
import rtx.nv.utils.render.renderitem.BuiltRenderItem;
import rtx.nv.utils.render.renderitem.CachedItemGeometry;
import rtx.nv.utils.render.renderitem.CachedItemQuad;
import rtx.nv.utils.render.renderitem.CustomItemRenderState;
import rtx.nv.utils.render.renderitem.ItemTexture;

public final class CustomItemRenderer
implements AutoCloseable {
    private static final int[] EMPTY_TINTS = new int[0];
    private static final int MAX_ITEM_QUADS = 4096;
    private static final int MAX_GEOMETRY_CACHE_ENTRIES = 512;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 65536;
    private static final float MIN_VISIBLE_AREA = 2.0E-6f;
    private static final float MIN_BOUNDS_SPAN = 1.0E-4f;
    private static final float PRESERVED_MODEL_SCALE_CAP = 1.08f;
    private static final float PRESERVED_MODEL_DEPTH_EPSILON = 0.02f;
    private static final float SHADE_UP = 0.98f;
    private static final float SHADE_SIDE_LIGHT = 0.9f;
    private static final float SHADE_SIDE = 0.84f;
    private static final float SHADE_SIDE_DARK = 0.78f;
    private static final float SHADE_DOWN = 0.72f;
    private static volatile CustomItemRenderer instance;
    private static final VertexFormat ITEM_VERTEX_FORMAT;
    public static final RenderPipeline ITEM_PIPELINE;
    private final Map<Identifier, CachedTexture> textures = new HashMap<Identifier, CachedTexture>();
    private final Map<GeometryCacheKey, CachedItemGeometry> geometryCache = new LinkedHashMap<GeometryCacheKey, CachedItemGeometry>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<GeometryCacheKey, CachedItemGeometry> entry) {
            return this.size() > 512;
        }
    };
    private final Map<FrameBatchKey, CustomItemRenderState> frameBatches = new LinkedHashMap<FrameBatchKey, CustomItemRenderState>(32);
    private final float[] preparedGlints = new float[4096];
    private int preparedQuadCount;
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty;

    private CustomItemRenderer() {
    }

    static {
        ITEM_VERTEX_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).add("LineWidth", VertexFormatElement.LINE_WIDTH).build();
        ITEM_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(CustomItemRenderer.id("pipeline/item")).withVertexShader(CustomItemRenderer.id("core/item")).withFragmentShader(CustomItemRenderer.id("core/item")).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ItemParamsArray", UniformType.UNIFORM_BUFFER).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withVertexFormat(ITEM_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).build();
    }

    public void flush() {
        this.activeGraphics = null;
        this.frameBatches.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static CustomItemRenderer getInstance() {
        CustomItemRenderer customItemRenderer = instance;
        if (customItemRenderer != null) return customItemRenderer;
        Class<CustomItemRenderer> clazz = CustomItemRenderer.class;
        synchronized (CustomItemRenderer.class) {
            customItemRenderer = instance;
            if (customItemRenderer != null) return customItemRenderer;
            instance = customItemRenderer = new CustomItemRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return customItemRenderer;
        }
    }

    @Override
    public void close() {
        this.frameBatches.clear();
        this.preparedQuadCount = 0;
        this.geometryCache.clear();
        this.textures.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltRenderItem builtRenderItem) {
        this.submit(this.activeGraphics, builtRenderItem);
    }

    private void submit(DrawContext drawContext, BuiltRenderItem builtRenderItem) {
        if (drawContext == null || builtRenderItem == null || !builtRenderItem.visible()) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getItemModelManager() == null) {
            return;
        }
        try {
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            PoseKey poseKey = PoseKey.of((Matrix3x2f)matrix3x2f);
            CachedItemGeometry cachedItemGeometry = this.resolveGeometry(minecraftClient, builtRenderItem);
            if (cachedItemGeometry.specialRenderer() != null) {
                this.submitSpecialItem(drawContext, builtRenderItem);
                return;
            }
            for (CachedItemQuad cachedItemQuad : cachedItemGeometry.quads()) {
                ItemTexture itemTexture = this.resolveTexture(cachedItemQuad.atlas());
                if (itemTexture == null) continue;
                float f = builtRenderItem.options().glintMode().enabled(builtRenderItem.stack(), cachedItemQuad.foil()) ? builtRenderItem.options().glintStrength() : 0.0f;
                int n2 = CustomItemRenderer.multiplyColor(builtRenderItem.options().color(), cachedItemQuad.tint(), builtRenderItem.options().alpha());
                this.submitQuad(guiRenderState, n, matrix3x2f, poseKey, itemTexture, cachedItemQuad, builtRenderItem.x(), builtRenderItem.y(), builtRenderItem.size(), n2, f);
            }
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[RenderItem] Failed to submit item: {}", (Object)builtRenderItem.stack().getName().getString(), (Object)runtimeException);
        }
    }

    int reserve(float f) {
        int n;
        if ((n = this.preparedQuadCount++) == 4096) {
            return -1;
        }
        this.preparedGlints[n] = f;
        this.paramsDirty = true;
        return n;
    }

    private static int normalizeColor(int n) {
        if ((n & 0xFF000000) == 0 && (n & 0xFFFFFF) != 0) {
            return n | 0xFF000000;
        }
        return n;
    }

    public static void closeInstance() {
        CustomItemRenderer customItemRenderer = instance;
        if (customItemRenderer != null) {
            customItemRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedQuadCount = 0;
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedQuadCount == 0 || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public boolean isItemPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == ITEM_PIPELINE;
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    private static float cross(float f, float f2, float f3, float f4) {
        return f * f4 - f3 * f2;
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedQuadCount == 0) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("ItemParamsArray", gpuBuffer);
        }
    }

    public void clearCaches() {
        this.geometryCache.clear();
        this.textures.clear();
    }

    private ItemTexture resolveTexture(Identifier identifier) {
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
        if (abstractTexture == null || abstractTexture.getGlTextureView() == null) {
            return null;
        }
        CachedTexture cachedTexture = this.textures.get(identifier);
        if (cachedTexture != null && cachedTexture.texture() == abstractTexture) {
            return cachedTexture.value();
        }
        TextureSetup textureSetup = TextureSetup.of((GpuTextureView)abstractTexture.getGlTextureView(), (GpuSampler)RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
        ItemTexture itemTexture = new ItemTexture(identifier, textureSetup);
        this.textures.put(identifier, new CachedTexture(abstractTexture, itemTexture));
        return itemTexture;
    }

    private static Vector3f transformPosition(Vector3fc vector3fc, Matrix4f matrix4f) {
        return new Vector3f(vector3fc).mulPosition((Matrix4fc)matrix4f);
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 65536L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_item_params", 136, 65536L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack) {
        ByteBuffer byteBuffer = memoryStack.calloc(65536);
        float f = (float)(System.nanoTime() % 30000000000L) / 1.0E9f;
        for (int i = 0; i < this.preparedQuadCount; ++i) {
            int n = i * 4 * 4;
            byteBuffer.putFloat(n, this.preparedGlints[i]);
            byteBuffer.putFloat(n + 4, f);
            byteBuffer.putFloat(n + 8, 0.0f);
            byteBuffer.putFloat(n + 12, 0.0f);
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_item_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }

    private static int shadeColor(int n, float f) {
        n = CustomItemRenderer.normalizeColor(n);
        int n2 = n >>> 24 & 0xFF;
        int n3 = CustomItemRenderer.clampInt(Math.round((float)(n >>> 16 & 0xFF) * f), 0, 255);
        int n4 = CustomItemRenderer.clampInt(Math.round((float)(n >>> 8 & 0xFF) * f), 0, 255);
        int n5 = CustomItemRenderer.clampInt(Math.round((float)(n & 0xFF) * f), 0, 255);
        return n2 << 24 | n3 << 16 | n4 << 8 | n5;
    }

    private void submitQuad(GuiRenderState guiRenderState, int n, Matrix3x2f matrix3x2f, PoseKey poseKey, ItemTexture itemTexture, CachedItemQuad cachedItemQuad, float f, float f2, float f3, int n2, float f4) {
        FrameBatchKey frameBatchKey = new FrameBatchKey(guiRenderState, n, itemTexture.id(), poseKey);
        CustomItemRenderState itemRenderState = this.frameBatches.get(frameBatchKey);
        if (itemRenderState == null) {
            itemRenderState = new CustomItemRenderState(matrix3x2f, itemTexture);
            itemRenderState.add(cachedItemQuad, f, f2, f3, n2, f4);
            this.frameBatches.put(frameBatchKey, itemRenderState);
            guiRenderState.addSimpleElement((SimpleGuiElementRenderState)itemRenderState);
        } else {
            itemRenderState.add(cachedItemQuad, f, f2, f3, n2, f4);
        }
    }

    private static float faceShade(Direction direction) {
        if (direction == null) {
            return 0.84f;
        }
        return switch (direction) {
            case UP -> 0.98f;
            case DOWN -> 0.72f;
            case NORTH -> 0.9f;
            case SOUTH -> 0.84f;
            case EAST, WEST -> 0.78f;
        };
    }

    private static int tintColor(BakedQuad bakedQuad, int[] nArray) {
        int n;
        int n2 = -1;
        if (bakedQuad.hasTint() && (n = bakedQuad.tintIndex()) >= 0 && n < nArray.length) {
            n2 = CustomItemRenderer.normalizeColor(nArray[n]);
        }
        return n2;
    }

    private static int clampInt(int n, int n2, int n3) {
        return Math.max(n2, Math.min(n3, n));
    }

    private CachedItemGeometry buildGeometry(MinecraftClient minecraftClient, ItemStack itemStack, int n) {
        ItemRenderState itemRenderState = new ItemRenderState();
        minecraftClient.getItemModelManager().clearAndUpdate(itemRenderState, itemStack, ItemDisplayContext.GUI, (World)minecraftClient.world, (HeldItemContext)minecraftClient.player, n);
        ItemStackRenderStateAccessor itemStackRenderStateAccessor = (ItemStackRenderStateAccessor)itemRenderState;
        ItemRenderState.LayerRenderState[] layerRenderStateArray = itemStackRenderStateAccessor.nv_getLayers();
        int n2 = Math.min(itemStackRenderStateAccessor.nv_getActiveLayerCount(), layerRenderStateArray.length);
        for (int i = 0; i < n2; ++i) {
            if (!this.hasSpecialRenderer(layerRenderStateArray[i])) continue;
            return new CachedItemGeometry(List.of(), itemRenderState.isAnimated(), true);
        }
        ArrayList<RawItemQuad> arrayList = new ArrayList<RawItemQuad>(Math.max(8, n2 * 8));
        Bounds bounds = new Bounds();
        for (int i = 0; i < n2; ++i) {
            this.collectLayerGeometry(arrayList, bounds, layerRenderStateArray[i]);
        }
        arrayList.sort(Comparator.comparingDouble(RawItemQuad::depth));
        NormalizedBounds normalizedBounds = NormalizedBounds.of((Bounds)bounds);
        ArrayList<CachedItemQuad> arrayList2 = new ArrayList<CachedItemQuad>(arrayList.size());
        for (RawItemQuad rawItemQuad : arrayList) {
            arrayList2.add(this.buildCachedQuad(rawItemQuad, normalizedBounds));
        }
        return new CachedItemGeometry(List.copyOf(arrayList2), itemRenderState.isAnimated(), false);
    }

    private CachedItemGeometry resolveGeometry(MinecraftClient minecraftClient, BuiltRenderItem builtRenderItem) {
        GeometryCacheKey geometryCacheKey = GeometryCacheKey.of((ItemStack)builtRenderItem.stack(), (int)builtRenderItem.seed());
        CachedItemGeometry cachedItemGeometry = this.geometryCache.get(geometryCacheKey);
        if (cachedItemGeometry != null) {
            return cachedItemGeometry;
        }
        CachedItemGeometry cachedItemGeometry2 = this.buildGeometry(minecraftClient, builtRenderItem.stack(), builtRenderItem.seed());
        if (!cachedItemGeometry2.animated()) {
            this.geometryCache.put(geometryCacheKey, cachedItemGeometry2);
        }
        return cachedItemGeometry2;
    }

    private static float projectedArea(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        return Math.abs(CustomItemRenderer.cross(f, f2, f3, f4) + CustomItemRenderer.cross(f3, f4, f5, f6) + CustomItemRenderer.cross(f5, f6, f7, f8) + CustomItemRenderer.cross(f7, f8, f, f2)) * 0.5f;
    }

    private Matrix4f layerTransform(ItemLayerRenderStateAccessor itemLayerRenderStateAccessor) {
        MatrixStack.Entry entry = new MatrixStack.Entry();
        Transformation transformation = itemLayerRenderStateAccessor.nv_getItemTransform();
        if (transformation == null) {
            transformation = Transformation.IDENTITY;
        }
        transformation.apply(false, entry);
        return new Matrix4f((Matrix4fc)entry.getPositionMatrix());
    }

    private RawItemQuad buildRawQuad(BakedQuad bakedQuad, Matrix4f matrix4f, int n, boolean bl, boolean bl2) {
        Vector3fc vector3fc = bakedQuad.position0();
        Vector3fc vector3fc2 = bakedQuad.position1();
        Vector3fc vector3fc3 = bakedQuad.position2();
        Vector3fc vector3fc4 = bakedQuad.position3();
        Vector3f vector3f = CustomItemRenderer.transformPosition(vector3fc, matrix4f);
        Vector3f vector3f2 = CustomItemRenderer.transformPosition(vector3fc2, matrix4f);
        Vector3f vector3f3 = CustomItemRenderer.transformPosition(vector3fc3, matrix4f);
        Vector3f vector3f4 = CustomItemRenderer.transformPosition(vector3fc4, matrix4f);
        long l = bakedQuad.packedUV0();
        long l2 = bakedQuad.packedUV1();
        long l3 = bakedQuad.packedUV2();
        long l4 = bakedQuad.packedUV3();
        float f = (vector3f.z + vector3f2.z + vector3f3.z + vector3f4.z) * 0.25f;
        int n2 = bl2 ? CustomItemRenderer.shadeColor(n, CustomItemRenderer.faceShade(bakedQuad.face())) : n;
        return new RawItemQuad(bakedQuad.sprite().getAtlasId(), vector3f.x, vector3f.y, Vector2f.getX((long)l), Vector2f.getY((long)l), vector3f2.x, vector3f2.y, Vector2f.getX((long)l2), Vector2f.getY((long)l2), vector3f3.x, vector3f3.y, Vector2f.getX((long)l3), Vector2f.getY((long)l3), vector3f4.x, vector3f4.y, Vector2f.getX((long)l4), Vector2f.getY((long)l4), n2, bl, f, CustomItemRenderer.projectedArea(vector3f.x, vector3f.y, vector3f2.x, vector3f2.y, vector3f3.x, vector3f3.y, vector3f4.x, vector3f4.y), vector3f.z, vector3f2.z, vector3f3.z, vector3f4.z);
    }

    private static int multiplyColor(int n, int n2, float f) {
        n = CustomItemRenderer.normalizeColor(n);
        n2 = CustomItemRenderer.normalizeColor(n2);
        int n3 = Math.round((float)(n >>> 24 & 0xFF) * ((float)(n2 >>> 24 & 0xFF) / 255.0f) * f);
        int n4 = (n >>> 16 & 0xFF) * (n2 >>> 16 & 0xFF) / 255;
        int n5 = (n >>> 8 & 0xFF) * (n2 >>> 8 & 0xFF) / 255;
        int n6 = (n & 0xFF) * (n2 & 0xFF) / 255;
        return CustomItemRenderer.clampInt(n3, 0, 255) << 24 | n4 << 16 | n5 << 8 | n6;
    }

    private void collectLayerGeometry(List<RawItemQuad> list, Bounds bounds, ItemRenderState.LayerRenderState layerRenderState) {
        if (layerRenderState == null) {
            return;
        }
        List<BakedQuad> list2 = layerRenderState.getQuads();
        if (list2.isEmpty()) {
            return;
        }
        ItemLayerRenderStateAccessor itemLayerRenderStateAccessor = (ItemLayerRenderStateAccessor)layerRenderState;
        ItemRenderState.Glint glint = itemLayerRenderStateAccessor.nv_getFoilType();
        boolean bl = glint != ItemRenderState.Glint.NONE;
        int[] nArray = itemLayerRenderStateAccessor.nv_getTintLayers();
        if (nArray == null) {
            nArray = EMPTY_TINTS;
        }
        Matrix4f matrix4f = this.layerTransform(itemLayerRenderStateAccessor);
        boolean bl2 = itemLayerRenderStateAccessor.nv_getUsesBlockLight();
        if (bl2) {
            bounds.markPreserveModelScale();
        }
        for (BakedQuad bakedQuad : list2) {
            RawItemQuad rawItemQuad = this.buildRawQuad(bakedQuad, matrix4f, CustomItemRenderer.tintColor(bakedQuad, nArray), bl, bl2);
            if (rawItemQuad.area() < 2.0E-6f) continue;
            bounds.include(rawItemQuad.x0(), rawItemQuad.y0(), rawItemQuad.z0());
            bounds.include(rawItemQuad.x1(), rawItemQuad.y1(), rawItemQuad.z1());
            bounds.include(rawItemQuad.x2(), rawItemQuad.y2(), rawItemQuad.z2());
            bounds.include(rawItemQuad.x3(), rawItemQuad.y3(), rawItemQuad.z3());
            list.add(rawItemQuad);
        }
    }

    private boolean hasSpecialRenderer(ItemRenderState.LayerRenderState layerRenderState) {
        return layerRenderState != null && ((ItemLayerRenderStateAccessor)layerRenderState).nv_getSpecialRenderer() != null;
    }

    private CachedItemQuad buildCachedQuad(RawItemQuad rawItemQuad, NormalizedBounds normalizedBounds) {
        return new CachedItemQuad(rawItemQuad.atlas(), normalizedBounds.x(rawItemQuad.x0()), normalizedBounds.y(rawItemQuad.y0()), rawItemQuad.u0(), rawItemQuad.v0(), normalizedBounds.x(rawItemQuad.x1()), normalizedBounds.y(rawItemQuad.y1()), rawItemQuad.u1(), rawItemQuad.v1(), normalizedBounds.x(rawItemQuad.x2()), normalizedBounds.y(rawItemQuad.y2()), rawItemQuad.u2(), rawItemQuad.v2(), normalizedBounds.x(rawItemQuad.x3()), normalizedBounds.y(rawItemQuad.y3()), rawItemQuad.u3(), rawItemQuad.v3(), rawItemQuad.tint(), rawItemQuad.foil());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void submitSpecialItem(DrawContext drawContext, BuiltRenderItem builtRenderItem) {
        float f = builtRenderItem.size() / 16.0f;
        if (f <= 0.0f) {
            return;
        }
        drawContext.getMatrices().pushMatrix();
        try {
            Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f)drawContext.getMatrices());
            drawContext.getMatrices().translate(builtRenderItem.x(), builtRenderItem.y());
            drawContext.getMatrices().scale(f);
            drawContext.drawItem(builtRenderItem.stack(), 0, 0, builtRenderItem.seed());
        }
        finally {
            drawContext.getMatrices().popMatrix();
        }
    }

    public static record CachedTexture(AbstractTexture texture, ItemTexture value) {}
    public static record GeometryCacheKey(ItemStack stack, int seed) {
        public static GeometryCacheKey of(ItemStack s, int seed) {
            return new GeometryCacheKey(s, seed);
        }
    }
    public static record FrameBatchKey(GuiRenderState renderState, int layerSerial, Identifier textureId, PoseKey poseKey) {}
    public static record RawItemQuad(Identifier atlas, float x0, float y0, float u0, float v0, float x1, float y1, float u1, float v1, float x2, float y2, float u2, float v2, float x3, float y3, float u3, float v3, int tint, boolean foil, float depth, float area, float z0, float z1, float z2, float z3) {}

    private static class Bounds {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        boolean preserveModelScale = false;

        void include(float x, float y, float z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        void markPreserveModelScale() {
            preserveModelScale = true;
        }
    }

    private static class NormalizedBounds {
        final float minX, minY, spanX, spanY;

        NormalizedBounds(float minX, float minY, float spanX, float spanY) {
            this.minX = minX;
            this.minY = minY;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        static NormalizedBounds of(Bounds b) {
            float sx = Math.max(MIN_BOUNDS_SPAN, b.maxX - b.minX);
            float sy = Math.max(MIN_BOUNDS_SPAN, b.maxY - b.minY);
            return new NormalizedBounds(b.minX, b.minY, sx, sy);
        }

        float x(float val) {
            return (val - minX) / spanX;
        }

        float y(float val) {
            return (val - minY) / spanY;
        }
    }
}

