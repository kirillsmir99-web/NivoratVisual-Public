package rtx.nv.api.mods.shulkerview;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import rtx.nv.api.mods.shulkerview.tooltip.ShulkerPreviewClientTooltipComponent;
import rtx.nv.api.mods.shulkerview.tooltip.ShulkerPreviewTooltipComponent;

public final class ShulkerViewMod {
    private static boolean initialized;

    private ShulkerViewMod() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        TooltipComponentCallback.EVENT.register(tooltipData -> {
            if (tooltipData instanceof ShulkerPreviewTooltipComponent) {
                ShulkerPreviewTooltipComponent shulkerPreviewTooltipComponent = (ShulkerPreviewTooltipComponent)tooltipData;
                return new ShulkerPreviewClientTooltipComponent(shulkerPreviewTooltipComponent);
            }
            return null;
        });
    }
}

