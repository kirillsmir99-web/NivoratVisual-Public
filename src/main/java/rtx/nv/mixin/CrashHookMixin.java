package rtx.nv.mixin;

import java.io.File;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.crash.CrashReporter;

@Mixin(MinecraftClient.class)
public class CrashHookMixin {
    @Inject(method="printCrashReport(Lnet/minecraft/util/crash/CrashReport;)V", at={@At(value="HEAD")}, require = 0)
    private void nv_onCrash(CrashReport report, CallbackInfo ci) {
        try {
            CrashReporter.report((String)report.getMessage(), (Throwable)report.getCause());
        }
        catch (Throwable throwable) {
        }
    }
}
