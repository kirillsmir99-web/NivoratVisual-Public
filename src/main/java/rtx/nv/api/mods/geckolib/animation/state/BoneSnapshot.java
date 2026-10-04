package rtx.nv.api.mods.geckolib.animation.state;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;

public class BoneSnapshot {
    private final GeoBone bone;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;
    private float translateX;
    private float translateY;
    private float translateZ;
    private float rotX;
    private float rotY;
    private float rotZ;
    private boolean skipRender = false;
    private boolean skipChildrenRender = false;

    protected BoneSnapshot(GeoBone geoBone) {
        this.bone = geoBone;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        return this.bone.name().equals(((BoneSnapshot)object).bone.name());
    }

    public int hashCode() {
        return this.bone.name().hashCode();
    }

    public boolean isHidden() {
        return this.skipRender;
    }

    public void scale(MatrixStack matrixStack) {
        if (this.hasScale()) {
            matrixStack.scale(this.getScaleX(), this.getScaleY(), this.getScaleZ());
        }
    }

    public void apply() {
        this.bone.frameSnapshot = this;
    }

    public static BoneSnapshot create(GeoBone geoBone) {
        return new BoneSnapshot(geoBone);
    }

    public void cleanup() {
        this.bone.frameSnapshot = null;
    }

    public void rotate(MatrixStack matrixStack) {
        if (this.getRotZ() != 0.0f) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotation(this.getRotZ()));
        }
        if (this.getRotY() != 0.0f) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotation(this.getRotY()));
        }
        if (this.getRotX() != 0.0f) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotation(this.getRotX()));
        }
    }

    public BoneSnapshot setScale(float f, float f2, float f3) {
        this.scaleX = f;
        this.scaleY = f2;
        this.scaleZ = f3;
        return this;
    }

    public BoneSnapshot skipChildrenRender(boolean bl) {
        this.skipChildrenRender = bl;
        return this;
    }

    public BoneSnapshot setRotation(float f, float f2, float f3) {
        this.rotX = f;
        this.rotY = f2;
        this.rotZ = f3;
        return this;
    }

    public GeoBone getBone() {
        return this.bone;
    }

    public BoneSnapshot setTranslation(float f, float f2, float f3) {
        this.translateX = f;
        this.translateY = f2;
        this.translateZ = f3;
        return this;
    }

    public void translate(MatrixStack matrixStack) {
        if (this.hasTranslation()) {
            matrixStack.translate(-this.getTranslateX() / 16.0f, this.getTranslateY() / 16.0f, this.getTranslateZ() / 16.0f);
        }
    }

    public BoneSnapshot skipRender(boolean bl) {
        this.skipRender = bl;
        return this;
    }

    public boolean areChildrenHidden() {
        return this.skipChildrenRender;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public float getTranslateX() {
        return this.translateX;
    }

    public float getTranslateY() {
        return this.translateY;
    }

    public boolean hasScale() {
        return this.scaleX != 1.0f || this.scaleY != 1.0f || this.scaleZ != 1.0f;
    }

    public BoneSnapshot setScaleX(float f) {
        this.scaleX = f;
        return this;
    }

    public BoneSnapshot setScaleY(float f) {
        this.scaleY = f;
        return this;
    }

    public BoneSnapshot setScaleZ(float f) {
        this.scaleZ = f;
        return this;
    }

    public BoneSnapshot setRotX(float f) {
        this.rotX = f;
        return this;
    }

    public BoneSnapshot setRotY(float f) {
        this.rotY = f;
        return this;
    }

    public BoneSnapshot setRotZ(float f) {
        this.rotZ = f;
        return this;
    }

    public float getRotZ() {
        return this.rotZ;
    }

    public float getScaleZ() {
        return this.scaleZ;
    }

    public float getRotX() {
        return this.rotX;
    }

    public float getRotY() {
        return this.rotY;
    }

    public float getTranslateZ() {
        return this.translateZ;
    }

    public BoneSnapshot setTranslateZ(float f) {
        this.translateZ = f;
        return this;
    }

    public boolean hasTranslation() {
        return this.translateX != 0.0f || this.translateY != 0.0f || this.translateZ != 0.0f;
    }

    public boolean hasRotation() {
        return this.rotX != 0.0f || this.rotY != 0.0f || this.rotZ != 0.0f;
    }

    public BoneSnapshot setTranslateX(float f) {
        this.translateX = f;
        return this;
    }

    public BoneSnapshot setTranslateY(float f) {
        this.translateY = f;
        return this;
    }
}

