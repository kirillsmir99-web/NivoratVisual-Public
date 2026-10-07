package rtx.nv.api.drags.components;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.input.MouseButtonEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.EvoPlayerModule;
import rtx.nv.api.music.CoverTextureManager;
import rtx.nv.api.music.MusicManager;
import rtx.nv.api.music.TrackState;
import rtx.nv.api.music.lyrics.LyricsManager;
import rtx.nv.api.music.lyrics.SubtitleBubble;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class EvoPlayerComp extends Draggable {
    private static final float BASE_ISLAND_W = 176.0f;
    private static final float BASE_ISLAND_H = 24.0f;
    private static final float BASE_PLAYER_W = 208.0f;
    private static final float BASE_PLAYER_H = 76.0f;

    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation animatedW = new SmoothAnimation();
    private final SmoothAnimation animatedH = new SmoothAnimation();
    private boolean lastTargetVisible = false;
    private boolean expanded = false;

    private final float[] miniEq = new float[5];
    private TrackState displayedState = TrackState.INACTIVE;

    // Scrubber interaction
    private boolean isScrubbing = false;
    private float scrubProgress = 0.0f;
    private float scrubHover = 0.0f;

    // Button hovers
    private float hoverPrev = 0.0f;
    private float hoverPlay = 0.0f;
    private float hoverNext = 0.0f;
    private float hoverFav = 0.0f;

    public EvoPlayerComp() {
        super("evo_player", Position.screenWidth() - BASE_ISLAND_W - 12.0f, 12.0f);
        this.visibility.set(0.0);
        this.animatedW.set(BASE_ISLAND_W);
        this.animatedH.set(BASE_ISLAND_H);
        EventBus.get().subscribe(this);
    }

    @Override
    public void resetToDefault() {
        float defaultX = Position.screenWidth() - this.width() - 12.0f;
        float defaultY = 12.0f;
        this.getDrag().setTargetX(defaultX);
        this.getDrag().setTargetY(defaultY);
        this.getDrag().syncToTarget();
    }

    @Override
    public String displayName() {
        return "Evo Player";
    }

    public float getScale() {
        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        return mod != null ? mod.getScale() : 1.0f;
    }

    @Override
    public float width() {
        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        boolean isPlayer = mod != null && (mod.cardStyle.is("Плеер") || this.expanded);
        float baseW = isPlayer ? BASE_PLAYER_W : BASE_ISLAND_W;
        return baseW * this.getScale();
    }

    @Override
    public float height() {
        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        boolean isPlayer = mod != null && (mod.cardStyle.is("Плеер") || this.expanded);
        float baseH = isPlayer ? BASE_PLAYER_H : BASE_ISLAND_H;
        return baseH * this.getScale();
    }

    @Override
    protected float getX() { return Position.clampX(super.getX(), this.width()); }

    @Override
    protected float getY() { return Position.clampY(super.getY(), this.height()); }

    @Override
    public boolean isInteractive() {
        return this.shouldShow();
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        if (mod != null) {
            for (rtx.nv.api.modules.settings.Setting s : mod.getSettings().all()) {
                Setting ui = SettingsFactory.create(s);
                if (ui != null) {
                    list.add(ui);
                }
            }
        }
        return list;
    }

    private boolean shouldShow() {
        var drag = this.getDrag();
        if (!drag.isDragging()) {
            float clampedX = Position.clampX(drag.getTargetX(), this.width());
            float clampedY = Position.clampY(drag.getTargetY(), this.height());
            if (Math.abs(clampedX - drag.getTargetX()) > .01f || Math.abs(clampedY - drag.getTargetY()) > .01f) {
                drag.setTargetX(clampedX); drag.setTargetY(clampedY); drag.syncToTarget();
            }
        }
        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        if (mod == null || !mod.isEnabled()) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden) {
            return false;
        }

        if (DragSystem.get().isDragModeActive()) {
            return true;
        }

        TrackState state = MusicManager.get().getClient().getState();
        return state.isActive() || this.isScrubbing;
    }

    @Override
    protected void render(DrawContext drawContext) {
        boolean target = this.shouldShow();
        if (target != this.lastTargetVisible) {
            this.visibility.run(target ? 1.0 : 0.0, target ? 0.22 : 0.15, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = target;
        }
        this.visibility.update();
        float alpha = (float) this.visibility.get();
        if (alpha <= 0.01f) {
            return;
        }

        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        if (mod == null) {
            return;
        }

        mod.syncWorldLyricsConfig();

        float scale = this.getScale();
        float x = this.getX();
        float y = this.getY();

        boolean isPlayer = mod.cardStyle.is("Плеер") || this.expanded;
        float targetW = isPlayer ? BASE_PLAYER_W : BASE_ISLAND_W;
        float targetH = isPlayer ? BASE_PLAYER_H : BASE_ISLAND_H;

        this.animatedW.run(targetW, 0.22, Easings.CUBIC_OUT, false);
        this.animatedH.run(targetH, 0.22, Easings.CUBIC_OUT, false);
        this.animatedW.update();
        this.animatedH.update();

        float rw = (float) this.animatedW.get();
        float rh = (float) this.animatedH.get();

        TrackState state = MusicManager.get().getClient().getState();
        if (state.isActive()) this.displayedState = state;
        else if (!target && this.displayedState.isActive()) state = this.displayedState;
        int dominant = MusicManager.get().getCoverManager().getDominantColor();
        int accentA = ClientAccent.gradientA((int) (255 * alpha));
        int accentB = ClientAccent.gradientB((int) (255 * alpha));

        drawContext.getMatrices().pushMatrix();
        if (Math.abs(scale - 1.0f) > 0.001f) {
            drawContext.getMatrices().translate(x, y);
            drawContext.getMatrices().scale(scale, scale);
            drawContext.getMatrices().translate(-x, -y);
        }

        Render2D.beginFrame(drawContext);
        this.renderCard(drawContext, x, y, rw, rh, alpha, state, mod, dominant, accentA, accentB);
        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }

    private void renderCard(DrawContext drawContext, float x, float y, float rw, float rh, float alpha,
                            TrackState state, EvoPlayerModule mod,
                            int dominant, int accentA, int accentB) {
        boolean isExpanded = rh >= 45.0f;
        float cornerRadius = isExpanded ? 10.0f : (rh * 0.5f);

        // 1. Неоновый блум
        int glowCol = MusicHudComp.boostGlowColor(dominant, accentA);
        Render2D.glow(new BuiltGlow(
            x - 2.0f, y - 2.0f, rw + 4.0f, rh + 4.0f,
            new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius},
            glowCol, 0.60f, 7.0f, 0.20f * alpha
        ));

        // 2. Аутентичная подложка матового тёмного стекла EvoVis
        int bg = ColorUtil.rgba(13, 16, 23, (int) (236 * alpha));
        Render2D.rect(x, y, rw, rh, cornerRadius, bg);

        // 3. Акцентный ореол (горизонтальный мягкий градиент слева в тон трека)
        if (mod.accentWash.getValue()) {
            float washW = Math.min(rw * 0.62f, rh * 3.2f);
            int washCol = dominant != 0 ? dominant : accentA;
            int cLeft = ColorUtil.multAlpha(washCol, 0.36f * alpha);
            int cRight = ColorUtil.multAlpha(washCol, 0.0f);
            Render2D.rect(x, y, washW, rh, cornerRadius, 0.0f, 0.0f, cornerRadius, cLeft, cRight, cRight, cLeft);
        }

        // 4. Тонкая окантовка карточки
        int borderCol = ColorUtil.rgba(255, 255, 255, (int) (28 * alpha));
        Render2D.outline(x, y, rw, rh, cornerRadius, 1.0f, borderCol);

        // 5. Отрисовка контента
        if (!isExpanded) {
            this.renderIsland(x, y, rw, rh, alpha, state, mod, dominant, accentA, accentB);
        } else {
            this.renderPlayer(x, y, rw, rh, alpha, state, mod, dominant, accentA, accentB);
        }
    }

    private void renderIsland(float x, float y, float rw, float rh, float alpha,
                              TrackState state, EvoPlayerModule mod, int dominant, int accentA, int accentB) {
        float iconBoxX = x + 5.0f;
        float iconBoxY = y + 4.5f;
        float iconBoxSize = 15.0f;
        CoverTextureManager coverMgr = MusicManager.get().getCoverManager();

        if (coverMgr.hasCustomCover()) {
            Render2D.image("nv:dynamic/album_cover", iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0f, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
            Render2D.outline(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));
        } else {
            Render2D.rect(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0f, ColorUtil.rgba(255, 255, 255, (int) (25 * alpha)));
            float[] b = Fonts.NV.msdfBounds(NvIcons.MUSIC, 7.5f);
            Fonts.NV.msdf(NvIcons.MUSIC, iconBoxX + iconBoxSize * 0.5f - (b[0] + b[2]) * 0.5f,
                iconBoxY + iconBoxSize * 0.5f - (b[1] + b[3]) * 0.5f, 7.5f, ColorUtil.rgba(255, 255, 255, (int) (230 * alpha)));
        }

        // Правый 5-полосный эквалайзер
        float eqW = 5 * 2.8f;
        float eqX = x + rw - eqW - 7.0f;
        float eqY = y + rh * 0.5f;
        for (int b = 0; b < 5; b++) {
            float barVal = state.isPlaying() ? rtx.nv.api.music.AudioSpectrum.level(b, 5) : 0.0f;
            float targetH = Math.max(1.5f, barVal * 8.5f);
            this.miniEq[b] += (targetH - this.miniEq[b]) * 0.35f;
            float curH = this.miniEq[b];
            float bx = eqX + b * 2.8f;
            int barCol = ColorUtil.lerpColor(accentA, accentB, b / 4.0f);
            Render2D.rect(bx, eqY - curH * 0.5f, 1.4f, curH, 0.7f, ColorUtil.multAlpha(barCol, state.isPlaying() ? 0.90f * alpha : 0.40f * alpha));
        }

        // Центрированный текст (название или караоке субтитры)
        float textStartX = iconBoxX + iconBoxSize + 6.0f;
        float textMaxW = eqX - textStartX - 5.0f;

        String lyricText = null;
        if (mod.watermarkLyrics.getValue()) {
            List<SubtitleBubble> bubbles = LyricsManager.get().getActiveBubbles();
            if (!bubbles.isEmpty()) {
                SubtitleBubble bubble = bubbles.get(bubbles.size() - 1);
                if (bubble != null && bubble.line != null && !bubble.line.text.isEmpty()) {
                    lyricText = bubble.line.text;
                }
            }
        }

        float textCenterY = y + rh * 0.5f;
        if (lyricText != null && !lyricText.isBlank()) {
            float fontSize = 5.2f;
            String dispLyric = MusicHudComp.truncate(lyricText, textMaxW, fontSize, false);
            float[] b = Fonts.MONTSERRAT_MEDIUM.msdfBounds(dispLyric, fontSize);
            float ty = textCenterY - (b[1] + b[3]) * 0.5f;
            Fonts.MONTSERRAT_MEDIUM.msdf(dispLyric, textStartX, ty, fontSize, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        } else {
            String title = state.isActive() ? state.title() : "Музыка не играет";
            float fontSize = 5.4f;
            String dispTitle = MusicHudComp.truncate(title, textMaxW, fontSize, true);
            float[] b = Fonts.MONTSERRAT_BOLD.msdfBounds(dispTitle, fontSize);
            float ty = textCenterY - (b[1] + b[3]) * 0.5f;
            Fonts.MONTSERRAT_BOLD.msdf(dispTitle, textStartX, ty, fontSize, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        }
    }

    private void renderPlayer(float x, float y, float rw, float rh, float alpha,
                             TrackState state, EvoPlayerModule mod, int dominant, int accentA, int accentB) {
        // 1. Обложка альбома 34x34 со скруглением
        float coverSize = 34.0f;
        float coverX = x + 9.0f;
        float coverY = y + 9.0f;
        CoverTextureManager coverMgr = MusicManager.get().getCoverManager();

        if (coverMgr.hasCustomCover()) {
            Render2D.image("nv:dynamic/album_cover", coverX, coverY, coverSize, coverSize, 5.0f, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
            Render2D.outline(coverX, coverY, coverSize, coverSize, 5.0f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));
        } else {
            Render2D.rect(coverX, coverY, coverSize, coverSize, 5.0f, ColorUtil.rgba(255, 255, 255, (int) (24 * alpha)));
            Render2D.outline(coverX, coverY, coverSize, coverSize, 5.0f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));
            float[] b = Fonts.NV.msdfBounds(NvIcons.MUSIC, 12.0f);
            Fonts.NV.msdf(NvIcons.MUSIC, coverX + coverSize * 0.5f - (b[0] + b[2]) * 0.5f,
                coverY + coverSize * 0.5f - (b[1] + b[3]) * 0.5f, 12.0f, ColorUtil.rgba(255, 255, 255, (int) (230 * alpha)));
        }

        // 2. Спектральный 5-полосный эквалайзер
        float eqW = 5 * 2.8f;
        float eqX = x + rw - eqW - 9.0f;
        float eqY = y + 9.0f + 17.0f;
        for (int b = 0; b < 5; b++) {
            float barVal = state.isPlaying() ? rtx.nv.api.music.AudioSpectrum.level(b, 5) : 0.0f;
            float targetH = Math.max(1.8f, barVal * 12.0f);
            this.miniEq[b] += (targetH - this.miniEq[b]) * 0.35f;
            float curH = this.miniEq[b];
            float bx = eqX + b * 2.8f;
            int barCol = ColorUtil.lerpColor(accentA, accentB, b / 4.0f);
            Render2D.rect(bx, eqY - curH * 0.5f, 1.5f, curH, 0.75f, ColorUtil.multAlpha(barCol, state.isPlaying() ? 0.90f * alpha : 0.40f * alpha));
        }

        // 3. Текст (название, артист или караоке)
        float textStartX = coverX + coverSize + 8.0f;
        float textMaxW = eqX - textStartX - 6.0f;
        float titleY = y + 10.0f;
        float artistY = titleY + 10.5f;

        String title = state.isActive() ? state.title() : "Музыка не играет";
        String artist = state.isActive() ? state.artist() : "Запустите трек в плеере";

        String dispTitle = MusicHudComp.truncate(title, textMaxW, 6.2f, true);
        Fonts.MONTSERRAT_BOLD.msdf(dispTitle, textStartX, titleY, 6.2f, ColorUtil.rgba(255, 255, 255, (int) (250 * alpha)));

        String lyricText = null;
        if (mod.watermarkLyrics.getValue()) {
            List<SubtitleBubble> bubbles = LyricsManager.get().getActiveBubbles();
            if (!bubbles.isEmpty()) {
                SubtitleBubble bubble = bubbles.get(bubbles.size() - 1);
                if (bubble != null && bubble.line != null && !bubble.line.text.isEmpty()) {
                    lyricText = bubble.line.text;
                }
            }
        }

        if (lyricText != null && !lyricText.isBlank()) {
            String dispLyric = MusicHudComp.truncate(lyricText, textMaxW, 5.0f, false);
            Fonts.MONTSERRAT_MEDIUM.msdf(dispLyric, textStartX, artistY, 5.0f, accentA);
        } else {
            String dispArtist = MusicHudComp.truncate(artist, textMaxW, 5.0f, false);
            Fonts.MONTSERRAT_MEDIUM.msdf(dispArtist, textStartX, artistY, 5.0f, ColorUtil.rgba(180, 185, 200, (int) (210 * alpha)));
        }

        // 4. Полоса воспроизведения и скруббинг
        float barX = x + 9.0f;
        float barY = y + 9.0f + coverSize + 6.0f;
        float barW = rw - 18.0f;
        float barH = 3.0f;

        MinecraftClient mc = MinecraftClient.getInstance();
        float mx = Position.mouseX();
        float my = Position.mouseY();
        float scale = this.getScale();
        float localMx = (mx - x) / scale + x;
        float localMy = (my - y) / scale + y;

        boolean inScreen = mc != null && mc.currentScreen != null;
        boolean hoverTimeline = inScreen && localMy >= barY - 4.0f && localMy <= barY + barH + 4.0f
            && localMx >= barX && localMx <= barX + barW;

        if (hoverTimeline || this.isScrubbing) {
            this.scrubHover = Math.min(1.0f, this.scrubHover + 0.15f);
        } else {
            this.scrubHover = Math.max(0.0f, this.scrubHover - 0.12f);
        }

        if (this.isScrubbing && inScreen) {
            long handle = mc.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                this.scrubProgress = Math.max(0.0f, Math.min(1.0f, (localMx - barX) / barW));
            } else {
                this.isScrubbing = false;
                double targetSeconds = this.scrubProgress * (state.durationMs() / 1000.0);
                MusicManager.get().seekToSeconds(targetSeconds);
            }
        }

        float curBarH = barH + this.scrubHover * 1.2f;
        float curBarY = barY - this.scrubHover * 0.6f;
        Render2D.rect(barX, curBarY, barW, curBarH, 1.5f, ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)));

        float currentProgress = this.isScrubbing ? this.scrubProgress : state.progress();
        if (currentProgress > 0.0f) {
            float fillW = Math.max(2.5f, barW * currentProgress);
            int fillCol = dominant != 0 ? ColorUtil.lerpColor(dominant, accentA, 0.25f) : ColorUtil.lerpColor(accentA, accentB, currentProgress);
            Render2D.rect(barX, curBarY, fillW, curBarH, 1.5f, ColorUtil.multAlpha(fillCol, alpha));

            if (this.scrubHover > 0.05f || this.isScrubbing) {
                float knobX = barX + barW * currentProgress;
                float knobY = curBarY + curBarH * 0.5f;
                float knobRad = 2.8f + this.scrubHover * 0.7f;
                Render2D.circle(knobX, knobY, knobRad, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
                Render2D.circle(knobX, knobY, knobRad + 1.2f, ColorUtil.multAlpha(fillCol, 0.65f * alpha));

                if (this.isScrubbing) {
                    long scrubMs = (long) (this.scrubProgress * state.durationMs());
                    String scrubTime = TrackState.formatTime(scrubMs);
                    float tipW = Fonts.MONTSERRAT_BOLD.width(scrubTime, 4.5f) + 6.0f;
                    float tipH = 9.0f;
                    float tipX = knobX - tipW * 0.5f;
                    float tipY = knobY - knobRad - tipH - 2.5f;
                    Render2D.rect(tipX, tipY, tipW, tipH, 2.0f, ColorUtil.rgba(14, 16, 22, (int) (245 * alpha)));
                    Render2D.outline(tipX, tipY, tipW, tipH, 2.0f, 0.8f, fillCol);
                    Fonts.MONTSERRAT_BOLD.msdf(scrubTime, tipX + 3.0f, tipY + 1.2f, 4.5f, 0xFFFFFFFF);
                }
            }
        }

        // 5. Отметки времени
        float timeY = barY + 5.0f;
        long posMs = this.isScrubbing ? (long) (this.scrubProgress * state.durationMs()) : state.currentPositionMs();
        String posStr = TrackState.formatTime(posMs);
        long remainMs = Math.max(0L, state.durationMs() - posMs);
        String remainStr = "·" + TrackState.formatTime(remainMs);

        float timeSize = 4.8f;
        Fonts.MONTSERRAT_MEDIUM.msdf(posStr, barX, timeY, timeSize, ColorUtil.rgba(190, 195, 210, (int) (210 * alpha)));
        float remW = Fonts.MONTSERRAT_MEDIUM.width(remainStr, timeSize);
        Fonts.MONTSERRAT_MEDIUM.msdf(remainStr, barX + barW - remW, timeY, timeSize, ColorUtil.rgba(190, 195, 210, (int) (210 * alpha)));

        // 6. Центрированные кнопки управления плеером
        float btnCenterY = timeY + 11.5f;
        float bw = 15.0f;
        float bh = 13.0f;
        float spacing = 6.0f;
        float step = bw + spacing;
        float totalControlsW = bw * 4 + spacing * 3;
        float btnStartX = x + (rw - totalControlsW) * 0.5f;

        this.updateButtonHovers(x, y, scale, btnCenterY - bh * 0.5f, btnStartX, step);

        float btnRad = 3.5f;
        float by = btnCenterY - bh * 0.5f;
        this.renderBtn(btnStartX, by, NvIcons.PREVIOUS, this.hoverPrev, alpha, accentA, btnRad, false);
        String playIcon = state.isPlaying() ? NvIcons.PAUSE : NvIcons.PLAY;
        this.renderBtn(btnStartX + step, by, playIcon, this.hoverPlay, alpha, accentA, btnRad, true);
        this.renderBtn(btnStartX + step * 2, by, NvIcons.NEXT, this.hoverNext, alpha, accentA, btnRad, false);

        boolean isFav = state.isActive() && MusicManager.get().getFavorites().isFavorite(state.artist(), state.title());
        int favColor = isFav ? 0xFFFF4477 : accentA;
        this.renderBtn(btnStartX + step * 3, by, NvIcons.HEART, this.hoverFav, alpha, favColor, btnRad, false);
    }

    private void renderBtn(float bx, float by, String icon, float hover, float alpha, int accent, float radius, boolean isPlay) {
        float bw = 15.0f;
        float bh = 13.0f;

        int bg = ColorUtil.lerpColor(
            ColorUtil.rgba(255, 255, 255, (int) (14 * alpha)),
            ColorUtil.rgba(255, 255, 255, (int) (38 * alpha)),
            hover
        );
        if (isPlay) bg = ColorUtil.multAlpha(accent, .24f + .16f * hover);
        Render2D.rect(bx, by, bw, bh, radius, bg);

        int outline = ColorUtil.lerpColor(
            ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)),
            ColorUtil.multAlpha(accent, 0.8f * alpha),
            hover
        );
        Render2D.outline(bx, by, bw, bh, radius, 0.8f, outline);

        if (hover > 0.05f) {
            Render2D.glow(new BuiltGlow(
                bx - 1.0f, by - 1.0f, bw + 2.0f, bh + 2.0f,
                new float[]{radius, radius, radius, radius},
                accent, 0.75f, 6.0f, 0.28f * hover * alpha
            ));
        }

        int textCol = ColorUtil.lerpColor(
            ColorUtil.rgba(200, 205, 215, (int) (200 * alpha)),
            accent,
            hover
        );

        float cx = bx + bw * 0.5f;
        float cy = by + bh * 0.5f;
        float iconSize = 8.0f;

        float[] bounds = Fonts.NV.msdfBounds(icon, iconSize);
        Fonts.NV.msdf(icon, cx - (bounds[0] + bounds[2]) * .5f,
            cy - (bounds[1] + bounds[3]) * .5f, iconSize, textCol);
    }

    private void updateButtonHovers(float x, float y, float scale, float btnRowY, float contentStartX, float step) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen == null) {
            this.hoverPrev = Math.max(0.0f, this.hoverPrev - 0.15f);
            this.hoverPlay = Math.max(0.0f, this.hoverPlay - 0.15f);
            this.hoverNext = Math.max(0.0f, this.hoverNext - 0.15f);
            this.hoverFav = Math.max(0.0f, this.hoverFav - 0.15f);
            return;
        }

        float mx = Position.mouseX();
        float my = Position.mouseY();
        float localMx = (mx - x) / scale + x;
        float localMy = (my - y) / scale + y;
        float bh = 13.0f;
        float bw = 15.0f;

        boolean inRow = localMy >= btnRowY && localMy <= btnRowY + bh;
        boolean hp = inRow && localMx >= contentStartX && localMx <= contentStartX + bw;
        boolean hpl = inRow && localMx >= contentStartX + step && localMx <= contentStartX + step + bw;
        boolean hn = inRow && localMx >= contentStartX + step * 2 && localMx <= contentStartX + step * 2 + bw;
        boolean hf = inRow && localMx >= contentStartX + step * 3 && localMx <= contentStartX + step * 3 + bw;

        this.hoverPrev = Math.max(0.0f, Math.min(1.0f, this.hoverPrev + (hp ? 0.18f : -0.15f)));
        this.hoverPlay = Math.max(0.0f, Math.min(1.0f, this.hoverPlay + (hpl ? 0.18f : -0.15f)));
        this.hoverNext = Math.max(0.0f, Math.min(1.0f, this.hoverNext + (hn ? 0.18f : -0.15f)));
        this.hoverFav = Math.max(0.0f, Math.min(1.0f, this.hoverFav + (hf ? 0.18f : -0.15f)));
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.action != MouseButtonEvent.Action.PRESS) {
            return;
        }
        if (!this.shouldShow() || this.visibility.get() < 0.2f) {
            return;
        }

        EvoPlayerModule mod = ModuleManager.get().get(EvoPlayerModule.class);
        if (mod == null) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen == null) {
            return;
        }

        float scale = this.getScale();
        float x = this.getX();
        float y = this.getY();
        float w = this.width();
        float h = this.height();

        float mx = Position.mouseX();
        float my = Position.mouseY();

        if (mx < x || mx > x + w || my < y || my > y + h) {
            return;
        }

        TrackState state = MusicManager.get().getClient().getState();
        float localMx = (mx - x) / scale + x;
        float localMy = (my - y) / scale + y;
        float rw = (float) this.animatedW.get();
        float rh = (float) this.animatedH.get();

        boolean isExpanded = rh >= 45.0f;
        if (!isExpanded) {
            // Клик по компактному острову разворачивает его в плеер
            if (localMx >= x && localMx <= x + rw && localMy >= y && localMy <= y + rh) {
                this.expanded = true;
                event.cancel();
                return;
            }
        } else {
            // В развернутом плеере:
            // 1. Проверяем клик по таймлайну (скруббинг)
            float barX = x + 9.0f;
            float barY = y + 9.0f + 34.0f + 6.0f;
            float barW = rw - 18.0f;
            float barH = 3.0f;
            if (event.button == 0 && state.durationMs() > 0L) {
                if (localMy >= barY - 4.5f && localMy <= barY + barH + 4.5f && localMx >= barX && localMx <= barX + barW) {
                    this.isScrubbing = true;
                    this.scrubProgress = Math.max(0.0f, Math.min(1.0f, (localMx - barX) / barW));
                    event.cancel();
                    return;
                }
            }

            // 2. Проверяем кнопки управления
            float timeY = barY + 5.0f;
            float btnCenterY = timeY + 11.5f;
            float bw = 15.0f;
            float bh = 13.0f;
            float spacing = 6.0f;
            float step = bw + spacing;
            float totalControlsW = bw * 4 + spacing * 3;
            float btnStartX = x + (rw - totalControlsW) * 0.5f;
            float by = btnCenterY - bh * 0.5f;

            if (event.button == 0 && localMy >= by - 2.0f && localMy <= by + bh + 2.0f) {
                if (localMx >= btnStartX - 2.0f && localMx <= btnStartX + bw + 2.0f) {
                    MusicManager.get().previousTrack();
                    event.cancel();
                    return;
                }
                float pX = btnStartX + step;
                if (localMx >= pX - 2.0f && localMx <= pX + bw + 2.0f) {
                    MusicManager.get().playPause();
                    event.cancel();
                    return;
                }
                float nX = btnStartX + step * 2;
                if (localMx >= nX - 2.0f && localMx <= nX + bw + 2.0f) {
                    MusicManager.get().nextTrack();
                    event.cancel();
                    return;
                }
                float fX = btnStartX + step * 3;
                if (localMx >= fX - 2.0f && localMx <= fX + bw + 2.0f) {
                    MusicManager.get().toggleFavorite();
                    event.cancel();
                    return;
                }
            }

            // 3. Если клик мимо кнопок и таймлайна, а режим карточки «Остров»: сворачиваем назад
            if (mod.cardStyle.is("Остров") && localMx >= x && localMx <= x + rw && localMy >= y && localMy <= y + rh) {
                this.expanded = false;
                event.cancel();
            }
        }
    }
}
