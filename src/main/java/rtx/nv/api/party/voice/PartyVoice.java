package rtx.nv.api.party.voice;

public class PartyVoice {
    public static final PartyVoice INSTANCE = new PartyVoice();

    public static enum ActivationMode {
        VOICE,
        PTT
    }

    public void start() {}
    public boolean isAvailable() { return false; }
    public void stop() {}
    public void setPttHeld(boolean held) {}
    public String lastError() { return "Голосовой движок не реализован"; }
    public void setActivation(ActivationMode mode) {}
    public void setActivationMode(ActivationMode mode) {}
    public void setThreshold(float th) {}
    public void setVadSensitivity(float th) {}
    public void setVolume(float vol) {}
    public void setOutputGain(float vol) {}
    public void setAgc(boolean agc) {}
    public void setGain(float gain) {}
    public void setInputGain(float gain) {}
    public void setDenoise(boolean denoise) {}
    public void setMute(boolean mute) {}
    public void setMuted(boolean mute) {}
}
