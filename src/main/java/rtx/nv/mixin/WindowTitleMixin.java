package rtx.nv.mixin;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.ui.window.MainWindow;

@Mixin(Window.class)

public abstract class WindowTitleMixin {
    @Shadow
    @Final
    private long handle;

    @ModifyVariable(method="<init>", at=@At(value="HEAD"), argsOnly=true, ordinal=0, require = 0)
    private static String nv_forceInitialTitle(String original) {
        return MainWindow.getTitle();
    }

    @ModifyVariable(method="setTitle", at=@At(value="HEAD"), argsOnly=true, require = 0)
    private String nv_forceTitle(String original) {
        return MainWindow.getTitle();
    }

    @Inject(method="<init>", at={@At(value="TAIL")}, require = 0)
    private void nv_applyDarkTitleBar(WindowEventHandler eventHandler, MonitorTracker screenManager, WindowSettings displayData, String preferredFullscreenVideoMode, String title, CallbackInfo ci) {
        MainWindow.applyDarkMode(this.handle);
    }
}

