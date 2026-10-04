package rtx.nv.api.drags.components;

import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;

public final class MusicSubtitlesComp extends Draggable {
    private static MusicSubtitlesComp instance;
    public MusicSubtitlesComp() {
        super("music-subtitles", Position.screenWidth() * .5f - 110, Position.screenHeight() * .82f - 12);
        instance = this;
    }
    public static MusicSubtitlesComp get() { return instance; }
    @Override public float width() { return 220; }
    @Override public float height() { return 24; }
    @Override public String displayName() { return "Титры музыки 2D"; }
    @Override public boolean isVisible() {
        MusicHudModule mod = MusicHudModule.getInstance();
        return mod != null && mod.isEnabled() && mod.skyWords.getValue() && mod.wordsMode.is("На экране · 2D");
    }
    @Override public boolean isInteractive() { return isVisible(); }
    public float centerX() { return getX() + width() * .5f; }
    public float centerY() { return getY() + height() * .5f; }
    @Override protected void render(DrawContext context) {
        if (!isVisible() || !DragSystem.get().isDragModeActive()) return;
        Render2D.beginFrame(context);
        Render2D.outline(getX(), getY(), width(), height(), 4, .7f, 0x80FFFFFF);
        String text = "Титры музыки 2D · перетащите";
        Fonts.MONTSERRAT_MEDIUM.draw(text, centerX() - Fonts.MONTSERRAT_MEDIUM.width(text, 6) * .5f, centerY() - 3, 6, 0xBFFFFFFF);
        Render2D.flush();
    }
}
