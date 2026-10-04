package rtx.nv.api.mods.geckolib.cache.model;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import rtx.nv.api.mods.geckolib.animation.state.BoneSnapshot;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;
import rtx.nv.api.mods.geckolib.util.MiscUtil;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public abstract class GeoBone {
    protected final GeoBone parent;
    protected final String name;
    protected final GeoBone[] children;
    protected final float pivotX;
    protected final float pivotY;
    protected final float pivotZ;
    protected final float baseRotX;
    protected final float baseRotY;
    protected final float baseRotZ;
    public BoneSnapshot frameSnapshot = null;
    public RenderPassInfo.BonePositionListener[] positionListeners = null;

    protected GeoBone(GeoBone geoBone, String string, GeoBone[] geoBoneArray, float f, float f2, float f3, float f4, float f5, float f6) {
        this.parent = geoBone;
        this.name = string;
        this.children = geoBoneArray;
        this.pivotX = f;
        this.pivotY = f2;
        this.pivotZ = f3;
        this.baseRotX = f4;
        this.baseRotY = f5;
        this.baseRotZ = f6;
    }

    public String name() {
        return this.name;
    }

    public GeoBone parent() {
        return this.parent;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        GeoBone geoBone = (GeoBone)object;
        if (this.parent != geoBone.parent) {
            return false;
        }
        if (!this.name.equals(geoBone.name)) {
            return false;
        }
        if (!(MiscUtil.areFloatsEqual(this.pivotX, geoBone.pivotX) && MiscUtil.areFloatsEqual(this.pivotY, geoBone.pivotY) && MiscUtil.areFloatsEqual(this.pivotZ, geoBone.pivotZ))) {
            return false;
        }
        return Arrays.equals(this.children, geoBone.children);
    }

    public int hashCode() {
        return Objects.hash(this.parent == null ? "" : this.parent, this.name, Arrays.hashCode(this.children), Float.valueOf(this.pivotX), Float.valueOf(this.pivotY), Float.valueOf(this.pivotZ));
    }

    public GeoBone[] children() {
        return this.children;
    }

    public float pivotX() {
        return this.pivotX;
    }

    public float pivotY() {
        return this.pivotY;
    }

    public abstract <R extends GeoRenderState> void render(RenderPassInfo<R> var1, MatrixStack var2, VertexConsumer var3, int var4, int var5, int var6);

    public float baseRotY() {
        return this.baseRotY;
    }

    public float baseRotZ() {
        return this.baseRotZ;
    }

    public float pivotZ() {
        return this.pivotZ;
    }

    public float baseRotX() {
        return this.baseRotX;
    }

    public void updateBonePositionListeners(MatrixStack matrixStack, RenderPassInfo<?> renderPassInfo) {
        if (this.positionListeners != null) {
            Matrix4f matrix4f = new Matrix4f((Matrix4fc)matrixStack.peek().getPositionMatrix());
            Matrix4f matrix4f2 = RenderUtil.extractPoseFromRoot((Matrix4fc)matrix4f, renderPassInfo.getPreRenderMatrixState());
            Matrix4f matrix4f3 = RenderUtil.extractPoseFromRoot((Matrix4fc)matrix4f, renderPassInfo.getModelRenderMatrixState());
            Vec3d vec3d = renderPassInfo.renderState().getGeckolibData(DataTickets.POSITION);
            Matrix4f matrix4f4 = vec3d == null ? null : RenderUtil.addPosToMatrix(new Matrix4f((Matrix4fc)matrix4f2), vec3d);
            Vec3d vec3d2 = RenderUtil.renderPoseToPosition((Matrix4fc)matrix4f2, 1.0f, 1.0f, 1.0f);
            Vec3d vec3d3 = RenderUtil.renderPoseToPosition((Matrix4fc)matrix4f3, -16.0f, 16.0f, 16.0f);
            Vec3d vec3d4 = matrix4f4 == null ? null : RenderUtil.renderPoseToPosition((Matrix4fc)matrix4f4, 1.0f, 1.0f, 1.0f);
            for (int i = 0; i < this.positionListeners.length; ++i) {
                this.positionListeners[i].accept(vec3d4, vec3d3, vec3d2);
            }
        }
    }

    public void translateAwayFromPivotPoint(MatrixStack matrixStack) {
        matrixStack.translate(-this.pivotX() / 16.0f, -this.pivotY() / 16.0f, -this.pivotZ() / 16.0f);
    }

    public <R extends GeoRenderState> void positionAndRender(RenderPassInfo<R> renderPassInfo, VertexConsumer vertexConsumer, int n, int n2, int n3) {
        MatrixStack matrixStack = renderPassInfo.poseStack();
        matrixStack.push();
        RenderUtil.prepMatrixForBone(matrixStack, this);
        this.updateBonePositionListeners(matrixStack, renderPassInfo);
        this.render(renderPassInfo, matrixStack, vertexConsumer, n, n2, n3);
        this.renderChildren(renderPassInfo, matrixStack, vertexConsumer, n, n2, n3);
        matrixStack.pop();
    }

    public void renderChildren(RenderPassInfo<?> renderPassInfo, MatrixStack matrixStack, VertexConsumer vertexConsumer, int n, int n2, int n3) {
        if (this.frameSnapshot == null || !this.frameSnapshot.areChildrenHidden()) {
            for (GeoBone geoBone : this.children) {
                matrixStack.push();
                RenderUtil.prepMatrixForBoneAndUpdateListeners(matrixStack, geoBone, renderPassInfo);
                geoBone.render(renderPassInfo, matrixStack, vertexConsumer, n, n2, n3);
                geoBone.renderChildren(renderPassInfo, matrixStack, vertexConsumer, n, n2, n3);
                matrixStack.pop();
            }
        }
    }

    public void translateToPivotPoint(MatrixStack matrixStack) {
        matrixStack.translate(this.pivotX() / 16.0f, this.pivotY() / 16.0f, this.pivotZ() / 16.0f);
    }
}

