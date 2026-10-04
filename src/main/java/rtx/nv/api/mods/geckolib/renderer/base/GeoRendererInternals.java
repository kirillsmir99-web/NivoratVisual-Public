package rtx.nv.api.mods.geckolib.renderer.base;
import java.util.List;
import java.util.Map;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import rtx.nv.api.mods.geckolib.animation.AnimationProcessor;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.BoneSnapshots;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.PerBoneRender;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;
import rtx.nv.api.mods.geckolib.renderer.layer.GeoRenderLayer;
import rtx.nv.api.mods.geckolib.util.ClientUtil;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public interface GeoRendererInternals<T extends rtx.nv.api.mods.geckolib.animatable.GeoAnimatable, O, R extends GeoRenderState> {
    public void fireCompileRenderLayersEvent();

    public void fireCompileRenderStateEvent(T var1, O var2, R var3, float var4);

    public GeoModel<T> getGeoModel();

    default public List<GeoRenderLayer<T, O, R>> getRenderLayers() {
        return List.of();
    }

    public int getPackedOverlay(T var1, O var2, float var3, float var4);

    default public void captureDefaultRenderState(T t, O o, R r, float f) {
        long l = this.getInstanceId(t, o);
        r.addGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, l);
        r.addGeckolibData(DataTickets.ANIMATABLE_MANAGER, t.getAnimatableInstanceCache().getManagerForId(l));
        r.addGeckolibData(DataTickets.PARTIAL_TICK, (Float)Float.valueOf(f));
        r.addGeckolibData(DataTickets.RENDER_COLOR, this.getRenderColor(t, o, f));
        r.addGeckolibData(DataTickets.PACKED_OVERLAY, this.getPackedOverlay(t, o, 0.0f, f));
        r.addGeckolibData(DataTickets.IS_MOVING, false);
        r.addGeckolibData(DataTickets.ANIMATABLE_CLASS, t.getClass());
        if (!DataTickets.TICK.canExtractFrom(r)) {
            r.addGeckolibData(DataTickets.TICK, ClientUtil.getCurrentTick());
        }
    }

    public int getRenderColor(T var1, O var2, float var3);

    default public long getInstanceId(T t, O o) {
        return t.hashCode();
    }

    public void setMolangQueryValues(T var1, O var2, R var3, float var4);

    public boolean firePreRenderEvent(RenderPassInfo<R> var1, OrderedRenderCommandQueue var2);

    default public void applyRenderLayers(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        for (GeoRenderLayer<T, O, R> geoRenderLayer : this.getRenderLayers()) {
            geoRenderLayer.submitRenderTask(renderPassInfo, orderedRenderCommandQueue);
        }
    }

    default public void preApplyRenderLayers(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        for (GeoRenderLayer<T, O, R> geoRenderLayer : this.getRenderLayers()) {
            geoRenderLayer.preRender(renderPassInfo, orderedRenderCommandQueue);
            geoRenderLayer.addPerBoneRender(renderPassInfo, (geoBone, perBoneRender) -> renderPassInfo.addPerBoneRender(geoBone, perBoneRender));
        }
    }

    default public Identifier getTextureLocation(R r) {
        return this.getGeoModel().getTextureResource((GeoRenderState)r);
    }

    default public void submitPerBoneRenderTasks(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        Map<GeoBone, List<PerBoneRender<R>>> map = renderPassInfo.getBoneRenderTasks();
        if (map.isEmpty()) {
            return;
        }
        renderPassInfo.renderPosed(() -> {
            Matrix4f matrix4f = renderPassInfo.getModelRenderMatrixState();
            MatrixStack matrixStack = renderPassInfo.poseStack();
            matrixStack.push();
            matrixStack.peek().copy(renderPassInfo.getModelRenderMatrixPose());
            for (Map.Entry<GeoBone, List<PerBoneRender<R>>> entry : map.entrySet()) {
                GeoBone geoBone = entry.getKey();
                matrixStack.push();
                RenderUtil.transformToBone(matrixStack, geoBone);
                for (PerBoneRender<R> perBoneRender : entry.getValue()) {
                    matrixStack.push();
                    perBoneRender.submitRenderTask(renderPassInfo, geoBone, orderedRenderCommandQueue);
                    matrixStack.pop();
                }
                matrixStack.pop();
            }
            matrixStack.pop();
        });
    }

    default public void applyAnimationControllers(RenderPassInfo<R> renderPassInfo, BoneSnapshots boneSnapshots) {
        ControllerState[] controllerStateArray;
        for (ControllerState controllerState : controllerStateArray = (ControllerState[])renderPassInfo.getOrDefaultGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES, new ControllerState[0])) {
            AnimationProcessor.createBoneSnapshots(controllerState, boneSnapshots);
        }
    }

    public void addRenderData(T var1, O var2, R var3, float var4);

    public R createRenderState(T var1, O var2);

    default public R fillRenderState(T t, O o, R r, float f) {
        this.captureDefaultRenderState(t, o, r, f);
        this.addRenderData(t, o, r, f);
        this.getGeoModel().addAdditionalStateData(t, o, (GeoRenderState)r);
        for (GeoRenderLayer<T, O, R> geoRenderLayer : this.getRenderLayers()) {
            geoRenderLayer.addRenderData(t, o, r, f);
        }
        this.fireCompileRenderStateEvent(t, o, r, f);
        this.setMolangQueryValues(t, o, r, f);
        AnimationProcessor.extractControllerStates(t, r, this.getGeoModel());
        return r;
    }
}

