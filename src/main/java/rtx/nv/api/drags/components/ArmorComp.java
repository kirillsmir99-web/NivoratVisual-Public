package rtx.nv.api.drags.components;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix3x2f;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ArmorModule;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

public final class ArmorComp extends Draggable {
    private static final float ITEM_SIZE = 16.0f;
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final SmoothAnimation visibility = new SmoothAnimation();
    private int side;
    private int previewSide;
    private int sideBeforeDrag;
    private boolean wasDragging;
    private boolean sideResolved;

    private float width = 20.0f;
    private float height = 20.0f;
    private float previewWidth = this.width;
    private float previewHeight = this.height;

    private List<ItemStack> shownItems = List.of();
    private boolean shownIsLocal = true;

    public ArmorComp() {
        super("armor", 166.0f, 5.0f);
        this.visibility.set(0.0);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public float overlayHeight() {
        return this.previewHeight;
    }

    @Override
    public float overlayWidth() {
        return this.previewWidth;
    }

    @Override
    public boolean isInteractive() {
        return ArmorComp.componentEnabled();
    }

    private static boolean componentEnabled() {
        ArmorModule module = ModuleManager.get().get(ArmorModule.class);
        return module != null && module.isEnabled();
    }

    private static ArmorModule getModule() {
        return ModuleManager.get().get(ArmorModule.class);
    }

    private static LivingEntity resolveTarget(MinecraftClient mc) {
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living != mc.player) {
            if (living.isAlive() && !living.isRemoved() && living.getHealth() > 0.0f && !living.isInvisible()) {
                return living;
            }
        }
        if (mc.targetedEntity instanceof LivingEntity living && living != mc.player) {
            if (living.isAlive() && !living.isRemoved() && living.getHealth() > 0.0f && !living.isInvisible()) {
                return living;
            }
        }
        return null;
    }

    private static List<ItemStack> previewItems() {
        List<ItemStack> list = new ArrayList<>(4);
        list.add(Items.DIAMOND_HELMET.getDefaultStack());
        list.add(Items.DIAMOND_CHESTPLATE.getDefaultStack());
        list.add(Items.DIAMOND_LEGGINGS.getDefaultStack());
        list.add(Items.DIAMOND_BOOTS.getDefaultStack());
        return list;
    }

