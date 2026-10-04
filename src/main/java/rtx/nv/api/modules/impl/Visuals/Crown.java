package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.storage.friend.FriendUtils;

public class Crown extends Module {
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();
    private static Crown instance;

    private static final String MODE_SELF = "Только себя";
    private static final String MODE_FRIENDS = "Себя и друзей";
    private static final String MODE_ALL = "Всех";
    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private final SeparatorSetting generalSeparator = this.register(new SeparatorSetting("Основное"));
    private final ModeSetting renderMode = this.register(new ModeSetting("Отображать для", "На ком рисовать корону.", MODE_SELF, MODE_SELF, MODE_FRIENDS, MODE_ALL));
    private final NumberSetting crownSize = this.register(new NumberSetting("Размер", "Масштаб короны относительно головы игрока.", 1.0, 0.6, 1.5, 0.05));
    private final NumberSetting crownHeight = this.register(new NumberSetting("Высота", "Смещение короны по высоте над головой.", 0.0, -0.15, 0.25, 0.01));
    private final NumberSetting crownThickness = this.register(new NumberSetting("Толщина", "Толщина обода и кристаллов короны.", 1.0, 0.5, 2.0, 0.05));
    private final BooleanSetting crownGlow = this.register(new BooleanSetting("Свечение", "Яркое кристаллическое свечение короны.", true));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Режим цвета короны.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать второй цвет в режиме Свой.", false).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет короны.", new Color(255, 255, 255, 255)).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет короны.", new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue()));

    public Crown() {
        super("Crown", "Аккуратная кристальная корона на голове игрока.", Category.VISUALS);
        instance = this;
    }

    public static Crown getInstance() {
        Crown crown = ModuleManager.get().get(Crown.class);
        return crown != null ? crown : instance;
    }

    public boolean isGlow() {
        return this.crownGlow.getValue();
    }

    public boolean shouldRender(AbstractClientPlayerEntity player) {
        if (this.mc.player == null || player == null) {
            return false;
        }
        if (this.renderMode.is(MODE_ALL)) {
            return true;
        }
        if (this.renderMode.is(MODE_FRIENDS)) {
            return player == this.mc.player || FriendUtils.isFriend(player.getName().getString());
        }
        return player == this.mc.player;
    }

    public void renderModel(MatrixStack.Entry entry, VertexConsumer vertices, boolean hasHelmet) {
        float size = this.crownSize.getFloat();
        float heightOffset = this.crownHeight.getFloat();
        float thickness = this.crownThickness.getFloat();

        float baseHeadY = hasHelmet ? -0.54f : -0.49f;
        float baseY = baseHeadY - heightOffset;

        float outerR = (hasHelmet ? 0.320f : 0.278f) * size;
        float innerR = outerR - (0.030f * thickness * size);
        float bandH = 0.038f * size;

        int sectors = 16;
        for (int i = 0; i < sectors; i++) {
            float a1 = i * (float) (Math.PI * 2.0 / sectors);
            float a2 = (i + 1) * (float) (Math.PI * 2.0 / sectors);
            float midA = (a1 + a2) * 0.5f;

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);
            float cosM = (float) Math.cos(midA);
            float sinM = (float) Math.sin(midA);

            float ox1 = cos1 * outerR;
            float oz1 = sin1 * outerR;
            float ox2 = cos2 * outerR;
            float oz2 = sin2 * outerR;

            float ix1 = cos1 * innerR;
            float iz1 = sin1 * innerR;
            float ix2 = cos2 * innerR;
            float iz2 = sin2 * innerR;

            float yBot = baseY;
            float yTop = baseY - bandH;

            int cMain = this.resolveColor(i * 22, 1.0f);
            int cShade = this.resolveColor(i * 22 + 25, 0.72f);
            int cPeak = ColorUtil.lerpColor(cMain, 0xFFFFFFFF, 0.45f);

            // 1. Outer rim wall
            putQuad(vertices, entry,
                    ox1, yBot, oz1, cShade,
                    ox2, yBot, oz2, cShade,
                    ox2, yTop, oz2, cMain,
                    ox1, yTop, oz1, cMain);

            // 2. Inner rim wall
            putQuad(vertices, entry,
                    ix2, yBot, iz2, cShade,
                    ix1, yBot, iz1, cShade,
                    ix1, yTop, iz1, cShade,
                    ix2, yTop, iz2, cShade);

            // 3. Bottom rim cap
            putQuad(vertices, entry,
                    ix1, yBot, iz1, cShade,
                    ix2, yBot, iz2, cShade,
                    ox2, yBot, oz2, cShade,
                    ox1, yBot, oz1, cShade);

            // 4. Top rim bevel
            putQuad(vertices, entry,
                    ox1, yTop, oz1, cMain,
                    ox2, yTop, oz2, cMain,
                    ix2, yTop, iz2, cShade,
                    ix1, yTop, iz1, cShade);

            // 5. Crystal teeth (8 distinct faceted peaks on even sectors)
            if (i % 2 == 0) {
                boolean major = (i % 4 == 0);
                float toothHeight = (major ? 0.125f : 0.085f) * size;
                float toothR = (outerR + innerR) * 0.5f;
                float px = cosM * toothR;
                float py = yTop - toothHeight;
                float pz = sinM * toothR;

                // Front facet
                putTriangle(vertices, entry,
                        ox1, yTop, oz1, cMain,
                        ox2, yTop, oz2, cMain,
                        px, py, pz, cPeak);

                // Back facet
                putTriangle(vertices, entry,
                        ix2, yTop, iz2, cShade,
                        ix1, yTop, iz1, cShade,
                        px, py, pz, cPeak);

                // Left facet
                putTriangle(vertices, entry,
                        ix1, yTop, iz1, cShade,
                        ox1, yTop, oz1, cMain,
                        px, py, pz, cPeak);

                // Right facet
                putTriangle(vertices, entry,
                        ox2, yTop, oz2, cMain,
                        ix2, yTop, iz2, cShade,
                        px, py, pz, cPeak);
            }
        }
    }

    private static void putQuad(VertexConsumer consumer, MatrixStack.Entry entry,
                                float x1, float y1, float z1, int c1,
                                float x2, float y2, float z2, int c2,
                                float x3, float y3, float z3, int c3,
                                float x4, float y4, float z4, int c4) {
        consumer.vertex(entry, x1, y1, z1).color(c1);
        consumer.vertex(entry, x2, y2, z2).color(c2);
        consumer.vertex(entry, x3, y3, z3).color(c3);
        consumer.vertex(entry, x4, y4, z4).color(c4);
    }

    private static void putTriangle(VertexConsumer consumer, MatrixStack.Entry entry,
                                    float x1, float y1, float z1, int c1,
                                    float x2, float y2, float z2, int c2,
                                    float x3, float y3, float z3, int c3) {
        consumer.vertex(entry, x1, y1, z1).color(c1);
        consumer.vertex(entry, x2, y2, z2).color(c2);
        consumer.vertex(entry, x3, y3, z3).color(c3);
        consumer.vertex(entry, x3, y3, z3).color(c3);
    }

    private int resolveColor(int angleOffset, float alphaFactor) {
        if (this.colorMode.is(COLOR_CLIENT)) {
            int colorA = ThemeManager.gradientA(255);
            int colorB = ThemeManager.gradientB(255);
            float wave = (float) Math.sin((double) angleOffset * 0.05 + System.currentTimeMillis() * 0.003) * 0.5f + 0.5f;
            int base = ColorUtil.lerpColor(colorA, colorB, wave);
            return ColorUtil.multAlpha(base, alphaFactor);
        }

        int c1 = this.customColor.getColor();
        int c2 = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : c1;
        if (c1 == c2) {
            return ColorUtil.multAlpha(c1, alphaFactor);
        }
        float wave = (float) Math.sin((double) angleOffset * 0.05 + System.currentTimeMillis() * 0.003) * 0.5f + 0.5f;
        return ColorUtil.multAlpha(ColorUtil.lerpColor(c1, c2, wave), alphaFactor);
    }
}
