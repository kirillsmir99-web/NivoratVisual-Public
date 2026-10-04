package rtx.nv.mixin.accessor;

import java.util.List;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker("addFeature")
    boolean nv_callAddFeature(FeatureRenderer<?, ?> feature);

    @Accessor("features")
    List<FeatureRenderer<?, ?>> nv_getFeatures();
}
