package rtx.nv.api.ui.window;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

public final class WorldGuiCloseAnimation {
    private static final double DISTANCE = 7.5;
    private static final float NEAR_PLANE = 0.05f;
    private static final float CLOSE_STEP_PER_TICK = 0.08f;
    private static final float TICK_MS = 50.0f;
    private static final float REVERSE_MS = 250.0f;
    private static final float SCALE_END = 0.7f;
    private static final float MAX_BLUR_RADIUS = 9.0f;
    private static final Matrix4f lastProjMat = new Matrix4f();
    private static final Matrix4f lastViewMat = new Matrix4f();
    private static Vec3d lastCameraPos = Vec3d.ZERO;
    private static boolean hasWorldMatrices;
    private static boolean active;
    private static boolean reversing;
    private static long startNanos;
    private static float startValue;
    private static float frameValue;
    private static Vec3d anchor;
    private static float yaw;
    private static float pitch;
    private static float worldScaleX;
    private static float worldScaleY;
    private static int beginWidth;
    private static int beginHeight;
    private static float startAlpha;
    private static float startScreenScale;
    private static Vec3d reverseAnchor;
    private static float reverseYaw;
    private static float reversePitch;

    private WorldGuiCloseAnimation() {
    }

    static {
        anchor = Vec3d.ZERO;
        startAlpha = 1.0f;
        startScreenScale = 1.0f;
        reverseAnchor = Vec3d.ZERO;
    }

    private static float value() {
        float f = (float)(System.nanoTime() - startNanos) / 1000000.0f;
        if (reversing) {
            return startValue + (1.0f - startValue) * WorldGuiCloseAnimation.smoothStep(WorldGuiCloseAnimation.clamp01(f / 250.0f));
        }
        return Math.max(0.0f, startValue - f / 50.0f * 0.08f);
    }

    public static void reverse() {
        if (!active || reversing) {
            return;
        }
        reversing = true;
        startValue = WorldGuiCloseAnimation.clamp01(frameValue);
        startNanos = System.nanoTime();
        reverseAnchor = anchor;
        reverseYaw = yaw;
        reversePitch = pitch;
    }

