package rtx.nv.api.modules.impl.Visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.animations.DecelerateValue;

public final class BetterMinecraft extends Module {
    private static final Identifier SATURATION_FULL_SPRITE = Identifier.ofVanilla("hud/food_full");
    private static final Identifier SATURATION_HALF_SPRITE = Identifier.ofVanilla("hud/food_half");

    private final BooleanSetting chatAnimations = this.register(new BooleanSetting("Анимации чата", "Анимирует новые сообщения и открытие поля ввода чата.", true));
    private final SliderSetting chatDuration = this.register(new SliderSetting("Длительность чата", "Время плавного появления сообщений чата в миллисекундах.").range(100, 500).setValue(200).visible(this.chatAnimations::getValue));

    private final BooleanSetting tabAnimation = this.register(new BooleanSetting("Анимация таба", "Плавное появление и закрытие списка игроков от верхнего центра.", true));
    private final SliderSetting tabDuration = this.register(new SliderSetting("Длительность таба", "Время раскрытия списка игроков в миллисекундах.").range(100, 500).setValue(220).visible(this.tabAnimation::getValue));

    private final BooleanSetting inventoryAnimation = this.register(new BooleanSetting("Анимация инвентаря", "Плавное увеличение инвентаря и контейнеров при открытии.", true));
    private final SliderSetting inventoryDuration = this.register(new SliderSetting("Длительность инвентаря", "Время раскрытия экранов в миллисекундах.").range(100, 400).setValue(180).visible(this.inventoryAnimation::getValue));

    private final BooleanSetting itemMoveAnimation = this.register(new BooleanSetting("Перетаскивание предметов", "Предметы плавно перелетают в новый слот при перекладывании во всех экранах.", true));
    private final SliderSetting itemMoveDuration = this.register(new SliderSetting("Длительность перелета", "Время перелета предметов между слотами в миллисекундах.").range(100, 400).setValue(180).visible(this.itemMoveAnimation::getValue));

    private final BooleanSetting reducedIntensity = this.register(new BooleanSetting("Пониженная интенсивность", "Уменьшает амплитуду анимаций для спокойного визуального восприятия.", false));

    private final BooleanSetting hotbarAnimation = this.register(new BooleanSetting("Анимация хотбара", "Плавно перемещает рамку выбранной ячейки влево и вправо.", true));
    private final BooleanSetting hotbarChatLift = this.register(new BooleanSetting("Хотбар при чате", "Плавно поднимает хотбар при открытии чата и опускает при закрытии.", true));
    private final BooleanSetting saturationDisplay = this.register(new BooleanSetting("Отображение насыщенности", "Показывает текущее насыщение над ванильной строкой еды.", true));

    private static final float CHAT_LIFT_PX = 14.0f;
    private static final long HOTBAR_STALE_NANOS = 250000000L;
    private static final DecelerateValue chatLift = new DecelerateValue(260);
    private static final DecelerateValue hotbarSelection = new DecelerateValue(180);
    private static long hotbarSelectionFrameNanos;
    private static long inventoryOpenTime;

    public BetterMinecraft() {
        super("Better Minecraft", "Небольшие визуальные улучшения ванильного рендера.", Category.VISUALS);
    }

