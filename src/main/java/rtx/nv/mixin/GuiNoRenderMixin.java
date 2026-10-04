package rtx.nv.mixin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Visuals.Crosshair;
import rtx.nv.api.modules.impl.Visuals.NoRender;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)

public abstract class GuiNoRenderMixin {
    @Inject(method="renderScoreboardSidebar", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_hideScoreboard(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (NoRender.isActive("\u0422\u0430\u0431\u043b\u0438\u0446\u0430 \u0441\u0447\u0451\u0442\u0430")) {
            ci.cancel();
        }
    }

    @Inject(method="renderBossBarHud", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_hideBossBar(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (NoRender.isActive("\u041f\u043e\u043b\u043e\u0441\u0430 \u0431\u043e\u0441\u0441\u0430")) {
            ci.cancel();
        }
    }

    @Inject(method="renderCrosshair", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_hideVanillaCrosshair(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        Crosshair crosshair = ModuleManager.get().get(Crosshair.class);
        if (crosshair != null && crosshair.isEnabled()) {
            ci.cancel();
        }
    }
}