    public static void begin(float f, float f2) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.world == null || minecraftClient.player == null || minecraftClient.getWindow() == null || !hasWorldMatrices) {
            WorldGuiCloseAnimation.cancel();
            return;
        }
        Camera camera = minecraftClient.gameRenderer.getCamera();
        Vector3fc vector3fc = camera.getHorizontalPlane();
        double d = 7.5 * (double)minecraftClient.getWindow().getFramebufferHeight() / 1080.0;
        anchor = camera.getCameraPos().add((double)vector3fc.x() * d, (double)vector3fc.y() * d, (double)vector3fc.z() * d);
        yaw = camera.getYaw();
        pitch = camera.getPitch();
        float[] fArray = WorldGuiCloseAnimation.resolveWorldScales(minecraftClient.getWindow().getFramebufferWidth(), minecraftClient.getWindow().getFramebufferHeight());
        if (fArray == null) {
            WorldGuiCloseAnimation.cancel();
            return;
        }
        worldScaleX = fArray[0];
        worldScaleY = fArray[1];
        beginWidth = minecraftClient.getWindow().getFramebufferWidth();
        beginHeight = minecraftClient.getWindow().getFramebufferHeight();
        startAlpha = WorldGuiCloseAnimation.clamp01(f <= 0.0f ? 1.0f : f);
        startScreenScale = Float.isFinite(f2) && f2 > 1.0E-4f ? f2 : 1.0f;
        startValue = 1.0f;
        startNanos = System.nanoTime();
        frameValue = startValue;
        reversing = false;
        active = true;
    }

    public static void cancel() {
        active = false;
        reversing = false;
    }

    public static long remainingNanos() {
        if (!active) {
            return 0L;
        }
        if (reversing) {
            float f = (float)(System.nanoTime() - startNanos) / 1000000.0f;
            return (long)(Math.max(0.0f, 250.0f - f) * 1000000.0f);
        }
        return (long)(WorldGuiCloseAnimation.clamp01(frameValue) / 0.08f * 50.0f * 1000000.0f);
    }

    public static boolean isActive() {
        return active;
    }

    private static float scaleFactor(float f) {
        float f2 = 1.0f - f;
        float f3 = 2.70158f * f2 * f2 * f2 - 1.70158f * f2 * f2;
        return 1.0f + -0.3f * f3;
    }

    public static long token() {
        return startNanos;
    }

    public static float alpha() {
        if (!active) {
            return 0.0f;
        }
        float f = 1.0f - WorldGuiCloseAnimation.clamp01(frameValue);
        return WorldGuiCloseAnimation.clamp01(startAlpha * (1.0f - f * f * f));
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public static boolean isFinished() {
        return !active || (reversing ? frameValue >= 1.0f : frameValue <= 0.0f);
    }

    public static float blurRadius() {
        if (!active) {
            return 0.0f;
        }
        float f = 1.0f - WorldGuiCloseAnimation.clamp01(frameValue);
        return 9.0f * Math.max(f * f, 1.0f - startAlpha);
    }

    private static float smoothStep(float f) {
        return f * f * (3.0f - 2.0f * f);
    }

    public static void captureWorldMatrices(Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d vec3d) {
        lastProjMat.set((Matrix4fc)matrix4f);
        lastViewMat.set((Matrix4fc)matrix4f2);
        lastCameraPos = vec3d;
        hasWorldMatrices = true;
    }

    private static float[] project(Matrix4f matrix4f, float f, float f2, float f3, float f4) {
        float f5 = matrix4f.m00() * f + matrix4f.m10() * f2 + matrix4f.m30();
        float f6 = matrix4f.m01() * f + matrix4f.m11() * f2 + matrix4f.m31();
        float f7 = matrix4f.m03() * f + matrix4f.m13() * f2 + matrix4f.m33();
        if (f7 <= 0.05f) {
            return null;
        }
        float f8 = 1.0f / f7;
        return new float[]{(f5 * f8 * 0.5f + 0.5f) * f3, (1.0f - (f6 * f8 * 0.5f + 0.5f)) * f4};
    }

    public static boolean isReversing() {
        return active && reversing;
    }

    public static float screenScale() {
        return active ? startScreenScale : 1.0f;
    }

    public static boolean isDetachedRender() {
        return active && frameValue > 0.0f && (reversing || MinecraftClient.getInstance().currentScreen == null);
    }

    public static void updateFrame() {
        if (!active) {
            return;
        }
        frameValue = WorldGuiCloseAnimation.value();
        if (reversing) {
            WorldGuiCloseAnimation.alignToCamera();
        }
    }

    public static boolean surfaceChanged() {
        if (!active) {
            return false;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient == null || minecraftClient.getWindow() == null || minecraftClient.getWindow().getFramebufferWidth() != beginWidth || minecraftClient.getWindow().getFramebufferHeight() != beginHeight;
    }

    public static Matrix4f compositeMatrix(float f, float f2) {
        if (!active || !hasWorldMatrices) {
            return null;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.world == null || minecraftClient.currentScreen != null && !reversing) {
            return null;
        }
        if ((int)f != beginWidth || (int)f2 != beginHeight) {
            return null;
        }
        float f3 = WorldGuiCloseAnimation.scaleFactor(WorldGuiCloseAnimation.clamp01(frameValue));
        if (f3 <= 0.001f) {
            return null;
        }
        return WorldGuiCloseAnimation.buildMatrix(f3, f, f2);
    }

    public static float progress() {
        return active ? 1.0f - WorldGuiCloseAnimation.clamp01(frameValue) : 0.0f;
    }

    private static float[] resolveWorldScales(float f, float f2) {
        float f3;
        float f4;
        float f5 = f / 2.0f;
        float f6 = f2 / 2.0f;
        float f7 = (float)(WorldGuiCloseAnimation.anchor.x - WorldGuiCloseAnimation.lastCameraPos.x);
        float f8 = (float)(WorldGuiCloseAnimation.anchor.y - WorldGuiCloseAnimation.lastCameraPos.y);
        float f9 = (float)(WorldGuiCloseAnimation.anchor.z - WorldGuiCloseAnimation.lastCameraPos.z);
        Matrix4f matrix4f = new Matrix4f((Matrix4fc)lastProjMat).mul((Matrix4fc)lastViewMat).mul((Matrix4fc)new Matrix4f().translate(f7, f8, f9).rotateY((float)Math.toRadians(-yaw + 180.0f)).rotateX((float)Math.toRadians(-pitch + 180.0f)).translate(-f5, -f6, 0.0f));
        float[] fArray = WorldGuiCloseAnimation.project(matrix4f, 0.0f, f6, f, f2);
        float[] fArray2 = WorldGuiCloseAnimation.project(matrix4f, f, f6, f, f2);
        float[] fArray3 = WorldGuiCloseAnimation.project(matrix4f, f5, 0.0f, f, f2);
        float[] fArray4 = WorldGuiCloseAnimation.project(matrix4f, f5, f2, f, f2);
        float f10 = Float.NaN;
        if (fArray != null && fArray2 != null && Float.isFinite(f4 = f / Math.max(1.0E-4f, Math.abs(fArray2[0] - fArray[0]))) && f4 > 0.0f) {
            f10 = f4;
        }
        f4 = Float.NaN;
        if (fArray3 != null && fArray4 != null && Float.isFinite(f3 = f2 / Math.max(1.0E-4f, Math.abs(fArray4[1] - fArray3[1]))) && f3 > 0.0f) {
            f4 = f3;
        }
        if (Float.isNaN(f10) && Float.isNaN(f4)) {
            return null;
        }
        if (Float.isNaN(f10)) {
            f10 = f4;
        }
        if (Float.isNaN(f4)) {
            f4 = f10;
        }
        return new float[]{f10, f4};
    }

    private static float wrapDegrees(float f) {
        float f2 = f % 360.0f;
        if (f2 >= 180.0f) {
            f2 -= 360.0f;
        }
        if (f2 < -180.0f) {
            f2 += 360.0f;
        }
        return f2;
    }

    private static void alignToCamera() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.world == null || minecraftClient.gameRenderer == null || minecraftClient.getWindow() == null) {
            return;
        }
        Camera camera = minecraftClient.gameRenderer.getCamera();
        Vector3fc vector3fc = camera.getHorizontalPlane();
        double d = 7.5 * (double)minecraftClient.getWindow().getFramebufferHeight() / 1080.0;
        Vec3d vec3d = camera.getCameraPos().add((double)vector3fc.x() * d, (double)vector3fc.y() * d, (double)vector3fc.z() * d);
        float f = WorldGuiCloseAnimation.reverseBlend();
        anchor = new Vec3d(WorldGuiCloseAnimation.reverseAnchor.x + (vec3d.x - WorldGuiCloseAnimation.reverseAnchor.x) * (double)f, WorldGuiCloseAnimation.reverseAnchor.y + (vec3d.y - WorldGuiCloseAnimation.reverseAnchor.y) * (double)f, WorldGuiCloseAnimation.reverseAnchor.z + (vec3d.z - WorldGuiCloseAnimation.reverseAnchor.z) * (double)f);
        yaw = reverseYaw + WorldGuiCloseAnimation.wrapDegrees(camera.getYaw() - reverseYaw) * f;
        pitch = reversePitch + (camera.getPitch() - reversePitch) * f;
    }

    private static float reverseBlend() {
        float f = 1.0f - startValue;
        if (f <= 1.0E-4f) {
            return 1.0f;
        }
        return WorldGuiCloseAnimation.clamp01((frameValue - startValue) / f);
    }

    public static boolean hasCapturedWorldMatrices() {
        return hasWorldMatrices;
    }

    public static Matrix4f buildRemoteMatrix(Vec3d vec3d, float f, float f2, float f3, float f4, float f5, float f6) {
        if (!hasWorldMatrices || vec3d == null || f4 <= 0.001f) {
            return null;
        }
        float f7 = (float)(vec3d.x - WorldGuiCloseAnimation.lastCameraPos.x);
        float f8 = (float)(vec3d.y - WorldGuiCloseAnimation.lastCameraPos.y);
        float f9 = (float)(vec3d.z - WorldGuiCloseAnimation.lastCameraPos.z);
        Matrix4f matrix4f = new Matrix4f().translate(f7, f8, f9).rotateY((float)Math.toRadians(-f + 180.0f)).rotateX((float)Math.toRadians(-f2 + 180.0f)).scale(f3, f3, f3).scale(f4, f4, 1.0f).translate(-f5, -f6, 0.0f);
        return new Matrix4f((Matrix4fc)lastProjMat).mul((Matrix4fc)lastViewMat).mul((Matrix4fc)matrix4f);
    }

    private static Matrix4f buildMatrix(float f, float f2, float f3) {
        float f4 = f2 / 2.0f;
        float f5 = f3 / 2.0f;
        float f6 = (float)(WorldGuiCloseAnimation.anchor.x - WorldGuiCloseAnimation.lastCameraPos.x);
        float f7 = (float)(WorldGuiCloseAnimation.anchor.y - WorldGuiCloseAnimation.lastCameraPos.y);
        float f8 = (float)(WorldGuiCloseAnimation.anchor.z - WorldGuiCloseAnimation.lastCameraPos.z);
        Matrix4f matrix4f = new Matrix4f().translate(f6, f7, f8).rotateY((float)Math.toRadians(-yaw + 180.0f)).rotateX((float)Math.toRadians(-pitch + 180.0f)).scale(worldScaleX, worldScaleY, (worldScaleX + worldScaleY) * 0.5f).scale(f, f, 1.0f).translate(-f4, -f5, 0.0f);
        return new Matrix4f((Matrix4fc)lastProjMat).mul((Matrix4fc)lastViewMat).mul((Matrix4fc)matrix4f);
    }

    public static float liveYaw() {
        return yaw;
    }

    public static Vec3d liveAnchor() {
        return anchor;
    }

    public static float livePitch() {
        return pitch;
    }
}

