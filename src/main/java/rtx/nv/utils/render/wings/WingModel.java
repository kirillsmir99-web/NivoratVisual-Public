package rtx.nv.utils.render.wings;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WingModel {
    private final String id;
    private final String name;
    private final int textureWidth;
    private final int textureHeight;
    private final int totalTextureHeight;
    private final int frameCount;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final List<WingBone> rootBones;
    private final Map<String, WingAnimation> animations;

    public WingModel(String id, String name, int textureWidth, int textureHeight, int totalTextureHeight,
                     float offsetX, float offsetY, float offsetZ,
                     List<WingBone> rootBones, Map<String, WingAnimation> animations) {
        this.id = id;
        this.name = name;
        this.textureWidth = textureWidth > 0 ? textureWidth : 64;
        this.textureHeight = textureHeight > 0 ? textureHeight : 64;
        this.totalTextureHeight = totalTextureHeight > 0 ? totalTextureHeight : this.textureHeight;
        this.frameCount = Math.max(1, this.totalTextureHeight / this.textureHeight);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rootBones = rootBones != null ? rootBones : List.of();
        this.animations = animations != null ? animations : Map.of();
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light, float glideWeight, float limbSwing, float limbAmplitude, float age, float speed) {
        WingAnimation idle = animations.get("idle");
        if (idle == null && !animations.isEmpty()) idle = animations.values().iterator().next();
        WingAnimation walk = animations.getOrDefault("walk", animations.getOrDefault("run", idle));
        WingAnimation fly = animations.getOrDefault("fly", walk);
        float movement = Math.clamp(limbAmplitude * 3.0f, 0, 1);
        float glide = Math.clamp(glideWeight, 0, 1);
        float time = age * .05f * speed;
        Map<String, float[]> animRotations = new HashMap<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (WingAnimation animation : new WingAnimation[]{idle, walk, fly}) {
            if (animation != null) names.addAll(animation.boneKeyframes().keySet());
        }
        for (String bone : names) {
            float[] a = sample(idle, bone, time), b = sample(walk, bone, time), c = sample(fly, bone, time);
            float[] result = new float[3];
            for (int axis = 0; axis < 3; axis++) result[axis] = blendAngle(blendAngle(a[axis], b[axis], movement), c[axis], glide);
            animRotations.put(bone, result);
        }
        float idleFlap = 6 + (float)Math.sin(age * .08f * speed) * 4;
        float walkFlap = 8 + (float)Math.sin(age * .13f * speed) * limbAmplitude * 18;
        float flyFlap = 32 + (float)Math.sin(age * .25f * speed) * 5;
        float proceduralFlap = (idleFlap + (walkFlap - idleFlap) * movement) * (1 - glide) + flyFlap * glide;

        float vOffset = 0.0f;
        if (this.frameCount > 1) {
            int frame = (int) (((long)(age * .5f)) % this.frameCount);
            vOffset = (float) (frame * this.textureHeight);
        }

        float uScale = 1.0f / (float) this.textureWidth;
        float vScale = 1.0f / (float) this.totalTextureHeight;

        matrices.push();
        // Scale to block units and invert Y (Blockbench +Y is UP, player body +Y is DOWN)
        matrices.scale(1.0f / 16.0f, -1.0f / 16.0f, 1.0f / 16.0f);
        // Translate by model attachment offset so upper back anchor aligns with (0,0,0)
        matrices.translate(-this.offsetX, -this.offsetY, -this.offsetZ);

        boolean hasFly = animations.containsKey("fly");
        float extraGlideFlap = (!hasFly && glide > 0.01f) ? flyFlap * glide : 0.0f;

        for (WingBone bone : this.rootBones) {
            bone.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV, animRotations, proceduralFlap, extraGlideFlap, vOffset, vScale, uScale);
        }

        matrices.pop();
    }

    private static float[] sample(WingAnimation animation, String bone, float time) {
        float[] values = animation == null ? null : animation.sampleBone(bone, time);
        return values != null ? values : new float[3];
    }

    private static float blendAngle(float a, float b, float factor) {
        float delta = (b - a) % 360;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        return a + delta * factor;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getTextureWidth() {
        return this.textureWidth;
    }

    public int getTextureHeight() {
        return this.textureHeight;
    }
}
