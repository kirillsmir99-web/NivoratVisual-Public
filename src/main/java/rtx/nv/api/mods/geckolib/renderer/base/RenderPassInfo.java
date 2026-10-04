package rtx.nv.api.mods.geckolib.renderer.base;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animation.state.BoneSnapshot;
import rtx.nv.api.mods.geckolib.cache.model.BakedGeoModel;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.object.DeferredCache;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.PerBoneRender;

public class RenderPassInfo<R extends GeoRenderState> {
    protected final GeoRenderer<?, ?, R> renderer;
    protected final R renderState;
    protected final MatrixStack poseStack;
    protected final BakedGeoModel model;
    protected final CameraRenderState cameraState;
    protected final boolean willRender;
    protected final MatrixStack.Entry objectRenderPose;
    protected final MatrixStack.Entry modelRenderPose;
    protected final DeferredCache<List<RenderPassInfo.BoneUpdater<R>>, BoneSnapshot[]> boneUpdates = new DeferredCache<>(new ObjectArrayList<>(), (List<RenderPassInfo.BoneUpdater<R>> list) -> this.compileBoneUpdates(list));
    protected final Map<GeoBone, List<PerBoneRender<R>>> boneRenderTasks = new Reference2ObjectArrayMap();
    protected final Map<GeoBone, List<BonePositionListener>> bonePositionListeners = new Reference2ObjectArrayMap();

    protected RenderPassInfo(GeoRenderer<?, ?, R> geoRenderer, R r, MatrixStack matrixStack, BakedGeoModel bakedGeoModel, CameraRenderState cameraRenderState, boolean bl) {
        this.renderer = geoRenderer;
        this.renderState = r;
        this.poseStack = matrixStack;
        this.model = bakedGeoModel;
        this.cameraState = cameraRenderState;
        this.willRender = bl;
        this.objectRenderPose = new MatrixStack.Entry();
        this.modelRenderPose = new MatrixStack.Entry();
        this.objectRenderPose.copy(matrixStack.peek());
    }

    public static <R extends GeoRenderState> RenderPassInfo<R> create(GeoRenderer<?, ?, R> geoRenderer, R r, MatrixStack matrixStack, CameraRenderState cameraRenderState, boolean bl) {
        GeoModel geoModel = geoRenderer.getGeoModel();
        BakedGeoModel bakedGeoModel = geoModel.getBakedModel(geoModel.getModelResource(r));
        RenderPassInfo renderPassInfo = new RenderPassInfo(geoRenderer, r, matrixStack, bakedGeoModel, cameraRenderState, bl);
        renderPassInfo.addBoneUpdater(geoRenderer::applyAnimationControllers);
        renderPassInfo.addBoneUpdater(geoRenderer::adjustModelBonesForRender);
        return renderPassInfo;
    }

    public BakedGeoModel model() {
        return this.model;
    }

    public GeoRenderer<?, ?, R> renderer() {
        return this.renderer;
    }

    public CameraRenderState cameraState() {
        return this.cameraState;
    }

    public Matrix4f getModelRenderMatrixState() {
        if ((this.modelRenderPose.getPositionMatrix().properties() & 4) != 0) {
            throw new IllegalStateException("Attempting to access model render matrix state before it has been set");
        }
        return this.modelRenderPose.getPositionMatrix();
    }

    public void captureModelRenderPose() {
        this.modelRenderPose.copy(this.poseStack.peek());
    }

    public int renderColor() {
        return this.renderState.getOrDefaultGeckolibData(DataTickets.RENDER_COLOR, -1);
    }

    public void addBoneUpdater(RenderPassInfo.BoneUpdater<R> boneUpdater) {
        try {
            ((List)(Object)this.boneUpdates.getInput()).add(boneUpdater);
        }
        catch (IllegalStateException illegalStateException) {
            GeckoLibConstants.LOGGER.error("BoneUpdater added after render pass submission", (Throwable)illegalStateException);
        }
    }

