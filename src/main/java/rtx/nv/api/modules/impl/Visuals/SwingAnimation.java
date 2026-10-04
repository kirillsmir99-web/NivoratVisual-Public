package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.ui.UI;
import rtx.nv.mixin.accessor.LivingEntityAccessor;

public class SwingAnimation extends Module {
    private static final float BASE_X = 0.56f;
    private static final float BASE_Y = -0.52f;
    private static final float BASE_Z = -0.72f;

    public static final String MODE_SWING = "Взмах";
    public static final String MODE_ARC = "Дуга";
    public static final String MODE_IMPULSE = "Импульс";
    public static final String MODE_STANDARD = "Стандарт";

    private static final String[] PRESET_MODES = new String[] {
        MODE_SWING, MODE_ARC, MODE_IMPULSE, MODE_STANDARD
    };

    private static SwingAnimation instance;

    private final SeparatorSetting animationSeparator = this.register(new SeparatorSetting("Основная рука"));
    private final ModeSetting swingType = this.register(new ModeSetting("Тип", "Пресет анимации взмаха.", MODE_SWING, PRESET_MODES));
    private final NumberSetting hitStrength = this.register(new NumberSetting("Сила", "Амплитуда взмаха основной руки.", 1.0, 0.2, 3.0, 0.05));
    private final NumberSetting swingSpeed = this.register(new NumberSetting("Длительность", "Множитель длительности взмаха основной руки.", 1.0, 0.5, 4.0, 0.05));
    private final NumberSetting mainTilt = this.register(new NumberSetting("Наклон", "Угол наклона плоскости взмаха.", 0.0, -45.0, 45.0, 1.0));
    private final NumberSetting mainOffsetX = this.register(new NumberSetting("Смещение X", "Смещение по горизонтали.", 0.0, -1.0, 1.0, 0.02));
    private final NumberSetting mainOffsetY = this.register(new NumberSetting("Смещение Y", "Смещение по вертикали.", 0.0, -1.0, 1.0, 0.02));
    private final NumberSetting mainOffsetZ = this.register(new NumberSetting("Смещение Z", "Смещение по глубине.", 0.0, -1.0, 1.0, 0.02));

    private final SeparatorSetting offhandSeparator = this.register(new SeparatorSetting("Вторая рука"));
    private final BooleanSetting offhandEnabled = this.register(new BooleanSetting("Анимация второй руки", "Включает кастомную анимацию для левой руки.", true));
    private final BooleanSetting offhandModeSame = this.register(new BooleanSetting("Стиль как у основной", "Использовать тип анимации основной руки для левой руки.", true).visibleWhen(this.offhandEnabled::getValue));
    private final ModeSetting offhandSwingType = this.register(new ModeSetting("Тип второй руки", "Пресет анимации взмаха левой руки.", MODE_SWING, PRESET_MODES).visibleWhen(() -> this.offhandEnabled.getValue() && !this.offhandModeSame.getValue()));
    private final NumberSetting offhandHitStrength = this.register(new NumberSetting("Амплитуда второй руки", "Амплитуда взмаха левой руки.", 1.0, 0.2, 3.0, 0.05).visibleWhen(this.offhandEnabled::getValue));
    private final NumberSetting offhandSwingSpeed = this.register(new NumberSetting("Длительность второй руки", "Множитель длительности взмаха левой руки.", 1.0, 0.5, 4.0, 0.05).visibleWhen(this.offhandEnabled::getValue));
    private final NumberSetting offhandTilt = this.register(new NumberSetting("Наклон второй руки", "Угол наклона плоскости взмаха левой руки.", 0.0, -45.0, 45.0, 1.0).visibleWhen(this.offhandEnabled::getValue));
    private final NumberSetting offhandOffsetX = this.register(new NumberSetting("Смещение X второй руки", "Смещение левой руки по горизонтали.", 0.0, -1.0, 1.0, 0.02).visibleWhen(this.offhandEnabled::getValue));
    private final NumberSetting offhandOffsetY = this.register(new NumberSetting("Смещение Y второй руки", "Смещение левой руки по вертикали.", 0.0, -1.0, 1.0, 0.02).visibleWhen(this.offhandEnabled::getValue));
    private final NumberSetting offhandOffsetZ = this.register(new NumberSetting("Смещение Z второй руки", "Смещение левой руки по глубине.", 0.0, -1.0, 1.0, 0.02).visibleWhen(this.offhandEnabled::getValue));

    private final SeparatorSetting extraSeparator = this.register(new SeparatorSetting("Дополнительно"));
    private final BooleanSetting autoSwing = this.register(new BooleanSetting("Авто взмах", "Показывать локальный предпросмотр взмаха в настройках каждые две секунды.", false));
    private final BooleanSetting onlyOnTarget = this.register(new BooleanSetting("Только при наводке", "Проигрывать анимацию только когда прицел наведён на существо.", false));
    private final BooleanSetting smoothReattack = this.register(new BooleanSetting("Плавный повторный взмах", "Исключать визуальные скачки при частых повторных атаках.", true));
    private final BooleanSetting smoothItemSwitch = this.register(new BooleanSetting("Плавная смена предмета", "Исключать рывки при переключении слота хотбара.", true));

