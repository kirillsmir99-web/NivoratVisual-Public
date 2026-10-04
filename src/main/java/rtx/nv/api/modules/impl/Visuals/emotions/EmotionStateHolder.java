package rtx.nv.api.modules.impl.Visuals.emotions;
import rtx.nv.api.modules.impl.Visuals.emotions.Emotion;

public interface EmotionStateHolder {
    public float nv_getEmotionTime();

    public void nv_setEmotion(Emotion var1, float var2, float var3);

    public Emotion nv_getEmotion();

    public float nv_getEmotionWeight();
}

