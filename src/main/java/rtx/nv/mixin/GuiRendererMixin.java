package rtx.nv.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiWorld;
import rtx.nv.api.ui.UI;
import rtx.nv.mixin.accessor.GuiRendererDrawAccessor;
import rtx.nv.utils.render.post.guilayerblur.GuiCapture;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import rtx.nv.utils.render.render2d.ClientSplits;
import rtx.nv.utils.render.render2d.arc.ArcRenderer;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.circle.CircleRenderer;
import rtx.nv.utils.render.render2d.glass.GlassRenderer;
import rtx.nv.utils.render.render2d.glow.GlowRenderer;
import rtx.nv.utils.render.render2d.image.ImageRenderer;
import rtx.nv.utils.render.render2d.line.LineRenderer;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360Renderer;
import rtx.nv.utils.render.render2d.outline.outlinedefault.DefaultOutlineRenderer;
import rtx.nv.utils.render.render2d.outline.outlineglass.GlassOutlineRenderer;
import rtx.nv.utils.render.render2d.picker.PickerRenderer;
import rtx.nv.utils.render.render2d.radialglass.RadialGlassRenderer;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.DefaultRectangleRenderer;
import rtx.nv.utils.render.render2d.rectangle.recthalficon.HalfIconRectangleRenderer;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.HalftoneRectangleRenderer;
import rtx.nv.utils.render.render2d.ripple.RippleRenderer;
import rtx.nv.utils.render.render2d.sectormask.SectorMaskRenderer;
import rtx.nv.utils.render.render2d.shape.ShapeRenderer;
import rtx.nv.utils.render.render2d.shimmer.ShimmerRenderer;
import rtx.nv.utils.render.render2d.zippy.ZippyRenderer;
import rtx.nv.utils.render.renderitem.RenderItem;

