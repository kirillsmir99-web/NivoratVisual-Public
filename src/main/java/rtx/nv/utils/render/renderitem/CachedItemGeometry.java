package rtx.nv.utils.render.renderitem;

import java.util.List;

public record CachedItemGeometry(List<CachedItemQuad> quads, boolean animated, Object specialRenderer) {
    public CachedItemGeometry(List<CachedItemQuad> quads) {
        this(quads, false, null);
    }
    public CachedItemGeometry(List<CachedItemQuad> quads, Object specialRenderer) {
        this(quads, false, specialRenderer);
    }
}