    private static final HandTracker mainTracker = new HandTracker();
    private static final HandTracker offTracker = new HandTracker();

    private int realismSlashSide = -1;
    private boolean realismSlashReady = true;
    private boolean previewWasOpen;
    private long nextPreviewAt;
    private long previewSwingUntil;

    public SwingAnimation() {
        super("Swing Animation", "Кастомные анимации взмаха от первого лица.", Category.VISUALS);
        instance = this;
    }

    public static SwingAnimation getInstance() {
        SwingAnimation sa = ModuleManager.get().get(SwingAnimation.class);
        return sa != null ? sa : instance;
    }

    @Override
    protected void onDisable() {
        this.previewWasOpen = false;
        this.nextPreviewAt = 0L;
        this.previewSwingUntil = 0L;
        mainTracker.reset();
        offTracker.reset();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPost() || this.mc.player == null) {
            return;
        }
        boolean active = this.autoSwing.getValue() && UI.isSettingsOpenFor(this.getName());
        if (!active) {
            this.previewWasOpen = false;
            this.nextPreviewAt = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (!this.previewWasOpen || now >= this.nextPreviewAt) {
            this.triggerLocalPreview(now);
            this.nextPreviewAt = now + 1000L;
        }
        this.previewWasOpen = true;
    }

    public static boolean applyAnimation(MatrixStack matrixStack, Hand hand, float f) {
        return applyAnimation(matrixStack, hand, f, 1.0f);
    }

    public static boolean applyAnimation(MatrixStack matrixStack, Hand hand, float f, float equipProgress) {
        if (f <= 0.0f || f >= 1.0f) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        SwingAnimation sa = SwingAnimation.getInstance();
        if (sa == null || !sa.isEnabled() || mc.player == null || matrixStack == null) {
            return false;
        }

        if (hand == Hand.OFF_HAND && !sa.offhandEnabled.getValue()) {
            return false;
        }

        String selectedType = (hand == Hand.MAIN_HAND || sa.offhandModeSame.getValue())
            ? sa.swingType.getSelected()
            : sa.offhandSwingType.getSelected();

        if (MODE_STANDARD.equals(selectedType)) {
            return false;
        }

        ItemStack itemStack = mc.player.getStackInHand(hand);
        if (itemStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemStack)) {
            return false;
        }
        if (mc.player.isUsingItem() && mc.player.getActiveHand() == hand) {
            return false;
        }

        if (sa.onlyOnTarget.getValue()) {
            boolean preview = System.currentTimeMillis() < sa.previewSwingUntil;
            boolean targetLiving = mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity;
            if (!preview && !targetLiving) {
                return false;
            }
        }

        Arm mainArm = mc.player.getMainArm();
        Arm handArm = hand == Hand.MAIN_HAND ? mainArm : (mainArm == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT);
        int n = handArm == Arm.RIGHT ? 1 : -1;

        float strength = hand == Hand.MAIN_HAND ? sa.hitStrength.getFloat() : sa.offhandHitStrength.getFloat();
        float speed = hand == Hand.MAIN_HAND ? sa.swingSpeed.getFloat() : sa.offhandSwingSpeed.getFloat();
        float tilt = hand == Hand.MAIN_HAND ? sa.mainTilt.getFloat() : sa.offhandTilt.getFloat();
        float ox = hand == Hand.MAIN_HAND ? sa.mainOffsetX.getFloat() : sa.offhandOffsetX.getFloat();
        float oy = hand == Hand.MAIN_HAND ? sa.mainOffsetY.getFloat() : sa.offhandOffsetY.getFloat();
        float oz = hand == Hand.MAIN_HAND ? sa.mainOffsetZ.getFloat() : sa.offhandOffsetZ.getFloat();

        if (Math.abs(ox) > 0.001f || Math.abs(oy) > 0.001f || Math.abs(oz) > 0.001f) {
            matrixStack.translate((float)n * ox, oy, oz);
        }

