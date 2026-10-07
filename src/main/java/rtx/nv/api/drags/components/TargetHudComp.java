package rtx.nv.api.drags.components;
import rtx.nv.api.events.EventHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityPose;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtx.nv.api.ui.GuiEntityBounds;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Interface.TargetHudModule;
import rtx.nv.api.modules.impl.Visuals.NameTags;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.network.Network;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.BuiltRectangle;

public final class TargetHudComp
extends Draggable {
    private static final String NAME_FONT = "montserrat-semibold";
    private static final String INFO_FONT = "montserrat-medium";
    private static final float PAD = 6.0f;
    private static final float HEAD = 22.0f;
    private static final float HAT_SCALE = 1.18f;
    private static final float GAP = 7.0f;
    private static final float BAR_H = 3.0f;
    private static final float NAME_SIZE = 8.0f;
    private static final float INFO_SIZE = 6.0f;
    private static final float RADIUS = 7.0f;
    private static final float MIN_CONTENT_W = 74.0f;
    private static final float HEIGHT = 34.0f;
    private static final long HOLD_MS = 1500L;
    private static final int NAME_COLOR = -1;
    private static final int INFO_COLOR = -3618608;
    private static final int BAR_BG_COLOR = -14935006;
    private static final float NEW_PAD = 3.0f;
    private static final float NEW_GAP = 3.0f;
    private static final float NEW_HEAD = 15.0f;
    private static final float NEW_HEAD_RADIUS = 6.0f;
    private static final String NEW_FONT = "small-pixel";
    private static final float NEW_ICON = 8.0f;
    private static final float NEW_ICON_GAP = 3.0f;
    private static final float NEW_HEAD_ITEMS_GAP = 3.0f;
    private static final float NEW_BAR_H = 8.0f;
    private static final float NEW_BAR_RADIUS = 2.0f;
    private static final float NEW_MIN_CONTENT = 78.0f;
    private static final float NEW_TEXT_EDGE_PAD = 2.0f;
    private static final float NEW_BOTTOM_PAD = 5.0f;
    private static final float NEW_HEIGHT = 45.0f;
    private static final EquipmentSlot[] NEW_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final float ARMOR_SCALE = 0.5f;
    private static final float ARMOR_ICON = 8.0f;
    private static final float ARMOR_GAP = 3.0f;
    private static final float CLASSIC_ARMOR_EXTRA = 11.0f;
    private static final float NEW_ARMOR_COLLAPSE = 10.0f;
    private final SmoothAnimation visibility = new SmoothAnimation();
    private boolean lastTargetVisible;
    private LivingEntity target;
    private long lastSeenMs;
    private float healthDisplay;
    private float hpGrayDisplay = 1.0f;
    private float armorProgress = 1.0f;
    private final float[] slotAppear = new float[NEW_SLOTS.length];
    private float currentWidth = 120.0f;
    private float lastPinnedWidth = Float.NaN;
    private long lastNs = System.nanoTime();
    private float damagePulse;
    private int lastParticleHurtTime = -1;
    private int particleEntityId = Integer.MIN_VALUE;
    private final Vector4f followScratch = new Vector4f();
    private float followScreenX;
    private float followScreenY;
    private float followHalfH;
    private boolean followProjectionValid;
    private float followOffsetX;
    private float followOffsetY;
    private static final int HP_GREEN = 3530826;
    private static final int HP_ORANGE = 16751136;
    private static final int HP_RED = 16722480;

    public TargetHudComp() {
        super("targethud", 5.0f, 150.0f);
        this.visibility.set(0.0);
        EventBus.get().subscribe(this);
    }

    private static Text resolveName(LivingEntity livingEntity) {
        PlayerEntity playerEntity;
        Text text;
        if (livingEntity instanceof PlayerEntity && (text = NameTags.resolveDisplayName(playerEntity = (PlayerEntity)livingEntity)) != null) {
            return text;
        }
        return Text.literal((String)livingEntity.getName().getString());
    }

    private static String normalize(String string, boolean bl) {
        StringBuilder stringBuilder = new StringBuilder();
        string.codePoints().forEach(n -> stringBuilder.append(NameTags.displayGlyphCovered(n, bl)));
        return stringBuilder.toString();
    }

    @Override
    public float width() {
        return this.currentWidth;
    }

    @Override
    public float height() {
        if (TargetHudComp.isNewMode()) {
            return 45.0f - 10.0f * (1.0f - TargetHudComp.easeInOutCubic(this.armorProgress));
        }
        return 34.0f + 11.0f * TargetHudComp.easeInOutCubic(this.armorProgress);
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    private static int darken(int n, float f) {
        float f2 = 1.0f - Math.max(0.0f, Math.min(1.0f, f));
        int n2 = Math.round((float)(n >> 16 & 0xFF) * f2);
        int n3 = Math.round((float)(n >> 8 & 0xFF) * f2);
        int n4 = Math.round((float)(n & 0xFF) * f2);
        return TargetHudComp.clamp255(n2) << 16 | TargetHudComp.clamp255(n3) << 8 | TargetHudComp.clamp255(n4);
    }

    private static boolean isNewMode() {
        TargetHudModule targetHudModule = TargetHudComp.hudModule();
        return targetHudModule != null && targetHudModule.isNewMode();
    }

    @Override
    public boolean isInteractive() {
        return TargetHudComp.componentEnabled();
    }

    private void pinCenterOnResize() {
        float f;
        if (!Float.isNaN(this.lastPinnedWidth) && !this.getDrag().isDragging() && Math.abs(f = this.currentWidth - this.lastPinnedWidth) > 1.0E-4f) {
            this.getDrag().setTargetX(this.getDrag().getTargetX() - f * 0.5f);
            this.getDrag().syncToTarget();
        }
        this.lastPinnedWidth = this.currentWidth;
    }

    private static float easeInOutCubic(float f) {
        return (f = TargetHudComp.clamp01(f)) < 0.5f ? 4.0f * f * f * f : 1.0f - (float)Math.pow(-2.0f * f + 2.0f, 3.0) / 2.0f;
    }

    private void drawHeadBackground(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        float radius = Math.min(f5, f3 * 0.5f);
        int bg = ColorUtil.multAlpha(-14935006, f6 * 0.55f);
        Render2D.rect(f, f2, f3, f3, radius, bg);

        int borderBase = ClientAccent.accent(f6 * 0.28f);
        if (this.damagePulse > 0.005f) {
            int hurtColor = ColorUtil.multAlpha(0xFFFF3366, f6 * 0.9f);
            int pulsedBorder = ColorUtil.lerpColor(borderBase, hurtColor, this.damagePulse);
            Render2D.outline(f, f2, f3, f3, radius, 1.0f, pulsedBorder);
        } else {
            Render2D.outline(f, f2, f3, f3, radius, 1.0f, borderBase);
        }
    }

    private static int armorPieceCount(LivingEntity livingEntity) {
        if (!TargetHudComp.armorEnabled()) {
            return 0;
        }
        int n = 0;
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            if (livingEntity.getEquippedStack(equipmentSlot).isEmpty()) continue;
            ++n;
        }
        return n;
    }

    private void updateDamagePulse(LivingEntity livingEntity, float dt) {
        if (livingEntity == null) {
            this.damagePulse = 0.0f;
            return;
        }
        if (livingEntity.getId() != this.particleEntityId) {
            this.particleEntityId = livingEntity.getId();
            this.lastParticleHurtTime = livingEntity.hurtTime;
            this.damagePulse = 0.0f;
        } else {
            int n = livingEntity.hurtTime;
            if (n > this.lastParticleHurtTime && n > 0) {
                this.damagePulse = 1.0f;
            }
            this.lastParticleHurtTime = n;
        }
        this.damagePulse += (0.0f - this.damagePulse) * (1.0f - (float)Math.exp(-dt * 9.0f));
        if (this.damagePulse < 0.001f) {
            this.damagePulse = 0.0f;
        }
    }

    private static int healthColor(float f) {
        if ((f = Math.max(0.0f, Math.min(1.0f, f))) >= 0.8f) {
            return 3530826;
        }
        if (f >= 0.4f) {
            float f2 = (f - 0.4f) / 0.4f;
            return ColorUtil.lerpColor(16751136, 3530826, f2);
        }
        float f3 = f / 0.4f;
        return ColorUtil.lerpColor(16722480, 16751136, f3);
    }

    private static void drawBarFill(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        if (f3 <= 0.5f) {
            return;
        }
        float f9 = Math.min(f6, f5 * 0.5f);
        float f10 = -0.7f;
        float f11 = TargetHudComp.clamp01(f8);
        int n = Math.round(235.0f * f11) << 24;
        TargetHudModule targetHudModule = TargetHudComp.hudModule();
        if (targetHudModule != null && targetHudModule.barWhite()) {
            int n2 = Math.round(215.0f * f11) << 24 | 0xD7D7D7;
            Render2D.rect(new BuiltRectangle(f, f2, f3, f5, f9, n2).withSmoothness(f10));
            return;
        }
        if (targetHudModule != null && targetHudModule.barClient()) {
            Render2D.rect(new BuiltRectangle(f, f2, f3, f5, f9, -1).withPaletteGradient(3, 1.0f, TargetHudComp.clamp01(235.0f * f11 / 255.0f)).withSmoothness(f10));
            return;
        }
        int n3 = TargetHudComp.healthColor(f7) & 0xFFFFFF;
        int n4 = n | TargetHudComp.darken(n3, 0.45f);
        int n5 = n | n3;
        Render2D.rect(new BuiltRectangle(f, f2, f3, f5, f9, f9, f9, f9, n4, n5, n5, n4, f10));
    }

    private static int barFillColorAt(float f, float f2) {
        TargetHudModule targetHudModule = TargetHudComp.hudModule();
        if (targetHudModule != null && targetHudModule.barWhite()) {
            return -2631721;
        }
        if (targetHudModule != null && targetHudModule.barClient()) {
            return 0xFF000000 | ClientPalette.loopColor(TargetHudComp.clamp01(f) + ClientPalette.scrollPhase()) & 0xFFFFFF;
        }
        int n = TargetHudComp.healthColor(f2) & 0xFFFFFF;
        return 0xFF000000 | ColorUtil.lerpColor(TargetHudComp.darken(n, 0.45f), n, TargetHudComp.clamp01(f));
    }

    private static float easeOutCubic(float f) {
        float f2 = 1.0f - TargetHudComp.clamp01(f);
        return 1.0f - f2 * f2 * f2;
    }

    private void drawArmorCentered(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        float f5;
        if (TargetHudComp.armorPieceCount(livingEntity) <= 0) {
            return;
        }
        float f6 = 16.0f;
        float f7 = 8.0f / f6;
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(ARMOR_SLOTS.length);
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            arrayList.add(livingEntity.getEquippedStack(equipmentSlot));
        }
        float f8 = (float)arrayList.size() * 8.0f + (float)(arrayList.size() - 1) * 3.0f;
        float f9 = f - f8 * 0.5f;
        float f10 = Math.max(0.0f, Math.min(1.0f, f3));
        int n = Math.max(1, arrayList.size() - 1);
        Render2D.beginFrame(drawContext);
        for (int i = 0; i < arrayList.size(); ++i) {
            float f11 = 0.35f + 0.25f * ((float)i / (float)n);
            float f12 = TargetHudComp.easeOutCubic((f4 - f11) / 0.3f);
            if (!(f12 > 0.01f)) continue;
            f5 = f9 + (float)i * 11.0f;
            int n2 = Math.round(36.0f * f10 * f12) << 24;
            Render2D.rect(f5 - 1.0f, f2 - 1.0f, 10.0f, 10.0f, 2.0f * f12, n2);
        }
        Render2D.flush();
        float f13 = 4.0f;
        for (int i = 0; i < arrayList.size(); ++i) {
            float f14;
            ItemStack itemStack = (ItemStack)arrayList.get(i);
            if (itemStack.isEmpty() || (f14 = TargetHudComp.clamp01((f4 - (f5 = 0.45f + 0.25f * ((float)i / (float)n))) / 0.3f)) <= 0.01f) continue;
            float f15 = f7 * f10 * TargetHudComp.easeOutBack(f14);
            float f16 = f9 + (float)i * 11.0f;
            drawContext.getMatrices().pushMatrix();
            Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f)drawContext.getMatrices());
            drawContext.getMatrices().translate(f16 + f13, f2 + f13);
            drawContext.getMatrices().scale(f15, f15);
            drawContext.getMatrices().translate(-8.0f, -8.0f);
            drawContext.drawItem(itemStack, 0, 0);
            drawContext.getMatrices().popMatrix();
        }
    }


    private static boolean componentEnabled() {
        TargetHudModule targetHudModule = ModuleManager.get().get(TargetHudModule.class);
        return targetHudModule != null && targetHudModule.isEnabled();
    }

    private void updateFollow(float f) {
        float f2;
        boolean bl = TargetHudComp.followEnabled() && this.followProjectionValid && this.target != null && this.target != MinecraftClient.getInstance().player && !DragSystem.get().isDragModeActive();
        float f3 = 0.0f;
        float f4 = 0.0f;
        if (bl) {
            boolean bl2;
            f2 = this.width();
            float f5 = this.height();
            float f6 = Position.screenWidth();
            float f7 = Position.screenHeight();
            float f8 = this.followScreenX;
            float f9 = this.followScreenY;
            boolean bl3 = bl2 = f8 > -f6 * 0.1f && f8 < f6 * 1.1f && f9 > -f7 * 0.1f && f9 < f7 * 1.1f;
            if (bl2) {
                float f10 = Math.max(this.followHalfH, 14.0f);
                float f11 = f10 * 0.45f;
                float f12 = 10.0f;
                float f13 = 8.0f;
                float f14 = f8 / f6;
                float f15 = Math.min(1.0f, Math.abs(f14 - 0.5f) / 0.15f);
                float f16 = f14 < 0.5f ? f8 + f11 + f12 : f8 - f11 - f12 - f2;
                float f17 = f9 - f5 * 0.5f;
                float f18 = f8 - f2 * 0.5f;
                float f19 = f9 + f10 + f13;
                float f20 = f18 + (f16 - f18) * f15;
                float f21 = f19 + (f17 - f19) * f15;
                f20 = Position.clampX(f20, f2);
                f21 = Position.clampY(f21, f5);
                f3 = f20 - this.getX();
                f4 = f21 - this.getY();
            }
        }
        f2 = 1.0f - (float)Math.exp(-f * 12.0f);
        this.followOffsetX += (f3 - this.followOffsetX) * f2;
        this.followOffsetY += (f4 - this.followOffsetY) * f2;
        if (!bl && Math.abs(this.followOffsetX) < 0.5f && Math.abs(this.followOffsetY) < 0.5f) {
            this.followOffsetX = 0.0f;
            this.followOffsetY = 0.0f;
        }
    }

    private static void drawColoredName(Text text, float f, float f2, float f3, int n, float f4) {
        boolean bl = NameTags.isStylized(text.getString());
        StringBuilder stringBuilder = new StringBuilder();
        float[] fArray = new float[]{f};
        int[] nArray = new int[]{n};
        text.visit((style2, string) -> {
            TextVisitFactory.visitFormatted((String)string, (Style)style2, (n2, style, n3) -> {
                int n4;
                int n5 = n4 = style.getColor() != null ? 0xFF000000 | style.getColor().getRgb() : n;
                if (n4 != nArray[0] && !stringBuilder.isEmpty()) {
                    String currentStr = stringBuilder.toString();
                    Render2D.msdfText(NAME_FONT, currentStr, fArray[0], f2, f3, ColorUtil.multAlpha(nArray[0], f4));
                    fArray[0] = fArray[0] + Render2D.msdfWidth(NAME_FONT, currentStr, f3);
                    stringBuilder.setLength(0);
                }
                nArray[0] = n4;
                stringBuilder.append(NameTags.displayGlyphCovered(n3, bl));
                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        if (!stringBuilder.isEmpty()) {
            Render2D.msdfText(NAME_FONT, stringBuilder.toString(), fArray[0], f2, f3, ColorUtil.multAlpha(nArray[0], f4));
        }
    }

    private static boolean followEnabled() {
        TargetHudModule targetHudModule = TargetHudComp.hudModule();
        return targetHudModule != null && targetHudModule.followTarget();
    }

    private static int sampleBarPalette(int[] nArray, float f) {
        int n = nArray.length;
        if (n <= 1) {
            return n == 1 ? nArray[0] : 0xFFFFFF;
        }
        float f2 = TargetHudComp.clamp01(f) * (float)(n - 1);
        int n2 = (int)f2;
        if (n2 > n - 2) {
            n2 = n - 2;
        }
        return ColorUtil.lerpColor(nArray[n2] | 0xFF000000, nArray[n2 + 1] | 0xFF000000, f2 - (float)n2);
    }

    private static String distanceText(LivingEntity livingEntity) {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        float f = clientPlayerEntity != null ? clientPlayerEntity.distanceTo((Entity)livingEntity) : 0.0f;
        return Integer.toString(Math.round(f));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void drawTargetEntity3D(DrawContext drawContext, LivingEntity livingEntity, float x, float y, float w, float h, float alpha) {
        if (livingEntity == null || alpha <= 0.05f) {
            return;
        }
        Render2D.flush();

        GuiEntityBounds bounds = GuiEntityBounds.from(drawContext, x, y, w, h);
        int left = bounds.left();
        int top = bounds.top();
        int right = bounds.right();
        int bottom = bounds.bottom();
        int boxW = Math.max(1, right - left);
        int boxH = Math.max(1, bottom - top);
        float boxSize = Math.min(boxW, boxH);

        MinecraftClient mc = MinecraftClient.getInstance();
        EntityRenderManager dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderer renderer = dispatcher.getRenderer(livingEntity);
        if (renderer == null) {
            return;
        }

        EntityRenderState renderState = renderer.getAndUpdateRenderState(livingEntity, 1.0f);
        if (renderState == null) {
            return;
        }

        renderState.light = 0x00F000F0;
        if (renderState.shadowPieces != null) {
            renderState.shadowPieces.clear();
        }
        renderState.outlineColor = 0;

        float targetHeight = 1.8f;
        float targetWidth = 0.6f;

        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.hurt = false;
            livingState.deathTime = 0.0f;
            livingState.shaking = false;
            livingState.usingRiptide = false;
            livingState.touchingWater = false;

            livingState.limbSwingAnimationProgress = 0.0f;
            livingState.limbSwingAmplitude = 0.0f;

            livingState.bodyYaw = 180.0f + 22.0f;
            livingState.relativeHeadYaw = -8.0f;
            if (livingState.pose != EntityPose.GLIDING) {
                livingState.pitch = 0.0f;
            }

            if (livingState instanceof BipedEntityRenderState biped) {
                biped.itemUseTime = 0.0f;
                biped.leaningPitch = 0.0f;
                biped.crossbowPullTime = 0.0f;
            }

            livingState.width /= livingState.baseScale;
            livingState.height /= livingState.baseScale;
            livingState.baseScale = 1.0f;

            targetHeight = Math.max(0.7f, livingState.height);
            targetWidth = Math.max(0.5f, livingState.width);
        }

        float scaleByH = (boxSize * 0.88f) / targetHeight;
        float scaleByW = (boxSize * 0.88f) / targetWidth;
        float scale = Math.max(1.0f, Math.min(scaleByH, scaleByW));

        if (this.damagePulse > 0.001f) {
            float bounce = 1.0f - 0.06f * (float)Math.sin(this.damagePulse * Math.PI);
            scale *= bounce;
        }

        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf transform = new Quaternionf().rotateX((float)Math.toRadians(-6.0f));
        rotation.mul(transform);

        Vector3f offset = new Vector3f(0.0f, targetHeight * 0.5f, 0.0f);

        drawContext.addEntity(renderState, scale, offset, rotation, transform, left, top, right, bottom);
    }

    private void drawHeadForeground(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4, float f5, float f6) {
        if (livingEntity == null || f6 <= 0.01f) {
            return;
        }

        TargetHudModule module = TargetHudComp.hudModule();
        boolean use3D = module == null || module.is3DAvatar();

        if (use3D || !(livingEntity instanceof AbstractClientPlayerEntity)) {
            this.drawTargetEntity3D(drawContext, livingEntity, f, f2, f3, f3, f6);
            return;
        }

        AbstractClientPlayerEntity abstractClientPlayerEntity = (AbstractClientPlayerEntity)livingEntity;
        Identifier identifier = abstractClientPlayerEntity.getSkin().body().texturePath();
        int n = Math.max(0, Math.min(255, Math.round(f6 * 255.0f))) << 24 | 0xFFFFFF;
        if (this.damagePulse > 0.01f) {
            n = ColorUtil.lerpColor(n, 0xFFFF4466, this.damagePulse * 0.45f);
        }
        String string = identifier.toString();
        float radius = Math.min(f4, f3 * 0.5f);
        if (Render2D.imageReady(string)) {
            Render2D.imageUvNearest(string, f, f2, f3, f5, radius, radius, radius, 0.5f, 0.125f, 0.125f, 0.25f, 0.25f, n);
            float f7 = 0.105932206f;
            float f8 = (0.125f - f7) * 0.5f;
            Render2D.imageUvNearest(string, f, f2, f3, f5, radius, radius, radius, 0.5f, 0.625f + f8, 0.125f + f8, 0.75f - f8, 0.25f - f8, n);
        } else {
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, (int)f, (int)f2, 8.0f, 8.0f, (int)f3, (int)f3, 8, 8, 64, 64, n);
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, (int)f, (int)f2, 40.0f, 8.0f, (int)f3, (int)f3, 8, 8, 64, 64, n);
        }
    }

    private static boolean armorEnabled() {
        TargetHudModule targetHudModule = ModuleManager.get().get(TargetHudModule.class);
        return targetHudModule != null && targetHudModule.showArmor();
    }

    private void renderContent(DrawContext drawContext, float f, float f2) {
        if (TargetHudComp.isNewMode()) {
            this.renderNew(drawContext, f, f2);
            return;
        }
        boolean bl = TargetHudComp.armorEnabled() && TargetHudComp.armorPieceCount(this.target) > 0;
        float f3 = bl ? 6.0f : 10.0f;
        this.armorProgress += ((bl ? 1.0f : 0.0f) - this.armorProgress) * (1.0f - (float)Math.exp(-f * f3));
        this.armorProgress = TargetHudComp.clamp01(this.armorProgress);
        float f4 = TargetHudComp.easeInOutCubic(this.armorProgress);
        float f5 = 11.0f * f4;
        float f6 = 34.0f + f5;
        float f7 = this.getY() + 6.0f + f5 * 0.5f;
        float f8 = this.getX() + 6.0f + 11.0f;
        this.updateDamagePulse(this.target, f);
        Text text = TargetHudComp.resolveName(this.target);
        float f10 = Math.max(1.0f, this.target.getMaxHealth());
        float f11 = Network.getResolvedHealth(this.target, true);
        float f12 = Math.max(0.0f, Math.min(1.0f, f11 / f10));
        this.healthDisplay += (f12 - this.healthDisplay) * (1.0f - (float)Math.exp(-f * 12.0f));
        String string = Math.round(f12 * 100.0f) + "% · " + TargetHudComp.distanceText(this.target) + "m";
        float f13 = TargetHudComp.coloredWidth(text, 8.0f);
        float f14 = Render2D.msdfWidth(INFO_FONT, string, 6.0f);
        float f15 = Math.max(74.0f, Math.max(f13, f14));
        this.currentWidth = 35.0f + f15 + 6.0f;
        this.pinCenterOnResize();
        float f16 = this.getX();
        float f17 = this.getY();
        float f18 = 0.94f + f2 * 0.06f;
        float f19 = f16 + this.currentWidth * 0.5f;
        float f20 = f17 + f6 * 0.5f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f19, f20);
        drawContext.getMatrices().scale(f18);
        drawContext.getMatrices().translate(-f19, -f20);
        Render2D.beginFrame(drawContext);
        RectUtil.drawClientRect(f16, f17, this.currentWidth, f6, 7.0f, f2);
        Render2D.flush();
        Render2D.beginFrame(drawContext);
        float f21 = f16 + 6.0f + 22.0f + 7.0f;
        float f22 = f21 + f15 * 0.5f;
        float f23 = 8.0f * f4;
        float f24 = (22.0f + f5 - 8.0f - f23 - 3.0f - 6.0f) / (2.0f + f4);
        float f25 = f17 + 6.0f;
        float f26 = f25 + 8.0f + f24 * f4;
        float f27 = f26 + f23 + f24;
        float f28 = f27 + 3.0f + f24;
        TargetHudComp.drawColoredName(text, f22 - f13 * 0.5f, f25, 8.0f, -1, f2 * 0.85f);
        float f29 = f21;
        float f30 = f15;
        float f31 = TargetHudComp.barRadius(3.0f);
        Render2D.rect(f29, f27 + 1.0f, f30, 3.0f, f31, ColorUtil.multAlpha(-14935006, f2 * 0.5f));
        float f32 = f30 * this.healthDisplay;
        TargetHudComp.drawBarFill(f29, f27 + 1.0f, f32, f30, 3.0f, f31, this.healthDisplay, f2);
        Render2D.msdfText(INFO_FONT, string, f22 - f14 * 0.5f, f28, 6.0f, ColorUtil.multAlpha(-3618608, f2));
        this.drawHeadBackground(drawContext, this.target, f16 + 6.0f + 1.0f, f7, 22.0f, 11.0f, 11.0f, f2, 0.0f);
        Render2D.flush();
        this.drawHeadForeground(drawContext, this.target, f16 + 6.0f + 1.0f, f7, 22.0f, 11.0f, 11.0f, f2);
        Render2D.flush();
        if (this.armorProgress > 0.02f) {
            this.drawArmorCentered(drawContext, this.target, f22, f26 + 1.0f, f2, this.armorProgress);
        }
        drawContext.getMatrices().popMatrix();
    }

    private static float coloredWidth(Text text, float f) {
        boolean bl = NameTags.isStylized(text.getString());
        float[] fArray = new float[]{0.0f};
        text.visit((style, string) -> {
            fArray[0] = fArray[0] + Render2D.msdfWidth(NAME_FONT, TargetHudComp.normalize(TargetHudComp.stripCodes(string), bl), f);
            return Optional.empty();
        }, Style.EMPTY);
        return fArray[0];
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void render(DrawContext drawContext) {
        boolean bl;
        boolean bl2;
        LivingEntity livingEntity;
        long l = System.nanoTime();
        float f = Math.min(0.1f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        boolean bl3 = DragSystem.get().isDragModeActive();
        Object object = livingEntity = bl3 ? MinecraftClient.getInstance().player : TargetHudComp.hoveredTarget();
        if (livingEntity != null) {
            this.target = livingEntity;
            this.lastSeenMs = System.currentTimeMillis();
        }
        boolean bl4 = this.target != null && !bl3 && this.target.isInvisible();
        boolean bl5 = this.target != null && !bl4 && (bl3 || System.currentTimeMillis() - this.lastSeenMs <= 1500L);
        boolean bl6 = bl2 = TargetHudComp.componentEnabled() && bl5;
        if (bl2 != this.lastTargetVisible) {
            this.visibility.run(bl2 ? 1.0 : 0.0, bl2 ? 0.18 : 0.12, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = bl2;
        }
        this.visibility.update();
        this.updateFollow(f);
        float f2 = this.visibility.get();
        if (f2 <= 0.01f) {
            if (!bl2) {
                this.target = null;
            }
            return;
        }
        if (this.target == null) {
            return;
        }
        boolean bl7 = bl = Math.abs(this.followOffsetX) > 0.01f || Math.abs(this.followOffsetY) > 0.01f;
        if (bl) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(this.followOffsetX, this.followOffsetY);
        }
        try {
            this.renderContent(drawContext, f, f2);
        }
        finally {
            if (bl) {
                drawContext.getMatrices().popMatrix();
            }
        }
    }

    private void renderNew(DrawContext drawContext, float f, float f2) {
        int n;
        float f3;
        float f4 = this.getX();
        float f5 = this.getY();
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(NEW_SLOTS.length);
        boolean bl = false;
        for (EquipmentSlot equipmentSlot : NEW_SLOTS) {
            ItemStack itemStack = this.target.getEquippedStack(equipmentSlot);
            arrayList.add(itemStack);
            bl |= !itemStack.isEmpty();
        }
        boolean bl2 = TargetHudComp.armorEnabled() && bl;
        float f6 = bl2 ? 6.0f : 10.0f;
        this.armorProgress += ((bl2 ? 1.0f : 0.0f) - this.armorProgress) * (1.0f - (float)Math.exp(-f * f6));
        this.armorProgress = TargetHudComp.clamp01(this.armorProgress);
        float f7 = TargetHudComp.easeInOutCubic(this.armorProgress);
        float f8 = 45.0f - 10.0f * (1.0f - f7);
        float f9 = f5 + f8 - 5.0f - 8.0f;
        float f10 = f4 + 3.0f;
        float f11 = f5 + 3.0f;
        float f12 = f9 - 3.0f - f11;
        float f13 = 26.0f;
        float f14 = f3 = Math.max(4.5f, 6.0f * TargetHudComp.clamp01(f12 / Math.max(f13, 1.0f)));
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule != null && interfaceModule.rectCornerRadius.getFloat() > 6.0f) {
            f14 = Math.min(interfaceModule.rectCornerRadius.getFloat(), f12 * 0.5f);
        }
        this.updateDamagePulse(this.target, f);
        Text text = TargetHudComp.resolveName(this.target);
        float f15 = Math.max(1.0f, this.target.getMaxHealth());
        float f16 = Network.getResolvedHealth(this.target, true);
        float f17 = TargetHudComp.clamp01(f16 / f15);
        this.healthDisplay += (f17 - this.healthDisplay) * (1.0f - (float)Math.exp(-f * 12.0f));
        float f18 = (float)arrayList.size() * 8.0f + (float)(arrayList.size() - 1) * 3.0f;
        float f19 = TargetHudComp.coloredWidth(text, 8.0f);
        float f20 = f10 + f12 + 3.0f;
        float f21 = TargetHudComp.easeOutCubic(TargetHudComp.clamp01(this.armorProgress / 0.6f));
        float f22 = (f18 + 2.0f) * f21;
        float f23 = Math.max(f19, f22);
        this.currentWidth = Math.max(84.0f, f20 - f4 + f23 + 3.0f);
        this.pinCenterOnResize();
        f4 = this.getX();
        f5 = this.getY();
        f9 = f5 + f8 - 5.0f - 8.0f;
        f10 = f4 + 3.0f;
        f11 = f5 + 3.0f;
        f20 = f10 + f12 + 3.0f;
        float f24 = 0.94f + f2 * 0.06f;
        float f25 = f4 + this.currentWidth * 0.5f;
        float f26 = f5 + f8 * 0.5f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f25, f26);
        drawContext.getMatrices().scale(f24);
        drawContext.getMatrices().translate(-f25, -f26);
        Render2D.beginFrame(drawContext);
        RectUtil.drawClientRect(f4, f5, this.currentWidth, f8, 7.0f, f2);
        Render2D.flush();
        Render2D.beginFrame(drawContext);
        float f27 = 4.0f;
        float f28 = 10.0f;
        float f29 = f11 + f12 - f28;
        float f30 = f29 + f28 * 0.5f;
        float f31 = f20 + 1.0f;
        float f32 = f11 + f12 * 0.5f - 4.0f;
        float f33 = (f11 + f29 - 8.0f) * 0.5f;
        float f34 = TargetHudComp.lerp(f32, f33, f7) - 1.5f * (1.0f - f7);
        float f35 = f20;
        TargetHudComp.drawColoredName(text, f35, f34, 8.0f, -1, f2 * 0.85f);
        float f36 = f4 + 3.0f;
        float f37 = this.currentWidth - 6.0f;
        float f38 = TargetHudComp.barRadius(8.0f);
        Render2D.rect(f36, f9, f37, 8.0f, f38, ColorUtil.multAlpha(-14935006, f2 * 0.6f));
        float f39 = f37 * this.healthDisplay;
        TargetHudComp.drawBarFill(f36, f9, f39, f37, 8.0f, f38, this.healthDisplay, f2);
        String string = Math.round(Math.max(0.0f, f16)) + "HP";
        float f40 = Render2D.msdfWidth(NEW_FONT, string, 6.0f);
        float f41 = f36 + (f37 - f40) * 0.5f + 0.75f;
        float f42 = f36 + f39 - f40 - 2.0f;
        float f43 = Math.max(Math.min(f41, f42), f36 + 2.0f);
        float f44 = f9 + 1.0f - 0.3f;
        float f45 = f40 <= 0.0f ? 0.0f : TargetHudComp.clamp01((f36 + f39 - f43) / f40);
        float f46 = TargetHudComp.clamp01((f43 + f40 * 0.5f - f36) / Math.max(f37, 1.0f));
        int n2 = TargetHudComp.barFillColorAt(f46, this.healthDisplay);
        int n3 = ColorUtil.lerpColor(-14935006, n2, f45);
        float f47 = (0.2126f * (float)(n3 >> 16 & 0xFF) + 0.7152f * (float)(n3 >> 8 & 0xFF) + 0.0722f * (float)(n3 & 0xFF)) / 255.0f;
        float f48 = TargetHudComp.clamp01(1.0f - f47 * 1.35f);
        this.hpGrayDisplay += (f48 - this.hpGrayDisplay) * (1.0f - (float)Math.exp(-f * 10.0f));
        int n4 = Math.round(TargetHudComp.clamp01(this.hpGrayDisplay) * 255.0f);
        int n5 = 0xFF000000 | n4 << 16 | n4 << 8 | n4;
        Render2D.msdfText(NEW_FONT, string, f43, f44, 6.0f, ColorUtil.multAlpha(n5, f2));
        float f49 = f4 + this.currentWidth - 3.0f + 1.0f;
        int n6 = Math.max(1, arrayList.size() - 1);
        float f50 = 1.0f - (float)Math.exp(-f * 14.0f);
        float f51 = 1.0f - (float)Math.exp(-f * 30.0f);
        float f52 = f31;
        for (int i = 0; i < arrayList.size(); ++i) {
            float f53 = 0.35f + 0.25f * ((float)i / (float)n6);
            boolean bl3 = f52 - 1.0f + f28 <= f49;
            n = this.armorProgress > f53 && bl3 ? 1 : 0;
            this.slotAppear[i] = TargetHudComp.clamp01(this.slotAppear[i] + ((n != 0 ? 1.0f : 0.0f) - this.slotAppear[i]) * (n != 0 ? f50 : f51));
            f52 += 11.0f;
        }
        if (this.armorProgress > 0.02f) {
            float f54 = f31;
            for (int i = 0; i < arrayList.size(); ++i) {
                float f55 = TargetHudComp.easeOutCubic(this.slotAppear[i]);
                if (f55 > 0.01f) {
                    n = Math.round(36.0f * TargetHudComp.clamp01(f2) * f55) << 24;
                    Render2D.rect(f54 - 1.0f, f29, f28, f28, 2.0f * f55, n);
                }
                f54 += 11.0f;
            }
        }
        this.drawHeadBackground(drawContext, this.target, f10, f11, f12, f3, f14, f2, 1.0f * (1.0f - f7));
        Render2D.flush();
        this.drawHeadForeground(drawContext, this.target, f10, f11, f12, f3, f14, f2);
        Render2D.flush();
        float f56 = 0.5f * TargetHudComp.clamp01(f2);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float f57 = f31;
        for (n = 0; n < arrayList.size(); ++n) {
            float f58;
            ItemStack itemStack = (ItemStack)arrayList.get(n);
            if (!itemStack.isEmpty() && this.armorProgress > 0.02f && (f58 = TargetHudComp.clamp01((this.slotAppear[n] - 0.35f) / 0.65f)) > 0.01f) {
                float f59 = f56 * TargetHudComp.easeOutBack(f58);
                drawContext.getMatrices().pushMatrix();
                Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f)drawContext.getMatrices());
                drawContext.getMatrices().translate(f57 + f27, f30);
                drawContext.getMatrices().scale(f59, f59);
                drawContext.getMatrices().translate(-8.0f, -8.0f);
                drawContext.drawItem(itemStack, 0, 0);
                drawContext.drawStackOverlay(textRenderer, itemStack, 0, 0);
                drawContext.getMatrices().popMatrix();
            }
            f57 += 11.0f;
        }
        drawContext.getMatrices().popMatrix();
    }

    private static float barRadius(float f) {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f2 = interfaceModule == null ? f * 0.5f : interfaceModule.rectCornerRadius.getFloat();
        return Math.max(0.0f, Math.min(f2, f * 0.5f));
    }



    private static TargetHudModule hudModule() {
        return ModuleManager.get().get(TargetHudModule.class);
    }

    private static String stripCodes(String string) {
        String string2 = Formatting.strip((String)string);
        return string2 == null ? "" : string2;
    }

    private static int clamp255(int n) {
        return n < 0 ? 0 : Math.min(n, 255);
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * TargetHudComp.clamp01(f3);
    }

    private static LivingEntity hoveredTarget() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        HitResult hitResult = minecraftClient.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }
        if (!(entityHitResult.getEntity() instanceof LivingEntity livingEntity) || livingEntity == minecraftClient.player) {
            return null;
        }
        if (!livingEntity.isAlive() || livingEntity.isRemoved() || livingEntity.getHealth() <= 0.0f) {
            return null;
        }
        if (livingEntity.isInvisible()) {
            return null;
        }
        return livingEntity;
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        float f;
        this.followProjectionValid = false;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (!TargetHudComp.followEnabled() || this.target == null || minecraftClient.player == null || this.target == minecraftClient.player) {
            return;
        }
        float f2 = worldRenderEvent.getPartialTicks();
        Vec3d vec3d = worldRenderEvent.getCamera() == null ? minecraftClient.gameRenderer.getCamera().getCameraPos() : worldRenderEvent.getCamera().getCameraPos();
        Vec3d vec3d2 = this.target.getLerpedPos(f2);
        this.followScratch.set((float)(vec3d2.x - vec3d.x), (float)(vec3d2.y + (double)((this.target.getHeight() + 0.4f) * 0.5f) - vec3d.y), (float)(vec3d2.z - vec3d.z), 1.0f);
        worldRenderEvent.getPositionMatrix().transform(this.followScratch);
        worldRenderEvent.getProjectionMatrix().transform(this.followScratch);
        if (this.followScratch.w <= 1.0E-4f) {
            return;
        }
        float f3 = (this.followScratch.x / this.followScratch.w * 0.5f + 0.5f) * Position.screenWidth();
        float f4 = (1.0f - (this.followScratch.y / this.followScratch.w * 0.5f + 0.5f)) * Position.screenHeight();
        if (Float.isNaN(f3) || Float.isNaN(f4)) {
            return;
        }
        float f5 = 40.0f;
        this.followScratch.set((float)(vec3d2.x - vec3d.x), (float)(vec3d2.y + (double)this.target.getHeight() + (double)0.4f - vec3d.y), (float)(vec3d2.z - vec3d.z), 1.0f);
        worldRenderEvent.getPositionMatrix().transform(this.followScratch);
        worldRenderEvent.getProjectionMatrix().transform(this.followScratch);
        if (this.followScratch.w > 1.0E-4f && !Float.isNaN(f = (1.0f - (this.followScratch.y / this.followScratch.w * 0.5f + 0.5f)) * Position.screenHeight())) {
            f5 = Math.abs(f4 - f);
        }
        this.followScreenX = f3;
        this.followScreenY = f4;
        this.followHalfH = f5;
        this.followProjectionValid = true;
    }

    private static float easeOutBack(float f) {
        f = TargetHudComp.clamp01(f);
        float f2 = 1.70158f;
        float f3 = f2 + 1.0f;
        float f4 = f - 1.0f;
        return 1.0f + f3 * f4 * f4 * f4 + f2 * f4 * f4;
    }
}

