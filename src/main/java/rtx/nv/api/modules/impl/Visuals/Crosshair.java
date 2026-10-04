package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ButtonSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.modules.settings.impl.TextSetting;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.crosshair.CrosshairEditorScreen;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360Range;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.BuiltRectangle;

public class Crosshair extends Module {
    public static final int GRID = 15;

    public static final String MODE_RING = "Кольцо";
    public static final String MODE_DOT = "Точка";
    public static final String MODE_CROSS = "Крест";
    public static final String MODE_SEGMENTS = "Сегменты";
    public static final String MODE_CUSTOM = "Свой";

    public static final String COLOR_CLIENT = "Клиент";
    public static final String COLOR_CUSTOM = "Свой";
    public static final String COLOR_WHITE = "Белый";

    public static final String PRESET_CUSTOM = "Пользовательский";
    public static final String PRESET_MINIMAL = "Client Минимал";
    public static final String PRESET_CLASSIC = "Классический";
    public static final String PRESET_TACTICAL = "Тактический";
    public static final String PRESET_DOT = "Точка";

    private final SeparatorSetting formSeparator = this.register(new SeparatorSetting("Форма и профили"));
    private final ModeSetting mode = this.register(new ModeSetting(
        "Внешний вид",
        "Основная форма игрового прицела.",
        MODE_RING,
        MODE_RING,
        MODE_DOT,
        MODE_CROSS,
        MODE_SEGMENTS,
        MODE_CUSTOM
    ));
    private final ModeSetting preset = this.register(new ModeSetting(
        "Профиль",
        "Быстрый выбор готового набора настроек прицела.",
        PRESET_MINIMAL,
        PRESET_CUSTOM,
        PRESET_MINIMAL,
        PRESET_CLASSIC,
        PRESET_TACTICAL,
        PRESET_DOT
    ));
    private final ButtonSetting applyPresetBtn = this.register(new ButtonSetting(
        "Применить профиль",
        "Загрузить выбранный готовый набор параметров."
    ).label("Применить").onClick(this::applyCurrentPreset));
    private final ButtonSetting resetBtn = this.register(new ButtonSetting(
        "Сбросить настройки",
        "Возврат к стандартному прицелу NV."
    ).label("Сброс").onClick(this::resetToDefaults));

    private final SeparatorSetting geometrySeparator = this.register(new SeparatorSetting("Геометрия"));
    private final SliderSetting size = this.register(new SliderSetting(
        "Размер",
        "Радиус или длина линий прицела."
    ).range(1.5f, 12.0f).increment(0.5f).setValue(4.5f));
    private final SliderSetting gap = this.register(new SliderSetting(
        "Зазор",
        "Расстояние элементов от центра экрана."
    ).range(0.0f, 8.0f).increment(0.5f).setValue(0.0f));
    private final SliderSetting thickness = this.register(new SliderSetting(
        "Толщина",
        "Толщина линий или кольца прицела."
    ).range(0.5f, 3.5f).increment(0.1f).setValue(1.4f));
    private final BooleanSetting drawDot = this.register(new BooleanSetting(
        "Точка в центре",
        "Отображать компактную точку в центре перекрестия.",
        true
    ));

    private final SeparatorSetting dynamicsSeparator = this.register(new SeparatorSetting("Динамика"));
    private final BooleanSetting dynamicMotion = this.register(new BooleanSetting(
        "Реакция на движение",
        "Плавная инерция прицела при быстром повороте мыши.",
        false
    ));
    private final BooleanSetting dynamicAttack = this.register(new BooleanSetting(
        "Реакция на атаку",
        "Индикация перезарядки оружия и кулдауна удара.",
        true
    ));
    private final BooleanSetting targetReact = this.register(new BooleanSetting(
        "Реакция на цель",
        "Акцентное свечение при наведении на врага.",
        true
    ));

