package rtx.nv.api.music.lyrics;

import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.music.MusicHelperManager;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;

public class MusicSubtitlesConfig {
    public static final MusicSubtitlesConfig INSTANCE = new MusicSubtitlesConfig();

    public boolean subtitlesEnabled = true;
    public String subtitlesMode = "WORLD_BLOCK";
    public float subtitlesScale = 1.0F;
    public float subtitlesDistance = 4.5F;
    public int subtitlesColor = -1;
    public boolean subtitlesBgEnabled = true;
    public boolean subtitlesSeeThrough = false;
    public int subtitlesBgColor = 0x0E121C;
    public float subtitlesBgAlpha = 0.75F;
    public int subtitlesBgRadius = 4;
    public boolean subtitlesTextShadow = true;
    public int subtitlesOffsetMs = 0;
    public boolean subtitlesVisibleInF1 = true;
    public String subtitlesAnimation = "KARAOKE_BOUNCE";
    public int subtitlesActiveColor = -10496;
    public boolean subtitlesGlow = true;

    public double sub2DPositionX = 0.5D;
    public double sub2DPositionY = 0.82D;

    public boolean sprintWordsEnabled = true;
    public int sprintWordsChunkSize = 2;
    public float sprintWordsDistance = 4.5F;
    public float sprintWordsHeight = 0.3F;
    public float sprintWordsSpread = 0.4F;
    public int sprintWordsTrailLength = 3;

    public int helperPort = 38472;
    public String yandexMusicToken = "";

    public void sync() {
        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null) {
            return;
        }
        this.subtitlesEnabled = mod.isEnabled() && mod.skyWords.getValue();
        this.subtitlesMode = switch (mod.wordsMode.getSelected()) {
            case "На экране · 2D" -> "HUD_BOTTOM";
            case "На блоке · 3D" -> "WORLD_BLOCK";
            default -> "WORLD_3D";
        };
        this.subtitlesScale = mod.wordsScale.getFloat();
        this.subtitlesBgEnabled = mod.wordsBackground.getValue();
        this.subtitlesAnimation = switch (mod.wordsAnimation.getValue()) {
            case "Караоке" -> "KARAOKE_BOUNCE"; case "Без движения" -> "CLASSIC"; default -> "NV_FLOW";
        };
        this.subtitlesOffsetMs = Math.round(mod.wordsOffset.getFloat());
        this.sprintWordsEnabled = false;
        this.sprintWordsSpread = mod.wordsSpread.getFloat();
        this.subtitlesDistance = mod.wordsDistance.getFloat();
        this.subtitlesActiveColor = ThemeManager.accent(255.0F);

        int hp = MusicHelperManager.get().getPort();
        if (hp > 0) {
            this.helperPort = hp;
        }
    }

    public int resolveActiveColor(int themeAccent) {
        return ThemeManager.accent(255.0F);
    }

    public int getTrackSyncOffset(String artist, String title) {
        return 0;
    }
}
