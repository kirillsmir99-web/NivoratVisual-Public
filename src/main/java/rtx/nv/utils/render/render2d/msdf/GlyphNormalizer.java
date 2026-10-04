package rtx.nv.utils.render.render2d.msdf;

public final class GlyphNormalizer {
    public static String normalize(int codePoint) {
        return Character.toString(codePoint);
    }

    public static boolean isSmallCap(int codePoint) {
        return false;
    }
}