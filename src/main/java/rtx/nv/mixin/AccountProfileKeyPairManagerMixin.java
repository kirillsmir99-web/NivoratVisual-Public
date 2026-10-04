package rtx.nv.mixin;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import net.minecraft.client.session.ProfileKeysImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProfileKeysImpl.class)
public abstract class AccountProfileKeyPairManagerMixin {
    @Redirect(method="fetchKeyPair", at=@At(value="INVOKE", target="Lcom/mojang/authlib/minecraft/UserApiService;getKeyPair()Lcom/mojang/authlib/yggdrasil/response/KeyPairResponse;"), require = 0)
    private KeyPairResponse nv_skipUnauthorizedProfileKeyFetch(UserApiService userApiService) {
        try {
            return userApiService.getKeyPair();
        }
        catch (MinecraftClientHttpException exception) {
            if (exception.getStatus() == 401) {
                return null;
            }
            throw exception;
        }
    }
}

