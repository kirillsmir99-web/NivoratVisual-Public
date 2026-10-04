package rtx.nv.utils.render.renderitem;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import rtx.nv.NV;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.renderitem.BuiltRenderItem;
import rtx.nv.utils.render.renderitem.CustomItemRenderer;
import rtx.nv.utils.render.renderitem.RenderItemOptions;

public final class RenderItem {
    private static final String DECORATION_FONT = "semi_bold";
    private static boolean reloadListenerRegistered;

    private RenderItem() {
    }

    public static void flush() {
        RenderItem.renderer().flush();
    }

    public static void close() {
        CustomItemRenderer.closeInstance();
        NV.LOGGER.info("[RenderItem] RenderItem closed");
    }

    public static void init() {
        RenderItem.registerReloadListener();
        NV.LOGGER.info("[RenderItem] RenderItem initialized");
    }

    public static void item(ItemStack itemStack, float f, float f2, float f3, float f4) {
        RenderItem.item(itemStack, f, f2, f3, RenderItemOptions.defaults().alpha(f4));
    }

    public static void item(ItemStack itemStack, float f, float f2, float f3) {
        RenderItem.item(itemStack, f, f2, f3, RenderItemOptions.defaults());
    }

    public static void item(ItemStack itemStack, float f, float f2, float f3, RenderItemOptions renderItemOptions) {
        BuiltRenderItem builtRenderItem = new BuiltRenderItem(itemStack, f, f2, f3, renderItemOptions, 0);
        RenderItem.renderer().enqueue(builtRenderItem);
        RenderItem.drawDecorations(builtRenderItem);
    }

    public static void item(String string, float f, float f2, float f3, RenderItemOptions renderItemOptions) {
        RenderItem.item(RenderItem.stackOf(string), f, f2, f3, renderItemOptions);
    }

    public static void item(String string, float f, float f2, float f3) {
        RenderItem.item(RenderItem.stackOf(string), f, f2, f3, RenderItemOptions.defaults());
    }

    private static void registerReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        Identifier identifier = Identifier.of((String)"nv", (String)"render_item_cache");
        ResourceLoader.get((ResourceType)ResourceType.CLIENT_RESOURCES).registerReloader(identifier, (ResourceReloader)((SynchronousResourceReloader)resourceManager -> RenderItem.clearCaches()));
        reloadListenerRegistered = true;
    }

    private static CustomItemRenderer renderer() {
        return CustomItemRenderer.getInstance();
    }

    public static void beginGuiFrame() {
        RenderItem.renderer().beginGuiFrame();
    }

    public static void prepareBuffers() {
        RenderItem.renderer().prepareBuffers();
    }

    public static boolean isItemPipeline(RenderPipeline renderPipeline) {
        return RenderItem.renderer().isItemPipeline(renderPipeline);
    }

    public static void beginFrame(DrawContext drawContext) {
        RenderItem.renderer().beginFrame(drawContext);
    }

    public static void bindParams(RenderPass renderPass) {
        RenderItem.renderer().bindParams(renderPass);
    }

    public static void clearCaches() {
        RenderItem.renderer().clearCaches();
    }

    private static ItemStack stackOf(String string) {
        Identifier identifier;
        if (string == null || string.isBlank()) {
            return ItemStack.EMPTY;
        }
        Identifier identifier2 = identifier = string.indexOf(58) >= 0 ? Identifier.tryParse((String)string.trim()) : Identifier.ofVanilla((String)string.trim());
        if (identifier == null) {
            return ItemStack.EMPTY;
        }
        Item item = Registries.ITEM.getOptionalValue(identifier).orElse(null);
        return item == null ? ItemStack.EMPTY : item.getDefaultStack();
    }

    private static void drawCount(int n, float f, float f2, float f3, float f4) {
        String string = Integer.toString(n);
        float f5 = Math.max(6.0f, f3 * 0.34f);
        float f6 = Render2D.textWidth(DECORATION_FONT, string, f5);
        float f7 = f + f3 - f6 + Math.max(1.0f, f3 * 0.04f);
        float f8 = f2 + f3 - f5 - Math.max(0.0f, f3 * 0.01f);
        int n2 = Math.round(170.0f * f4);
        int n3 = Math.round(255.0f * f4);
        Render2D.text(DECORATION_FONT, string, f7 + 1.0f, f8 + 1.0f, f5, ColorUtil.rgba(0, 0, 0, n2));
        Render2D.text(DECORATION_FONT, string, f7, f8, f5, ColorUtil.rgba(255, 255, 255, n3));
    }

    private static void drawDurabilityBar(ItemStack itemStack, float f, float f2, float f3, float f4) {
        float f5 = Math.max(8.0f, f3 * 0.78f);
        float f6 = Math.max(1.5f, f3 * 0.07f);
        float f7 = f + (f3 - f5) * 0.5f;
        float f8 = f2 + f3 - f6 - Math.max(1.0f, f3 * 0.08f);
        float f9 = Math.max(0.0f, Math.min(1.0f, (float)itemStack.getItemBarStep() / 13.0f));
        int n = itemStack.getItemBarColor();
        Render2D.rect(f7, f8, f5, f6, f6 * 0.5f, ColorUtil.rgba(0, 0, 0, Math.round(150.0f * f4)));
        Render2D.rect(f7, f8, Math.max(1.0f, f5 * f9), f6, f6 * 0.5f, ColorUtil.rgba(n >>> 16 & 0xFF, n >>> 8 & 0xFF, n & 0xFF, Math.round(240.0f * f4)));
    }

    private static void drawDecorations(BuiltRenderItem builtRenderItem) {
        if (builtRenderItem == null || !builtRenderItem.visible()) {
            return;
        }
        ItemStack itemStack = builtRenderItem.stack();
        RenderItemOptions renderItemOptions = builtRenderItem.options();
        if (renderItemOptions.showDurability() && itemStack.isItemBarVisible()) {
            RenderItem.drawDurabilityBar(itemStack, builtRenderItem.x(), builtRenderItem.y(), builtRenderItem.size(), renderItemOptions.alpha());
        }
        if (renderItemOptions.showCount() && itemStack.getCount() > 1) {
            RenderItem.drawCount(itemStack.getCount(), builtRenderItem.x(), builtRenderItem.y(), builtRenderItem.size(), renderItemOptions.alpha());
        }
    }
}

