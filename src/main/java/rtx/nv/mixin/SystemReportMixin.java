package rtx.nv.mixin;
import net.minecraft.util.SystemDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.util.SystemDetails.class)

public abstract class SystemReportMixin {
    @Inject(method="tryAddGroup", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_skipOshiHardwareReport(String group, Runnable runnable, CallbackInfo ci) {
        if ("hardware".equals(group)) {
            ci.cancel();
        }
    }
}

