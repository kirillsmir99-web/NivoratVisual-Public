package rtx.nv.utils.render.render2d.font;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

final class FontQuality {
    static final int ATLAS_SIZE = 2048;
    static final int ATLAS_GAP = 2;
    static final int MAX_LAYOUT_CACHE = 2048;

    private FontQuality() {
    }

    static float snapOrigin(float f) {
        float f2 = Render2DCoordinateSpace.designGuiScale();
        return (float)Math.round(f * f2) / f2;
    }

    static int glyphPadding(int n) {
        return Math.max(4, n + 2);
    }

    static float coverageWeight(float f) {
        if (f <= 8.0f) {
            return 0.32f;
        }
        if (f <= 10.0f) {
            return 0.22f;
        }
        if (f <= 12.0f) {
            return 0.14f;
        }
        if (f <= 14.0f) {
            return 0.08f;
        }
        return 0.0f;
    }

    static int oversampleFor(float f) {
        if (f <= 10.0f) {
            return 6;
        }
        if (f <= 14.0f) {
            return 5;
        }
        if (f <= 28.0f) {
            return 4;
        }
        if (f <= 64.0f) {
            return 3;
        }
        return 2;
    }
}

