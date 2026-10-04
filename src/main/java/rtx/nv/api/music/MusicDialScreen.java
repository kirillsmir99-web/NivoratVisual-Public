package rtx.nv.api.music;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import rtx.nv.api.drags.Position;
import rtx.nv.api.drags.components.MusicHudComp;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.ui.BaseScreen;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.sounds.SoundManager;

public final class MusicDialScreen extends BaseScreen {
    private static final float CARD_W = 224.0f;
    private static final float CARD_H = 134.0f;

    private final SmoothAnimation openAnim = new SmoothAnimation();
    private boolean closing = false;

    // Hover animations
    private float hoverClose = 0.0f;
    private float hoverPrev = 0.0f;
    private float hoverPlay = 0.0f;
    private float hoverNext = 0.0f;
    private float hoverFav = 0.0f;
    private float hoverSettings = 0.0f;
    private float hoverCover = 0.0f;

    public enum ToolBtnType {
        PREV, PLAY, PAUSE, NEXT, FAV, SETTINGS
    }

    public MusicDialScreen() {
        super(Text.literal("Music Manager"));
        this.openAnim.set(0.0);
        this.openAnim.run(1.0, 0.24, Easings.BACK_OUT, false);
    }

    private float getCardW(boolean isMinimal) {
        return isMinimal ? 186.0f : CARD_W;
    }

    private float getCardH(boolean isMinimal) {
        return isMinimal ? 104.0f : CARD_H;
    }

    @Override
    public void close() {
        if (!this.closing) {
            this.closing = true;
            this.openAnim.run(0.0, 0.16, Easings.EXPO_IN, false);
            return;
        }
        super.close();
    }

    @Override
    protected void renderScreen(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.openAnim.update();
        float open = this.openAnim.get();
        if (this.closing && open <= 0.02f) {
            super.close();
            return;
        }
        if (open <= 0.01f) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) {
            return;
        }

        float sw = Position.screenWidth();
        float sh = Position.screenHeight();
        float cx = sw * 0.5f;
        float cy = sh * 0.5f;

        // Smooth backdrop dimming
        int dimColor = ColorUtil.rgba(4, 6, 10, (int) (155 * open));
        Render2D.rect(0, 0, sw, sh, 0.0f, dimColor);

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        String style = mod != null ? mod.style.getValue() : "Стеклянный";
        boolean isSquare = "Квадратный".equals(style);
        boolean isMinimal = "Минимализм".equals(style);

        float cardW = this.getCardW(isMinimal);
        float cardH = this.getCardH(isMinimal);
        float cardX = cx - cardW * 0.5f;
        float cardY = cy - cardH * 0.5f;
        rtx.nv.api.drags.HudLayoutCoordinator.reserve("music-controls", cardX, cardY, cardW, cardH);

        TrackState state = MusicManager.get().getClient().getState();
        int dominant = MusicManager.get().getCoverManager().getDominantColor();
        int accentA = ClientAccent.gradientA((int) (255 * open));
        int accentB = ClientAccent.gradientB((int) (255 * open));

        float cardRad = isSquare ? 0.0f : (isMinimal ? 6.5f : 8.0f);
        float itemRad = isSquare ? 0.0f : (isMinimal ? 4.0f : 5.0f);

        // Soft ambient glow around card
        Render2D.glow(new BuiltGlow(
            cardX - 4.0f, cardY - 4.0f, cardW + 8.0f, cardH + 8.0f,
            new float[]{cardRad, cardRad, cardRad, cardRad},
            dominant, 0.9f, 14.0f, 0.30f * open
        ));

