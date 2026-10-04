package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.render.DrawEvent;
import rtx.nv.api.mods.shulkerview.ShulkerPreviewHelper;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.renderitem.BuiltRenderItem;
import rtx.nv.utils.render.renderitem.CustomItemRenderer;

public final class ShulkerPreview
extends Module {
    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final float SLOT = 18.0f;
    private static final float PAD = 8.0f;
    private static final float OVERLAY_W = 178.0f;
    private static final float OVERLAY_H = 70.0f;
    private boolean wasCtrlPressed = false;

    public ShulkerPreview() {
        super("Shulker Preview", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0441\u043e\u0434\u0435\u0440\u0436\u0438\u043c\u043e\u0435 \u0448\u0430\u043b\u043a\u0435\u0440\u0430. SHIFT \u2014 \u0437\u0430\u0433\u043b\u044f\u043d\u0443\u0442\u044c, CTRL \u2014 \u0437\u0430\u043a\u0440\u0435\u043f\u0438\u0442\u044c.", Category.UTILS);
    }

    public static boolean enabled() {
        ShulkerPreview shulkerPreview = ModuleManager.get().get(ShulkerPreview.class);
        return shulkerPreview != null && shulkerPreview.isEnabled();
    }

    @Override
    protected void onDisable() {
        ShulkerPreviewHelper.unfreeze();
        this.wasCtrlPressed = false;
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre() || this.mc.player == null) {
            return;
        }
        boolean bl = ShulkerPreviewHelper.ctrlKeyPressed();
        if (bl && !this.wasCtrlPressed) {
            ShulkerPreviewHelper.tryFreeze();
        } else if (!bl && this.wasCtrlPressed) {
            ShulkerPreviewHelper.unfreeze();
        }
        this.wasCtrlPressed = bl;
    }

    private float clampX(float f, float f2) {
        float f3 = this.mc.getWindow().getScaledWidth();
        return MathHelper.clamp((float)f, (float)4.0f, (float)(f3 - f2 - 4.0f));
    }

    private float clampY(float f, float f2) {
        float f3 = this.mc.getWindow().getScaledHeight();
        return MathHelper.clamp((float)f, (float)4.0f, (float)(f3 - f2 - 4.0f));
    }

    @EventHandler
    private void onDraw(DrawEvent drawEvent) {
        int n;
        if (!ShulkerPreviewHelper.isFrozen()) {
            return;
        }
        ItemStack itemStack = ShulkerPreviewHelper.getFrozenShulker();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        DrawContext drawContext = drawEvent.getGraphics();
        if (drawContext == null) {
            return;
        }
        List<ItemStack> list = ShulkerPreviewHelper.getItems(itemStack);
        float f = (float)this.mc.mouse.getX() * (float)this.mc.getWindow().getScaleFactor() / (float)this.mc.getWindow().getWidth() * (float)this.mc.getWindow().getScaledWidth();
        float f2 = (float)this.mc.mouse.getY() * (float)this.mc.getWindow().getScaleFactor() / (float)this.mc.getWindow().getHeight() * (float)this.mc.getWindow().getScaledHeight();
        float f3 = this.clampX(ShulkerPreviewHelper.getFrozenOverlayX(), 178.0f);
        float f4 = this.clampY(ShulkerPreviewHelper.getFrozenOverlayY(), 70.0f);
        int n2 = ShulkerPreviewHelper.shulkerColor(itemStack);
        int n3 = n2 >> 16 & 0xFF;
        int n4 = n2 >> 8 & 0xFF;
        int n5 = n2 & 0xFF;
        int n6 = ColorUtil.rgba(Math.max(8, n3 / 6), Math.max(8, n4 / 6), Math.max(8, n5 / 6), 230);
        int n7 = ColorUtil.rgba(Math.min(255, n3 + 60), Math.min(255, n4 + 60), Math.min(255, n5 + 60), 180);
        int n8 = ColorUtil.rgba(Math.min(255, n3 + 30), Math.min(255, n4 + 30), Math.min(255, n5 + 30), 100);
        Render2D.beginFrame(drawContext);
        Render2D.blur(f3, f4, 178.0f, 70.0f, 5.0f, 14.0f, 1.0f, n6);
        Render2D.rect(f3, f4, 178.0f, 70.0f, 5.0f, n6);
        Render2D.outline(f3, f4, 178.0f, 70.0f, 5.0f, 1.0f, n7);
        Render2D.outline(f3 + 1.0f, f4 + 1.0f, 176.0f, 68.0f, 4.0f, 0.5f, n8);
        ItemStack itemStack2 = null;
        for (int i = 0; i < list.size(); ++i) {
            boolean bl;
            float f5 = f3 + 8.0f + (float)(i % 9) * 18.0f;
            float f6 = f4 + 8.0f + (float)(i / 9) * 18.0f;
            boolean bl2 = bl = f >= f5 - 1.0f && f < f5 + 17.0f && f2 >= f6 - 1.0f && f2 < f6 + 17.0f;
            if (!bl) continue;
            Render2D.rect(f5 - 2.0f, f6 - 2.0f, 20.0f, 20.0f, 3.0f, ColorUtil.rgba(255, 255, 255, 50));
            Render2D.outline(f5 - 2.0f, f6 - 2.0f, 20.0f, 20.0f, 3.0f, 0.75f, ColorUtil.rgba(255, 255, 255, 120));
            if (list.get(i).isEmpty()) continue;
            itemStack2 = list.get(i);
        }
        Render2D.flush();
        CustomItemRenderer customItemRenderer = CustomItemRenderer.getInstance();
        customItemRenderer.beginFrame(drawContext);
        for (n = 0; n < list.size(); ++n) {
            ItemStack itemStack3 = list.get(n);
            if (itemStack3.isEmpty()) continue;
            float f7 = f3 + 8.0f + (float)(n % 9) * 18.0f;
            float f8 = f4 + 8.0f + (float)(n / 9) * 18.0f;
            customItemRenderer.enqueue(new BuiltRenderItem(itemStack3, f7, f8, 16.0f));
        }
        for (n = 0; n < list.size(); ++n) {
            ItemStack itemStack4 = list.get(n);
            if (itemStack4.isEmpty() || itemStack4.getCount() <= 1) continue;
            int n9 = Math.round(f3 + 8.0f + (float)(n % 9) * 18.0f);
            int n10 = Math.round(f4 + 8.0f + (float)(n / 9) * 18.0f);
            drawContext.drawStackOverlay(this.mc.textRenderer, itemStack4, n9, n10);
        }
        if (itemStack2 != null && ShulkerPreviewHelper.previewKeyPressed()) {
            List list2 = itemStack2.getTooltip(Item.TooltipContext.create((World)(Object)this.mc.world), (PlayerEntity)(Object)this.mc.player, (TooltipType)(this.mc.options.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC));
            Optional optional = itemStack2.getTooltipData();
            drawContext.drawTooltip(this.mc.textRenderer, list2, optional, (int)f, (int)f2);
        }
    }
}

