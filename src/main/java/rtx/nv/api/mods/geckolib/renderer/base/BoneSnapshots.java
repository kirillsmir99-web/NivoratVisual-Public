package rtx.nv.api.mods.geckolib.renderer.base;
import java.util.Optional;
import java.util.function.Consumer;
import rtx.nv.api.mods.geckolib.animation.state.BoneSnapshot;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;

public interface BoneSnapshots {
    public Optional<BoneSnapshot> get(String var1);

    default public BoneSnapshot get(GeoBone geoBone) {
        return geoBone.frameSnapshot != null ? geoBone.frameSnapshot : this.get(geoBone.name()).get();
    }

    default public void ifPresent(String string, Consumer<BoneSnapshot> consumer) {
        this.get(string).ifPresent(consumer);
    }
}

