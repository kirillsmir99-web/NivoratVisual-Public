package rtx.nv.api.modules.impl.Interface;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Arm;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class CustomHotbar extends InterfaceComponentModule {
    private static CustomHotbar instance;

    public final ModeSetting style = this.register(new ModeSetting(
        "Стиль",
        "Стиль оформления хотбара: Квадратный или Стеклянный.",
        "Квадратный",
        rtx.nv.ClientEdition.isTrial() ? new String[]{"Квадратный"} : new String[]{"Квадратный", "Стеклянный"}
    ).visibleWhen(() -> !rtx.nv.ClientEdition.isTrial()));

    public final NumberSetting cornerRadius = this.register(new NumberSetting(
        "Скругление",
        "Радиус скругления углов хотбара и слотов.",
        3.5, 0.0, 12.0, 0.5
    )).visibleWhen(() -> !rtx.nv.ClientEdition.isTrial() && "Стеклянный".equalsIgnoreCase(this.style.getValue()));

    public final ModeSetting animationEasing = this.register(new ModeSetting(
        "Анимация",
        "Тип интерполяции перемещения селектора ячейки.",
        "Пружина",
        rtx.nv.ClientEdition.isTrial() ? new String[]{"Пружина", "Expo"} : new String[]{"Пружина", "Expo", "Плавная", "Линейная"}
    ));

    public final NumberSetting animationSpeed = this.register(new NumberSetting(
        "Скорость",
        "Скорость перемещения селектора ячейки.",
        0.22, 0.05, 0.60, 0.01
    ));

    public final BooleanSetting showNumbers = this.register(new BooleanSetting(
        "Цифры 1:9",
        "Отображать порядковые номера ячеек от 1 до 9.",
        true
    )).visibleWhen(() -> !rtx.nv.ClientEdition.isTrial());

    public final BooleanSetting glow = this.register(new BooleanSetting(
        "Свечение ячейки",
        "Неоновое свечение вокруг выбранной ячейки хотбара.",
        true
    )).visibleWhen(() -> !rtx.nv.ClientEdition.isTrial());

    public final NumberSetting glowIntensity = this.register(new NumberSetting(
        "Сила свечения ячейки",
        "Яркость и радиус свечения активной ячейки хотбара.",
        1.0, 0.2, 2.5, 0.1
    )).visibleWhen(() -> !rtx.nv.ClientEdition.isTrial() && this.glow.getValue());

    public final BooleanSetting slotDividers = this.register(new BooleanSetting(
        "Разделители",
        "Отображать вертикальные разделители между ячейками хотбара.",
        true
    ));

    public final BooleanSetting slotIndicator = this.register(new BooleanSetting(
        "Индикатор",
        "Светящийся акцентный индикатор под активным номером ячейки.",
        true
    ));

    private final SmoothAnimation selection = new SmoothAnimation();
    private boolean selectionInitialized;

    private final SmoothAnimation styleTransition = new SmoothAnimation();
    private boolean styleInitialized;

    public CustomHotbar() {
        super("Custom Hotbar", "Заменяет ванильный хотбар клиентским дизайном с анимациями и номерами.");
        instance = this;
    }

    public static CustomHotbar getInstance() {
        return ModuleManager.get().get(CustomHotbar.class);
    }

    public static boolean isActive() {
        CustomHotbar customHotbar = CustomHotbar.getInstance();
        return customHotbar != null && customHotbar.isEnabled();
    }

    private Easing getEasing() {
        String val = this.animationEasing.getValue();
        if (rtx.nv.ClientEdition.isTrial()) {
            return "Expo".equalsIgnoreCase(val) ? Easings.EXPO_OUT : Easings.BACK_OUT;
        }
        return switch (val) {
            case "Плавная" -> Easings.CUBIC_OUT;
            case "Пружина" -> Easings.BACK_OUT;
            case "Линейная" -> Easings.LINEAR;
            default -> Easings.EXPO_OUT;
        };
    }

    public void render(DrawContext drawContext) {
        if (!this.isEnabled() || drawContext == null || this.mc.player == null) {
            return;
        }
        int selectedSlot = this.mc.player.getInventory().getSelectedSlot();
        if (!this.selectionInitialized) {
            this.selection.set(selectedSlot);
            this.selectionInitialized = true;
        }
        this.selection.run(selectedSlot, this.animationSpeed.getValue(), this.getEasing(), true);
        this.selection.update();

        boolean isTrial = rtx.nv.ClientEdition.isTrial();
        boolean isGlass = !isTrial && this.style.is("Стеклянный");
        if (!this.styleInitialized) {
            this.styleTransition.set(isGlass ? 1.0 : 0.0);
            this.styleInitialized = true;
        }
        this.styleTransition.run(isGlass ? 1.0 : 0.0, 0.35, Easings.CUBIC_OUT, true);
        this.styleTransition.update();
        float styleT = (float) this.styleTransition.get();

        float centerX = (float)drawContext.getScaledWindowWidth() * 0.5f - 1.0f;
        float barY = (float)drawContext.getScaledWindowHeight() - 23.0f;
        float barX = centerX - 91.0f;
        float barW = 182.0f;
        float barH = 24.0f;
        float radius = isGlass ? this.cornerRadius.getFloat() : 0.0f;

        float selSlot = (float) this.selection.get();
        float selCenterX = barX + 1.0f + selSlot * 20.0f + 10.0f;
        float selW = 22.0f;
        float selH = 22.0f;
        float selX = selCenterX - selW * 0.5f;
        float selY = barY + 1.0f;
        float selRadius = Math.min(radius + 0.5f, 6.0f);

        float invScale = 1.0f / Render2DCoordinateSpace.guiIndependentScale();
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(invScale, invScale);

        Render2D.beginFrame(drawContext);

        // Background blending: smoothly cross-fade between Square Matte and Glassmorphic
        if (styleT < 0.99f) {
            float sqAlpha = 1.0f - styleT;
            int bgCol = ColorUtil.multAlpha(0xEB0D1117, sqAlpha);
            int outCol = ColorUtil.multAlpha(0x35FFFFFF, sqAlpha);
            Render2D.rect(barX, barY, barW, barH, radius, bgCol);
            Render2D.outline(barX, barY, barW, barH, radius, 1.0f, outCol);
        }
        if (styleT > 0.01f) {
            RectUtil.drawClientRect(barX, barY, barW, barH, radius, styleT);
            int outCol = ColorUtil.multAlpha(0x40FFFFFF, styleT);
            Render2D.outline(barX, barY, barW, barH, radius, 0.75f, outCol);
        }

        // Slot dividers between all 9 cells
        if (this.slotDividers.getValue()) {
            for (int i = 1; i < 9; i++) {
                float divX = barX + 1.0f + i * 20.0f;
                int divCol = ColorUtil.multAlpha(0x38FFFFFF, 1.0f - styleT * 0.3f);
                Render2D.rect(divX, barY + 3.0f, 1.0f, barH - 6.0f, 0.5f, divCol);
            }
        }

        int[] cornerColors = ClientPalette.cornerColors(0.95f);

        boolean activeGlow = !isTrial && this.glow.getValue();
        // Radiant multi-layer glow around active slot
        if (activeGlow) {
            float gi = this.glowIntensity.getFloat();
            Render2D.glow(new BuiltGlow(selX - 2.0f, selY - 2.0f, selW + 4.0f, selH + 4.0f, new float[]{selRadius, selRadius, selRadius, selRadius}, cornerColors[0], 0.95f * gi, 8.0f * gi, 0.5f * gi));
            Render2D.glow(new BuiltGlow(selX, selY, selW, selH, new float[]{selRadius, selRadius, selRadius, selRadius}, cornerColors[0], 0.7f * gi, 3.5f * gi, 0.65f * gi));
        }

        // Active selector box: juicy, bold and prominent
        int selFill = ColorUtil.multAlpha(0x30FFFFFF, 1.0f - styleT * 0.2f);
        Render2D.rect(selX, selY, selW, selH, selRadius, selFill);
        Render2D.outline(selX, selY, selW, selH, selRadius, 1.5f, cornerColors[0], cornerColors[1], cornerColors[2], cornerColors[3]);

        // Indicator under active slot
        if (this.slotIndicator.getValue()) {
            float dotW = 8.0f;
            float dotH = 2.0f;
            float dotX = selCenterX - dotW * 0.5f;
            float dotY = barY + barH - 2.5f;
            Render2D.rect(dotX, dotY, dotW, dotH, 1.0f, cornerColors[0]);
            if (activeGlow) {
                float gi = this.glowIntensity.getFloat();
                Render2D.glow(new BuiltGlow(dotX - 1.0f, dotY - 1.0f, dotW + 2.0f, dotH + 2.0f, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, cornerColors[0], 0.85f * gi, 4.0f * gi, 0.55f * gi));
            }
        }

        // Offhand stack
        if (!this.mc.player.getOffHandStack().isEmpty()) {
            boolean leftArm = this.mc.player.getMainArm().getOpposite() == Arm.LEFT;
            float offX = leftArm ? centerX - 120.0f : centerX + 97.0f;
            float offY = barY - 1.0f;
            float offSize = 25.0f;
            float offRadius = Math.min(radius, 6.0f);
            if (styleT < 0.99f) {
                float sqAlpha = 1.0f - styleT;
                Render2D.rect(offX, offY, offSize, offSize, offRadius, ColorUtil.multAlpha(0xEB0D1117, sqAlpha));
                int[] offColors = ClientPalette.cornerColors(0.6f * sqAlpha);
                Render2D.outline(offX, offY, offSize, offSize, offRadius, 1.0f, offColors[0], offColors[1], offColors[2], offColors[3]);
            }
            if (styleT > 0.01f) {
                RectUtil.drawClientRect(offX, offY, offSize, offSize, offRadius, styleT);
                int[] offColors = ClientPalette.cornerColors(0.5647f * styleT);
                Render2D.outline(offX, offY, offSize, offSize, offRadius, 0.75f, offColors[0], offColors[1], offColors[2], offColors[3]);
            }
        }

        // Draw numbers 1 to 9 for empty slots with handsome Montserrat Bold font
        if (!isTrial && this.showNumbers.getValue()) {
            for (int i = 0; i < 9; i++) {
                boolean hasItem = !this.mc.player.getInventory().getStack(i).isEmpty();
                if (!hasItem) {
                    String num = String.valueOf(i + 1);
                    float slotCenterX = barX + 1.0f + i * 20.0f + 10.0f;
                    float numW = Fonts.MONTSERRAT_BOLD.width(num, 9.0f);
                    float numX = slotCenterX - numW * 0.5f;
                    float numY = barY + (barH - 9.0f) * 0.5f - 2.5f;
                    float dist = Math.abs(selSlot - (float) i);
                    float selFactor = Math.max(0.0f, Math.min(1.0f, 1.0f - dist * 2.2f));
                    int alpha = (int) (126 + (255 - 126) * selFactor);
                    int col = (alpha << 24) | 0x00FFFFFF;
                    Fonts.MONTSERRAT_BOLD.msdf(num, numX + 0.5f, numY + 0.5f, 9.0f, 0x60000000);
                    Fonts.MONTSERRAT_BOLD.msdf(num, numX, numY, 9.0f, col);
                }
            }
        }

        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }

    public void renderOverlay(DrawContext drawContext) {
        if (!this.isEnabled() || drawContext == null || this.mc.player == null) {
            return;
        }
        if (rtx.nv.ClientEdition.isTrial() || !this.showNumbers.getValue()) {
            return;
        }
        float centerX = (float)drawContext.getScaledWindowWidth() * 0.5f - 1.0f;
        float barY = (float)drawContext.getScaledWindowHeight() - 23.0f;
        float barX = centerX - 91.0f;

        float invScale = 1.0f / Render2DCoordinateSpace.guiIndependentScale();
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(invScale, invScale);

        Render2D.beginFrame(drawContext);

        float selSlot = (float) this.selection.get();
        // Draw numbers 1 to 9 for occupied slots at the top-center with handsome mini badge
        for (int i = 0; i < 9; i++) {
            boolean hasItem = !this.mc.player.getInventory().getStack(i).isEmpty();
            if (hasItem) {
                String num = String.valueOf(i + 1);
                float slotCenterX = barX + 1.0f + i * 20.0f + 10.0f;
                float numW = Fonts.MONTSERRAT_BOLD.width(num, 6.5f);
                float numX = slotCenterX - numW * 0.5f;
                float numY = barY + 1.5f;
                float dist = Math.abs(selSlot - (float) i);
                float selFactor = Math.max(0.0f, Math.min(1.0f, 1.0f - dist * 2.2f));
                int alpha = (int) (176 + (255 - 176) * selFactor);
                int col = (alpha << 24) | 0x00FFFFFF;

                // Subtle micro tag badge behind text for optimal contrast over colorful items
                int badgeAlpha = (int) (0x85 + (0x40 * selFactor));
                Render2D.rect(numX - 2.5f, numY - 0.5f, numW + 5.0f, 7.5f, 2.0f, (badgeAlpha << 24));
                Fonts.MONTSERRAT_BOLD.msdf(num, numX, numY, 6.5f, col);
            }
        }

        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }

    @Override
    protected void onDisable() {
        this.selectionInitialized = false;
        this.styleInitialized = false;
    }
}
