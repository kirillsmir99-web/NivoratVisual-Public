package rtx.nv.api.modules.impl.Visuals.emotions;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import rtx.nv.api.modules.impl.Visuals.emotions.Emotion;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPlayback;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPreviewState;
import rtx.nv.mixin.emotions.PictureInPictureRendererAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.sectormask.BuiltSectorMask;
import rtx.nv.utils.render.render2d.sectormask.SectorMaskRenderer;

public final class EmotionPreviewRenderer
extends SpecialGuiElementRenderer<EmotionPreviewState> {
    private static final float TAU = (float)Math.PI * 2;

    public EmotionPreviewRenderer(VertexConsumerProvider.Immediate immediate) {
        super(immediate);
    }

    @Override
    public Class<EmotionPreviewState> getElementClass() {
        return EmotionPreviewState.class;
    }

    @Override
    protected String getName() {
        return "nv emotion previews";
    }

    @Override
    protected float getYOffset(int height, int windowScaleFactor) {
        return (float)height / 2.0f;
    }

    protected void blitTexture(EmotionPreviewState emotionPreviewState, GuiRenderState guiRenderState) {
        GpuTextureView gpuTextureView = ((PictureInPictureRendererAccessor)((Object)this)).nv_textureView();
        if (gpuTextureView == null) {
            return;
        }
        Matrix3x2f matrix3x2f = new Matrix3x2f((Matrix3x2fc)emotionPreviewState.pose());
        Render2DCoordinateSpace.applyGuiScaleIndependence(matrix3x2f);
        SectorMaskRenderer.getInstance().submit(guiRenderState, matrix3x2f, new BuiltSectorMask(emotionPreviewState.designX, emotionPreviewState.designY, emotionPreviewState.designSize, emotionPreviewState.innerRadius, emotionPreviewState.outerRadius, emotionPreviewState.emotions.size(), emotionPreviewState.gapRadians, emotionPreviewState.corner, 0.8f, emotionPreviewState.hoverIndex, emotionPreviewState.hoverGrow, emotionPreviewState.hoverShrink, emotionPreviewState.hoverZoom, emotionPreviewState.ringRadius, emotionPreviewState.cellWidthDesign, emotionPreviewState.cellHeightDesign, emotionPreviewState.columns, emotionPreviewState.rows, emotionPreviewState.alpha, gpuTextureView), emotionPreviewState.scissorArea());
    }

    @Override
    protected void render(EmotionPreviewState emotionPreviewState, MatrixStack matrixStack) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player == null || emotionPreviewState.emotions.isEmpty()) {
            return;
        }
        minecraftClient.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
        EntityRenderManager entityRenderManager = minecraftClient.getEntityRenderDispatcher();
        EntityRenderer entityRenderer = entityRenderManager.getRenderer((Entity)minecraftClient.player);
        RenderDispatcher renderDispatcher = minecraftClient.gameRenderer.getEntityRenderDispatcher();
        Quaternionf quaternionf = new Quaternionf().rotateZ((float)Math.PI);
        float f = (float)emotionPreviewState.columns * emotionPreviewState.cellWidth;
        float f2 = (float)emotionPreviewState.rows * emotionPreviewState.cellHeight;
        for (int i = 0; i < emotionPreviewState.emotions.size(); ++i) {
            EntityRenderState entityRenderState;
            int n = i % emotionPreviewState.columns;
            int n2 = i / emotionPreviewState.columns;
            float f3 = ((float)n + 0.5f) * emotionPreviewState.cellWidth - f * 0.5f;
            float f4 = ((float)n2 + 0.5f) * emotionPreviewState.cellHeight - f2 * 0.5f;
            EmotionPlayback.beginPreview((Emotion)emotionPreviewState.emotions.get(i), (float)emotionPreviewState.time);
            try {
                entityRenderState = entityRenderer.getAndUpdateRenderState((Entity)minecraftClient.player, 1.0f);
            }
            catch (RuntimeException runtimeException) {
                continue;
            }
            finally {
                EmotionPlayback.endPreview();
            }
            entityRenderState.light = 0xF000F0;
            entityRenderState.outlineColor = 0;
            entityRenderState.shadowPieces.clear();
            if (entityRenderState instanceof LivingEntityRenderState) {
                LivingEntityRenderState livingEntityRenderState = (LivingEntityRenderState)entityRenderState;
                livingEntityRenderState.bodyYaw = 180.0f;
                livingEntityRenderState.relativeHeadYaw = 0.0f;
                livingEntityRenderState.pitch = 0.0f;
                livingEntityRenderState.width /= livingEntityRenderState.baseScale;
                livingEntityRenderState.height /= livingEntityRenderState.baseScale;
                livingEntityRenderState.baseScale = 1.0f;
            }
            float f5 = entityRenderState.height;
            CameraRenderState cameraRenderState = new CameraRenderState();
            cameraRenderState.orientation = new Quaternionf().rotateY((float)Math.PI);
            matrixStack.push();
            matrixStack.translate(f3, f4, 0.0f);
            matrixStack.scale(emotionPreviewState.modelScale, emotionPreviewState.modelScale, emotionPreviewState.modelScale);
            matrixStack.translate(0.0f, f5 * 0.5f + emotionPreviewState.modelDrop, 0.0f);
            matrixStack.multiply((Quaternionfc)quaternionf);
            entityRenderManager.render(entityRenderState, cameraRenderState, 0.0, 0.0, 0.0, matrixStack, (OrderedRenderCommandQueue)renderDispatcher.getQueue());
            renderDispatcher.render();
            matrixStack.pop();
        }
    }
}

