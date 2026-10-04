package rtx.nv.api.drags.components;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2f;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Interface.InventoryModule;
import rtx.nv.api.modules.impl.Visuals.ItemHighlight;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.impl.SliderSetting;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

public final class InventoryComp extends Draggable {
    private static final float RADIUS = 5.0f;
    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS = 3;
    private static final int TOTAL = 27;
    private static final float CELL = 11.0f;
    private static final float PAD = 4.0f;
    private static final float PANEL_WIDTH = 107.0f;
    private static final Easing SMOOTH = d -> d * d * (3.0 - 2.0 * d);

    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation extentAnimation = new SmoothAnimation();
    private List<ItemStack> stacks = new ArrayList<ItemStack>();
    private final float[] slotProgress = new float[TOTAL];
    private final ItemStack[] shownStacks = new ItemStack[TOTAL];
    private float currentWidth = PANEL_WIDTH;
    private float currentHeight = 41.0f;
    private boolean everHadContent;
    private long sizeCollapsedAtMs;
    private long lastNs = System.nanoTime();

    public InventoryComp() {
        super("inventory", 5.0f, 82.0f);
        this.visibility.set(0.0);
        this.extentAnimation.set(0.0);
    }

    private static InventoryModule module() {
        return ModuleManager.get().get(InventoryModule.class);
    }

    public float getScale() {
        InventoryModule inventoryModule = module();
        return inventoryModule != null ? inventoryModule.getScale() : 1.0f;
    }

    @Override
    public String displayName() {
        return "Inventory";
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        InventoryModule inventoryModule = module();
        if (inventoryModule != null) {
            list.add(new SliderSetting(inventoryModule.scale));
        }
        return list;
    }

    @Override
    public float width() {
        return this.currentWidth * this.getScale();
    }

    @Override
    public float height() {
        return this.currentHeight * this.getScale();
    }

    @Override
    public boolean isInteractive() {
        return InventoryComp.componentEnabled();
    }

    private static boolean componentEnabled() {
        InventoryModule inventoryModule = ModuleManager.get().get(InventoryModule.class);
        return inventoryModule != null && inventoryModule.isEnabled();
    }

