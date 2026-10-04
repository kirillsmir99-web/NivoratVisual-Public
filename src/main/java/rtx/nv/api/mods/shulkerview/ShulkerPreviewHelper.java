package rtx.nv.api.mods.shulkerview;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import rtx.nv.api.modules.impl.Utils.ShulkerPreview;

public final class ShulkerPreviewHelper {
    public static final int SHULKER_SIZE = 27;
    public static final int ROW_SIZE = 9;
    private static ItemStack frozenShulker = null;
    private static float frozenOverlayX = 0.0f;
    private static float frozenOverlayY = 0.0f;
    private static ItemStack lastShownShulker = null;
    private static float lastRenderedX = 0.0f;
    private static float lastRenderedY = 0.0f;
    private static long lastRenderedTime = 0L;

    private ShulkerPreviewHelper() {
    }

    public static boolean isFrozen() {
        return frozenShulker != null;
    }

    public static void tryFreeze() {
        long l = System.currentTimeMillis();
        if (lastShownShulker != null && ShulkerPreviewHelper.canPreview(lastShownShulker) && l - lastRenderedTime < 200L) {
            frozenShulker = lastShownShulker.copy();
            frozenOverlayX = lastRenderedX;
            frozenOverlayY = lastRenderedY;
        }
    }

    public static void unfreeze() {
        frozenShulker = null;
    }

    public static ItemStack getFrozenShulker() {
        return frozenShulker;
    }

    public static float getFrozenOverlayY() {
        return frozenOverlayY;
    }

    public static boolean previewKeyPressed() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient != null && minecraftClient.getWindow() != null && InputUtil.isKeyPressed((Window)minecraftClient.getWindow(), (int)340);
    }

    public static int shulkerColor(ItemStack itemStack) {
        DyeColor dyeColor = null;
        if (itemStack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ShulkerBoxBlock shulkerBoxBlock) {
                dyeColor = shulkerBoxBlock.getColor();
            }
        }
        int n = dyeColor == null ? 0x976797 : ShulkerPreviewHelper.dyeColor(dyeColor);
        return 0xFF000000 | n;
    }

    public static boolean ctrlKeyPressed() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient != null && minecraftClient.getWindow() != null && InputUtil.isKeyPressed((Window)minecraftClient.getWindow(), (int)341);
    }

    public static float getFrozenOverlayX() {
        return frozenOverlayX;
    }

    public static boolean shouldShowPreview(ItemStack itemStack) {
        if (!ShulkerPreviewHelper.canPreview(itemStack)) {
            return false;
        }
        if (ShulkerPreviewHelper.isFrozen()) {
            return false;
        }
        return ShulkerPreviewHelper.previewKeyPressed();
    }

    public static List<Text> tooltipLines(ItemStack itemStack) {
        if (!ShulkerPreviewHelper.canPreview(itemStack)) {
            return List.of();
        }
        MutableText mutableText = Text.literal((String)"Shift").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)).append((Text)Text.literal((String)": preview").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        MutableText mutableText2 = Text.literal((String)"  Ctrl").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)).append((Text)Text.literal((String)": pin  ").setStyle(Style.EMPTY.withColor(Formatting.GRAY))).append((Text)Text.literal((String)"Shift+Ctrl").setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append((Text)Text.literal((String)": item info").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        return List.of(Text.translatable((String)"container.shulkerbox.contains", (Object[])new Object[]{ShulkerPreviewHelper.contentStackCount(itemStack)}), mutableText, mutableText2);
    }

    public static List<ItemStack> getItems(ItemStack itemStack) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize((int)27, ItemStack.EMPTY);
        ContainerComponent containerComponent = (ContainerComponent)itemStack.get(DataComponentTypes.CONTAINER);
        if (containerComponent != null) {
            containerComponent.copyTo(defaultedList);
        }
        return List.copyOf(defaultedList);
    }

    private static boolean hasVisibleContent(ItemStack itemStack) {
        ContainerComponent containerComponent = (ContainerComponent)itemStack.get(DataComponentTypes.CONTAINER);
        if (containerComponent == null) {
            return false;
        }
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize((int)27, ItemStack.EMPTY);
        containerComponent.copyTo(defaultedList);
        for (ItemStack itemStack2 : defaultedList) {
            if (itemStack2.isEmpty() || itemStack2.getItem() == Items.AIR) continue;
            return true;
        }
        return false;
    }

    public static List<ItemStack> getCompactItems(ItemStack itemStack3) {
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>();
        for (ItemStack itemStack4 : ShulkerPreviewHelper.getItems(itemStack3)) {
            if (itemStack4.isEmpty() || itemStack4.getItem() == Items.AIR) continue;
            ItemStack itemStack5 = ShulkerPreviewHelper.findMatching(arrayList, itemStack4);
            if (itemStack5 == null) {
                arrayList.add(itemStack4.copy());
                continue;
            }
            itemStack5.increment(itemStack4.getCount());
        }
        arrayList.sort((itemStack, itemStack2) -> Integer.compare(itemStack2.getCount(), itemStack.getCount()));
        return arrayList;
    }

    public static int contentStackCount(ItemStack itemStack) {
        int n = 0;
        for (ItemStack itemStack2 : ShulkerPreviewHelper.getItems(itemStack)) {
            if (itemStack2.isEmpty() || itemStack2.getItem() == Items.AIR) continue;
            ++n;
        }
        return n;
    }

    public static void recordTooltipPosition(ItemStack itemStack, int n, int n2) {
        lastShownShulker = itemStack;
        lastRenderedX = n;
        lastRenderedY = n2;
        lastRenderedTime = System.currentTimeMillis();
    }

    private static ItemStack findMatching(List<ItemStack> list, ItemStack itemStack) {
        for (ItemStack itemStack2 : list) {
            if (!ItemStack.areEqual((ItemStack)itemStack2, (ItemStack)itemStack)) continue;
            return itemStack2;
        }
        return null;
    }

    private static int dyeColor(DyeColor dyeColor) {
        int n = dyeColor.getEntityColor();
        int n2 = Math.max(38, n >> 16 & 0xFF);
        int n3 = Math.max(38, n >> 8 & 0xFF);
        int n4 = Math.max(38, n & 0xFF);
        return n2 << 16 | n3 << 8 | n4;
    }

    public static boolean canPreview(ItemStack itemStack) {
        BlockItem blockItem;
        Item item;
        return ShulkerPreview.enabled() && itemStack != null && !itemStack.isEmpty() && (item = itemStack.getItem()) instanceof BlockItem && (blockItem = (BlockItem)item).getBlock() instanceof ShulkerBoxBlock && itemStack.get(DataComponentTypes.CONTAINER) != null && ShulkerPreviewHelper.hasVisibleContent(itemStack);
    }
}

