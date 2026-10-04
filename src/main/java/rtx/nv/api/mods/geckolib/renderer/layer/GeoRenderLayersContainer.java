package rtx.nv.api.mods.geckolib.renderer.layer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;

public class GeoRenderLayersContainer<T extends GeoAnimatable, O, R extends GeoRenderState> {
    private final GeoRenderer<T, O, R> renderer;
    private final List<GeoRenderLayer<T, O, R>> layers = new ObjectArrayList<>();
    private boolean compiledLayers = false;

    public GeoRenderLayersContainer(GeoRenderer<T, O, R> geoRenderer) {
        this.renderer = geoRenderer;
    }

    public void fireCompileRenderLayersEvent() {
        this.compiledLayers = true;
        this.renderer.fireCompileRenderLayersEvent();
    }

    public List<GeoRenderLayer<T, O, R>> getRenderLayers() {
        if (!this.compiledLayers) {
            this.fireCompileRenderLayersEvent();
        }
        return this.layers;
    }

    public void addLayer(GeoRenderLayer<T, O, R> geoRenderLayer) {
        this.layers.add(geoRenderLayer);
    }
}
