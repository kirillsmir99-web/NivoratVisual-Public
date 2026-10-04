package rtx.nv.utils.render.render2d.effecticon;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.effecticon.BuiltEffectIcon;
import rtx.nv.utils.render.render2d.effecticon.EffectIconQuad;
import rtx.nv.utils.render.render2d.effecticon.EffectIconRenderState;
import rtx.nv.utils.render.render2d.effecticon.EffectIconTexture;

public final class EffectIconRenderer
implements AutoCloseable {
    private static volatile EffectIconRenderer instance;
    private static final VertexFormat EFFECT_ICON_VERTEX_FORMAT;
    public static final RenderPipeline EFFECT_ICON_PIPELINE;
    private final Map<Identifier, CachedTexture> textures = new HashMap<Identifier, CachedTexture>();
    private final Map<FrameBatchKey, EffectIconRenderState> frameBatches = new LinkedHashMap<FrameBatchKey, EffectIconRenderState>(16);
    private DrawContext activeGraphics;

    private EffectIconRenderer() {
    }

    static {
        EFFECT_ICON_VERTEX_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).build();
        EFFECT_ICON_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(EffectIconRenderer.id("pipeline/effect_icon")).withVertexShader(EffectIconRenderer.id("core/effect_icon")).withFragmentShader(EffectIconRenderer.id("core/effect_icon")).withVertexFormat(EFFECT_ICON_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).build();
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
    public static EffectIconRenderer getInstance() {
        EffectIconRenderer effectIconRenderer = instance;
        if (effectIconRenderer != null) return effectIconRenderer;
        Class<EffectIconRenderer> clazz = EffectIconRenderer.class;
        synchronized (EffectIconRenderer.class) {
            effectIconRenderer = instance;
            if (effectIconRenderer != null) return effectIconRenderer;
            instance = effectIconRenderer = new EffectIconRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return effectIconRenderer;
        }
    }

    @Override
    public void close() {
        this.frameBatches.clear();
        this.textures.clear();
        this.activeGraphics = null;
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltEffectIcon builtEffectIcon) {
        this.submit(this.activeGraphics, builtEffectIcon);
    }

    private void submit(DrawContext drawContext, BuiltEffectIcon builtEffectIcon) {
        if (drawContext == null || builtEffectIcon == null || builtEffectIcon.effect == null || !builtEffectIcon.effect.hasKeyAndValue() || builtEffectIcon.size <= 0.0f || (builtEffectIcon.color >>> 24 & 0xFF) == 0 && (builtEffectIcon.color & 0xFFFFFF) == 0) {
            return;
        }
        Sprite sprite = this.resolveSprite(builtEffectIcon);
        if (sprite == null) {
            return;
        }
        EffectIconTexture effectIconTexture = this.resolveTexture(sprite.getAtlasId());
        if (effectIconTexture == null) {
            return;
        }
        EffectIconQuad effectIconQuad = new EffectIconQuad(builtEffectIcon.x, builtEffectIcon.y, builtEffectIcon.size, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), EffectIconRenderer.normalizeColor(builtEffectIcon.color));
        try {
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            FrameBatchKey frameBatchKey = new FrameBatchKey(guiRenderState, n, effectIconTexture.id(), PoseKey.of((Matrix3x2f)matrix3x2f));
            EffectIconRenderState effectIconRenderState = this.frameBatches.get(frameBatchKey);
            if (effectIconRenderState == null) {
                effectIconRenderState = new EffectIconRenderState(matrix3x2f, effectIconTexture);
                effectIconRenderState.add(effectIconQuad);
                this.frameBatches.put(frameBatchKey, effectIconRenderState);
                guiRenderState.addSimpleElement((SimpleGuiElementRenderState)effectIconRenderState);
            } else {
                effectIconRenderState.add(effectIconQuad);
            }
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[EffectIconRenderer] Failed to submit effect icon", (Throwable)runtimeException);
        }
    }

    private static int normalizeColor(int n) {
        if ((n & 0xFF000000) == 0 && (n & 0xFFFFFF) != 0) {
            return n | 0xFF000000;
        }
        return n;
    }

    public static void closeInstance() {
        EffectIconRenderer effectIconRenderer = instance;
        if (effectIconRenderer != null) {
            effectIconRenderer.close();
            instance = null;
        }
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    private Sprite resolveSprite(BuiltEffectIcon builtEffectIcon) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || builtEffectIcon.effect == null) {
            return null;
        }
        SpriteAtlasTexture spriteAtlasTexture = minecraftClient.getAtlasManager().getAtlasTexture(Atlases.GUI);
        Identifier identifier = InGameHud.getEffectTexture((RegistryEntry)builtEffectIcon.effect);
        return spriteAtlasTexture.getSprite(identifier);
    }

    public void barrier() {
        this.frameBatches.clear();
    }

    private EffectIconTexture resolveTexture(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getTextureManager() == null) {
            return null;
        }
        AbstractTexture abstractTexture = minecraftClient.getTextureManager().getTexture(identifier);
        if (abstractTexture == null || abstractTexture.getGlTextureView() == null) {
            return null;
        }
        CachedTexture cachedTexture = this.textures.get(identifier);
        if (cachedTexture != null && cachedTexture.texture == abstractTexture) {
            return cachedTexture.value;
        }
        TextureSetup textureSetup = TextureSetup.of((GpuTextureView)abstractTexture.getGlTextureView(), (GpuSampler)RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
        EffectIconTexture effectIconTexture = new EffectIconTexture(identifier, textureSetup);
        this.textures.put(identifier, new CachedTexture(abstractTexture, effectIconTexture));
        return effectIconTexture;
    }

    public void draw(DrawContext drawContext, BuiltEffectIcon builtEffectIcon) {
        this.submit(drawContext, builtEffectIcon);
    }

    public static record CachedTexture(AbstractTexture texture, EffectIconTexture value) {}
    public static record FrameBatchKey(GuiRenderState renderState, int layerSerial, Identifier textureId, PoseKey poseKey) {}
}