        // Card body based on active client style
        if (isSquare) {
            // High-tech Cyberplate
            Render2D.rect(cardX, cardY, cardW, cardH, 0.0f, ColorUtil.rgba(11, 13, 19, (int) (248 * open)));
            Render2D.rect(cardX, cardY, cardW, 2.0f, 0.0f, accentA);
            Render2D.outline(cardX, cardY, cardW, cardH, 0.0f, 1.0f, ColorUtil.rgba(255, 255, 255, (int) (55 * open)));
            Render2D.outline(cardX + 1.5f, cardY + 1.5f, cardW - 3.0f, cardH - 3.0f, 0.0f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (18 * open)));
        } else if (isMinimal) {
            // Compact sleek capsule
            Render2D.rect(cardX, cardY, cardW, cardH, cardRad, ColorUtil.rgba(12, 15, 22, (int) (240 * open)));
            Render2D.outline(cardX, cardY, cardW, cardH, cardRad, 1.0f, ColorUtil.multAlpha(accentA, 0.65f * open));
        } else {
            // Glassmorphism
            RectUtil.drawClientRectFixedRadius(cardX, cardY, cardW, cardH, cardRad, open, 0.0f);
            Render2D.outline(cardX, cardY, cardW, cardH, cardRad, 1.0f, ColorUtil.rgba(255, 255, 255, (int) (35 * open)));
        }

        // --- Header ---
        String headerTitle = isSquare ? "[NV // AUDIO CONTROL]" : (isMinimal ? "МУЗЫКА" : "УПРАВЛЕНИЕ МУЗЫКОЙ");
        float headerFont = isMinimal ? 4.4f : 5.0f;
        float headerX = cardX + (isMinimal ? 8.0f : 10.0f);
        float headerY = cardY + (isMinimal ? 6.5f : 7.5f);
        Fonts.MONTSERRAT_BOLD.msdf(headerTitle, headerX, headerY, headerFont, accentA);

        // Close button [✕]
        float closeBtnSize = isMinimal ? 11.0f : 13.0f;
        float closeBtnX = cardX + cardW - closeBtnSize - (isMinimal ? 4.5f : 5.5f);
        float closeBtnY = cardY + (isMinimal ? 4.5f : 5.5f);
        boolean inClose = mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnSize && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnSize;
        this.hoverClose = MathHelper.clamp(this.hoverClose + (inClose ? 0.2f : -0.15f), 0.0f, 1.0f);

        int closeBg = ColorUtil.rgba(255, 255, 255, (int) (24 * this.hoverClose * open));
        Render2D.rect(closeBtnX, closeBtnY, closeBtnSize, closeBtnSize, itemRad, closeBg);
        int closeCol = ColorUtil.lerpColor(ColorUtil.rgba(160, 165, 180, (int) (200 * open)), 0xFFFF5566, this.hoverClose);
        Fonts.NV.msdf(NvIcons.CLOSE, closeBtnX + (isMinimal ? 2.5f : 3.2f), closeBtnY + (isMinimal ? 2.0f : 2.5f), isMinimal ? 4.4f : 5.0f, closeCol);

        // Subtle header divider
        float divY = cardY + (isMinimal ? 17.0f : 19.5f);
        Render2D.rect(cardX + 8.0f, divY, cardW - 16.0f, 1.0f, isSquare ? 0.0f : 0.5f, ColorUtil.rgba(255, 255, 255, (int) (14 * open)));

        // --- Track Info Area ---
        float coverSize = isMinimal ? 26.0f : 36.0f;
        float coverX = cardX + (isMinimal ? 8.0f : 10.0f);
        float coverY = divY + (isMinimal ? 4.0f : 4.5f);

        boolean inCover = mouseX >= coverX && mouseX <= coverX + coverSize && mouseY >= coverY && mouseY <= coverY + coverSize;
        this.hoverCover = MathHelper.clamp(this.hoverCover + (inCover ? 0.18f : -0.15f), 0.0f, 1.0f);

        CoverTextureManager coverMgr = MusicManager.get().getCoverManager();
        String coverTex = coverMgr.hasCustomCover() ? "nv:dynamic/album_cover" : "nv:textures/gui/default_cover.png";
        Render2D.image(coverTex, coverX, coverY, coverSize, coverSize, itemRad, -1);

        int coverOutlineCol = ColorUtil.lerpColor(
            ColorUtil.rgba(255, 255, 255, (int) (40 * open)),
            ColorUtil.multAlpha(dominant, 0.9f * open),
            this.hoverCover
        );
        Render2D.outline(coverX, coverY, coverSize, coverSize, itemRad, 1.0f, coverOutlineCol);

        // Live equalizer bars inside cover if playing
        if (state.isPlaying()) {
            long time = System.currentTimeMillis();
            int eqBars = isMinimal ? 3 : 4;
            float eqBoxW = eqBars * 2.8f + 1.8f;
            float eqBoxH = isMinimal ? 8.5f : 10.0f;
            float eqX = coverX + coverSize - eqBoxW - 1.0f;
            float eqY = coverY + coverSize - eqBoxH - 1.0f;
            Render2D.rect(eqX, eqY, eqBoxW, eqBoxH, isSquare ? 0.0f : 2.0f, ColorUtil.rgba(10, 12, 16, (int) (215 * open)));

            for (int b = 0; b < eqBars; b++) {
                float barVal = MusicHudComp.computeEqualizerBar(b, eqBars, time);
                float hBar = (isMinimal ? 1.5f : 1.8f) + barVal * (isMinimal ? 4.5f : 6.0f);
                int barCol = ColorUtil.lerpColor(accentA, accentB, b / (float) (eqBars - 1));
                Render2D.rect(eqX + b * 2.8f + 0.9f, eqY + (isMinimal ? 7.2f : 8.5f) - hBar, 1.6f, hBar, isSquare ? 0.0f : 0.6f, ColorUtil.multAlpha(barCol, open));
            }
        }

        // Title and artist
        float textX = coverX + coverSize + (isMinimal ? 7.0f : 9.0f);
        float maxTextW = cardW - (textX - cardX) - (isMinimal ? 8.0f : 10.0f);

        String title = state.isActive() ? state.title() : "Музыка не играет";
        String artist = state.isActive() ? state.artist() : "Запустите плеер на компьютере";

        float titleFont = isMinimal ? 5.2f : 6.0f;
        float artistFont = isMinimal ? 4.2f : 4.8f;
        String dispTitle = truncate(title, maxTextW, titleFont, Fonts.MONTSERRAT_BOLD);
        Fonts.MONTSERRAT_BOLD.msdf(dispTitle, textX, coverY + 1.0f, titleFont, ColorUtil.rgba(255, 255, 255, (int) (250 * open)));

        String dispArtist = truncate(artist, maxTextW, artistFont, Fonts.MONTSERRAT_MEDIUM);
        Fonts.MONTSERRAT_MEDIUM.msdf(dispArtist, textX, coverY + (isMinimal ? 9.5f : 11.5f), artistFont, ColorUtil.rgba(175, 180, 195, (int) (220 * open)));

        // Status badge pill
        float badgeX = textX;
        float badgeY = coverY + (isMinimal ? 17.5f : 23.5f);
        float badgeH = isMinimal ? 8.5f : 10.0f;
        float badgeFont = isMinimal ? 3.6f : 4.0f;
        boolean isPlaying = state.isPlaying();
        boolean isActive = state.isActive();

        String statusText = !isActive ? "Остановлено" : (isPlaying ? "Воспроизведение" : "На паузе");
        int statusDotCol = !isActive ? 0xFF888899 : (isPlaying ? 0xFF44FF88 : 0xFFFFAA33);
        float badgeW = Fonts.MONTSERRAT_MEDIUM.width(statusText, badgeFont) + (isMinimal ? 11.0f : 14.0f);

        Render2D.rect(badgeX, badgeY, badgeW, badgeH, isSquare ? 0.0f : (badgeH * 0.5f), ColorUtil.rgba(255, 255, 255, (int) (14 * open)));
        Render2D.outline(badgeX, badgeY, badgeW, badgeH, isSquare ? 0.0f : (badgeH * 0.5f), 0.8f, ColorUtil.rgba(255, 255, 255, (int) (22 * open)));

        Render2D.circle(badgeX + (isMinimal ? 4.0f : 5.0f), badgeY + badgeH * 0.5f, isMinimal ? 1.6f : 2.0f, ColorUtil.multAlpha(statusDotCol, open));
        Fonts.MONTSERRAT_MEDIUM.msdf(statusText, badgeX + (isMinimal ? 8.0f : 10.0f), badgeY + (isMinimal ? 1.8f : 2.0f), badgeFont, ColorUtil.rgba(220, 225, 235, (int) (220 * open)));

        // --- Timeline Section ---
        float timeY = coverY + coverSize + (isMinimal ? 5.5f : 5.0f);
        float barX = cardX + (isMinimal ? 8.0f : 10.0f);
        float barW = cardW - (isMinimal ? 16.0f : 20.0f);
        float barH = isMinimal ? 2.0f : 2.5f;

        String posStr = state.isActive() ? state.formattedPosition() : "00:00";
        String durStr = state.isActive() ? state.formattedDuration() : "00:00";

        float timeFont = isMinimal ? 4.0f : 4.6f;
        int timeCol = ColorUtil.rgba(225, 232, 245, (int) (230 * open));
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (160 * open));
        Fonts.MONTSERRAT_MEDIUM.msdf(posStr, barX + 0.5f, timeY + 0.5f, timeFont, shadowCol);
        Fonts.MONTSERRAT_MEDIUM.msdf(posStr, barX, timeY, timeFont, timeCol);
        float durW = Fonts.MONTSERRAT_MEDIUM.width(durStr, timeFont);
        Fonts.MONTSERRAT_MEDIUM.msdf(durStr, barX + barW - durW + 0.5f, timeY + 0.5f, timeFont, shadowCol);
        Fonts.MONTSERRAT_MEDIUM.msdf(durStr, barX + barW - durW, timeY, timeFont, timeCol);

        // Progress bar
        float trackY = timeY + (isMinimal ? 6.0f : 7.5f);
        Render2D.rect(barX, trackY, barW, barH, isSquare ? 0.0f : 1.25f, ColorUtil.rgba(255, 255, 255, (int) (22 * open)));

        float prog = state.progress();
        if (prog > 0.0f) {
            float fillW = Math.max(2.0f, barW * prog);
            int fillCol = ColorUtil.lerpColor(accentA, accentB, prog);
            Render2D.rect(barX, trackY, fillW, barH, isSquare ? 0.0f : 1.25f, fillCol);
            Render2D.circle(barX + fillW, trackY + barH * 0.5f, isMinimal ? 2.2f : 2.8f, ColorUtil.multAlpha(fillCol, open));
        }

        // --- Bottom Action Buttons Toolbar ---
        float btnY = trackY + barH + (isMinimal ? 5.5f : 7.0f);
        float btnH = isMinimal ? 22.0f : 28.0f;
        float bSideW = isMinimal ? 22.0f : 28.0f;
        float bCenterW = isMinimal ? 34.0f : 44.0f;
        float spacing = isMinimal ? 5.0f : 7.0f;
        float totalBtnsW = bSideW * 4 + bCenterW + spacing * 4;
        float btnStartX = cardX + (cardW - totalBtnsW) * 0.5f;

        // 1. Previous
        float bPrevX = btnStartX;
        boolean inPrev = mouseX >= bPrevX && mouseX <= bPrevX + bSideW && mouseY >= btnY && mouseY <= btnY + btnH;
        this.hoverPrev = MathHelper.clamp(this.hoverPrev + (inPrev ? 0.2f : -0.15f), 0.0f, 1.0f);
        this.renderToolButton(bPrevX, btnY, bSideW, btnH, ToolBtnType.PREV, this.hoverPrev, open, accentA, false, itemRad);

        // 2. Play / Pause
        float bPlayX = bPrevX + bSideW + spacing;
        boolean inPlay = mouseX >= bPlayX && mouseX <= bPlayX + bCenterW && mouseY >= btnY && mouseY <= btnY + btnH;
        this.hoverPlay = MathHelper.clamp(this.hoverPlay + (inPlay ? 0.2f : -0.15f), 0.0f, 1.0f);
        ToolBtnType playType = state.isPlaying() ? ToolBtnType.PAUSE : ToolBtnType.PLAY;
        this.renderToolButton(bPlayX, btnY, bCenterW, btnH, playType, this.hoverPlay, open, accentA, true, itemRad);

        // 3. Next
        float bNextX = bPlayX + bCenterW + spacing;
        boolean inNext = mouseX >= bNextX && mouseX <= bNextX + bSideW && mouseY >= btnY && mouseY <= btnY + btnH;
        this.hoverNext = MathHelper.clamp(this.hoverNext + (inNext ? 0.2f : -0.15f), 0.0f, 1.0f);
        this.renderToolButton(bNextX, btnY, bSideW, btnH, ToolBtnType.NEXT, this.hoverNext, open, accentA, false, itemRad);

        // 4. Favorite
        float bFavX = bNextX + bSideW + spacing;
        boolean inFav = mouseX >= bFavX && mouseX <= bFavX + bSideW && mouseY >= btnY && mouseY <= btnY + btnH;
        this.hoverFav = MathHelper.clamp(this.hoverFav + (inFav ? 0.2f : -0.15f), 0.0f, 1.0f);
        boolean isFav = state.isActive() && MusicManager.get().getFavorites().isFavorite(state.artist(), state.title());
        int favCol = isFav ? 0xFFFF4477 : 0xFFCCD0DD;
        this.renderToolButton(bFavX, btnY, bSideW, btnH, ToolBtnType.FAV, this.hoverFav, open, favCol, isFav, itemRad);

        // 5. Settings / ClickGUI shortcut
        float bSetX = bFavX + bSideW + spacing;
        boolean inSet = mouseX >= bSetX && mouseX <= bSetX + bSideW && mouseY >= btnY && mouseY <= btnY + btnH;
        this.hoverSettings = MathHelper.clamp(this.hoverSettings + (inSet ? 0.2f : -0.15f), 0.0f, 1.0f);
        this.renderToolButton(bSetX, btnY, bSideW, btnH, ToolBtnType.SETTINGS, this.hoverSettings, open, accentA, false, itemRad);
    }

    private void renderToolButton(float bx, float by, float bw, float bh, ToolBtnType type, float hover, float open, int accent, boolean highlighted, float radius) {
        int baseBg = highlighted
            ? ColorUtil.multAlpha(accent, (0.35f + 0.25f * hover) * open)
            : ColorUtil.lerpColor(
                ColorUtil.rgba(255, 255, 255, (int) (14 * open)),
                ColorUtil.rgba(255, 255, 255, (int) (38 * open)),
                hover
            );
        Render2D.rect(bx, by, bw, bh, radius, baseBg);

        int outline = ColorUtil.lerpColor(
            ColorUtil.rgba(255, 255, 255, (int) (22 * open)),
            ColorUtil.multAlpha(accent, 0.85f * open),
            highlighted ? 0.7f + 0.3f * hover : hover
        );
        Render2D.outline(bx, by, bw, bh, radius, 1.0f, outline);

        if (hover > 0.05f) {
            Render2D.glow(new BuiltGlow(
                bx - 1.0f, by - 1.0f, bw + 2.0f, bh + 2.0f,
                new float[]{radius, radius, radius, radius},
                accent, 0.8f, 6.0f, 0.25f * hover * open
            ));
        }

        int iconCol = ColorUtil.lerpColor(
            ColorUtil.rgba(205, 210, 225, (int) (225 * open)),
            accent,
            highlighted ? 1.0f : hover
        );

        float cx = bx + bw * 0.5f;
        float cy = by + bh * 0.5f;
        float s = Math.min(1.0f, bh / 28.0f);

        float iconSize = 7.0f * s;
        String icon = switch (type) {
            case PREV -> NvIcons.PREVIOUS;
            case PLAY -> NvIcons.PLAY;
            case PAUSE -> NvIcons.PAUSE;
            case NEXT -> NvIcons.NEXT;
            case FAV -> NvIcons.HEART;
            case SETTINGS -> NvIcons.SETTINGS;
        };

        float[] iconBounds = Fonts.NV.msdfBounds(icon, iconSize);
        Fonts.NV.msdf(icon, cx - (iconBounds[0]+iconBounds[2])*.5f,
            cy - (iconBounds[1]+iconBounds[3])*.5f, iconSize, iconCol);
    }

    private static String truncate(String text, float maxW, float size, Fonts font) {
        if (text == null) return "";
        if (font.width(text, size) <= maxW) return text;
        String cur = text;
        while (cur.length() > 3 && font.width(cur + "...", size) > maxW) {
            cur = cur.substring(0, cur.length() - 1);
        }
        return cur + "...";
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closing) {
            return true;
        }

        if (click == null || click.button() != 0) {
            if (click != null && click.button() == 1) {
                this.close();
                return true;
            }
            return super.mouseClicked(click, doubled);
        }

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        String style = mod != null ? mod.style.getValue() : "Стеклянный";
        boolean isMinimal = "Минимализм".equals(style);

        float sw = Position.screenWidth();
        float sh = Position.screenHeight();
        float cx = sw * 0.5f;
        float cy = sh * 0.5f;
        float cardW = this.getCardW(isMinimal);
        float cardH = this.getCardH(isMinimal);
        float cardX = cx - cardW * 0.5f;
        float cardY = cy - cardH * 0.5f;

        double mx = click.x();
        double my = click.y();

        // Check close button
        float closeBtnSize = isMinimal ? 11.0f : 13.0f;
        float closeBtnX = cardX + cardW - closeBtnSize - (isMinimal ? 4.5f : 5.5f);
        float closeBtnY = cardY + (isMinimal ? 4.5f : 5.5f);
        if (mx >= closeBtnX && mx <= closeBtnX + closeBtnSize && my >= closeBtnY && my <= closeBtnY + closeBtnSize) {
            this.close();
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.0f);
            return true;
        }

        // Action toolbar buttons
        float coverSize = isMinimal ? 26.0f : 36.0f;
        float divY = cardY + (isMinimal ? 17.0f : 19.5f);
        float coverY = divY + (isMinimal ? 4.0f : 4.5f);
        float timeY = coverY + coverSize + (isMinimal ? 5.5f : 5.0f);
        float barH = isMinimal ? 2.0f : 2.5f;
        float trackY = timeY + (isMinimal ? 6.0f : 7.5f);
        float btnY = trackY + barH + (isMinimal ? 5.5f : 7.0f);
        float btnH = isMinimal ? 22.0f : 28.0f;
        float bSideW = isMinimal ? 22.0f : 28.0f;
        float bCenterW = isMinimal ? 34.0f : 44.0f;
        float spacing = isMinimal ? 5.0f : 7.0f;
        float totalBtnsW = bSideW * 4 + bCenterW + spacing * 4;
        float btnStartX = cardX + (cardW - totalBtnsW) * 0.5f;

        // Timeline scrub click
        float barX = cardX + (isMinimal ? 10.0f : 14.0f);
        float barW = cardW - (isMinimal ? 20.0f : 28.0f);
        TrackState state = MusicManager.get().getClient().getState();
        if (state.durationMs() > 0L && mx >= barX - 2.0f && mx <= barX + barW + 2.0f && my >= trackY - 5.0f && my <= trackY + barH + 5.0f) {
            double targetProgress = Math.max(0.0, Math.min(1.0, (mx - barX) / barW));
            double targetSeconds = targetProgress * (state.durationMs() / 1000.0);
            MusicManager.get().seekToSeconds(targetSeconds);
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.15f);
            return true;
        }

        // 1. Previous
        float bPrevX = btnStartX;
        if (mx >= bPrevX - 2.0f && mx <= bPrevX + bSideW + 2.0f && my >= btnY - 2.0f && my <= btnY + btnH + 2.0f) {
            MusicManager.get().previousTrack();
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.1f);
            return true;
        }

        // 2. Play / Pause
        float bPlayX = bPrevX + bSideW + spacing;
        if (mx >= bPlayX - 2.0f && mx <= bPlayX + bCenterW + 2.0f && my >= btnY - 2.0f && my <= btnY + btnH + 2.0f) {
            MusicManager.get().playPause();
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.2f);
            return true;
        }

        // 3. Next
        float bNextX = bPlayX + bCenterW + spacing;
        if (mx >= bNextX - 2.0f && mx <= bNextX + bSideW + 2.0f && my >= btnY - 2.0f && my <= btnY + btnH + 2.0f) {
            MusicManager.get().nextTrack();
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.1f);
            return true;
        }

        // 4. Favorite
        float bFavX = bNextX + bSideW + spacing;
        if (mx >= bFavX - 2.0f && mx <= bFavX + bSideW + 2.0f && my >= btnY - 2.0f && my <= btnY + btnH + 2.0f) {
            MusicManager.get().toggleFavorite();
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.3f);
            return true;
        }

        // 5. Settings (open ClickGUI and focus Music HUD settings popup)
        float bSetX = bFavX + bSideW + spacing;
        if (mx >= bSetX - 2.0f && mx <= bSetX + bSideW + 2.0f && my >= btnY - 2.0f && my <= btnY + btnH + 2.0f) {
            this.close();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.setScreen(rtx.nv.api.ui.UI.INSTANCE);
                rtx.nv.api.ui.UI.INSTANCE.openModuleSettings(ModuleManager.get().get(MusicHudModule.class));
            }
            SoundManager.playSoundDirect(SoundManager.CLICK, 1.0f, 1.0f);
            return true;
        }

        // Click outside modal card closes it
        if (mx < cardX || mx > cardX + cardW || my < cardY || my > cardY + cardH) {
            this.close();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.closing) {
            return true;
        }
        if (input != null) {
            int key = input.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                this.close();
                return true;
            }
            MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
            int dialKey = mod != null && mod.dialKey.isBound() ? mod.dialKey.getKey() : GLFW.GLFW_KEY_R;
            if (key == dialKey) {
                this.close();
                return true;
            }
            if (key == GLFW.GLFW_KEY_SPACE) {
                MusicManager.get().playPause();
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
                MusicManager.get().nextTrack();
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
                MusicManager.get().previousTrack();
                return true;
            }
            if (key == GLFW.GLFW_KEY_F) {
                MusicManager.get().toggleFavorite();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
