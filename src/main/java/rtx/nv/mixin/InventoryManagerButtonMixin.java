package rtx.nv.mixin;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.invmanager.InventoryArranger;
import rtx.nv.api.invmanager.InventoryManagerScreen;
import rtx.nv.utils.inventory.InventoryTemplates;

@Mixin(InventoryScreen.class)
public abstract class InventoryManagerButtonMixin extends HandledScreen<PlayerScreenHandler> {
    @Unique
    private ButtonWidget nv_setsButton;
    @Unique
    private ButtonWidget nv_arrangeButton;

    protected InventoryManagerButtonMixin(PlayerScreenHandler menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = {@At("TAIL")}, require = 0)
    private void nv_addManagerButtons(CallbackInfo ci) {
        if (this.nv_setsButton != null) {
            this.remove(this.nv_setsButton);
        }
        if (this.nv_arrangeButton != null) {
            this.remove(this.nv_arrangeButton);
        }
        int bw = 90;
        int bx = this.x - bw - 4;
        this.nv_setsButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Сеты"), btn -> MinecraftClient.getInstance().setScreen(new InventoryManagerScreen()))
                .dimensions(bx, this.y, bw, 20)
                .build()
        );
        this.nv_arrangeButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal(InventoryArranger.isActive() ? "Раскладываю…" : "Разложить"), btn -> InventoryManagerButtonMixin.triggerArrange())
                .dimensions(bx, this.y + 23, bw, 20)
                .build()
        );
    }

    @Inject(method = "render", at = {@At("TAIL")}, require = 0)
    private void nv_tickArranger(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (InventoryArranger.isActive()) {
            InventoryArranger.tick((HandledScreen<?>)(Object)this, InventoryTemplates.active());
        }
    }

    private static boolean canArrange() {
        InventoryTemplates.Template template = InventoryTemplates.active();
        if (template == null) {
            return false;
        }
        if (InventoryArranger.isActive()) {
            return true;
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof HandledScreen<?> acs) {
            return InventoryArranger.needsArrange(acs.getScreenHandler(), template);
        }
        return false;
    }

    private static void triggerArrange() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof HandledScreen<?> acs) {
            InventoryArranger.start(acs);
        }
    }
}
