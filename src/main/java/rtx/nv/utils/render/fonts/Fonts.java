package rtx.nv.utils.render.fonts;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.Render2D;

public enum Fonts {
    MONTSERRAT_REGULAR("montserrat-regular"),
    MONTSERRAT_MEDIUM("montserrat-medium"),
    MONTSERRAT_SEMIBOLD("montserrat-semibold"),
    MONTSERRAT_BOLD("montserrat-bold"),
    MONTSERRAT_EXTRABOLD("montserrat-extrabold"),
    MONTSERRAT_BLACK("montserrat-black"),
    MONTSERRAT_LIGHT("montserrat-light"),
    MONTSERRAT_EXTRALIGHT("montserrat-extralight"),
    MONTSERRAT_THIN("montserrat-thin"),
    SF("sf-regular"),
    SF_MEDIUM("sf-medium"),
    SF_BOLD("sf-bold"),
    NV("nv"),
    I2("i2"),
    EVENT_ICONS("event-icons"),
    INV_ICONS("inv-icons"),
    SMALL_PIXEL("small-pixel"),
    MAINMENU("mainmenu"),
    HEART("heart");

    private final String id;

    private Fonts(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public float width(String string, float f) {
        return Render2D.textWidth(this.id, string, f);
    }

    public void wave(String string, float f, float f2, float f3, int n, float f4) {
        Render2D.msdfWave(this.id, string, f, f2, f3, n, f4);
    }

    public void fade(String string, float f, float f2, float f3, int n, float f4, float f5, float f6, boolean bl, boolean bl2) {
        Render2D.textFade(this.id, string, f, f2, f3, n, f4, f5, f6, bl, bl2);
    }

    public void fade(String string, float f, float f2, float f3, int n, float f4, float f5, float f6, float f7, float f8) {
        Render2D.textFade(this.id, string, f, f2, f3, n, f4, f5, f6, f7, f8);
    }

    public void msdf(String string, float f, float f2, float f3, int n, float f4, float f5, float f6) {
        Render2D.msdfText(this.id, string, f, f2, f3, n, f4, f5, f6);
    }

    public void msdf(String string, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        Render2D.msdfText(this.id, string, f, f2, f3, n, n2, n3, n4);
    }

    public void msdf(String string, float f, float f2, float f3, int n) {
        Render2D.msdfText(this.id, string, f, f2, f3, n);
    }

    public void shimmer(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        Render2D.msdfShimmer(this.id, string, f, f2, f3, f4, f5, f6, f7);
    }

    public void shimmer(String string, float f, float f2, float f3, int n, float f4, float f5, float f6, float f7) {
        Render2D.msdfShimmer(this.id, string, f, f2, f3, n, f4, f5, f6, f7);
    }

    public float[] msdfBounds(String string, float f) {
        return Render2D.msdfBounds(this.id, string, f);
    }

    public float msdfWidth(String string, float f) {
        return Render2D.msdfWidth(this.id, string, f);
    }

    public void draw(DrawContext drawContext, String string, float f, float f2, float f3, int n) {
        Render2D.text(drawContext, this.id, string, f, f2, f3, n);
    }

    public void draw(String string, float f, float f2, float f3, int n) {
        Render2D.text(this.id, string, f, f2, f3, n);
    }

    public void draw(String string, float f, float f2, float f3, int n, int n2, int n3, int n4, float f4, float f5, float f6) {
        Render2D.text(this.id, string, f, f2, f3, n, n2, n3, n4, f4, f5, f6);
    }

    public void draw(String string, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        Render2D.text(this.id, string, f, f2, f3, n, n2, n3, n4);
    }

    public void draw(String string, float f, float f2, float f3, int n, float f4, float f5, float f6) {
        Render2D.text(this.id, string, f, f2, f3, n, f4, f5, f6);
    }

    public void msdfFade(String string, float f, float f2, float f3, int n, float f4, float f5, float f6, float f7, float f8) {
        Render2D.msdfTextFade(this.id, string, f, f2, f3, n, f4, f5, f6, f7, f8);
    }

    public boolean hasGlyph(int n) {
        return rtx.nv.utils.render.render2d.msdf.MsdfFonts.hasGlyph(this.id, n);
    }

    public boolean hasGlyph(String string) {
        return rtx.nv.utils.render.render2d.msdf.MsdfFonts.hasGlyph(this.id, string);
    }
}

