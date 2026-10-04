package rtx.nv.api.drags.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.KeyStrokesModule;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class KeyStrokesComp extends Draggable {
    private static final float GAP = 3.5f;
    private static final String FONT = "montserrat-bold";
    private static final int KEY_W = 0;
    private static final int KEY_A = 1;
    private static final int KEY_S = 2;
    private static final int KEY_D = 3;
    private static final int KEY_LMB = 4;
    private static final int KEY_RMB = 5;
    private static final int KEY_SPACE = 6;

    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation[] press = new SmoothAnimation[7];

    public KeyStrokesComp() {
        super("keystrokes", 10.0f, 220.0f);
        this.visibility.set(0.0);
        for (int i = 0; i < this.press.length; ++i) {
            this.press[i] = new SmoothAnimation();
            this.press[i].set(0.0);
        }
    }

    private static KeyStrokesModule module() {
        return ModuleManager.get().get(KeyStrokesModule.class);
    }

    @Override
    public String displayName() {
        return "KeyStrokes";
    }

    private float getKeySize() {
        KeyStrokesModule m = module();
        return m != null ? m.keySize.getFloat() : 24.0f;
    }

    @Override
    public float width() {
        float key = this.getKeySize();
        return key * 3.0f + GAP * 2.0f;
    }

    @Override
    public float height() {
        KeyStrokesModule m = module();
        float key = this.getKeySize();
        float mouseH = key * 0.78f;
        float spaceH = key * 0.52f;
        float h = key * 2.0f + GAP;
        if (m == null || m.showMouse.getValue()) {
            h += mouseH + GAP;
        }
        if (m == null || m.showSpace.getValue()) {
            h += spaceH + GAP;
        }
        return h;
    }

    @Override
    public boolean isInteractive() {
        KeyStrokesModule m = module();
        return m != null && m.isEnabled();
    }

    @Override
    public boolean isVisible() {
        KeyStrokesModule m = module();
        return m != null && m.isEnabled();
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        KeyStrokesModule m = module();
        if (m != null) {
            for (rtx.nv.api.modules.settings.Setting s : m.getSettings().all()) {
                Setting ui = SettingsFactory.create(s);
                if (ui != null) {
                    list.add(ui);
                }
            }
        }
        return list;
    }

    private static final class KeyBox {
        final int index;
        final float x, y, w, h;
        final String label;
        final String subLabel;
        final boolean pressed;

        KeyBox(int index, float x, float y, float w, float h, String label, String subLabel, boolean pressed) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
            this.subLabel = subLabel;
            this.pressed = pressed;
        }
    }

    private String lastPreset = "";

    private void syncPreset(KeyStrokesModule m) {
        if (m == null) return;
        String currentPreset = m.preset.getValue();
        if (!currentPreset.equals(this.lastPreset)) {
            this.lastPreset = currentPreset;
            switch (currentPreset) {
                case "Квадратный" -> {
                    m.style.setSelected("Нестеклянный");
                    m.cornerRadius.setValue(0.0f);
                    m.glow.setValue(true);
                    m.keySize.setValue(24.0f);
                    m.showMouse.setValue(true);
                    m.showSpace.setValue(true);
                }
                case "Стеклянный" -> {
                    m.style.setSelected("Стеклянный");
                    m.cornerRadius.setValue(0.0f);
                    m.glow.setValue(true);
                    m.keySize.setValue(24.0f);
                    m.showMouse.setValue(true);
                    m.showSpace.setValue(true);
                }
                case "Скругленный" -> {
                    m.style.setSelected("Стеклянный");
                    m.cornerRadius.setValue(6.0f);
                    m.glow.setValue(true);
                    m.keySize.setValue(24.0f);
                    m.showMouse.setValue(true);
                    m.showSpace.setValue(true);
                }
                case "Компактный" -> {
                    m.style.setSelected("Нестеклянный");
                    m.cornerRadius.setValue(0.0f);
                    m.glow.setValue(true);
                    m.keySize.setValue(20.0f);
                    m.showMouse.setValue(true);
                    m.showSpace.setValue(false);
                }
            }
        }
    }

    @Override
    protected void render(DrawContext drawContext) {
        KeyStrokesModule m = module();
        this.syncPreset(m);
        boolean active = m != null && m.isEnabled();
        this.visibility.run(active ? 1.0 : 0.0, active ? 0.18 : 0.12, Easings.CUBIC_OUT, true);
        this.visibility.update();
        float vis = this.visibility.get();
        if (vis <= 0.01f) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        boolean showMouse = m == null || m.showMouse.getValue();
        boolean showCps = m != null && m.showCps.getValue();
        boolean showSpace = m == null || m.showSpace.getValue();

        float x = this.getX();
        float y = this.getY();
        float key = this.getKeySize();
        float mouseH = key * 0.78f;
        float spaceH = key * 0.52f;
        float radius = m != null ? m.cornerRadius.getFloat() : 4.5f;
        String style = m != null ? m.style.getValue() : "Квадратный";
        boolean glow = m == null || m.glow.getValue();
        float glowIntensity = m != null ? m.glowIntensity.getFloat() : 1.0f;

        List<KeyBox> boxes = new ArrayList<>(7);

        // Row 0: W (centered over S)
        float curY = y;
        boxes.add(new KeyBox(KEY_W, x + key + GAP, curY, key, key, keyLabel(mc.options.forwardKey), null, mc.options.forwardKey.isPressed()));

        // Row 1: A, S, D
        curY += key + GAP;
        boxes.add(new KeyBox(KEY_A, x, curY, key, key, keyLabel(mc.options.leftKey), null, mc.options.leftKey.isPressed()));
        boxes.add(new KeyBox(KEY_S, x + key + GAP, curY, key, key, keyLabel(mc.options.backKey), null, mc.options.backKey.isPressed()));
        boxes.add(new KeyBox(KEY_D, x + (key + GAP) * 2.0f, curY, key, key, keyLabel(mc.options.rightKey), null, mc.options.rightKey.isPressed()));

        // Row 2: LMB, RMB
        if (showMouse) {
            curY += key + GAP;
            float mouseW = (this.width() - GAP) * 0.5f;
            String lmbCps = showCps && m != null ? m.leftCps() + " CPS" : null;
            String rmbCps = showCps && m != null ? m.rightCps() + " CPS" : null;
            boxes.add(new KeyBox(KEY_LMB, x, curY, mouseW, mouseH, "LMB", lmbCps, mc.options.attackKey.isPressed()));
            boxes.add(new KeyBox(KEY_RMB, x + mouseW + GAP, curY, mouseW, mouseH, "RMB", rmbCps, mc.options.useKey.isPressed()));
        }

        // Row 3: SPACE
        if (showSpace) {
            curY += (showMouse ? mouseH : key) + GAP;
            boxes.add(new KeyBox(KEY_SPACE, x, curY, this.width(), spaceH, null, null, mc.options.jumpKey.isPressed()));
        }

        // Update animation progress for all keys
        float[] progress = new float[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            KeyBox box = boxes.get(i);
            SmoothAnimation anim = this.press[box.index];
            anim.run(box.pressed ? 1.0 : 0.0, box.pressed ? 0.08 : 0.12, Easings.CUBIC_OUT, true);
            anim.update();
            progress[i] = anim.get();
        }

        int[] cornerColors = ClientPalette.cornerColors(0.95f * vis);
        int themeCol = cornerColors[0];

        // ══════════════════════════════════════════════════════════════
        // PASS 1: Background tiles, theme glow, and outlines
        // ══════════════════════════════════════════════════════════════
        Render2D.beginFrame(drawContext);
        for (int i = 0; i < boxes.size(); i++) {
            KeyBox box = boxes.get(i);
            float p = progress[i];
            float sink = 0.5f * p;
            float kx = box.x + sink;
            float ky = box.y + sink;
            float kw = box.w - sink * 2.0f;
            float kh = box.h - sink * 2.0f;
            float r = Math.min(radius, Math.min(kw, kh) * 0.5f);

            if ("Стеклянный".equals(style)) {
                if (p > 0.01f) {
                    RectUtil.drawClientRectFixedRadiusNoGlow(kx, ky, kw, kh, r, vis);
                    if (glow) {
                        Render2D.glow(new BuiltGlow(kx, ky, kw, kh, new float[]{r, r, r, r}, themeCol, 0.65f * glowIntensity, 4.0f * glowIntensity, 0.35f * glowIntensity * p * vis));
                    }
                    Render2D.outline(kx, ky, kw, kh, r, 1.25f, themeCol);
                } else {
                    RectUtil.drawClientRectFixedRadiusNoGlow(kx, ky, kw, kh, r, 0.8f * vis);
                    int outCol = ColorUtil.multAlpha(0x30FFFFFF, vis);
                    Render2D.outline(kx, ky, kw, kh, r, 0.75f, outCol);
                }
            } else {
                // Квадратный (острый или скругленный по ползунку)
                int baseCol = ColorUtil.multAlpha(0xCC11151F, vis);
                Render2D.rect(kx, ky, kw, kh, r, baseCol);
                if (p > 0.01f) {
                    int fillCol = ColorUtil.multAlpha(ClientAccent.accent(255.0f), 0.35f * p * vis);
                    Render2D.rect(kx, ky, kw, kh, r, fillCol);
                    Render2D.rect(kx, ky, kw, kh, r, ColorUtil.multAlpha(0x20FFFFFF, p * vis));
                    if (glow) {
                        Render2D.glow(new BuiltGlow(kx, ky, kw, kh, new float[]{r, r, r, r}, themeCol, 0.65f * glowIntensity, 4.0f * glowIntensity, 0.35f * glowIntensity * p * vis));
                    }
                    Render2D.outline(kx, ky, kw, kh, r, 1.25f, themeCol);
                } else {
                    int outCol = ColorUtil.multAlpha(0x2EFFFFFF, vis);
                    Render2D.outline(kx, ky, kw, kh, r, 0.75f, outCol);
                }
            }
        }
        Render2D.flush();

        // ══════════════════════════════════════════════════════════════
        // PASS 2: Typography & icons on top (always crisp, never occluded)
        // ══════════════════════════════════════════════════════════════
        Render2D.beginFrame(drawContext);
        for (int i = 0; i < boxes.size(); i++) {
            KeyBox box = boxes.get(i);
            float p = progress[i];
            float sink = 0.5f * p;
            float kx = box.x + sink;
            float ky = box.y + sink;
            float kw = box.w - sink * 2.0f;
            float kh = box.h - sink * 2.0f;

            if (box.label == null) {
                // Spacebar indicator pill
                float barW = kw * 0.42f;
                float barH = 3.0f;
                float barX = kx + (kw - barW) * 0.5f;
                float barY = ky + (kh - barH) * 0.5f;
                int barCol = ColorUtil.multAlpha(ColorUtil.lerpColor(0x9AFFFFFF, 0xFFFFFFFF, p), vis);
                Render2D.rect(barX + 0.5f, barY + 0.5f, barW, barH, 1.5f, ColorUtil.multAlpha(0x90000000, vis));
                Render2D.rect(barX, barY, barW, barH, 1.5f, barCol);
                continue;
            }

            int textCol = ColorUtil.multAlpha(ColorUtil.lerpColor(0xD8FFFFFF, 0xFFFFFFFF, p), vis);
            int shadowCol = ColorUtil.multAlpha(0xA0000000, vis);

            if (box.subLabel != null) {
                // Dual-line LMB / RMB with CPS counter
                float mainSize = 6.8f;
                float subSize = 5.2f;
                float mainW = Render2D.msdfWidth(FONT, box.label, mainSize);
                float subW = Render2D.msdfWidth(FONT, box.subLabel, subSize);
                float mainX = kx + (kw - mainW) * 0.5f;
                float subX = kx + (kw - subW) * 0.5f;
                float mainY = ky + (kh * 0.38f) - mainSize * 0.5f;
                float subY = ky + (kh * 0.72f) - subSize * 0.5f;
                Render2D.msdfText(FONT, box.label, mainX + 0.5f, mainY + 0.5f, mainSize, shadowCol);
                Render2D.msdfText(FONT, box.label, mainX, mainY, mainSize, textCol);
                int subCol = ColorUtil.multAlpha(ColorUtil.lerpColor(0x90FFFFFF, 0xEEFFFFFF, p), vis);
                Render2D.msdfText(FONT, box.subLabel, subX + 0.5f, subY + 0.5f, subSize, shadowCol);
                Render2D.msdfText(FONT, box.subLabel, subX, subY, subSize, subCol);
            } else {
                // Single centered text (W, A, S, D or LMB/RMB without CPS)
                float fontSize = (box.index >= KEY_LMB) ? 7.5f : (key * 0.44f);
                float tw = Render2D.msdfWidth(FONT, box.label, fontSize);
                float tx = kx + (kw - tw) * 0.5f;
                float ty = ky + (kh - fontSize) * 0.5f - fontSize * 0.08f;
                Render2D.msdfText(FONT, box.label, tx + 0.5f, ty + 0.5f, fontSize, shadowCol);
                Render2D.msdfText(FONT, box.label, tx, ty, fontSize, textCol);
            }
        }
        Render2D.flush();
    }

    private static String keyLabel(KeyBinding keyBinding) {
        String key = keyBinding.getBoundKeyTranslationKey();
        if (key == null || key.isBlank()) {
            return "?";
        }
        if (key.startsWith("key.keyboard.")) {
            key = key.substring("key.keyboard.".length());
        } else if (key.startsWith("key.mouse.")) {
            key = "M" + key.substring("key.mouse.".length());
        }
        return key.replace('.', ' ').toUpperCase(Locale.ROOT);
    }
}
