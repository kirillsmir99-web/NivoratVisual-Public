package rtx.nv.api.modules.settings.impl;

import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;
import rtx.nv.utils.animations.AnimationUtil;

public class PositionSettings extends Setting {
    private float x;
    private float y;
    private float z;
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private float minZ;
    private float maxZ;
    private float defaultX;
    private float defaultY;
    private float defaultZ;
    private boolean defaultXCaptured;
    private boolean defaultYCaptured;
    private boolean defaultZCaptured;
    private float zStep;
    private final AnimationUtil zAnimation = new AnimationUtil();

    public PositionSettings(String name, float x, float y) {
        super(name);
        this.x = x;
        this.y = y;
        this.defaultX = x;
        this.defaultY = y;
    }

    public PositionSettings(String name, float x, float y, float z) {
        super(name);
        this.x = x;
        this.y = y;
        this.z = z;
        this.defaultX = x;
        this.defaultY = y;
        this.defaultZ = z;
    }

    public PositionSettings setValue(float f, float f2) {
        this.setX(f);
        this.setY(f2);
        return this;
    }

    public PositionSettings visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public boolean isDefaultXCaptured() {
        return this.defaultXCaptured;
    }

    public boolean isDefaultYCaptured() {
        return this.defaultYCaptured;
    }

    public boolean isDefaultZCaptured() {
        return this.defaultZCaptured;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
        this.notifyChanged();
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
        this.notifyChanged();
    }

    public float getZ() {
        return this.z;
    }

    public void setZ(float z) {
        this.z = z;
        this.notifyChanged();
    }

    public float getMinX() {
        return this.minX;
    }

    public void setMinX(float minX) {
        this.minX = minX;
    }

    public float getMaxX() {
        return this.maxX;
    }

    public void setMaxX(float maxX) {
        this.maxX = maxX;
    }

    public float getMinY() {
        return this.minY;
    }

    public void setMinY(float minY) {
        this.minY = minY;
    }

    public float getMaxY() {
        return this.maxY;
    }

    public void setMaxY(float maxY) {
        this.maxY = maxY;
    }

    public float getMinZ() {
        return this.minZ;
    }

    public void setMinZ(float minZ) {
        this.minZ = minZ;
    }

    public float getMaxZ() {
        return this.maxZ;
    }

    public void setMaxZ(float maxZ) {
        this.maxZ = maxZ;
    }

    public float getDefaultX() {
        return this.defaultX;
    }

    public float getDefaultY() {
        return this.defaultY;
    }

    public float getDefaultZ() {
        return this.defaultZ;
    }

    public float getZStep() {
        return this.zStep;
    }

    public void setZStep(float zStep) {
        this.zStep = zStep;
    }

    public AnimationUtil getZAnimation() {
        return this.zAnimation;
    }
}
