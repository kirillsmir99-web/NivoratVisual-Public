package rtx.nv.api.modules.impl.Visuals.emotions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.math.MathHelper;

public class EmotionPlayback {
    private static Emotion activeEmotion = null;
    private static boolean playing = false;
    private static float speed = 1.0f;
    private static boolean looping = false;
    private static float progress = 0.0f;
    private static long localStartedAt = 0L;

    private static boolean stopping = false;
    private static long stopStartedAt = 0L;
    private static float stopFromWeight = 0.0f;
    private static Emotion stoppedEmotion = null;
    private static float stoppedProgress = 0.0f;

    private static Emotion previewEmotion = null;
    private static float previewTime = 0.0f;

    private static final Map<UUID, RemotePlayback> remotePlayers = new ConcurrentHashMap<>();

    private static class RemotePlayback {
        final Emotion emotion;
        final long startedAt;
        final float speed;
        final boolean looping;

        RemotePlayback(Emotion emotion, long startedAt, float speed, boolean looping) {
            this.emotion = emotion;
            this.startedAt = startedAt;
            this.speed = speed;
            this.looping = looping;
        }
    }

    public static void play(Emotion emotion) {
        activeEmotion = emotion;
        playing = true;
        stopping = false;
        stoppedEmotion = null;
        localStartedAt = System.currentTimeMillis();
        progress = 0.0f;
    }

    public static void stop() {
        if (!playing || activeEmotion == null) {
            cancel();
            return;
        }
        stoppedEmotion = activeEmotion;
        stoppedProgress = progress;
        stopFromWeight = calculateLocalWeight();
        stopStartedAt = System.currentTimeMillis();
        stopping = true;
        playing = false;
        activeEmotion = null;
    }

    public static void cancel() {
        playing = false;
        stopping = false;
        activeEmotion = null;
        stoppedEmotion = null;
        progress = 0.0f;
    }

    public static boolean isStopping() {
        return stopping;
    }

    public static void clearRemote() {
        remotePlayers.clear();
    }

    public static void setRemote(UUID uuid, Emotion emotion, long startedAt, float speed, boolean looping) {
        if (uuid == null) {
            return;
        }
        if (emotion == null) {
            remotePlayers.remove(uuid);
        } else {
            remotePlayers.put(uuid, new RemotePlayback(emotion, startedAt, speed, looping));
        }
    }

    public static void setSpeed(float s) {
        speed = Math.max(0.1f, s);
    }

    public static void setLooping(boolean l) {
        looping = l;
    }

