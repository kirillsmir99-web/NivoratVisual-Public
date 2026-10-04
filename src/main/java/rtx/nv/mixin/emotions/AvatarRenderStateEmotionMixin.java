package rtx.nv.mixin.emotions;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.modules.impl.Visuals.emotions.Emotion;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionStateHolder;

@Mixin(net.minecraft.client.render.entity.state.PlayerEntityRenderState.class)

public abstract class AvatarRenderStateEmotionMixin
implements EmotionStateHolder {
    @Unique
    private Emotion nv_emotion;
    @Unique
    private float nv_emotionTime;
    @Unique
    private float nv_emotionWeight;

    @Override
    public Emotion nv_getEmotion() {
        return this.nv_emotion;
    }

    @Override
    public float nv_getEmotionTime() {
        return this.nv_emotionTime;
    }

    @Override
    public float nv_getEmotionWeight() {
        return this.nv_emotionWeight;
    }

    @Override
    public void nv_setEmotion(Emotion emotion, float time, float weight) {
        this.nv_emotion = emotion;
        this.nv_emotionTime = time;
        this.nv_emotionWeight = weight;
    }
}