    private final SeparatorSetting styleSeparator = this.register(new SeparatorSetting("Оформление"));
    private final ModeSetting colorMode = this.register(new ModeSetting(
        "Режим цвета",
        "Источник цветовой палитры для прицела.",
        COLOR_CLIENT,
        COLOR_CLIENT,
        COLOR_CUSTOM,
        COLOR_WHITE
    ));
    private final ColorSetting customColor = this.register(new ColorSetting(
        "Свой цвет",
        "Пользовательский цвет линий прицела.",
        new Color(255, 255, 255, 255)
    ).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final BooleanSetting outline = this.register(new BooleanSetting(
        "Контур",
        "Контрастная темная подложка для читаемости на любом фоне.",
        true
    ));
    private final SliderSetting opacity = this.register(new SliderSetting(
        "Прозрачность",
        "Общая прозрачность элементов прицела."
    ).range(0.2f, 1.0f).increment(0.05f).setValue(1.0f));

    private final SeparatorSetting customSeparator = this.register(new SeparatorSetting("Пользовательский прицел").visible(() -> this.mode.is(MODE_CUSTOM)));
    private final ButtonSetting customEditor = this.register(new ButtonSetting(
        "Редактор",
        "Открыть холст для рисования попиксельного прицела."
    ).label("Открыть").onClick(this::openEditor).visible(() -> this.mode.is(MODE_CUSTOM)));
    private final SliderSetting customScale = this.register(new SliderSetting(
        "Масштаб",
        "Размер одного пикселя холста."
    ).range(0.5f, 3.0f).increment(0.5f).setValue(1.0f).visible(() -> this.mode.is(MODE_CUSTOM)));
    private final TextSetting customPixels = this.register(new TextSetting(
        "Пиксели",
        "Бинарная матрица пиксельного прицела."
    ).setText(Crosshair.defaultGrid()).visible(() -> false));

    private float redTarget = 1.0f;
    private float prevYaw;
    private float prevPitch;
    private boolean prevInit;
    private long lastNs = System.nanoTime();

    public Crosshair() {
        super("Crosshair", "Изменяет внешний вид и поведение прицела", Category.VISUALS);
    }

    private void applyCurrentPreset() {
        String p = this.preset.getValue();
        if (PRESET_MINIMAL.equals(p) || "NV Минимал".equals(p)) {
            this.mode.setSelected(MODE_RING);
            this.size.setValue(4.5f);
            this.thickness.setValue(1.4f);
            this.gap.setValue(0.0f);
            this.drawDot.setValue(true);
            this.outline.setValue(true);
            this.dynamicMotion.setValue(false);
            this.dynamicAttack.setValue(true);
            this.targetReact.setValue(true);
            this.colorMode.setSelected(COLOR_CLIENT);
            this.opacity.setValue(1.0f);
        } else if (PRESET_CLASSIC.equals(p)) {
            this.mode.setSelected(MODE_CROSS);
            this.size.setValue(4.0f);
            this.thickness.setValue(1.0f);
            this.gap.setValue(2.0f);
            this.drawDot.setValue(false);
            this.outline.setValue(true);
            this.dynamicMotion.setValue(false);
            this.dynamicAttack.setValue(true);
            this.targetReact.setValue(true);
            this.colorMode.setSelected(COLOR_WHITE);
            this.opacity.setValue(1.0f);
        } else if (PRESET_TACTICAL.equals(p)) {
            this.mode.setSelected(MODE_SEGMENTS);
            this.size.setValue(5.5f);
            this.thickness.setValue(1.2f);
            this.gap.setValue(2.5f);
            this.drawDot.setValue(true);
            this.outline.setValue(true);
            this.dynamicMotion.setValue(false);
            this.dynamicAttack.setValue(true);
            this.targetReact.setValue(true);
            this.colorMode.setSelected(COLOR_CLIENT);
            this.opacity.setValue(0.95f);
        } else if (PRESET_DOT.equals(p)) {
            this.mode.setSelected(MODE_DOT);
            this.size.setValue(2.0f);
            this.thickness.setValue(1.0f);
            this.gap.setValue(0.0f);
            this.drawDot.setValue(true);
            this.outline.setValue(true);
            this.dynamicMotion.setValue(false);
            this.dynamicAttack.setValue(false);
            this.targetReact.setValue(true);
            this.colorMode.setSelected(COLOR_CLIENT);
            this.opacity.setValue(1.0f);
        }
    }

    private void resetToDefaults() {
        this.preset.setSelected(PRESET_MINIMAL);
        this.applyCurrentPreset();
    }

    @Override
    protected void onEnable() {
        this.prevInit = false;
    }

    private void openEditor() {
        CrosshairEditorScreen screen = new CrosshairEditorScreen(this, UI.INSTANCE);
        if (this.mc.currentScreen == UI.INSTANCE) {
            UI.closeInto(screen);
        } else {
            this.mc.setScreen((Screen)screen);
        }
    }

    private int resolveBaseColor() {
        int color;
        if (this.colorMode.is(COLOR_CLIENT)) {
            color = ClientAccent.accentOpaque();
        } else if (this.colorMode.is(COLOR_WHITE)) {
            color = 0xFFFFFFFF;
        } else {
            color = this.customColor.getValue() | 0xFF000000;
        }

        if (this.targetReact.getValue() && this.redTarget > 1.05f) {
            float blend = MathHelper.clamp((this.redTarget - 1.0f) / 4.0f, 0.0f, 1.0f);
            color = ClientAccent.mix(color, 0xFFFF3333, blend);
        }

        return ColorUtil.multAlpha(color, this.opacity.getFloat());
    }

    private static void sharpRect(float x, float y, float w, float h, int color) {
        if (w <= 0.0f || h <= 0.0f) return;
        Render2D.rect(new BuiltRectangle(x, y, w, h, 0.0f, color).withSmoothness(0.0f));
    }

    @EventHandler
    private void onHudRender(HudRenderEvent event) {
        if (!this.isEnabled() || this.mc.player == null || this.mc.world == null) {
            return;
        }
        if (!this.mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        DrawContext context = event.getGraphics();
        float partialTick = event.getPartialTick();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (float)(now - this.lastNs) / 1.0E9f);
        this.lastNs = now;

        float screenW = Position.screenWidth();
        float screenH = Position.screenHeight();
        float cx = screenW * 0.5f;
        float cy = screenH * 0.5f;

        if (this.dynamicMotion.getValue()) {
            float yaw = this.mc.player.getYaw();
            float pitch = this.mc.player.getPitch();
            if (!this.prevInit) {
                this.prevYaw = yaw;
                this.prevPitch = pitch;
                this.prevInit = true;
            }
            float dYaw = (yaw - this.prevYaw) * 0.45f;
            float dPitch = (pitch - this.prevPitch) * 0.45f;
            if (Float.isFinite(dYaw)) cx += dYaw;
            if (Float.isFinite(dPitch)) cy += dPitch;

            float margin = 40.0f;
            cx = MathHelper.clamp(cx, margin, screenW - margin);
            cy = MathHelper.clamp(cy, margin, screenH - margin);

            float speed = 1.0f - (float)Math.exp(-dt * 4.0f);
            this.prevYaw += (yaw - this.prevYaw) * speed;
            this.prevPitch += (pitch - this.prevPitch) * speed;
        } else {
            this.prevInit = false;
        }

        boolean onEntity = this.mc.crosshairTarget instanceof EntityHitResult;
        float targetGoal = onEntity ? 5.0f : 1.0f;
        this.redTarget += (targetGoal - this.redTarget) * Math.min(1.0f, dt * 10.0f);

        Render2D.beginFrame(context);

        int color = this.resolveBaseColor();
        int outlineColor = ColorUtil.rgba(4, 6, 10, Math.round(180.0f * this.opacity.getFloat()));

        String m = this.mode.getValue();
        switch (m) {
            case MODE_RING -> this.renderRing(cx, cy, partialTick, color, outlineColor);
            case MODE_DOT -> this.renderDot(cx, cy, color, outlineColor);
            case MODE_CROSS -> this.renderCross(cx, cy, partialTick, color, outlineColor);
            case MODE_SEGMENTS -> this.renderSegments(cx, cy, partialTick, color, outlineColor);
            case MODE_CUSTOM -> this.renderCustom(cx, cy, color);
            default -> this.renderRing(cx, cy, partialTick, color, outlineColor);
        }

        Render2D.flush();
    }

    private void renderRing(float cx, float cy, float partialTick, int color, int outlineColor) {
        float r = this.size.getFloat();
        float th = this.thickness.getFloat();

        float arc = 360.0f;
        if (this.dynamicAttack.getValue() && this.mc.player != null) {
            float cooldown = this.mc.player.getAttackCooldownProgress(partialTick);
            if (cooldown < 0.999f) {
                arc = 360.0f * Math.max(0.05f, cooldown);
            }
        }

        if (this.outline.getValue()) {
            if (arc >= 359.0f) {
                Render2D.outline360(cx - r, cy - r, r * 2.0f, r * 2.0f, r, th + 1.2f, outlineColor, new Outline360Range[0]);
            } else {
                List<Outline360Range> ranges = List.of(Outline360Range.of(0.0f, arc, outlineColor));
                Render2D.outline360(cx - r, cy - r, r * 2.0f, r * 2.0f, r, th + 1.2f, outlineColor, ranges);
            }
        }

        if (arc >= 359.0f) {
            Render2D.outline360(cx - r, cy - r, r * 2.0f, r * 2.0f, r, th, color, new Outline360Range[0]);
        } else {
            List<Outline360Range> ranges = List.of(Outline360Range.of(0.0f, arc, color));
            Render2D.outline360(cx - r, cy - r, r * 2.0f, r * 2.0f, r, th, color, ranges);
        }

        if (this.drawDot.getValue()) {
            this.renderDot(cx, cy, color, outlineColor);
        }
    }

    private void renderDot(float cx, float cy, int color, int outlineColor) {
        float d = Math.max(1.2f, this.thickness.getFloat());
        float half = d * 0.5f;
        if (this.outline.getValue()) {
            sharpRect(cx - half - 0.75f, cy - half - 0.75f, d + 1.5f, d + 1.5f, outlineColor);
        }
        sharpRect(cx - half, cy - half, d, d, color);
    }

    private void renderCross(float cx, float cy, float partialTick, int color, int outlineColor) {
        float len = this.size.getFloat();
        float th = this.thickness.getFloat();
        float g = this.gap.getFloat();

        if (this.dynamicAttack.getValue() && this.mc.player != null) {
            float cooldown = this.mc.player.getAttackCooldownProgress(partialTick);
            if (cooldown < 0.999f) {
                g += (1.0f - cooldown) * 5.0f;
            }
        }

        float halfTh = th * 0.5f;

        if (this.outline.getValue()) {
            float o = 0.75f;
            sharpRect(cx - halfTh - o, cy - g - len - o, th + o * 2.0f, len + o * 2.0f, outlineColor);
            sharpRect(cx - halfTh - o, cy + g - o, th + o * 2.0f, len + o * 2.0f, outlineColor);
            sharpRect(cx - g - len - o, cy - halfTh - o, len + o * 2.0f, th + o * 2.0f, outlineColor);
            sharpRect(cx + g - o, cy - halfTh - o, len + o * 2.0f, th + o * 2.0f, outlineColor);
        }

        sharpRect(cx - halfTh, cy - g - len, th, len, color);
        sharpRect(cx - halfTh, cy + g, th, len, color);
        sharpRect(cx - g - len, cy - halfTh, len, th, color);
        sharpRect(cx + g, cy - halfTh, len, th, color);

        if (this.drawDot.getValue()) {
            this.renderDot(cx, cy, color, outlineColor);
        }
    }

    private void renderSegments(float cx, float cy, float partialTick, int color, int outlineColor) {
        float r = this.size.getFloat();
        float th = this.thickness.getFloat();
        float gapDeg = MathHelper.clamp(this.gap.getFloat() * 4.0f + 12.0f, 6.0f, 35.0f);

        if (this.dynamicAttack.getValue() && this.mc.player != null) {
            float cooldown = this.mc.player.getAttackCooldownProgress(partialTick);
            if (cooldown < 0.999f) {
                r += (1.0f - cooldown) * 3.0f;
            }
        }

        List<Outline360Range> segRanges = new ArrayList<>(4);
        List<Outline360Range> outlineRanges = new ArrayList<>(4);

        for (int s = 0; s < 4; ++s) {
            float start = (float)s * 90.0f + gapDeg;
            float end = (float)(s + 1) * 90.0f - gapDeg;
            segRanges.add(Outline360Range.of(start, end, color));
            outlineRanges.add(Outline360Range.of(start, end, outlineColor));
        }

        if (this.outline.getValue()) {
            Render2D.outline360(cx - r, cy - r, r * 2.0f, r * 2.0f, r, th + 1.2f, outlineColor, outlineRanges);
        }

        Render2D.outline360(cx - r, cy - r, r * 2.0f, r * 2.0f, r, th, color, segRanges);

        if (this.drawDot.getValue()) {
            this.renderDot(cx, cy, color, outlineColor);
        }
    }

    private void renderCustom(float cx, float cy, int baseColor) {
        boolean[] grid = this.customGrid();
        float scale = this.customScale.getValue();
        this.renderGridStyled(grid, cx, cy, scale, baseColor);
    }

    public void renderGridStyled(boolean[] grid, float cx, float cy, float scale, int baseColor) {
        float totalSize = 15.0f * scale;
        float startX = cx - totalSize * 0.5f;
        float startY = cy - totalSize * 0.5f;

        if (this.outline.getValue()) {
            int dark = ColorUtil.rgba(0, 0, 0, Math.round(200.0f * this.opacity.getFloat()));
            float pad = Math.min(0.5f, scale * 0.5f);
            for (int i = 0; i < grid.length; ++i) {
                if (!grid[i]) continue;
                int row = i / 15;
                int col = i % 15;
                sharpRect(startX + (float)col * scale - pad, startY + (float)row * scale - pad, scale + pad * 2.0f, scale + pad * 2.0f, dark);
            }
        }

        for (int i = 0; i < grid.length; ++i) {
            if (!grid[i]) continue;
            int row = i / 15;
            int col = i % 15;
            sharpRect(startX + (float)col * scale, startY + (float)row * scale, scale, scale, baseColor);
        }
    }

    public boolean[] customGrid() {
        String str = this.customPixels.getText();
        boolean[] grid = new boolean[225];
        if (str == null || str.length() != grid.length) {
            str = Crosshair.defaultGrid();
        }
        for (int i = 0; i < grid.length; ++i) {
            grid[i] = str.charAt(i) == '1';
        }
        return grid;
    }

    public void setCustomGrid(boolean[] grid) {
        StringBuilder sb = new StringBuilder(225);
        for (int i = 0; i < 225; ++i) {
            sb.append((char)(i < grid.length && grid[i] ? '1' : '0'));
        }
        this.customPixels.setText(sb.toString());
    }

    public static String defaultGrid() {
        StringBuilder sb = new StringBuilder(225);
        int center = 7;
        for (int i = 0; i < 225; ++i) {
            int row = i / 15;
            int col = i % 15;
            boolean v = col == center && (row >= center - 5 && row <= center - 2 || row >= center + 2 && row <= center + 5);
            boolean h = row == center && (col >= center - 5 && col <= center - 2 || col >= center + 2 && col <= center + 5);
            boolean dot = row == center && col == center;
            sb.append(v || h || dot ? '1' : '0');
        }
        return sb.toString();
    }
}