    private static List<ItemStack> collectItems(boolean targetSource) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity source = targetSource ? resolveTarget(mc) : mc.player;
        if (source == null) {
            return List.of();
        }
        List<ItemStack> list = new ArrayList<>(ARMOR_SLOTS.length);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = source.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    private static String resolveDurabilityText(ItemStack stack, boolean isLocal) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) {
            return "";
        }
        int max = stack.getMaxDamage();
        if (isLocal) {
            int remaining = Math.max(0, max - stack.getDamage());
            int pct = Math.round(((float) remaining / (float) max) * 100.0f);
            return pct + "%";
        }
        if (stack.getDamage() > 0) {
            int remaining = Math.max(0, max - stack.getDamage());
            int pct = Math.round(((float) remaining / (float) max) * 100.0f);
            return pct + "%";
        }
        return "100%";
    }

    private static int dockFromPosition(float x, float w, float screenW) {
        if (x >= screenW - w - 6.0f) {
            return 1;
        }
        if (x <= 6.0f) {
            return -1;
        }
        return 0;
    }

    private static int edgeSide(float mouseX, float screenW, int currentSide) {
        if (currentSide > 0) {
            return mouseX < screenW - 26.0f ? 0 : 1;
        }
        if (currentSide < 0) {
            return mouseX > 26.0f ? 0 : -1;
        }
        if (mouseX >= screenW - 16.0f) {
            return 1;
        }
        if (mouseX <= 16.0f) {
            return -1;
        }
        return 0;
    }

    private void updateSide(boolean dragging, boolean fits, float thickness, float screenW) {
        boolean started = !this.wasDragging && dragging;
        boolean stopped = this.wasDragging && !dragging;
        this.wasDragging = dragging;

        if (started) {
            this.sideBeforeDrag = this.side;
        }
        if (dragging) {
            int side = fits ? edgeSide(Position.mouseX(), screenW, this.previewSide) : 0;
            if ((side != 0) != (this.previewSide != 0)) {
                this.getDrag().swapGrabOffset();
            }
            this.previewSide = side;
            this.sideResolved = true;
            return;
        }
        if (stopped) {
            this.side = this.getDrag().wasCancelled() ? this.sideBeforeDrag : this.previewSide;
        } else if (!this.sideResolved) {
            this.side = fits ? dockFromPosition(this.getDrag().getTargetX(), thickness, screenW) : 0;
            this.sideResolved = true;
        }
        this.previewSide = this.side;
        if (this.side > 0) {
            this.getDrag().setTargetX(screenW - thickness - 5.0f);
        } else if (this.side < 0) {
            this.getDrag().setTargetX(5.0f);
        }
    }

    @Override
    protected void render(DrawContext drawContext) {
        ArmorModule module = getModule();
        boolean enabled = module != null && module.isEnabled();
        boolean isDragging = DragSystem.get().isDragModeActive();

        boolean targetSource = module != null && module.isTargetSource();
        List<ItemStack> items = enabled ? collectItems(targetSource) : List.of();
        boolean isLocal = !targetSource;

        if (items.isEmpty() && enabled && isDragging) {
            items = previewItems();
            isLocal = true;
        }

        if (!items.isEmpty()) {
            this.shownItems = items;
            this.shownIsLocal = isLocal;
        }

        boolean shouldShow = enabled && !items.isEmpty();
        this.visibility.run(shouldShow ? 1.0 : 0.0, shouldShow ? 0.18 : 0.12, Easings.CUBIC_OUT, true);
        this.visibility.update();
        float alpha = this.visibility.get();
        if (alpha <= 0.01f || this.shownItems.isEmpty()) {
            return;
        }

        boolean showDurability = module != null && module.showDurability();
        boolean showBar = module != null && module.showDurabilityBar();
        boolean isTransparent = module != null && module.isTransparentStyle();
        boolean isSlots = module == null || module.isSlotsStyle();
        List<ItemStack> list = this.shownItems;
        int count = list.size();

        float cellW = 20.0f;
        float cellH = showDurability ? (showBar ? 27.0f : 25.0f) : 19.0f;
        float gap = isTransparent ? 2.5f : (isSlots ? 2.5f : 1.0f);

        float screenW = Math.max(1.0f, Position.screenWidth());
        float totalHorizW = 3.0f + count * cellW + Math.max(0, count - 1) * gap + 1.0f;
        float totalHorizH = 3.0f + cellH + 1.0f;

        float totalVertW = 3.0f + cellW + 1.0f;
        float totalVertH = 3.0f + count * cellH + Math.max(0, count - 1) * gap + 1.0f;

        boolean fits = totalHorizW <= screenW - 10.0f;
        this.updateSide(this.getDrag().isDragging(), fits, totalVertW, screenW);

        boolean isVertical = this.side != 0;
        this.width = isVertical ? totalVertW : totalHorizW;
        this.height = isVertical ? totalVertH : totalHorizH;
        this.previewWidth = (this.previewSide != 0) ? totalVertW : totalHorizW;
        this.previewHeight = (this.previewSide != 0) ? totalVertH : totalHorizH;

        float posX = this.getX();
        float posY = this.getY();
        float scale = 0.90f + alpha * 0.10f;
        float centerX = posX + this.width * 0.5f;
        float centerY = posY + this.height * 0.5f;
        float offsetY = (1.0f - alpha) * 6.0f;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(centerX, centerY + offsetY);
        drawContext.getMatrices().scale(scale);
        drawContext.getMatrices().translate(-centerX, -(centerY + offsetY));

        if (!isTransparent && !isSlots) {
            Render2D.beginFrame(drawContext);
            RectUtil.drawClientRectFixedRadius(posX, posY, this.width, this.height, 4.5f, alpha);
            Render2D.flush();
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float curOffset = 2.0f;

        for (int i = 0; i < count; i++) {
            ItemStack stack = list.get(i);
            float slotX = posX + (isVertical ? 2.0f : curOffset);
            float slotY = posY + (isVertical ? curOffset : 2.0f);

            if (!isTransparent && isSlots) {
                Render2D.beginFrame(drawContext);
                RectUtil.drawClientRectFixedRadius(slotX, slotY, cellW, cellH, 3.5f, alpha);
                Render2D.flush();
            }

            drawContext.getMatrices().pushMatrix();
            Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f) drawContext.getMatrices());
            float itemX = slotX + (cellW - ITEM_SIZE) * 0.5f;
            float itemY = slotY + 1.5f;
            drawContext.getMatrices().translate(itemX + 8.0f, itemY + 8.0f);
            drawContext.getMatrices().scale(alpha, alpha);
            drawContext.getMatrices().translate(-8.0f, -8.0f);
            drawContext.drawItem(stack, 0, 0);
            drawContext.drawStackOverlay(textRenderer, stack, 0, 0);
            drawContext.getMatrices().popMatrix();

            if (showDurability) {
                float durRatio = resolveDurabilityRatio(stack, this.shownIsLocal);
                String durText = resolveDurabilityText(stack, this.shownIsLocal);
                float curY = slotY + ITEM_SIZE + 2.0f;

                Render2D.beginFrame(drawContext);
                if (showBar && durRatio >= 0.0f) {
                    float barTotalW = 15.0f;
                    float barH = 1.8f;
                    float barX = slotX + (cellW - barTotalW) * 0.5f;
                    int barBg = ColorUtil.rgba(16, 18, 24, (int)(180 * alpha));
                    Render2D.rect(barX, curY, barTotalW, barH, 0.9f, barBg);
                    float fillW = Math.max(1.0f, barTotalW * durRatio);
                    int barCol = getDurabilityColor(durRatio, alpha);
                    Render2D.rect(barX, curY, fillW, barH, 0.9f, barCol);
                    curY += barH + 1.5f;
                }

                if (!durText.isEmpty()) {
                    float durFontSize = 6.8f;
                    float textW = Render2D.msdfWidth("montserrat-semibold", durText, durFontSize);
                    float textX = slotX + (cellW - textW) * 0.5f;
                    int durColor = ColorUtil.multAlpha(-1, alpha * 0.90f);
                    int shadowCol = ColorUtil.rgba(0, 0, 0, (int)(150 * alpha));
                    Render2D.msdfText("montserrat-semibold", durText, textX + 0.5f, curY + 0.5f, durFontSize, shadowCol);
                    Render2D.msdfText("montserrat-semibold", durText, textX, curY, durFontSize, durColor);
                }
                Render2D.flush();
            }

            curOffset += (isVertical ? cellH : cellW) + gap;
        }

        drawContext.getMatrices().popMatrix();
    }

    private static float resolveDurabilityRatio(ItemStack stack, boolean isLocal) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) {
            return -1.0f;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) return 1.0f;
        int remaining = Math.max(0, max - stack.getDamage());
        return Math.max(0.0f, Math.min(1.0f, (float) remaining / (float) max));
    }

    private static int getDurabilityColor(float ratio, float alpha) {
        int r, g, b;
        if (ratio > 0.6f) {
            float t = (ratio - 0.6f) / 0.4f;
            r = (int)(255 * (1.0f - t) + 70 * t);
            g = (int)(215 * (1.0f - t) + 235 * t);
            b = (int)(50 * (1.0f - t) + 95 * t);
        } else if (ratio > 0.25f) {
            float t = (ratio - 0.25f) / 0.35f;
            r = 255;
            g = (int)(75 + 140 * t);
            b = 40;
        } else {
            r = 255;
            g = 55;
            b = 60;
        }
        return ColorUtil.rgba(r, g, b, (int)(230 * alpha));
    }
}