    public static boolean chatAnimationsEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.isEnabled() && mod.chatAnimations.getValue();
    }

    public static long chatAnimDurationMs() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null ? (long) mod.chatDuration.getInt() : 200L;
    }

    public static boolean tabAnimationEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.isEnabled() && mod.tabAnimation.getValue();
    }

    public static long tabAnimDurationMs() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null ? (long) mod.tabDuration.getInt() : 220L;
    }

    public static boolean inventoryAnimationEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.isEnabled() && mod.inventoryAnimation.getValue();
    }

    public static long inventoryAnimDurationMs() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null ? (long) mod.inventoryDuration.getInt() : 180L;
    }

    public static float inventoryScale() {
        if (!inventoryAnimationEnabled()) {
            return 1.0f;
        }
        if (inventoryOpenTime == 0L) {
            inventoryOpenTime = System.currentTimeMillis();
        }
        long duration = inventoryAnimDurationMs();
        float progress = Math.min(1.0f, (float)(System.currentTimeMillis() - inventoryOpenTime) / (float)duration);
        if (progress >= 1.0f) {
            return 1.0f;
        }
        float t = 1.0f - progress;
        float ease = 1.0f - t * t * t;
        float startScale = isReducedIntensity() ? 0.95f : 0.92f;
        return startScale + (1.0f - startScale) * ease;
    }

    public static float inventorySlideOffset() {
        if (!inventoryAnimationEnabled()) {
            return 0.0f;
        }
        if (inventoryOpenTime == 0L) {
            return 0.0f;
        }
        long duration = inventoryAnimDurationMs();
        float progress = Math.min(1.0f, (float)(System.currentTimeMillis() - inventoryOpenTime) / (float)duration);
        if (progress >= 1.0f) {
            return 0.0f;
        }
        float t = 1.0f - progress;
        float ease = 1.0f - t * t * t;
        float maxSlide = isReducedIntensity() ? 5.0f : 9.0f;
        return (1.0f - ease) * maxSlide;
    }

    public static void markInventoryOpen() {
        inventoryOpenTime = System.currentTimeMillis();
    }

    public static boolean itemMoveAnimationEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.isEnabled() && mod.itemMoveAnimation.getValue();
    }

    public static long itemMoveAnimDurationMs() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null ? (long) mod.itemMoveDuration.getInt() : 180L;
    }

    public static boolean isReducedIntensity() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.isEnabled() && mod.reducedIntensity.getValue();
    }

    public boolean shouldRenderCapeWaves() {
        return this.isEnabled();
    }

    public boolean shouldAnimateHotbar() {
        return this.isEnabled() && this.hotbarAnimation.getValue();
    }

    public boolean shouldLiftHotbarOnChat() {
        return this.isEnabled() && this.hotbarChatLift.getValue();
    }

    public static boolean hotbarAnimationEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.shouldAnimateHotbar();
    }

    public boolean shouldDisplaySaturation() {
        return this.isEnabled() && this.saturationDisplay.getValue();
    }

    public static boolean hotbarChatLiftEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.shouldLiftHotbarOnChat();
    }

    public static boolean capeWavesEnabled() {
        BetterMinecraft mod = ModuleManager.get().get(BetterMinecraft.class);
        return mod != null && mod.shouldRenderCapeWaves();
    }

    public static int animateHotbarSelectionX(int n) {
        long l = System.nanoTime();
        long l2 = l - hotbarSelectionFrameNanos;
        hotbarSelectionFrameNanos = l;
        if (!BetterMinecraft.hotbarAnimationEnabled() || l2 <= 0L || l2 > 250000000L) {
            hotbarSelection.snap(n);
            return n;
        }
        return Math.round(hotbarSelection.update(n));
    }

    public static void renderSaturation(DrawContext drawContext, PlayerEntity playerEntity, int n, int n2) {
        float f;
        BetterMinecraft betterMinecraft = ModuleManager.get().get(BetterMinecraft.class);
        if (betterMinecraft == null || !betterMinecraft.shouldDisplaySaturation()) {
            return;
        }
        float f2 = playerEntity.getHungerManager().getSaturationLevel();
        int n3 = 9;
        for (int i = 0; i < 10 && !((f = f2 - (float)i * 2.0f) <= 0.0f); ++i) {
            Identifier identifier = f > 1.0f ? SATURATION_FULL_SPRITE : SATURATION_HALF_SPRITE;
            int n4 = n2 - i * 8 - 10 + (10 - n3) / 2;
            drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, n4, n - n3 - 1, n3, n3);
        }
    }

    public static float chatHotbarLiftOffset() {
        boolean bl = BetterMinecraft.hotbarChatLiftEnabled() && MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
        return chatLift.update(bl ? 14.0f : 0.0f);
    }
}
