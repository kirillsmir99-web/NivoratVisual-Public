package rtx.nv.api.drags.components;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Interface.WatermarkModule;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.discord.rpc.DiscordRPCManager;
import rtx.nv.utils.network.Network;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class WatermarkComp extends Draggable {
    private static final float H = 20.0f;
    private static final String INFO_FONT = "montserrat-medium";
    private static final String FALLBACK_NAME = "Player";
    private static final DateTimeFormatter TIME_FORMAT_24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation animatedWidth = new SmoothAnimation();
    private final SmoothAnimation serverAnim = new SmoothAnimation();
    private final SmoothAnimation fpsAnim = new SmoothAnimation();
    private final SmoothAnimation nickAnim = new SmoothAnimation();
    private final SmoothAnimation timeAnim = new SmoothAnimation();
    private final SmoothAnimation combatAnim = new SmoothAnimation();
    private boolean widthInitialized;
    private boolean lastTargetVisible;
    private float currentWidth = 80.0f;

    private String cachedServer = "";
    private float serverWidth;
    private String cachedFps = "";
    private float fpsWidth;
    private String cachedName = "";
    private float nameWidth;
    private String timeText = "";
    private float timeWidth;
    private String cachedCombat = "";
    private float combatWidth;
    private boolean centered = true;

    public WatermarkComp() {
        super("watermark", (Position.screenWidth() - 100.0f) * 0.5f, 6.0f);
        this.centered = true;
        this.visibility.set(1.0);
        this.serverAnim.set(1.0);
        this.fpsAnim.set(1.0);
        this.nickAnim.set(1.0);
        this.timeAnim.set(1.0);
        this.combatAnim.set(0.0);
    }

    public boolean isCentered() {
        return this.centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    @Override
    public void resetToDefault() {
        this.centered = true;
        float defaultX = (Position.screenWidth() - this.width()) * 0.5f;
        float defaultY = 6.0f;
        this.getDrag().setTargetX(defaultX);
        this.getDrag().setTargetY(defaultY);
        this.getDrag().syncToTarget();
    }

    @Override
    public String displayName() {
        return "Watermark";
    }

    public float getScale() {
        WatermarkModule mod = ModuleManager.get().get(WatermarkModule.class);
        return mod != null ? mod.scale() : 1.0f;
    }

    @Override
    public float width() {
        float w = this.currentWidth > 0.0f ? this.currentWidth : this.computeDesiredWidth();
        return w * this.getScale();
    }

    @Override
    public float height() {
        return H * this.getScale();
    }

    @Override
    public float getX() {
        if (rtx.nv.ClientEdition.isTrial() || this.centered) {
            return (Position.screenWidth() - this.width()) * 0.5f;
        }
        return super.getX();
    }

    @Override
    public float getY() {
        if (rtx.nv.ClientEdition.isTrial()) {
            return 6.0f;
        }
        return super.getY();
    }

    @Override
    public boolean isInteractive() {
        if (rtx.nv.ClientEdition.isTrial()) {
            return false;
        }
        return this.shouldShow();
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        if (watermarkModule != null) {
            for (rtx.nv.api.modules.settings.Setting s : watermarkModule.getSettings().all()) {
                Setting ui = SettingsFactory.create(s);
                if (ui != null) {
                    list.add(ui);
                }
            }
        }
        return list;
    }

    private static int indexedPaletteColor(int[] palette, float phase) {
        int n = palette.length;
        if (n <= 1) {
            return palette[0];
        }
        float f2 = WatermarkComp.normalizedCycle(phase) / 360.0f;
        float f3 = f2 < 0.5f ? f2 * 2.0f : (1.0f - f2) * 2.0f;
        float f4 = f3 * (float)(n - 1);
        int n2 = (int)f4;
        if (n2 > n - 2) {
            n2 = n - 2;
        }
        return ColorUtil.lerpColor(palette[n2], palette[n2 + 1], f4 - (float)n2);
    }

    private static int[] watermarkPalette(InterfaceModule interfaceModule) {
        int[] nArray = ClientAccent.currentPalette();
        if (nArray == null || nArray.length == 0) {
            int n = ColorUtil.lerpColor(-2234369, -1, 0.18f);
            return new int[]{n, ColorUtil.lerpColor(n, -1, 0.46f)};
        }
        int[] nArray2 = new int[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            nArray2[i] = ColorUtil.lerpColor(WatermarkComp.opaque(nArray[i]), -1, 0.18f);
        }
        return nArray2;
    }

    private static float indexedGradientPhase(WatermarkModule mod) {
        if (mod != null && !mod.chromaWave.getValue()) {
            return 0.0f;
        }
        float speed = mod != null ? mod.waveSpeed.getFloat() : 1.2f;
        long period = Math.max(100L, (long)(1200.0f / Math.max(0.1f, speed)));
        return (float)(System.currentTimeMillis() % period) / (float)period * 360.0f;
    }

    private static float normalizedCycle(float f) {
        float f2 = f % 360.0f;
        return f2 < 0.0f ? f2 + 360.0f : f2;
    }

    private String serverName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isInSingleplayer() || mc.isIntegratedServerRunning()) {
            return rtx.nv.api.localization.Lang.get("watermark.singleplayer", "Одиночная игра");
        }
        String name = DiscordRPCManager.detectServerName();
        if (name == null || name.isBlank() || "В игре".equals(name) || "В главном меню".equals(name)) {
            if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
                return mc.getCurrentServerEntry().address;
            }
            return rtx.nv.api.localization.Lang.get("watermark.online", "Онлайн");
        }
        return name;
    }

    private String fpsPingText() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int fps = mc.getCurrentFps();
        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }
        if (ping <= 0) {
            ping = Network.getRealTimePing();
        }
        if (ping > 0) {
            return fps + " FPS • " + ping + "ms";
        }
        return fps + " FPS";
    }

    private void updateInfoCache() {
        WatermarkModule mod = ModuleManager.get().get(WatermarkModule.class);
        boolean showServer = mod != null && mod.showServer.getValue();
        boolean showFps = mod != null && mod.showFps.getValue();
        boolean showNick = mod == null || mod.showNick.getValue();
        boolean showTime = mod == null || mod.showTime.getValue();

        if (showServer) {
            String s = this.serverName();
            if (!s.equals(this.cachedServer) || this.serverWidth <= 0.0f) {
                this.cachedServer = s;
                this.serverWidth = WatermarkComp.measureString(INFO_FONT, s, 8.0f);
            }
        }
        if (showFps) {
            String f = this.fpsPingText();
            if (!f.equals(this.cachedFps) || this.fpsWidth <= 0.0f) {
                this.cachedFps = f;
                this.fpsWidth = WatermarkComp.measureString(INFO_FONT, f, 8.0f);
            }
        }
        if (showNick) {
            String n = WatermarkComp.playerName();
            if (!n.equals(this.cachedName) || this.nameWidth <= 0.0f) {
                this.cachedName = n;
                this.nameWidth = WatermarkComp.measureString(INFO_FONT, n, 8.0f);
            }
        }
        if (showTime) {
            boolean is12 = mod != null && "12 часов".equals(mod.timeFormat.getValue());
            DateTimeFormatter fmt = is12 ? TIME_FORMAT_12 : TIME_FORMAT_24;
            String t = LocalTime.now(java.time.ZoneId.systemDefault()).format(fmt);
            if (!t.equals(this.timeText) || this.timeWidth <= 0.0f) {
                this.timeText = t;
                this.timeWidth = WatermarkComp.measureString(INFO_FONT, t, 8.0f);
            }
        }
        boolean showCombat = mod != null && mod.showCombatTag.getValue() && rtx.nv.utils.combat.CombatTagTracker.isInCombat();
        if (showCombat) {
            String c = rtx.nv.utils.combat.CombatTagTracker.getFormattedCombatTag();
            if (!c.equals(this.cachedCombat) || this.combatWidth <= 0.0f) {
                this.cachedCombat = c;
                this.combatWidth = WatermarkComp.measureString(INFO_FONT, c, 8.0f);
            }
        } else {
            this.cachedCombat = "";
            this.combatWidth = 0.0f;
        }
    }

    private static float measureString(String font, String text, float size) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        float w = 0.0f;
        int codeLength;
        for (int i = 0; i < text.length(); i += codeLength) {
            int cp = text.codePointAt(i);
            codeLength = Character.charCount(cp);
            String glyph = text.substring(i, i + codeLength);
            w += Render2D.msdfWidth(font, glyph, size);
        }
        return w;
    }

    private static float drawGradientString(String font, String text, float x, float y, float size, int startIndex, int[] palette, float alpha, float phase, boolean isMinimal) {
        float effAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (effAlpha <= 0.003921569f || text == null || text.isEmpty()) {
            return 0.0f;
        }
        float curX = x;
        int idx = startIndex;
        int codeLength;
        for (int i = 0; i < text.length(); i += codeLength) {
            int cp = text.codePointAt(i);
            codeLength = Character.charCount(cp);
            String glyph = text.substring(i, i + codeLength);
            int shadowCol = ColorUtil.multAlpha(0xFF000000, effAlpha * (isMinimal ? 0.8f : 0.45f));
            Render2D.msdfText(font, glyph, curX + 0.5f, y + 0.5f, size, shadowCol);
            int color = ColorUtil.multAlpha(WatermarkComp.indexedPaletteColor(palette, phase + (float)idx * 15.0f), effAlpha);
            Render2D.msdfText(font, glyph, curX, y, size, color);
            curX += Render2D.msdfWidth(font, glyph, size);
            ++idx;
        }
        return curX - x;
    }

    private static void drawVerticalSeparator(float x, float centerY, float alpha) {
        float h = 8.5f;
        float y = centerY - h * 0.5f;
        int sepColor = ColorUtil.multAlpha(0xFFFFFFFF, alpha * 0.22f);
        Render2D.rect(x, y, 1.0f, h, 0.5f, sepColor);
    }

    private void updateChipAnimations(boolean showServer, boolean showFps, boolean showNick, boolean showTime, boolean showCombat) {
        updateSingleChipAnim(this.serverAnim, showServer);
        updateSingleChipAnim(this.fpsAnim, showFps);
        updateSingleChipAnim(this.nickAnim, showNick);
        updateSingleChipAnim(this.timeAnim, showTime);
        updateSingleChipAnim(this.combatAnim, showCombat);
    }

    private static void updateSingleChipAnim(SmoothAnimation anim, boolean target) {
        double targetVal = target ? 1.0 : 0.0;
        if (Math.abs(anim.getToValue() - targetVal) > 0.01) {
            anim.run(targetVal, target ? 0.22 : 0.18, Easings.CUBIC_OUT, false);
        }
        anim.update();
    }

    public float computeDesiredWidth() {
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        this.updateInfoCache();

        boolean showServer = watermarkModule != null && watermarkModule.showServer.getValue() && !this.cachedServer.isEmpty();
        boolean showFps = watermarkModule != null && watermarkModule.showFps.getValue() && !this.cachedFps.isEmpty();
        boolean showNick = watermarkModule == null || watermarkModule.showNick.getValue() && !this.cachedName.isEmpty();
        boolean showTime = watermarkModule == null || watermarkModule.showTime.getValue() && !this.timeText.isEmpty();
        boolean showCombat = watermarkModule != null && watermarkModule.showCombatTag.getValue() && rtx.nv.utils.combat.CombatTagTracker.isInCombat() && !this.cachedCombat.isEmpty();

        this.updateChipAnimations(showServer, showFps, showNick, showTime, showCombat);

        float padX = 12.5f;
        float sepGap = 13.0f;
        float totalW = padX + 11.0f; // pad + logo

        float sF = (float) this.serverAnim.get();
        if (sF > 0.005f) totalW += (sepGap + this.serverWidth) * sF;

        float fF = (float) this.fpsAnim.get();
        if (fF > 0.005f) totalW += (sepGap + this.fpsWidth) * fF;

        float nF = (float) this.nickAnim.get();
        if (nF > 0.005f) totalW += (sepGap + this.nameWidth) * nF;

        float tF = (float) this.timeAnim.get();
        if (tF > 0.005f) totalW += (sepGap + this.timeWidth) * tF;

        float cF = (float) this.combatAnim.get();
        if (cF > 0.005f) totalW += (sepGap + this.combatWidth) * cF;

        totalW += padX;
        return Math.max(36.0f, totalW);
    }

    @Override
    protected void render(DrawContext drawContext) {
        boolean bl = this.shouldShow();
        if (bl != this.lastTargetVisible) {
            this.visibility.run(bl ? 1.0 : 0.0, bl ? 0.18 : 0.12, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = bl;
        }
        this.visibility.update();
        float f = this.visibility.get();
        if (bl && f <= 0.01f) {
            f = 0.01f;
        }
        if (f <= 0.01f && !bl) {
            return;
        }

        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        float targetW = this.computeDesiredWidth();
        if (!this.widthInitialized || this.currentWidth <= 0.0f) {
            this.animatedWidth.set(targetW);
            this.currentWidth = targetW;
            this.widthInitialized = true;
        } else {
            if (this.currentWidth < 60.0f && targetW > 100.0f) {
                this.animatedWidth.set(targetW);
                this.currentWidth = targetW;
            } else if (Math.abs(this.animatedWidth.getToValue() - (double) targetW) > 0.5) {
                this.animatedWidth.run(targetW, 0.22, Easings.CUBIC_OUT, false);
            }
        }
        this.animatedWidth.update();
        this.currentWidth = (float) this.animatedWidth.get();

        float padX = 12.5f;
        float sepGap = 13.0f;
        float screenW = Position.screenWidth();

        // Calculate absolute required width for all visible chips so capsule is NEVER smaller than text
        float actualChipsW = padX + 11.0f;
        float sF = (float) this.serverAnim.get();
        if (sF > 0.005f && !this.cachedServer.isEmpty()) actualChipsW += (sepGap + this.serverWidth) * sF;
        float fF = (float) this.fpsAnim.get();
        if (fF > 0.005f && !this.cachedFps.isEmpty()) actualChipsW += (sepGap + this.fpsWidth) * fF;
        float nF = (float) this.nickAnim.get();
        if (nF > 0.005f && !this.cachedName.isEmpty()) actualChipsW += (sepGap + this.nameWidth) * nF;
        float tF = (float) this.timeAnim.get();
        if (tF > 0.005f && !this.timeText.isEmpty()) actualChipsW += (sepGap + this.timeWidth) * tF;
        float cF = (float) this.combatAnim.get();
        if (cF > 0.005f && !this.cachedCombat.isEmpty()) actualChipsW += (sepGap + this.combatWidth) * cF;
        actualChipsW += padX;

        if (this.currentWidth < actualChipsW) {
            this.currentWidth = actualChipsW;
            this.animatedWidth.set(actualChipsW);
        }

        float originX;
        float originY;
        if (rtx.nv.ClientEdition.isTrial()) {
            this.centered = true;
            originX = (screenW - this.currentWidth) * 0.5f;
            originY = 6.0f;
            this.getDrag().setTargetX(originX);
            this.getDrag().setTargetY(originY);
            this.getDrag().syncToTarget();
        } else {
            if (this.getDrag().isDragging()) {
                float curCenterX = this.getDrag().getTargetX() + this.currentWidth * 0.5f;
                if (Math.abs(curCenterX - screenW * 0.5f) <= 14.0f) {
                    this.centered = true;
                    this.getDrag().setTargetX((screenW - this.currentWidth) * 0.5f);
                    this.getDrag().setSnapLineX(screenW * 0.5f);
                } else {
                    this.centered = false;
                }
            }

            if (this.centered) {
                originX = (screenW - this.currentWidth) * 0.5f;
                originY = this.getY();
                this.getDrag().setTargetX(originX);
                if (!this.getDrag().isDragging()) {
                    this.getDrag().syncToTarget();
                }
            } else {
                originX = this.getX();
                originY = this.getY();
            }
        }

        // Ensure originX is always within screen bounds and never clipped on the right
        originX = Math.max(0.0f, Math.min(screenW - this.currentWidth, originX));

        String style = watermarkModule != null ? watermarkModule.style.getValue() : "Капсула";
        if (style == null || style.isBlank() || "Пузырь".equalsIgnoreCase(style)) {
            style = "Капсула";
        }
        boolean isBubble = false;
        boolean isPill = !"Минимализм".equalsIgnoreCase(style);
        boolean isMinimal = "Минимализм".equalsIgnoreCase(style);

        boolean inCombat = rtx.nv.utils.combat.CombatTagTracker.isInCombat();
        boolean hasCombatGlow = inCombat && watermarkModule != null && watermarkModule.combatGlow.getValue();
        float combatPulse = hasCombatGlow ? (float)(0.60f + 0.40f * Math.sin(System.currentTimeMillis() * 0.007)) : 0.0f;

        float s = this.getScale();
        float animScale = 0.92f + f * 0.08f;
        float totalScale = s * animScale;
        float centerX = originX + this.currentWidth * 0.5f;
        float centerY = originY + 10.0f;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(centerX, centerY);
        drawContext.getMatrices().scale(totalScale, totalScale);
        drawContext.getMatrices().translate(-centerX, -centerY);

        Render2D.beginFrame(drawContext);

        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        int[] palette = WatermarkComp.watermarkPalette(interfaceModule);
        float phase = WatermarkComp.indexedGradientPhase(watermarkModule);

        float capsuleW = Math.max(this.currentWidth, actualChipsW);

        // Background styling: clean glass capsule without waveEdge distortion
        if (isBubble) {
            int bubbleBg = ColorUtil.rgba(18, 22, 34, (int)(160 * f));
            Render2D.rect(originX, originY, capsuleW, 20.0f, 10.0f, bubbleBg);
            WatermarkComp.drawWatermarkGlass(originX, originY, capsuleW, 20.0f, 10.0f, f, true);

            int bubbleBorder = ColorUtil.rgba(220, 240, 255, (int)(65 * f));
            if (hasCombatGlow) {
                bubbleBorder = ColorUtil.lerpColor(bubbleBorder, 0xFFFF3344, combatPulse * f);
            }
            Render2D.outline(originX, originY, capsuleW, 20.0f, 10.0f, 0.85f, bubbleBorder);

            if (hasCombatGlow) {
                float[] radii = new float[]{10.0f, 10.0f, 10.0f, 10.0f};
                Render2D.glow(new BuiltGlow(originX, originY, capsuleW, 20.0f, radii, 0xFFFF2A3D, 0.55f * combatPulse, 8.0f, f));
            } else {
                RectUtil.drawClientGlowOnly(originX, originY, capsuleW, 20.0f, 10.0f, f * 0.85f);
            }
        } else if (isPill) {
            int bgCol = ColorUtil.rgba(12, 14, 22, (int)(200 * f));
            Render2D.rect(originX, originY, capsuleW, 20.0f, 10.0f, bgCol);
            WatermarkComp.drawWatermarkGlass(originX, originY, capsuleW, 20.0f, 10.0f, f, false);

            int borderCol = ColorUtil.rgba(255, 255, 255, (int)(75 * f));
            if (hasCombatGlow) {
                borderCol = ColorUtil.lerpColor(borderCol, 0xFFFF3344, combatPulse * f);
            }
            Render2D.outline(originX, originY, capsuleW, 20.0f, 10.0f, 0.9f, borderCol);
            if (hasCombatGlow) {
                float[] radii = new float[]{10.0f, 10.0f, 10.0f, 10.0f};
                Render2D.glow(new BuiltGlow(originX, originY, capsuleW, 20.0f, radii, 0xFFFF2A3D, 0.55f * combatPulse, 8.0f, f));
            } else {
                RectUtil.drawClientGlowOnly(originX, originY, capsuleW, 20.0f, 10.0f, f * 0.90f);
            }
        } else if (hasCombatGlow) {
            float[] radii = new float[]{10.0f, 10.0f, 10.0f, 10.0f};
            Render2D.glow(new BuiltGlow(originX, originY, capsuleW, 20.0f, radii, 0xFFFF2A3D, 0.35f * combatPulse, 6.0f, f));
        }

        // Layout items
        float curX = originX + padX;
        float textY = originY + (20.0f - 8.5f) * 0.5f - 0.5f;
        float middleY = originY + 10.0f;
        int glyphIdx = 1;

        // 1. Logo (Natural pure white color, never tinted by themes)
        float logoY = originY + (20.0f - 11.0f) * 0.5f;
        rtx.nv.api.ui.BrandMark.draw(curX, logoY, 11.0f, f);
        curX += 11.0f;

        // 2. Server Chip
        sF = (float) this.serverAnim.get();
        if (sF > 0.01f && !this.cachedServer.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, sF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * sF;
            WatermarkComp.drawGradientString(INFO_FONT, this.cachedServer, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.serverWidth * sF;
            glyphIdx += Math.round(this.serverWidth / 8.0f) + 1;
        }

        // 3. FPS Chip
        fF = (float) this.fpsAnim.get();
        if (fF > 0.01f && !this.cachedFps.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, fF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * fF;
            WatermarkComp.drawGradientString(INFO_FONT, this.cachedFps, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.fpsWidth * fF;
            glyphIdx += Math.round(this.fpsWidth / 8.0f) + 1;
        }

        // 4. Nick Chip
        nF = (float) this.nickAnim.get();
        if (nF > 0.01f && !this.cachedName.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, nF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * nF;
            WatermarkComp.drawGradientString(INFO_FONT, this.cachedName, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.nameWidth * nF;
            glyphIdx += Math.round(this.nameWidth / 8.0f) + 1;
        }

        // 5. Time Chip
        tF = (float) this.timeAnim.get();
        if (tF > 0.01f && !this.timeText.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, tF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * tF;
            WatermarkComp.drawGradientString(INFO_FONT, this.timeText, curX, textY + 0.5f, 8.0f, glyphIdx, palette, chipAlpha, phase, isMinimal);
            curX += this.timeWidth * tF;
            glyphIdx += Math.round(this.timeWidth / 8.0f) + 1;
        }

        // 6. Combat Tag Chip
        cF = (float) this.combatAnim.get();
        if (cF > 0.01f && !this.cachedCombat.isEmpty()) {
            float chipAlpha = f * Math.min(1.0f, cF * 1.25f);
            drawVerticalSeparator(curX + sepGap * 0.5f - 0.5f, middleY, chipAlpha);
            curX += sepGap * cF;
            int[] combatPalette = new int[]{0xFFFF3B4E, 0xFFFF7B94};
            WatermarkComp.drawGradientString(INFO_FONT, this.cachedCombat, curX, textY + 0.5f, 8.0f, glyphIdx, combatPalette, chipAlpha, phase, isMinimal);
            curX += this.combatWidth * cF;
        }

        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }

    private static String playerName() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.getSession() != null && minecraftClient.getSession().getUsername() != null && !minecraftClient.getSession().getUsername().isBlank()) {
            return minecraftClient.getSession().getUsername();
        }
        if (minecraftClient.player != null && minecraftClient.player.getGameProfile().name() != null && !minecraftClient.player.getGameProfile().name().isBlank()) {
            return minecraftClient.player.getGameProfile().name();
        }
        return FALLBACK_NAME;
    }

    private boolean shouldShow() {
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        if (watermarkModule == null || !watermarkModule.isEnabled()) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (watermarkModule.hideInF3.getValue() && mc.getDebugHud() != null && mc.getDebugHud().shouldShowDebugHud()) {
            return false;
        }
        return true;
    }

    private static void drawWatermarkGlass(float x, float y, float w, float h, float radius, float alpha, boolean bubble) {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float effAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        float r = Math.max(0.0f, Math.min(radius, Math.min(w, h) * 0.5f));
        float blur = rtx.nv.api.ui.theme.ThemeManager.blurRadius(interfaceModule == null ? 18.0f : interfaceModule.rectBackdropBlur.getFloat());
        int primaryCol = interfaceModule == null ? -857872385 : interfaceModule.clientPrimaryColor();
        boolean useSec = interfaceModule != null && interfaceModule.usesSecondClientColor();
        int secCol = useSec ? interfaceModule.clientSecondaryColor() : primaryCol;
        float secBlend = RectUtil.updateSecondColorBlend(useSec);
        int finalSec = ColorUtil.lerpColor(primaryCol, secCol, secBlend);
        float colorOffset = RectUtil.updateColorOffset(interfaceModule != null && interfaceModule.clientColorMovement());
        rtx.nv.api.ui.theme.ThemeProfile profile = rtx.nv.api.ui.theme.ThemeManager.currentProfile();
        float pEdge = profile != null ? profile.edge : 1.0f;
        float pSharp = profile != null ? profile.specular : 1.0f;
        float pRefract = profile != null ? profile.refraction : 1.0f;
        float pDarkening = profile != null ? profile.darkening : 1.0f;
        float edgeStrength = (interfaceModule == null ? 0.18f : interfaceModule.rectEdgeStrength.getFloat()) * pEdge * (bubble ? 1.35f : 1.0f);
        float edgeSharpness = (interfaceModule == null ? 55.0f : interfaceModule.rectEdgeSharpness.getFloat()) * pSharp;
        float refractStrength = (interfaceModule == null ? 0.3f : interfaceModule.rectRefractionStrength.getFloat()) * pRefract;

        rtx.nv.utils.render.render2d.glass.BuiltGlass builtGlass = new rtx.nv.utils.render.render2d.glass.BuiltGlass(
                x, y, w, h, r, r, r, r, primaryCol, effAlpha, edgeSharpness, primaryCol, pDarkening, true, edgeStrength, refractStrength, 0.5f, 0.0f)
                .withBlurRadius(blur)
                .withSecondColor(finalSec, colorOffset);
        Render2D.glass(builtGlass);
    }

    private static int opaque(int n) {
        return n & 0xFFFFFF | 0xFF000000;
    }
}
