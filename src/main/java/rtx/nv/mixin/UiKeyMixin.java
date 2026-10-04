package rtx.nv.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.api.ui.UI;
import rtx.nv.utils.sounds.Sounds;

@Mixin(net.minecraft.client.Keyboard.class)
public abstract class UiKeyMixin {
    @Inject(method = "onKey", at = @At("HEAD"), require = 0, cancellable = true)
    private void nv_uiKey(long window, int action, KeyInput keyEvent, CallbackInfo ci) {
        if (action != 1) {
            return;
        }
        ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
        int toggleKey = (clickGui != null && clickGui.getBind().getCode() > 0) ? clickGui.getBind().getCode() : 344;
        if (keyEvent.key() != toggleKey && keyEvent.key() != 344) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }
        if (mc.currentScreen == null) {
            mc.setScreen((Screen)UI.INSTANCE);
            Sounds.play("gui_open");
            ci.cancel();
        }
    }
}
