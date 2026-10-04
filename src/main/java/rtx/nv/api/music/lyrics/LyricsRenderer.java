package rtx.nv.api.music.lyrics;

import rtx.nv.api.music.NowPlayingClient;
import rtx.nv.api.music.subtitles.MusicSubtitlesRenderer;

public class LyricsRenderer extends MusicSubtitlesRenderer {
    public static LyricsRenderer get() {
        return new LyricsRenderer(LyricsManager.get(), rtx.nv.api.music.MusicManager.get().getClient());
    }

    public static void init() {
        MusicSubtitlesRenderer.init();
    }

    public LyricsRenderer(LyricsManager var1, NowPlayingClient var2) {
        super(var1, var2);
    }
}
