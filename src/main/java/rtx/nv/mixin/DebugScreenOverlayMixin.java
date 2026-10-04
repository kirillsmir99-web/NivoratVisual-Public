package rtx.nv.mixin;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Utils.StreamerMode;

@Mixin(net.minecraft.client.gui.hud.DebugHud.class)

public abstract class DebugScreenOverlayMixin {
    @Inject(method="drawText", at={@At(value="HEAD")}, require = 0)
    private void nv_maskCoords(DrawContext graphics, List<String> lines, boolean alignLeft, CallbackInfo ci) {
        if (!StreamerMode.hideCoords()) {
            return;
        }
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            String masked = DebugScreenOverlayMixin.nv_mask(line);
            if (masked == line) continue;
            lines.set(i, masked);
        }
    }

    private static String nv_mask(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        if (line.startsWith("XYZ:")) {
            return "XYZ: # / # / #";
        }
        if (line.startsWith("Block:")) {
            return "Block: # # #";
        }
        if (line.startsWith("Chunk:")) {
            return "Chunk: # # #";
        }
        int targeted = line.indexOf("Targeted Block:");
        if (targeted >= 0) {
            return line.substring(0, targeted) + "Targeted Block: #, #, #";
        }
        return line;
    }
}

