package rtx.nv.api.invmanager;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import rtx.nv.utils.inventory.InventoryTemplates;

public final class InventoryArranger {
    private static boolean active = false;

    private InventoryArranger() {}

    public static boolean isActive() {
        return active;
    }

    public static boolean needsArrange(ScreenHandler handler, InventoryTemplates.Template template) {
        return false;
    }

    public static void start(HandledScreen<?> screen) {
        active = true;
    }

    public static void tick(HandledScreen<?> screen, InventoryTemplates.Template template) {
    }
}
