package rtx.nv.mixin.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.utils.render.cape.CapeGradient;

@Mixin(AbstractClientPlayerEntity.class)
public class CustomCapeMixin {
    @Unique
    private static final Identifier NV_CAPE_ASSET_ID = Identifier.of("nv", "capes/cape");
    @Unique
    private static final Identifier NV_CAPE_TEXTURE = Identifier.of("nv", "textures/capes/cape.png");
    @Unique
    private static final Identifier VANILLA_ELYTRA_ASSET_ID = Identifier.of("minecraft", "entity/equipment/wings/elytra");
    @Unique
    private static final Identifier VANILLA_ELYTRA_TEXTURE = Identifier.of("minecraft", "textures/entity/equipment/wings/elytra.png");
    @Unique
    private static final AssetInfo.TextureAsset NV_CAPE_ASSET = new AssetInfo.TextureAssetInfo(NV_CAPE_ASSET_ID, NV_CAPE_TEXTURE);
    @Unique
    private static final AssetInfo.TextureAsset VANILLA_ELYTRA_ASSET = new AssetInfo.TextureAssetInfo(VANILLA_ELYTRA_ASSET_ID, VANILLA_ELYTRA_TEXTURE);

    @Inject(method="getSkin", at={@At(value="RETURN")}, cancellable=true, require = 1)
    private void nv_replaceCape(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)(Object)this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        rtx.nv.api.modules.impl.Visuals.Cape cape = rtx.nv.api.modules.impl.Visuals.Cape.getInstance();
        if (cape == null || !cape.shouldRender(player)) {
            return;
        }
        CapeGradient.tick();
        SkinTextures skin = (SkinTextures)cir.getReturnValue();
        if (skin != null) {
            cir.setReturnValue(new SkinTextures(skin.body(), CapeGradient.asset(), skin.elytra() == null ? VANILLA_ELYTRA_ASSET : skin.elytra(), skin.model(), skin.secure()));
        }
    }
}