    private static List<ItemStack> readInventory() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(TOTAL);
        if (minecraftClient.player != null) {
            PlayerInventory playerInventory = minecraftClient.player.getInventory();
            for (int i = 9; i < 36; ++i) {
                arrayList.add(playerInventory.getStack(i));
            }
        }
        return arrayList;
    }

    private boolean computeTargetVisible(boolean hasAnyState) {
        if (hasAnyState) {
            this.everHadContent = true;
            this.sizeCollapsedAtMs = 0L;
            return true;
        }
        if (!this.everHadContent) {
            return false;
        }
        boolean active = this.extentAnimation.get() > 0.02f;
        if (active) {
            this.sizeCollapsedAtMs = 0L;
            return true;
        }
        if (this.sizeCollapsedAtMs == 0L) {
            this.sizeCollapsedAtMs = System.currentTimeMillis();
        }
        return System.currentTimeMillis() - this.sizeCollapsedAtMs < 100L;
    }

    private static float cellX(float f, int n) {
        return f + PAD + (float)(n % ITEMS_PER_ROW) * CELL;
    }

    private static float cellY(float f, int n) {
        return f + PAD + (float)(n / ITEMS_PER_ROW) * CELL;
    }

    private void drawSeparators(float f, float f2, float f3, int maxRow, boolean showAll) {
        for (int row = 0; row <= maxRow; ++row) {
            float rowPres = row == maxRow ? this.pres(row, showAll) : 1.0f;
            if (rowPres <= 0.02f) continue;
            int color = ColorUtil.multAlpha(-1, f3 * 0.12f * rowPres);
            int rowStart = row * ITEMS_PER_ROW;
            for (int col = 0; col < ITEMS_PER_ROW - 1; ++col) {
                int slot = rowStart + col;
                Render2D.rect(InventoryComp.cellX(f, slot) + CELL - 0.25f, InventoryComp.cellY(f2, slot) + 1.0f, 0.5f, 9.0f, color);
            }
            if (row >= maxRow) continue;
            float nextPres = row + 1 == maxRow ? this.pres(row + 1, showAll) : 1.0f;
            int hColor = ColorUtil.multAlpha(-1, f3 * 0.12f * Math.min(rowPres, nextPres));
            for (int col = 0; col < ITEMS_PER_ROW; ++col) {
                int slot = rowStart + col;
                Render2D.rect(InventoryComp.cellX(f, slot) + 1.0f, InventoryComp.cellY(f2, slot) + CELL - 0.25f, 9.0f, 0.5f, hColor);
            }
        }
    }

    @Override
    protected void render(DrawContext drawContext) {
        boolean enabled = InventoryComp.componentEnabled();
        if (!enabled) {
            this.visibility.set(0.0);
            return;
        }
        boolean dragMode = DragSystem.get().isDragModeActive();
        this.stacks = InventoryComp.readInventory();
        boolean hasItems = false;
        for (ItemStack itemStack : this.stacks) {
            if (!itemStack.isEmpty()) {
                hasItems = true;
                break;
            }
        }
        boolean forcePreview = dragMode && !hasItems;

        int maxRowWithItem = -1;
        for (int i = 0; i < ROWS; ++i) {
            if (!forcePreview && !this.rowHasItem(i) && !(this.pres(i, false) > 0.02f)) continue;
            maxRowWithItem = i;
        }
        float targetRows = maxRowWithItem + 1;
        if (Math.abs(this.extentAnimation.getToValue() - (double)targetRows) > 0.01) {
            this.extentAnimation.run(targetRows, targetRows >= this.extentAnimation.get() ? 0.22 : 0.18, SMOOTH, false);
        }
        this.extentAnimation.update();
        float currentRows = this.extentAnimation.get();

        boolean anyState = hasItems || dragMode;
        float targetHeight = PAD * 2.0f + currentRows * CELL;
        this.currentHeight = targetHeight;
        this.currentWidth = PANEL_WIDTH;

        boolean targetVisible = this.computeTargetVisible(anyState);
        this.visibility.run(targetVisible ? 1.0 : 0.0, targetVisible ? 0.18 : 0.12, Easings.CUBIC_OUT, true);
        this.visibility.update();
        float vis = this.visibility.get();
        boolean fullyVisible = vis >= 0.9f;

        long nowNs = System.nanoTime();
        float deltaSec = Math.min(0.1f, (float)(nowNs - this.lastNs) / 1.0E9f);
        this.lastNs = nowNs;
        float slotLerp = 1.0f - (float)Math.exp(-deltaSec * 13.0f);

        for (int i = 0; i < TOTAL; ++i) {
            ItemStack itemStack = i < this.stacks.size() ? this.stacks.get(i) : ItemStack.EMPTY;
            if (!itemStack.isEmpty()) {
                this.shownStacks[i] = itemStack;
            }
            float targetP = !itemStack.isEmpty() && fullyVisible ? 1.0f : 0.0f;
            this.slotProgress[i] = this.slotProgress[i] + (targetP - this.slotProgress[i]) * slotLerp;
        }

        if (vis <= 0.01f) {
            return;
        }

        float originX = this.getX();
        float originY = this.getY();
        float scale = this.getScale();
        float animScale = 0.90f + vis * 0.10f;
        float centerX = originX + this.currentWidth * 0.5f;
        float centerY = originY + this.currentHeight * 0.5f;
        float offsetY = (1.0f - vis) * 6.0f;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(originX, originY);
        drawContext.getMatrices().scale(scale, scale);
        drawContext.getMatrices().translate(-originX, -originY);

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(centerX, centerY + offsetY);
        drawContext.getMatrices().scale(animScale);
        drawContext.getMatrices().translate(-centerX, -(centerY + offsetY));

        Render2D.beginFrame(drawContext);
        RectUtil.drawClientRect(originX, originY, this.currentWidth, this.currentHeight, RADIUS, vis);
        Render2D.flush();

        Render2D.beginFrame(drawContext);
        Render2D.pushScissor(drawContext, originX, originY, this.currentWidth, this.currentHeight);
        this.drawSeparators(originX, originY, vis, maxRowWithItem, forcePreview);
        Render2D.flush();

        this.drawItems(drawContext, originX, originY, vis, maxRowWithItem);
        Render2D.popScissor(drawContext);

        drawContext.getMatrices().popMatrix();
        drawContext.getMatrices().popMatrix();
    }

    private void drawItems(DrawContext drawContext, float f, float f2, float f3, int maxRow) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        ItemHighlight itemHighlight = ItemHighlight.getInstance();
        if (itemHighlight != null && itemHighlight.isEnabled()) {
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            float radius = interfaceModule == null ? 2.0f : interfaceModule.rectCornerRadius.getFloat();
            radius = Math.min(4.0f, Math.max(2.0f, radius));
            Render2D.beginFrame(drawContext);
            for (int i = 0; i < TOTAL; ++i) {
                float p = this.slotProgress[i];
                ItemStack itemStack = this.shownStacks[i];
                if (p <= 0.02f || itemStack == null || itemStack.isEmpty()) continue;
                int bg = itemHighlight.backgroundFor(itemStack, false);
                boolean isBottomRow = i / ITEMS_PER_ROW == maxRow;
                float brRadius = isBottomRow && i % ITEMS_PER_ROW == ITEMS_PER_ROW - 1 ? radius : 2.0f;
                float blRadius = isBottomRow && i % ITEMS_PER_ROW == 0 ? radius : 2.0f;
                itemHighlight.drawRoundedSlotBackground(
                    InventoryComp.cellX(f, i) + 1.5f,
                    InventoryComp.cellY(f2, i) + 1.5f,
                    8.0f, bg, f3 * p, 2.0f, 2.0f, brRadius, blRadius
                );
            }
            Render2D.flush();
        }

        for (int i = 0; i < TOTAL; ++i) {
            float p = this.slotProgress[i];
            ItemStack itemStack = this.shownStacks[i];
            if (p <= 0.02f || itemStack == null || itemStack.isEmpty()) continue;
            float alpha = Math.max(0.0f, Math.min(1.0f, f3 * p));
            if (alpha <= 0.01f) continue;

            float itemSize = 8.0f * InventoryComp.easeOutBack(p);
            float itemScale = (itemSize / 16.0f) * alpha;
            float slotCenterX = InventoryComp.cellX(f, i) + 5.5f;
            float slotCenterY = InventoryComp.cellY(f2, i) + 5.5f;

            drawContext.getMatrices().pushMatrix();
            Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f)drawContext.getMatrices());
            drawContext.getMatrices().translate(slotCenterX, slotCenterY);
            drawContext.getMatrices().scale(itemScale, itemScale);
            drawContext.getMatrices().translate(-8.0f, -8.0f);
            drawContext.drawItem(itemStack, 0, 0);
            drawContext.drawStackOverlay(textRenderer, itemStack, 0, 0);
            drawContext.getMatrices().popMatrix();
        }
    }

    private boolean rowHasItem(int row) {
        int rowStart = row * ITEMS_PER_ROW;
        for (int col = 0; col < ITEMS_PER_ROW; ++col) {
            int slot = rowStart + col;
            if (slot >= TOTAL || slot >= this.stacks.size() || this.stacks.get(slot).isEmpty()) continue;
            return true;
        }
        return false;
    }

    private float pres(int row, boolean force) {
        if (force) {
            return 1.0f;
        }
        float maxP = 0.0f;
        int rowStart = row * ITEMS_PER_ROW;
        for (int col = 0; col < ITEMS_PER_ROW; ++col) {
            int slot = rowStart + col;
            if (slot >= TOTAL) continue;
            maxP = Math.max(maxP, this.slotProgress[slot]);
        }
        return maxP;
    }

    private static float easeOutBack(float f) {
        float f2 = 1.70158f;
        float f3 = f2 + 1.0f;
        float f4 = f - 1.0f;
        return 1.0f + f3 * f4 * f4 * f4 + f2 * f4 * f4;
    }
}