        if (Math.abs(tilt) > 0.01f) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)n * tilt));
        }

        HandTracker tracker = hand == Hand.MAIN_HAND ? mainTracker : offTracker;
        float effectiveF = sa.smoothReattack.getValue() ? tracker.smoothProgress(f, true, speed) : f;

        renderPreset(matrixStack, selectedType, n, effectiveF, strength, sa);
        return true;
    }

    private static void renderPreset(MatrixStack matrixStack, String type, int n, float f, float strength, SwingAnimation sa) {
        switch (type) {
            case MODE_ARC: {
                // Wide, fluid horizontal katana swipe across the screen
                float g = MathHelper.sin(MathHelper.sqrt(f) * (float)Math.PI);
                float f2 = MathHelper.sin(f * f * (float)Math.PI);
                float arc = (float)Math.sin(Math.pow(f, 0.75) * Math.PI);

                matrixStack.translate((float)n * (-0.44f * g * strength), 0.06f * (1.0f - f) * g * strength, -0.20f * g * strength);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)n * (45.0f + f2 * -48.0f * strength)));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)n * (arc * 30.0f * strength)));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -58.0f * strength));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)n * -45.0f));
                break;
            }
            case MODE_IMPULSE: {
                // Sharp piercing forward thrust
                float thrust = MathHelper.sin(MathHelper.sqrt(f) * (float)Math.PI);
                float f1 = MathHelper.sin(f * (float)Math.PI);
                float returnCurve = (float)Math.sin(Math.pow(f, 1.6) * Math.PI);

                matrixStack.translate((float)n * (-0.12f * thrust * strength), -0.03f * thrust * strength, -0.32f * f1 * strength);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)n * (45.0f + thrust * -16.0f * strength)));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)n * (returnCurve * -12.0f * strength)));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f1 * -32.0f * strength));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)n * -45.0f));
                break;
            }
            case MODE_SWING:
            default: {
                // Classic 1.7 Punchy PvP Slash
                float g = MathHelper.sin(MathHelper.sqrt(f) * (float)Math.PI);
                float h = MathHelper.sin(MathHelper.sqrt(f) * (float)Math.PI * 2.0f);
                float f1 = MathHelper.sin(f * (float)Math.PI);
                float f2 = MathHelper.sin(f * f * (float)Math.PI);

                matrixStack.translate((float)n * (-0.36f * g * strength), 0.16f * h * strength, -0.16f * f1 * strength);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)n * (45.0f + f2 * -22.0f * strength)));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)n * (g * -22.0f * strength)));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0f * strength));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)n * -45.0f));
                break;
            }
        }
    }

    private void triggerLocalPreview(long now) {
        LivingEntityAccessor accessor = (LivingEntityAccessor)(Object)this.mc.player;
        accessor.nv_setSwingTime(-1);
        accessor.nv_setSwinging(true);
        accessor.nv_setSwingingArm(Hand.MAIN_HAND);
        Integer dur = SwingAnimation.currentSwingDuration();
        this.previewSwingUntil = now + (dur == null ? 400L : (long)dur * 50L + 100L);
    }

    public static Integer currentSwingDuration() {
        MinecraftClient mc = MinecraftClient.getInstance();
        SwingAnimation sa = SwingAnimation.getInstance();
        if (sa == null || !sa.isEnabled() || mc.player == null || sa.swingType.is(MODE_STANDARD)) {
            return null;
        }
        float speed = sa.swingSpeed.getFloat();
        float baseTicks = 6.0f * Math.max(0.3f, speed);
        if (StatusEffectUtil.hasHaste(mc.player)) {
            baseTicks *= (float)(6 - (1 + StatusEffectUtil.getHasteAmplifier(mc.player))) / 6.0f;
        } else if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            baseTicks *= (float)(6 + (1 + mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2) / 6.0f;
        }
        return Math.max(1, (int)Math.ceil(baseTicks));
    }

    private static class HandTracker {
        float lastRaw = 0.0f;
        float blendedProgress = 0.0f;
        float startProgress = 0.0f;
        long restartMs = 0L;
        int lastSelectedSlot = -1;
        float smoothedEquip = 1.0f;
        ItemStack lastHeldItem = ItemStack.EMPTY;

        void reset() {
            this.lastRaw = 0.0f;
            this.blendedProgress = 0.0f;
            this.startProgress = 0.0f;
            this.restartMs = 0L;
            this.lastSelectedSlot = -1;
            this.smoothedEquip = 1.0f;
            this.lastHeldItem = ItemStack.EMPTY;
        }

        float smoothProgress(float raw, boolean smoothReattack, float speedMult) {
            long now = System.currentTimeMillis();
            if (smoothReattack && raw < this.lastRaw - 0.12f && this.lastRaw > 0.08f && this.lastRaw < 0.92f) {
                this.startProgress = this.blendedProgress;
                this.restartMs = now;
            }

            if (this.restartMs > 0L) {
                float elapsed = (now - this.restartMs) / 1000.0f;
                float duration = 0.12f / Math.max(0.5f, speedMult);
                float blend = MathHelper.clamp(elapsed / duration, 0.0f, 1.0f);
                float eased = blend * blend * (3.0f - 2.0f * blend);
                this.blendedProgress = MathHelper.lerp(eased, this.startProgress, raw);
                if (blend >= 1.0f || raw > 0.45f) {
                    this.restartMs = 0L;
                }
            } else {
                this.blendedProgress = raw;
            }

            this.lastRaw = raw;
            return this.blendedProgress;
        }

        float smoothEquip(float rawEquip, boolean smoothSwitch, int currentSlot, ItemStack currentStack) {
            if (!smoothSwitch) {
                return rawEquip;
            }
            if (this.lastSelectedSlot != currentSlot || !ItemStack.areItemsEqual(this.lastHeldItem, currentStack)) {
                this.lastSelectedSlot = currentSlot;
                this.lastHeldItem = currentStack.copy();
            }
            this.smoothedEquip += (rawEquip - this.smoothedEquip) * 0.35f;
            if (Math.abs(rawEquip - this.smoothedEquip) < 0.005f) {
                this.smoothedEquip = rawEquip;
            }
            return this.smoothedEquip;
        }
    }
}