    public void addPerBoneRender(GeoBone geoBone2, PerBoneRender<R> perBoneRender) {
        this.boneRenderTasks.computeIfAbsent(geoBone2, geoBone -> new ObjectArrayList()).add(perBoneRender);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void renderPosed(Runnable runnable) {
        int n;
        BoneSnapshot[] boneSnapshotArray = (BoneSnapshot[])this.boneUpdates.compute();
        for (n = 0; n < boneSnapshotArray.length; ++n) {
            boneSnapshotArray[n].apply();
        }
        if (!this.bonePositionListeners.isEmpty()) {
            for (Map.Entry<GeoBone, List<BonePositionListener>> object : this.bonePositionListeners.entrySet()) {
                object.getKey().positionListeners = object.getValue().toArray(new BonePositionListener[0]);
            }
        }
        try {
            runnable.run();
        }
        catch (Exception exception) {
            try {
                GeckoLibConstants.LOGGER.error("Error while rendering GeckoLib model", (Throwable)exception);
            }
            catch (Throwable throwable) {
                for (int i = 0; i < boneSnapshotArray.length; ++i) {
                    boneSnapshotArray[i].cleanup();
                }
                if (!this.bonePositionListeners.isEmpty()) {
                    for (GeoBone geoBone : this.bonePositionListeners.keySet()) {
                        geoBone.positionListeners = null;
                    }
                }
                throw throwable;
            }
            for (int i = 0; i < boneSnapshotArray.length; ++i) {
                boneSnapshotArray[i].cleanup();
            }
            if (!this.bonePositionListeners.isEmpty()) {
                for (GeoBone geoBone : this.bonePositionListeners.keySet()) {
                    geoBone.positionListeners = null;
                }
            }
        }
        for (n = 0; n < boneSnapshotArray.length; ++n) {
            boneSnapshotArray[n].cleanup();
        }
        if (!this.bonePositionListeners.isEmpty()) {
            for (GeoBone geoBone : this.bonePositionListeners.keySet()) {
                geoBone.positionListeners = null;
            }
        }
    }

    public Map<GeoBone, List<PerBoneRender<R>>> getBoneRenderTasks() {
        return this.boneRenderTasks;
    }

    public MatrixStack.Entry getModelRenderMatrixPose() {
        return this.modelRenderPose;
    }

    public int packedLight() {
        return this.renderState.getPackedLight();
    }

    public int packedOverlay() {
        return this.renderState.getOrDefaultGeckolibData(DataTickets.PACKED_OVERLAY, OverlayTexture.DEFAULT_UV);
    }

    public <D> D getOrDefaultGeckolibData(DataTicket<D> dataTicket, D d) {
        return this.renderState().getOrDefaultGeckolibData(dataTicket, d);
    }

    public <D> D getGeckolibData(DataTicket<D> dataTicket) {
        return this.renderState().getGeckolibData(dataTicket);
    }

    public MatrixStack poseStack() {
        return this.poseStack;
    }

    public R renderState() {
        return this.renderState;
    }

    public Matrix4f getPreRenderMatrixState() {
        return this.objectRenderPose.getPositionMatrix();
    }

    public boolean willRender() {
        return this.willRender;
    }

    private /* synthetic */ Optional lambda_compileBoneUpdates_4(List list, String string) {
        return this.model.getBone(string).map(geoBone -> {
            if (geoBone.frameSnapshot == null) {
                geoBone.frameSnapshot = BoneSnapshot.create(geoBone);
                list.add(geoBone.frameSnapshot);
            }
            return geoBone.frameSnapshot;
        });
    }

    public void addBonePositionListener(String string, BonePositionListener bonePositionListener) {
        this.model.getBone(string).ifPresent(geoBone -> this.addBonePositionListener((GeoBone)geoBone, bonePositionListener));
    }

    public void addBonePositionListener(GeoBone geoBone2, BonePositionListener bonePositionListener) {
        this.bonePositionListeners.computeIfAbsent(geoBone2, geoBone -> new ObjectArrayList()).add(bonePositionListener);
    }

    public MatrixStack.Entry getPreRenderMatrixPose() {
        return this.objectRenderPose;
    }

    protected BoneSnapshot[] compileBoneUpdates(List<RenderPassInfo.BoneUpdater<R>> list) {
        ObjectArrayList objectArrayList = new ObjectArrayList(list.size());
        for (RenderPassInfo.BoneUpdater boneUpdater : list) {
            boneUpdater.run(this, arg_0 -> this.lambda_compileBoneUpdates_4((List)objectArrayList, arg_0));
        }
        BoneSnapshot[] boneSnapshotArray = (BoneSnapshot[]) objectArrayList.toArray(new BoneSnapshot[0]);
        for (int i = 0; i < boneSnapshotArray.length; ++i) {
            boneSnapshotArray[i].cleanup();
        }
        return boneSnapshotArray;
    }


    public static interface BoneUpdater<R extends GeoRenderState> {
        public void run(RenderPassInfo<R> var1, BoneSnapshots var2);
    }

    @FunctionalInterface
    public interface BonePositionListener {
        void accept(Vec3d pos, Vec3d rot, Vec3d scale);
    }
}

