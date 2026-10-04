package rtx.nv.mixin.geckolib.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.renderer.internal.GeckolibItemSpecialRenderer;

@Mixin(SpecialModelTypes.class)
public class SpecialModelRenderersMixin {
    @Shadow
    @Final
    private static Codecs.IdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked>> ID_MAPPER;

    @Inject(method="<clinit>", at={@At(value="TAIL")}, require = 0)
    private static void geckolib_addSpecialRenderer(CallbackInfo ci) {
        if (ID_MAPPER != null) {
            ID_MAPPER.put(GeckoLibConstants.id("geckolib"), GeckolibItemSpecialRenderer.Unbaked.MAP_CODEC);
        }
    }
}
