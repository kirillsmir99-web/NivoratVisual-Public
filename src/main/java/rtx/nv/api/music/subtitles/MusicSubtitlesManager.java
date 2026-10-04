package rtx.nv.api.music.subtitles;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import rtx.nv.api.music.MusicManager;
import rtx.nv.api.music.TrackState;
import rtx.nv.api.music.lyrics.LyricLine;
import rtx.nv.api.music.lyrics.LyricsManager;
import rtx.nv.api.music.lyrics.MusicSubtitlesConfig;
import rtx.nv.api.music.lyrics.SubtitleBubble;

public final class MusicSubtitlesManager {
    private static final MusicSubtitlesManager INSTANCE = new MusicSubtitlesManager();

    public static MusicSubtitlesManager get() {
        return INSTANCE;
    }

    private MusicSubtitlesManager() {
    }

    public void init() {
        LyricsManager.get().init();
    }

    public List<SubtitleBubble> getActiveBubbles() {
        return LyricsManager.get().getActiveBubbles();
    }

    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        MusicSubtitlesConfig.INSTANCE.sync();
        TrackState state = MusicManager.get().getClient().getState();
        int sw = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 800;
        int sh = mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 600;
        LyricsManager.get().update(state, sw, sh);
    }

    public LyricLine getCurrentLine(TrackState state) {
        return LyricsManager.get().getCurrentLine(state);
    }
}
