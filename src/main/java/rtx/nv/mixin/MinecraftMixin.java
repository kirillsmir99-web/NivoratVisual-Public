package rtx.nv.mixin;

import java.io.File;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.session.Session;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.mods.accountswitcher.IasService;
import rtx.nv.api.ui.window.WindowTitleAnimation;
import rtx.nv.utils.network.Network;
import rtx.nv.utils.session.SessionChanger;

@Mixin(MinecraftClient.class)
public abstract class MinecraftMixin {
    private static final String NV_DEFAULTS_MARKER = ".nv-defaults-applied";
    @Shadow
    @Final
    public GameOptions options;
    @Shadow
    @Final
    public File runDirectory;
    @Shadow
    @Mutable
    private Session session;

    private void nv_setSession(Session newSession) {
        this.session = newSession;
    }

    @Inject(method="<init>", at={@At(value="TAIL")}, require = 0)
    private void nv_initIas(CallbackInfo ci) {
        SessionChanger.setSessionSetter(this::nv_setSession);
        try {
            if (!FabricLoader.getInstance().isModLoaded("ias")) {
                IasService.ensureInitialized();
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    @Inject(method="close", at={@At(value="HEAD")}, require = 0)
    private void nv_closeIas(CallbackInfo ci) {
        try {
            if (!FabricLoader.getInstance().isModLoaded("ias")) {
                IasService.close();
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    @Inject(method="isMultiplayerEnabled", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_enableMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method="tick", at={@At(value="HEAD")}, require = 0)
    private void nv_preTickEvent(CallbackInfo ci) {
        Network.tick();
        WindowTitleAnimation.get().tick();
        EventBus.get().post(new TickEvent(TickEvent.Phase.PRE));
    }

    @Inject(method="tick", at={@At(value="RETURN")}, require = 0)
    private void nv_postTickEvent(CallbackInfo ci) {
        EventBus.get().post(new TickEvent(TickEvent.Phase.POST));
    }
}
