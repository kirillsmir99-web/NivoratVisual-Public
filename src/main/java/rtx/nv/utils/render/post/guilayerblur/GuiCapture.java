package rtx.nv.utils.render.post.guilayerblur;

public final class GuiCapture {
    private static GuiCapture.Source source;

    private GuiCapture() {
    }

    public static float scale() {
        return source != null ? source.captureScale() : 1.0f;
    }

    public static boolean isBound(GuiCapture.Source source) {
        return source != null && source == source;
    }

    public static void bind(GuiCapture.Source source) {
        source = source;
    }

    public static boolean active() {
        return source != null && source.captureActive();
    }

    public static float blurRadius() {
        return source != null ? source.captureBlurRadius() : 0.0f;
    }

    public static float shatterProgress() {
        return source != null ? source.shatterProgress() : 0.0f;
    }

    public static boolean emitPanelBoundary() {
        return source == null || source.emitPanelBoundary();
    }


    public static interface Source {
        public float shatterProgress();
    
        public float captureScale();
    
        public boolean captureActive();
    
        public float captureBlurRadius();
    
        default public boolean emitPanelBoundary() {
            return true;
        }
    }
}

