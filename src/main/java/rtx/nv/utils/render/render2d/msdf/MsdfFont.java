package rtx.nv.utils.render.render2d.msdf;
import java.util.Map;
import net.minecraft.util.Identifier;
import rtx.nv.utils.render.render2d.msdf.MsdfGlyph;

public final class MsdfFont {
    private static final MsdfGlyph MISSING = MsdfGlyph.nonDrawable(0.0f);
    private final Identifier atlasTexture;
    private final int atlasWidth;
    private final int atlasHeight;
    private final float lineHeight;
    private final float ascender;
    private final float descender;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Long, Float> kerning;
    private final MsdfGlyph fallback;

    MsdfFont(Identifier identifier, int n, int n2, float f, float f2, float f3, Map<Integer, MsdfGlyph> map, Map<Long, Float> map2) {
        this.atlasTexture = identifier;
        this.atlasWidth = n;
        this.atlasHeight = n2;
        this.lineHeight = f;
        this.ascender = f2;
        this.descender = f3;
        this.glyphs = map;
        this.kerning = map2;
        MsdfGlyph msdfGlyph = map.get(63);
        this.fallback = msdfGlyph != null ? msdfGlyph : MISSING;
    }

    public float width(String string, float f) {
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        float f2 = 0.0f;
        float f3 = 0.0f;
        int n = -1;
        int n2 = 0;
        while (n2 < string.length()) {
            int n3 = string.codePointAt(n2);
            n2 += Character.charCount(n3);
            if (n3 == 10) {
                f2 = Math.max(f2, f3);
                f3 = 0.0f;
                n = -1;
                continue;
            }
            MsdfGlyph msdfGlyph = this.glyph(n3);
            if (n != -1) {
                f3 += this.kerning(n, n3) * f;
            }
            f3 += msdfGlyph.advance() * f;
            n = n3;
        }
        return Math.max(f2, f3);
    }

    public float[] glyphBounds(String string, float f) {
        float[] fArray;
        if (string == null || string.isEmpty()) {
            return new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        }
        float f2 = this.ascender * f;
        float f3 = Float.MAX_VALUE;
        float f4 = -3.4028235E38f;
        float f5 = Float.MAX_VALUE;
        float f6 = -3.4028235E38f;
        boolean bl = false;
        float f7 = 0.0f;
        int n = -1;
        int n2 = 0;
        while (n2 < string.length()) {
            int n3 = string.codePointAt(n2);
            n2 += Character.charCount(n3);
            if (n3 == 10) {
                n = -1;
                continue;
            }
            MsdfGlyph msdfGlyph = this.glyph(n3);
            if (n != -1) {
                f7 += this.kerning(n, n3) * f;
            }
            if (msdfGlyph.drawable()) {
                f3 = Math.min(f3, f7 + msdfGlyph.planeLeft() * f);
                f4 = Math.max(f4, f7 + msdfGlyph.planeRight() * f);
                f5 = Math.min(f5, f2 - msdfGlyph.planeTop() * f);
                f6 = Math.max(f6, f2 - msdfGlyph.planeBottom() * f);
                bl = true;
            }
            f7 += msdfGlyph.advance() * f;
            n = n3;
        }
        if (bl) {
            float[] fArray2 = new float[4];
            fArray2[0] = f3;
            fArray2[1] = f5;
            fArray2[2] = f4;
            fArray = fArray2;
            fArray2[3] = f6;
        } else {
            float[] fArray3 = new float[4];
            fArray3[0] = 0.0f;
            fArray3[1] = 0.0f;
            fArray3[2] = 0.0f;
            fArray = fArray3;
            fArray3[3] = 0.0f;
        }
        return fArray;
    }

    public int atlasHeight() {
        return this.atlasHeight;
    }

    public boolean hasGlyph(int n) {
        return this.glyphs.containsKey(n);
    }

    MsdfGlyph glyph(int n) {
        MsdfGlyph msdfGlyph = this.glyphs.get(n);
        if (msdfGlyph != null) {
            return msdfGlyph;
        }
        if (n <= 32 || Character.isWhitespace(n) || !Character.isDefined(n)) {
            return MISSING;
        }
        return this.fallback;
    }

    public float ascent(float f) {
        return this.ascender * f;
    }

    public int atlasWidth() {
        return this.atlasWidth;
    }

    public Identifier atlasTexture() {
        return this.atlasTexture;
    }

    public float lineHeight(float f) {
        return this.lineHeight * f;
    }

    float kerning(int n, int n2) {
        if (this.kerning.isEmpty()) {
            return 0.0f;
        }
        Float f = this.kerning.get((long)n << 32 | (long)n2 & 0xFFFFFFFFL);
        return f == null ? 0.0f : f.floatValue();
    }

    public float descent(float f) {
        return this.descender * f;
    }
}

