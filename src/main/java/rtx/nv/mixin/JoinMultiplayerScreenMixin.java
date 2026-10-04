package rtx.nv.mixin;

import java.nio.file.Path;
import java.time.Duration;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.accountswitcher.ias.IAS;
import rtx.nv.api.mods.accountswitcher.ias.config.IASServerShortcutsConfig;
import rtx.nv.api.mods.accountswitcher.ias.screen.ServerShortcutButton;

@Mixin(MultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Shadow
    protected MultiplayerServerListWidget serverListWidget;
    @Shadow
    private ButtonWidget buttonEdit;
    @Shadow
    private ButtonWidget buttonDelete;

    protected JoinMultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void nv_addShortcutButtons(CallbackInfo ci) {
        try {
            rtx.nv.api.mods.accountswitcher.IasService.ensureInitialized();
            MultiplayerScreen screen = (MultiplayerScreen)(Object)this;
            int index = 0;
            Path configDir = IAS.configDirectory();
            if (configDir != null) {
                for (IASServerShortcutsConfig.ShortcutEntry shortcut : IASServerShortcutsConfig.load(configDir)) {
                    Identifier texture = Identifier.of("ias", "textures/gui/server_shortcuts/" + shortcut.icon() + ".png");
                    ServerShortcutButton button = new ServerShortcutButton(8 + index++ * 20, 8, () -> texture, shortcut.name(), pressed -> ConnectScreen.connect(screen, this.client, new ServerAddress(shortcut.address(), 25565), new ServerInfo(shortcut.name(), shortcut.address(), ServerInfo.ServerType.OTHER), false, null));
                    this.addDrawableChild(button);
                }
            }
        } catch (Throwable ignored) {
            // Prevent any shortcut button loading failure from crashing the MultiplayerScreen init
        }
    }
}