    public static void update() {
        if (playing && activeEmotion != null) {
            long elapsed = System.currentTimeMillis() - localStartedAt;
            float durMs = (activeEmotion.duration() * 1000.0f) / Math.max(0.1f, speed);
            float p = durMs > 0.0f ? (float) elapsed / durMs : 0.0f;
            if (p >= 1.0f) {
                if (looping) {
                    localStartedAt = System.currentTimeMillis();
                    progress = 0.0f;
                } else {
                    stop();
                }
            } else {
                progress = p;
            }
        }
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static Emotion active() {
        return activeEmotion;
    }

    public static Emotion currentEmotion() {
        return activeEmotion != null ? activeEmotion : stoppedEmotion;
    }

    public static float currentProgress() {
        return progress;
    }

    private static float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float calculateLocalWeight() {
        if (!playing || activeEmotion == null) {
            return 0.0f;
        }
        long elapsed = System.currentTimeMillis() - localStartedAt;
        float durMs = (activeEmotion.duration() * 1000.0f) / Math.max(0.1f, speed);
        float fadeMs = 250.0f;
        float fadeIn = smoothstep(MathHelper.clamp((float) elapsed / fadeMs, 0.0f, 1.0f));
        float fadeOut = looping ? 1.0f : smoothstep(MathHelper.clamp((durMs - (float) elapsed) / fadeMs, 0.0f, 1.0f));
        return Math.min(fadeIn, fadeOut);
    }

    public static void fill(EmotionStateHolder holder, PlayerLikeEntity entity) {
        if (holder == null) {
            return;
        }
        if (previewEmotion != null) {
            holder.nv_setEmotion(previewEmotion, previewTime, 1.0f);
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entity != null && entity.getUuid().equals(mc.player.getUuid())) {
            if (stopping && stoppedEmotion != null) {
                long elapsedStop = System.currentTimeMillis() - stopStartedAt;
                float stopDuration = 220.0f;
                if (elapsedStop < stopDuration) {
                    float stopT = smoothstep(1.0f - (float) elapsedStop / stopDuration);
                    float w = stopFromWeight * stopT;
                    holder.nv_setEmotion(stoppedEmotion, stoppedProgress, w);
                    return;
                } else {
                    stopping = false;
                    stoppedEmotion = null;
                }
            }
            if (playing && activeEmotion != null) {
                long elapsed = System.currentTimeMillis() - localStartedAt;
                float durMs = (activeEmotion.duration() * 1000.0f) / Math.max(0.1f, speed);
                float p = durMs > 0.0f ? (float) elapsed / durMs : 0.0f;
                if (p >= 1.0f) {
                    if (looping) {
                        p = p % 1.0f;
                        progress = p;
                    } else {
                        stop();
                        holder.nv_setEmotion(null, 0.0f, 0.0f);
                        return;
                    }
                } else {
                    progress = p;
                }

                float weight = calculateLocalWeight();
                holder.nv_setEmotion(activeEmotion, p, weight);
                return;
            }
        } else if (entity != null) {
            RemotePlayback remote = remotePlayers.get(entity.getUuid());
            if (remote != null && remote.emotion != null) {
                long elapsed = System.currentTimeMillis() - remote.startedAt;
                float durMs = (remote.emotion.duration() * 1000.0f) / Math.max(0.1f, remote.speed);
                float p = durMs > 0.0f ? (float) elapsed / durMs : 0.0f;
                if (p < 1.0f || remote.looping) {
                    if (remote.looping) {
                        p = p % 1.0f;
                    }
                    float fadeMs = 250.0f;
                    float fadeIn = smoothstep(MathHelper.clamp((float) elapsed / fadeMs, 0.0f, 1.0f));
                    float fadeOut = remote.looping ? 1.0f : smoothstep(MathHelper.clamp((durMs - (float) elapsed) / fadeMs, 0.0f, 1.0f));
                    float weight = Math.min(fadeIn, fadeOut);
                    holder.nv_setEmotion(remote.emotion, p, weight);
                    return;
                }
            }
        }
        holder.nv_setEmotion(null, 0.0f, 0.0f);
    }

    public static void applyTo(BipedEntityModel<?> model, EmotionStateHolder holder, float limbSwingAmplitude) {
        if (holder == null || model == null) {
            return;
        }
        Emotion emotion = holder.nv_getEmotion();
        if (emotion == null) {
            return;
        }
        float time = holder.nv_getEmotionTime();
        float weight = holder.nv_getEmotionWeight();
        if (weight <= 0.0f) {
            return;
        }
        float moveDamp = MathHelper.clamp(1.0f - limbSwingAmplitude * 2.0f, 0.0f, 1.0f);
        float w = weight * moveDamp;
        if (w <= 0.001f) {
            return;
        }

        EmotionPose pose = new EmotionPose();
        emotion.apply(pose, time);

        // Always ensure base origins are preserved
        model.rightArm.setOrigin(-5.0f, 2.0f, 0.0f);
        model.leftArm.setOrigin(5.0f, 2.0f, 0.0f);

        if (!Float.isNaN(pose.headX)) model.head.pitch = MathHelper.lerp(w, model.head.pitch, model.head.pitch + pose.headX);
        if (!Float.isNaN(pose.headY)) model.head.yaw = MathHelper.lerp(w, model.head.yaw, model.head.yaw + pose.headY);
        if (!Float.isNaN(pose.headZ)) model.head.roll = MathHelper.lerp(w, 0.0f, pose.headZ);
        model.hat.pitch = model.head.pitch;
        model.hat.yaw = model.head.yaw;
        model.hat.roll = model.head.roll;

        float baseBodyPitch = (model.body.pitch > 0.4f && model.body.pitch < 0.6f) ? 0.5f : 0.0f;
        float baseBodyYaw = 0.0f;
        float baseBodyRoll = 0.0f;

        if (!Float.isNaN(pose.bodyX)) model.body.pitch = MathHelper.lerp(w, baseBodyPitch, baseBodyPitch + pose.bodyX);
        if (!Float.isNaN(pose.bodyY)) model.body.yaw = MathHelper.lerp(w, baseBodyYaw, baseBodyYaw + pose.bodyY);
        if (!Float.isNaN(pose.bodyZ)) model.body.roll = MathHelper.lerp(w, baseBodyRoll, baseBodyRoll + pose.bodyZ);

        boolean hasRightArm = !Float.isNaN(pose.rightArmX) || !Float.isNaN(pose.rightArmY) || !Float.isNaN(pose.rightArmZ);
        if (hasRightArm) {
            float targetX = !Float.isNaN(pose.rightArmX) ? pose.rightArmX : 0.0f;
            float targetY = !Float.isNaN(pose.rightArmY) ? pose.rightArmY : 0.0f;
            float targetZ = !Float.isNaN(pose.rightArmZ) ? pose.rightArmZ : 0.06f;
            model.rightArm.pitch = MathHelper.lerp(w, model.rightArm.pitch, targetX + model.body.pitch);
            model.rightArm.yaw = MathHelper.lerp(w, model.rightArm.yaw, targetY + model.body.yaw);
            model.rightArm.roll = MathHelper.lerp(w, model.rightArm.roll, targetZ + model.body.roll);
        } else if (w > 0.01f) {
            model.rightArm.yaw += model.body.yaw * w;
            model.rightArm.pitch += model.body.pitch * w;
            model.rightArm.roll += model.body.roll * w;
        }

        boolean hasLeftArm = !Float.isNaN(pose.leftArmX) || !Float.isNaN(pose.leftArmY) || !Float.isNaN(pose.leftArmZ);
        if (hasLeftArm) {
            float targetX = !Float.isNaN(pose.leftArmX) ? pose.leftArmX : 0.0f;
            float targetY = !Float.isNaN(pose.leftArmY) ? pose.leftArmY : 0.0f;
            float targetZ = !Float.isNaN(pose.leftArmZ) ? pose.leftArmZ : -0.06f;
            model.leftArm.pitch = MathHelper.lerp(w, model.leftArm.pitch, targetX + model.body.pitch);
            model.leftArm.yaw = MathHelper.lerp(w, model.leftArm.yaw, targetY + model.body.yaw);
            model.leftArm.roll = MathHelper.lerp(w, model.leftArm.roll, targetZ + model.body.roll);
        } else if (w > 0.01f) {
            model.leftArm.yaw += model.body.yaw * w;
            model.leftArm.pitch += model.body.pitch * w;
            model.leftArm.roll += model.body.roll * w;
        }

        boolean hasRightLeg = !Float.isNaN(pose.rightLegX) || !Float.isNaN(pose.rightLegY) || !Float.isNaN(pose.rightLegZ);
        if (hasRightLeg) {
            float targetX = !Float.isNaN(pose.rightLegX) ? pose.rightLegX : 0.0f;
            float targetY = !Float.isNaN(pose.rightLegY) ? pose.rightLegY : 0.0f;
            float targetZ = !Float.isNaN(pose.rightLegZ) ? pose.rightLegZ : 0.0f;
            model.rightLeg.pitch = MathHelper.lerp(w, model.rightLeg.pitch, targetX);
            model.rightLeg.yaw = MathHelper.lerp(w, model.rightLeg.yaw, targetY);
            model.rightLeg.roll = MathHelper.lerp(w, model.rightLeg.roll, targetZ);
        }

        boolean hasLeftLeg = !Float.isNaN(pose.leftLegX) || !Float.isNaN(pose.leftLegY) || !Float.isNaN(pose.leftLegZ);
        if (hasLeftLeg) {
            float targetX = !Float.isNaN(pose.leftLegX) ? pose.leftLegX : 0.0f;
            float targetY = !Float.isNaN(pose.leftLegY) ? pose.leftLegY : 0.0f;
            float targetZ = !Float.isNaN(pose.leftLegZ) ? pose.leftLegZ : 0.0f;
            model.leftLeg.pitch = MathHelper.lerp(w, model.leftLeg.pitch, targetX);
            model.leftLeg.yaw = MathHelper.lerp(w, model.leftLeg.yaw, targetY);
            model.leftLeg.roll = MathHelper.lerp(w, model.leftLeg.roll, targetZ);
        }
    }

    public static void beginPreview(Emotion emotion, float time) {
        previewEmotion = emotion;
        previewTime = time;
    }

    public static void endPreview() {
        previewEmotion = null;
        previewTime = 0.0f;
    }
}
