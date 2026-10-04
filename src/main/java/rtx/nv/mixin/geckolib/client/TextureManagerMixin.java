package rtx.nv.mixin.geckolib.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.renderer.texture.GeckoLibAnimatedTexture;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
    @Shadow
    protected abstract TextureContents loadTexture(Identifier id, ReloadableTexture texture);

    @Shadow
    public abstract void registerTexture(Identifier id, AbstractTexture texture);

    @WrapOperation(method = "getTexture(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/texture/AbstractTexture;", at = @At(value = "NEW", target = "(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/texture/ResourceTexture;"), require = 0)
    private ResourceTexture geckolib_replaceAnimatableTexture(Identifier location, Operation<ResourceTexture> original) {
        GeckoLibAnimatedTexture animatableTexture = new GeckoLibAnimatedTexture(location);
        TextureContents contents = this.loadTexture(location, (ReloadableTexture) animatableTexture);
        if (animatableTexture.isAnimated()) {
            animatableTexture.reload(contents);
            this.registerTexture(location, (AbstractTexture) animatableTexture);
            return animatableTexture;
        }
        animatableTexture.close();
        return (ResourceTexture) original.call(location);
    }

    @WrapWithCondition(method = "getTexture(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/texture/AbstractTexture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;registerTexture(Lnet/minecraft/util/Identifier;Lnet/minecraft/client/texture/ReloadableTexture;)V"), require = 0)
    private boolean geckolib_skipAnimatableTextureRegistration(TextureManager textureManager, Identifier id, ReloadableTexture texture) {
        return !(texture instanceof GeckoLibAnimatedTexture);
    }
}