@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow
    @Final
    private List<?> draws;
    private RenderPass nv_currentRenderPass;
    private boolean nv_blurDrawActive;
    private boolean nv_glassDrawActive;
    private boolean nv_shapeDrawActive;
    private boolean nv_glowDrawActive;
    private boolean nv_glassOutlineDrawActive;
    private boolean nv_circleDrawActive;
    private boolean nv_arcDrawActive;
    private boolean nv_radialGlassDrawActive;
    private boolean nv_sectorMaskDrawActive;
    private boolean nv_pickerDrawActive;
    private boolean nv_rectangleDrawActive;
    private boolean nv_halfIconRectangleDrawActive;
    private boolean nv_halftoneRectangleDrawActive;
    private boolean nv_zippyDrawActive;
    private boolean nv_outlineDrawActive;
    private boolean nv_outline360DrawActive;
    private boolean nv_imageDrawActive;
    private boolean nv_lineDrawActive;
    private boolean nv_itemDrawActive;
    private boolean nv_rippleDrawActive;
    private boolean nv_shimmerDrawActive;

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), require = 0)
    private void nv_beginBlurFrame(CallbackInfo ci) {
        BlurFramebuffer.getInstance().beginGuiFrame();
        GlassRenderer.getInstance().beginGuiFrame();
        ShapeRenderer.getInstance().beginGuiFrame();
        GlowRenderer.getInstance().beginGuiFrame();
        GlassOutlineRenderer.getInstance().beginGuiFrame();
        CircleRenderer.getInstance().beginGuiFrame();
        ArcRenderer.getInstance().beginGuiFrame();
        RadialGlassRenderer.getInstance().beginGuiFrame();
        SectorMaskRenderer.getInstance().beginGuiFrame();
        PickerRenderer.getInstance().beginGuiFrame();
        DefaultRectangleRenderer.getInstance().beginGuiFrame();
        HalfIconRectangleRenderer.getInstance().beginGuiFrame();
        HalftoneRectangleRenderer.getInstance().beginGuiFrame();
        ZippyRenderer.getInstance().beginGuiFrame();
        DefaultOutlineRenderer.getInstance().beginGuiFrame();
        Outline360Renderer.getInstance().beginGuiFrame();
        ImageRenderer.getInstance().beginGuiFrame();
        LineRenderer.getInstance().beginGuiFrame();
        RippleRenderer.getInstance().beginGuiFrame();
        ShimmerRenderer.getInstance().beginGuiFrame();
        RenderItem.beginGuiFrame();
    }

    @Inject(method = "prepare()V", at = @At("HEAD"), require = 0)
    private void nv_preparePendingBlurResources(CallbackInfo ci) {
        ClientSplits.update();
        BlurFramebuffer.getInstance().preparePending();
        GlowRenderer.getInstance().preparePending();
    }

    @Inject(method = "prepare()V", at = @At("RETURN"), require = 0)
    private void nv_prepareRenderUniforms(CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareBuffers();
        GlassRenderer.getInstance().prepareBuffers();
        ShapeRenderer.getInstance().prepareBuffers();
        GlowRenderer.getInstance().prepareBuffers();
        GlassOutlineRenderer.getInstance().prepareBuffers();
        CircleRenderer.getInstance().prepareBuffers();
        ArcRenderer.getInstance().prepareBuffers();
        RadialGlassRenderer.getInstance().prepareBuffers();
        SectorMaskRenderer.getInstance().prepareBuffers();
        PickerRenderer.getInstance().prepareBuffers();
        DefaultRectangleRenderer.getInstance().prepareBuffers();
        HalfIconRectangleRenderer.getInstance().prepareBuffers();
        HalftoneRectangleRenderer.getInstance().prepareBuffers();
        ZippyRenderer.getInstance().prepareBuffers();
        DefaultOutlineRenderer.getInstance().prepareBuffers();
        Outline360Renderer.getInstance().prepareBuffers();
        ImageRenderer.getInstance().prepareBuffers();
        LineRenderer.getInstance().prepareBuffers();
        RippleRenderer.getInstance().prepareBuffers();
        ShimmerRenderer.getInstance().prepareBuffers();
        RenderItem.prepareBuffers();
    }

    @Inject(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), require = 0)
    private void nv_prepareBlurCapture(CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareGuiDraw();
        GuiLayerBlurRenderer.beginCapture(GuiCapture.active(), RemoteGuiWorld.captureRequested());
    }

    @WrapOperation(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"), require = 0)
    private void nv_routePanelRange(GuiRenderer instance, Supplier<String> label, Framebuffer target, GpuBufferSlice fog, GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to, Operation<Void> original) {
        int cursor = from;
        Framebuffer remoteTarget = GuiLayerBlurRenderer.remoteCaptureTarget();
        if (remoteTarget != null) {
            while (cursor < to) {
                if (GuiLayerBlurRenderer.isRemoteRouting()) {
                    int end = this.nv_findRemoteMark(cursor, to, false);
                    if (end < cursor) {
                        this.nv_drawCaptured(instance, label, remoteTarget, fog, transforms, indices, indexType, cursor, to, original);
                        return;
                    }
                    if (cursor < end) {
                        this.nv_drawCaptured(instance, label, remoteTarget, fog, transforms, indices, indexType, cursor, end, original);
                    }
                    GuiLayerBlurRenderer.setRemoteRouting(false);
                    cursor = end + 1;
                    continue;
                }
                int begin = this.nv_findRemoteMark(cursor, to, true);
                if (begin < cursor) break;
                if (cursor < begin) {
                    this.nv_routeLocal(instance, label, target, fog, transforms, indices, indexType, cursor, begin, original);
                }
                GuiLayerBlurRenderer.setRemoteRouting(true);
                cursor = begin + 1;
            }
            if (cursor >= to) {
                return;
            }
        }
        this.nv_routeLocal(instance, label, target, fog, transforms, indices, indexType, cursor, to, original);
    }

    @Unique
    private void nv_routeLocal(GuiRenderer instance, Supplier<String> label, Framebuffer target, GpuBufferSlice fog, GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to, Operation<Void> original) {
        if (from >= to) {
            return;
        }
        Framebuffer captureTarget = GuiLayerBlurRenderer.captureTarget();
        if (captureTarget == null) {
            int popupBoundary = UI.popupLayerCapturePending() ? this.nv_findBoundary(from, to, true) : -1;
            if (popupBoundary < from) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, from, to});
                return;
            }
            if (from < popupBoundary) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, from, popupBoundary});
            }
            if (UI.consumePopupLayerCapture()) {
                GuiMotionBlurRenderer.captureBackground(0.0f);
            }
            if (popupBoundary + 1 < to) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, popupBoundary + 1, to});
            }
            return;
        }
        int boundary = this.nv_findBoundary(from, to, false);
        if (boundary < from) {
            if (GuiCapture.emitPanelBoundary()) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, from, to});
            } else {
                this.nv_drawCaptured(instance, label, captureTarget, fog, transforms, indices, indexType, from, to, original);
            }
            return;
        }
        if (from < boundary) {
            this.nv_drawCaptured(instance, label, captureTarget, fog, transforms, indices, indexType, from, boundary, original);
        }
        if (boundary + 1 < to) {
            original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, boundary + 1, to});
        }
    }

    @Unique
    private int nv_findRemoteMark(int from, int to, boolean begin) {
        int end = Math.min(to, this.draws.size());
        for (int i = Math.max(0, from); i < end; ++i) {
            Object draw = this.draws.get(i);
            if (!(draw instanceof GuiRendererDrawAccessor)) continue;
            GuiRendererDrawAccessor accessor = (GuiRendererDrawAccessor)draw;
            RenderPipeline pipeline = accessor.nv_getPipeline();
            if (!(begin ? GuiLayerBlurRenderer.isRemoteBegin(pipeline) : GuiLayerBlurRenderer.isRemoteEnd(pipeline))) continue;
            return i;
        }
        return -1;
    }

    @Unique
    private void nv_drawCaptured(GuiRenderer instance, Supplier<String> label, Framebuffer captureTarget, GpuBufferSlice fog, GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to, Operation<Void> original) {
        int cursor = from;
        boolean remote = captureTarget == GuiLayerBlurRenderer.remoteCaptureTarget();
        int popupBoundary;
        int cardBegin;
        int cardEnd;
        int boundary;
        while (cursor < to && (boundary = GuiRendererMixin.nv_firstBoundary(popupBoundary = this.nv_findBoundary(cursor, to, true), cardBegin = remote ? this.nv_findRemoteCardMark(cursor, to, true) : -1, cardEnd = remote ? this.nv_findRemoteCardMark(cursor, to, false) : -1)) >= cursor) {
            if (cursor < boundary) {
                original.call(new Object[]{instance, label, captureTarget, fog, transforms, indices, indexType, cursor, boundary});
            }
            if (boundary == cardBegin) {
                RemoteGuiWorld.beginCardBlurDraw(captureTarget);
            } else if (boundary == cardEnd) {
                RemoteGuiWorld.endCardBlurDraw(captureTarget);
            } else {
                BlurFramebuffer.getInstance().recaptureWorldBackdrop();
            }
            cursor = boundary + 1;
        }
        if (cursor < to) {
            original.call(new Object[]{instance, label, captureTarget, fog, transforms, indices, indexType, cursor, to});
        }
    }

    @Unique
    private int nv_findRemoteCardMark(int from, int to, boolean begin) {
        int end = Math.min(to, this.draws.size());
        for (int i = Math.max(0, from); i < end; ++i) {
            Object draw = this.draws.get(i);
            if (!(draw instanceof GuiRendererDrawAccessor)) continue;
            GuiRendererDrawAccessor accessor = (GuiRendererDrawAccessor)draw;
            RenderPipeline pipeline = accessor.nv_getPipeline();
            if (!(begin ? GuiLayerBlurRenderer.isRemoteCardBegin(pipeline) : GuiLayerBlurRenderer.isRemoteCardEnd(pipeline))) continue;
            return i;
        }
        return -1;
    }

    @Unique
    private static int nv_firstBoundary(int first, int second, int third) {
        int result = -1;
        if (first >= 0) {
            result = first;
        }
        if (second >= 0 && (result < 0 || second < result)) {
            result = second;
        }
        if (third >= 0 && (result < 0 || third < result)) {
            result = third;
        }
        return result;
    }

    @Unique
    private int nv_findBoundary(int from, int to, boolean popup) {
        int end = Math.min(to, this.draws.size());
        for (int i = Math.max(0, from); i < end; ++i) {
            Object draw = this.draws.get(i);
            if (!(draw instanceof GuiRendererDrawAccessor)) continue;
            GuiRendererDrawAccessor accessor = (GuiRendererDrawAccessor)draw;
            RenderPipeline pipeline = accessor.nv_getPipeline();
            if (!(popup ? GuiLayerBlurRenderer.isPopupBoundary(pipeline) : GuiLayerBlurRenderer.isPanelBoundary(pipeline))) continue;
            return i;
        }
        return -1;
    }

    @Redirect(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V"), require = 0)
    private void nv_cardLayerMidCapture(GameRenderer gameRenderer) {
        if (GuiCapture.active()) {
            GuiLayerBlurRenderer.markPanelRange();
            UI.consumePanelSplitMark();
            UI.consumeCardStratumMark();
            UI.consumePopupStratumMark();
            UI.consumeVanillaBlurRequest();
            return;
        }
        if (UI.consumePanelSplitMark()) {
            UI.applyMainCompositeAtSplit();
            if (UI.consumeVanillaBlurRequest() && !UI.isOpen()) {
                gameRenderer.renderBlur();
            }
            return;
        }
        if (UI.consumePopupStratumMark()) {
            BlurFramebuffer.getInstance().recaptureBackdrop();
            if (UI.consumePopupBlurCapture()) {
                GuiMotionBlurRenderer.captureBackground(0.0f);
            }
            return;
        }
        if (UI.consumeCardStratumMark()) {
            GuiMotionBlurRenderer.captureBackground(0.0f);
            return;
        }
        if (UI.isOpen()) {
            return;
        }
        gameRenderer.renderBlur();
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"), require = 0)
    private void nv_trackPipeline(RenderPass renderPass, RenderPipeline pipeline) {
        this.nv_currentRenderPass = renderPass;
        this.nv_blurDrawActive = BlurFramebuffer.getInstance().isBlurPipeline(pipeline);
        this.nv_glassDrawActive = GlassRenderer.getInstance().isGlassPipeline(pipeline);
        this.nv_shapeDrawActive = ShapeRenderer.getInstance().isShapePipeline(pipeline);
        this.nv_glowDrawActive = GlowRenderer.getInstance().isGlowPipeline(pipeline);
        this.nv_glassOutlineDrawActive = GlassOutlineRenderer.getInstance().isGlassOutlinePipeline(pipeline);
        this.nv_circleDrawActive = CircleRenderer.getInstance().isCirclePipeline(pipeline);
        this.nv_arcDrawActive = ArcRenderer.getInstance().isArcPipeline(pipeline);
        this.nv_radialGlassDrawActive = RadialGlassRenderer.getInstance().isRadialGlassPipeline(pipeline);
        this.nv_sectorMaskDrawActive = SectorMaskRenderer.getInstance().isSectorMaskPipeline(pipeline);
        this.nv_pickerDrawActive = PickerRenderer.getInstance().isPickerPipeline(pipeline);
        this.nv_rectangleDrawActive = DefaultRectangleRenderer.getInstance().isRectanglePipeline(pipeline);
        this.nv_halfIconRectangleDrawActive = HalfIconRectangleRenderer.getInstance().isHalfIconRectanglePipeline(pipeline);
        this.nv_halftoneRectangleDrawActive = HalftoneRectangleRenderer.getInstance().isHalftoneRectanglePipeline(pipeline);
        this.nv_zippyDrawActive = ZippyRenderer.getInstance().isZippyPipeline(pipeline);
        this.nv_outlineDrawActive = DefaultOutlineRenderer.getInstance().isOutlinePipeline(pipeline);
        this.nv_outline360DrawActive = Outline360Renderer.getInstance().isOutline360Pipeline(pipeline);
        this.nv_imageDrawActive = ImageRenderer.getInstance().isImagePipeline(pipeline);
        this.nv_lineDrawActive = LineRenderer.getInstance().isLinePipeline(pipeline);
        this.nv_rippleDrawActive = RippleRenderer.getInstance().isRipplePipeline(pipeline);
        this.nv_shimmerDrawActive = ShimmerRenderer.getInstance().isShimmerPipeline(pipeline);
        this.nv_itemDrawActive = RenderItem.isItemPipeline(pipeline);
        renderPass.setPipeline(pipeline);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", shift = At.Shift.BEFORE), require = 0)
    private void nv_bindBlurParams(CallbackInfo ci) {
        if (this.nv_blurDrawActive && this.nv_currentRenderPass != null) {
            BlurFramebuffer.getInstance().bindBlurParams(this.nv_currentRenderPass);
        }
        if (this.nv_glassDrawActive && this.nv_currentRenderPass != null) {
            GlassRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_shapeDrawActive && this.nv_currentRenderPass != null) {
            ShapeRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_glowDrawActive && this.nv_currentRenderPass != null) {
            GlowRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_glassOutlineDrawActive && this.nv_currentRenderPass != null) {
            GlassOutlineRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_circleDrawActive && this.nv_currentRenderPass != null) {
            CircleRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_arcDrawActive && this.nv_currentRenderPass != null) {
            ArcRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_radialGlassDrawActive && this.nv_currentRenderPass != null) {
            RadialGlassRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_sectorMaskDrawActive && this.nv_currentRenderPass != null) {
            SectorMaskRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_pickerDrawActive && this.nv_currentRenderPass != null) {
            PickerRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_rectangleDrawActive && this.nv_currentRenderPass != null) {
            DefaultRectangleRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_halfIconRectangleDrawActive && this.nv_currentRenderPass != null) {
            HalfIconRectangleRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_halftoneRectangleDrawActive && this.nv_currentRenderPass != null) {
            HalftoneRectangleRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_zippyDrawActive && this.nv_currentRenderPass != null) {
            ZippyRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_outlineDrawActive && this.nv_currentRenderPass != null) {
            DefaultOutlineRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_outline360DrawActive && this.nv_currentRenderPass != null) {
            Outline360Renderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_imageDrawActive && this.nv_currentRenderPass != null) {
            ImageRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_lineDrawActive && this.nv_currentRenderPass != null) {
            LineRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_rippleDrawActive && this.nv_currentRenderPass != null) {
            RippleRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_shimmerDrawActive && this.nv_currentRenderPass != null) {
            ShimmerRenderer.getInstance().bindParams(this.nv_currentRenderPass);
        }
        if (this.nv_itemDrawActive && this.nv_currentRenderPass != null) {
            RenderItem.bindParams(this.nv_currentRenderPass);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At("RETURN"), require = 0)
    private void nv_clearTrackedPipeline(CallbackInfo ci) {
        this.nv_currentRenderPass = null;
        this.nv_blurDrawActive = false;
        this.nv_glassDrawActive = false;
        this.nv_shapeDrawActive = false;
        this.nv_glowDrawActive = false;
        this.nv_glassOutlineDrawActive = false;
        this.nv_circleDrawActive = false;
        this.nv_arcDrawActive = false;
        this.nv_radialGlassDrawActive = false;
        this.nv_sectorMaskDrawActive = false;
        this.nv_pickerDrawActive = false;
        this.nv_rectangleDrawActive = false;
        this.nv_halfIconRectangleDrawActive = false;
        this.nv_halftoneRectangleDrawActive = false;
        this.nv_zippyDrawActive = false;
        this.nv_outlineDrawActive = false;
        this.nv_outline360DrawActive = false;
        this.nv_imageDrawActive = false;
        this.nv_lineDrawActive = false;
        this.nv_itemDrawActive = false;
        this.nv_rippleDrawActive = false;
        this.nv_shimmerDrawActive = false;
    }
}
