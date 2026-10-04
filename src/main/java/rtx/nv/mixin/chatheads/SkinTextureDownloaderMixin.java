package rtx.nv.mixin.chatheads;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(net.minecraft.client.texture.PlayerSkinTextureDownloader.class)

public abstract class SkinTextureDownloaderMixin {
    @ModifyArg(method="registerTexture", at=@At(value="INVOKE", target="Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"), require = 0)
    private static Supplier<?> chatheads_registerBlendedHeadTexture(Supplier<?> supplier, @Local(argsOnly=true) AssetInfo.TextureAsset texture, @Local(argsOnly=true) NativeImage image) {
        return () -> {
            Identifier textureLocation = texture.texturePath();
            if (textureLocation.getPath().startsWith("skins/")) {
                MinecraftClient.getInstance().getTextureManager().registerTexture(ChatHeads.getBlendedHeadLocation(textureLocation), (AbstractTexture)new NativeImageBackedTexture(() -> "Chat Head of " + textureLocation.getPath(), ChatHeads.extractBlendedHead(image)));
                ChatHeads.blendedHeadTextures.add(textureLocation);
            }
            return supplier.get();
        };
    }
}

