package rtx.nv.api.modules.impl.Visuals.emotions;

public record EmotionRemoteState(String id, String name, String world, Emotion emotion, long startTime, float speed, boolean looping) {
    public EmotionRemoteState(String emotionId, long startTime, float speed, boolean looping) {
        this(emotionId, emotionId, "", Emotion.WAVE, startTime, speed, looping);
    }

    public String minecraftUsername() {
        return name;
    }

    public long startedAt() {
        return startTime;
    }
}
