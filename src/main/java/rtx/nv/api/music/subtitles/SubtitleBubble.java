package rtx.nv.api.music.subtitles;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import rtx.nv.api.music.lyrics.LyricLine;

public class SubtitleBubble extends rtx.nv.api.music.lyrics.SubtitleBubble {
    public SubtitleBubble(LyricLine line, float screenX, float screenY) {
        super(line, screenX, screenY, false);
    }

    public SubtitleBubble(LyricLine line, float screenX, float screenY, boolean isFastPaced) {
        super(line, screenX, screenY, isFastPaced);
    }

    public SubtitleBubble(LyricLine line, Vec3d worldPos, Quaternionf worldRot) {
        super(line, worldPos, worldRot, false, false);
    }

    public SubtitleBubble(LyricLine line, Vec3d worldPos, Quaternionf worldRot, boolean isSurfaceAttached) {
        super(line, worldPos, worldRot, isSurfaceAttached, false);
    }

    public SubtitleBubble(LyricLine line, Vec3d worldPos, Quaternionf worldRot, boolean isSurfaceAttached, boolean isFastPaced) {
        super(line, worldPos, worldRot, isSurfaceAttached, isFastPaced);
    }
}
