package rtx.nv.api.mods.geckolib.renderer.base;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.renderer.base.BoneSnapshots;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRendererInternals;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo.BoneUpdater;

public interface GeoRenderer<T extends rtx.nv.api.mods.geckolib.animatable.GeoAnimatable, O, R extends GeoRenderState>
extends GeoRendererInternals<T, O, R> {
    default public RenderLayer getRenderType(R r, Identifier identifier) {
        return RenderLayers.entityCutoutNoCull((Identifier)identifier);
    }

    default public float getMotionAnimThreshold(T t) {
        return 0.015f;
    }

    default public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float f, float f2) {
        if (f != 1.0f || f2 != 1.0f) {
            renderPassInfo.poseStack().scale(f, f2, f);
        }
    }

    default public void adjustRenderPose(RenderPassInfo<R> renderPassInfo) {
    }

    @Override
    default public int getPackedOverlay(T t, O o, float f, float f2) {
        return OverlayTexture.DEFAULT_UV;
    }

    @Override
    default public int getRenderColor(T t, O o, float f) {
        return -1;
    }

    default public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots boneSnapshots) {
    }

    @Override
    default public void setMolangQueryValues(T t, O o, R r, float f) {
    }

    default public void performRenderPass(R r, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
        this.performRenderPass(r, matrixStack, orderedRenderCommandQueue, cameraRenderState, null);
    }

    default public void performRenderPass(R r, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, RenderPassInfo.BoneUpdater<R> boneUpdater) {
        matrixStack.push();
        RenderLayer renderLayer = this.getRenderType(r, this.getTextureLocation(r));
        RenderPassInfo renderPassInfo = RenderPassInfo.create(this, r, matrixStack, cameraRenderState, renderLayer != null);
        if (boneUpdater != null) {
            renderPassInfo.addBoneUpdater(boneUpdater);
        }
        if (this.firePreRenderEvent(renderPassInfo, orderedRenderCommandQueue)) {
            this.preRenderPass(renderPassInfo, orderedRenderCommandQueue);
            this.scaleModelForRender(renderPassInfo, 1.0f, 1.0f);
            this.adjustRenderPose(renderPassInfo);
            this.preApplyRenderLayers(renderPassInfo, orderedRenderCommandQueue);
            renderPassInfo.captureModelRenderPose();
            this.submitRenderTasks(renderPassInfo, (RenderCommandQueue)orderedRenderCommandQueue, renderLayer);
            this.submitPerBoneRenderTasks(renderPassInfo, orderedRenderCommandQueue);
            this.applyRenderLayers(renderPassInfo, orderedRenderCommandQueue);
        }
        matrixStack.pop();
        this.postRenderPass(renderPassInfo, orderedRenderCommandQueue);
    }

    @Override
    default public void addRenderData(T t, O o, R r, float f) {
    }

    default public void submitRenderTasks(RenderPassInfo<R> renderPassInfo, RenderCommandQueue renderCommandQueue, RenderLayer renderLayer) {
        if (renderLayer == null) {
            return;
        }
        int n = renderPassInfo.packedLight();
        int n2 = renderPassInfo.packedOverlay();
        int n3 = renderPassInfo.renderColor();
        renderCommandQueue.submitCustom(renderPassInfo.poseStack(), renderLayer, (entry, vertexConsumer) -> {
            MatrixStack matrixStack = renderPassInfo.poseStack();
            matrixStack.push();
            matrixStack.peek().copy(entry);
            renderPassInfo.renderPosed(() -> renderPassInfo.model().render(renderPassInfo, vertexConsumer, n, n2, n3));
            matrixStack.pop();
        });
    }

    default public void preRenderPass(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
    }

    default public void postRenderPass(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
    }
}

