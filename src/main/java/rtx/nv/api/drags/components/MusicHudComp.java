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
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.music.CoverTextureManager;
import rtx.nv.api.music.MusicManager;
import rtx.nv.api.music.TrackState;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class MusicHudComp extends Draggable {
    private static final float BASE_W = 224.0f;
    private static final float BASE_H_WITH_CONTROLS = 60.0f;
    private static final float BASE_H_NO_CONTROLS = 46.0f;
    private static final float BASE_H_MODE2_WITH_CONTROLS = 58.0f;
    private static final float BASE_H_MODE2_NO_CONTROLS = 50.0f;
    private final float[] equalizerLevels = new float[12];
    private final float[] equalizerPeaks = new float[12];
    private TrackState displayedState = TrackState.INACTIVE;
    private long equalizerNanos;

    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation animatedW = new SmoothAnimation();
    private final SmoothAnimation animatedH = new SmoothAnimation();
    private boolean lastTargetVisible = false;

    // Autohide tracking
    private String lastKnownTrackTitle = "";
    private String lastKnownTrackArtist = "";
    private boolean wasTrackPlaying = false;
    private long lastTrackChangeMs = System.currentTimeMillis();
    private long lastUserInteractMs = System.currentTimeMillis();
    private long lastActiveMs = System.currentTimeMillis();

    // Scrubber interaction
    private boolean isScrubbing = false;
    private float scrubProgress = 0.0f;
    private float scrubHover = 0.0f;

    // Button hover animations
    private float hoverPrev = 0.0f;
    private float hoverPlay = 0.0f;
    private float hoverNext = 0.0f;
    private float hoverFav = 0.0f;
    private float hoverMenu = 0.0f;

    public MusicHudComp() {
        super("music_hud", Position.screenWidth() - BASE_W - 12.0f, 12.0f);
        this.visibility.set(0.0);
        this.animatedW.set(BASE_W);
        this.animatedH.set(BASE_H_WITH_CONTROLS);
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
        return "Music HUD";
    }

    public float getScale() {
        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        return mod != null ? mod.scale() : 1.0f;
    }

    @Override
    public float width() {
        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);

        return BASE_W * this.getScale();
    }

    @Override
    protected float getX() { return Position.clampX(super.getX(), this.width()); }

    @Override
    protected float getY() { return Position.clampY(super.getY(), this.height()); }

    @Override
    public float height() {
        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);

        boolean hasControls = mod == null || mod.controls.getValue();
        boolean isMode2 = mod != null && "Режим 2".equals(mod.mode.getValue());
        float baseH = isMode2
            ? (hasControls ? BASE_H_MODE2_WITH_CONTROLS : BASE_H_MODE2_NO_CONTROLS)
            : (hasControls ? BASE_H_WITH_CONTROLS : BASE_H_NO_CONTROLS);
        return baseH * this.getScale();
    }

    @Override
    public boolean isInteractive() {
        return this.shouldShow();
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
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
        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null || !mod.isEnabled()) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden) {
            return false;
        }

        if (DragSystem.get().isDragModeActive()) {
            this.lastUserInteractMs = System.currentTimeMillis();
            return true;
        }

        TrackState state = MusicManager.get().getClient().getState();
        if (!state.isActive()) {
            this.wasTrackPlaying = false;
            return false;
        }

        boolean trackChanged = !state.title().equalsIgnoreCase(this.lastKnownTrackTitle)
            || !state.artist().equalsIgnoreCase(this.lastKnownTrackArtist);
        boolean justStarted = !this.wasTrackPlaying && state.isPlaying();

        if (trackChanged || justStarted) {
            this.lastTrackChangeMs = System.currentTimeMillis();
        }
        this.lastKnownTrackTitle = state.title();
        this.lastKnownTrackArtist = state.artist();
        this.wasTrackPlaying = state.isPlaying();

        if (state.isPlaying()) {
            this.lastActiveMs = System.currentTimeMillis();
        }

        if (this.isScrubbing) {
            this.lastUserInteractMs = System.currentTimeMillis();
            return true;
        }

        if (!mod.autoHide.getValue()) {
            return true;
        }

        long hideDelayMs = (long) (mod.hideDelay.getFloat() * 1000.0f);
        if (mod.autoHideMode.is("После смены трека")) {
            long lastTrigger = Math.max(this.lastTrackChangeMs, this.lastUserInteractMs);
            long elapsed = System.currentTimeMillis() - lastTrigger;
            if (state.isPaused()) return true;
            return elapsed < hideDelayMs;
        } else {
            // "При остановке"
            if (state.isPlaying()) {
                return true;
            }
            long lastTrigger = Math.max(this.lastActiveMs, this.lastUserInteractMs);
            long elapsed = System.currentTimeMillis() - lastTrigger;
            return elapsed < hideDelayMs;
        }
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

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null) {
            return;
        }

        float scale = this.getScale();
        float x = this.getX();
        float y = this.getY();
        boolean hasControls = mod.controls.getValue();
        String style = mod.style.getValue();


        boolean isMode2 = "Режим 2".equals(mod.mode.getValue());
        float targetW = BASE_W;
        float targetH = isMode2
            ? (hasControls ? BASE_H_MODE2_WITH_CONTROLS : BASE_H_MODE2_NO_CONTROLS)
            : (hasControls ? BASE_H_WITH_CONTROLS : BASE_H_NO_CONTROLS);

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

        if (isMode2) {
            this.renderMode2Card(drawContext, x, y, rw, rh, alpha, state, mod, style, hasControls, dominant, accentA, accentB);
        } else {
            this.renderStandardCard(drawContext, x, y, rw, rh, alpha, state, mod, style, hasControls, dominant, accentA, accentB);
        }

        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }



    private void renderMode2Card(DrawContext drawContext, float x, float y, float rw, float rh, float alpha,
                                TrackState state, MusicHudModule mod, String style, boolean hasControls,
                                int dominant, int accentA, int accentB) {
        boolean isSquare = "Квадратный".equals(style);
        float cornerRadius = isSquare ? 3.0f : 10.0f;

        // 1. Неоновый ореол в тон обложки
        if (mod.glow.getValue()) {
            float glowInt = mod.glowIntensity.getFloat();
            int glowCol = boostGlowColor(dominant, accentA);
            Render2D.glow(new BuiltGlow(
                x - 2.0f, y - 2.0f, rw + 4.0f, rh + 4.0f,
                new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius},
                glowCol, 0.65f * glowInt, 8.0f * glowInt, 0.22f * alpha
            ));
        }

        // 2. Подложка матового стекла (Frost Glass) с размытием Кавасе или квадратная пластина
        if (isSquare) {
            int bg = ColorUtil.rgba(11, 14, 20, (int) (248 * alpha));
            Render2D.rect(x, y, rw, rh, cornerRadius, bg);
            Render2D.rect(x + 1, y + 7, 1.2f, rh - 14, .6f, ColorUtil.multAlpha(accentA, .65f));
            Render2D.rect(x + 7, y + 1, rw - 14, .6f, .3f, ColorUtil.rgba(255, 255, 255, (int)(24 * alpha)));
            int borderCol = ColorUtil.rgba(255, 255, 255, (int) (26 * alpha));
            Render2D.outline(x, y, rw, rh, cornerRadius, .8f, borderCol);
        } else {
            // Матовое стекло Frost Glass с размытием Кавасе
            RectUtil.drawClientRectFixedRadius(x, y, rw, rh, cornerRadius, alpha, 0.0f);
            Render2D.rect(x + 10, y + 1, rw - 20, .6f, .3f, ColorUtil.rgba(255, 255, 255, (int)(46 * alpha)));
            Render2D.rect(x + 3, y + 8, .55f, rh - 16, .25f, ColorUtil.multAlpha(accentA, .16f));
            int borderCol = ColorUtil.rgba(255, 255, 255, (int) (32 * alpha));
            Render2D.outline(x, y, rw, rh, cornerRadius, 1.0f, borderCol);
        }

        // 3. Обложка альбома (42x42 скругленный квадрат)
        float coverSize = 42.0f;
        float coverX = x + 8.0f;
        float coverY = y + 8.0f;
        float contentStartX = x + 10.0f;

        if (mod.showCover.getValue()) {
            CoverTextureManager coverMgr = MusicManager.get().getCoverManager();
            String coverTex = coverMgr.hasCustomCover() ? "nv:dynamic/album_cover" : "nv:textures/gui/default_cover.png";
            float coverRad = isSquare ? 2.5f : 6.0f;

            Render2D.image(coverTex, coverX, coverY, coverSize, coverSize, coverRad, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));
            Render2D.outline(coverX, coverY, coverSize, coverSize, coverRad, 1.0f, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));

            contentStartX = coverX + coverSize + 8.0f; // x + 58.0f
        }

        // 4. Анимированный аудио-эквалайзер (12 полос в такт треку)
        float eqW = 12 * 3.2f; // 38.4f
        float eqX = x + rw - eqW - 8.0f;
        float eqY = y + 9.5f;
        float eqMaxH = 11.5f;

        if (mod.equalizer.getValue() && state.isActive()) {
            long time = System.currentTimeMillis();
            long now = System.nanoTime();
            float dt = equalizerNanos == 0 ? .016f : Math.min(.1f, (now - equalizerNanos) / 1_000_000_000f);
            equalizerNanos = now;
            for (int b = 0; b < 12; b++) {
                float barVal = state.isPlaying() ? computeEqualizerBar(b, 12, time) : 0f;
                float speed = barVal > equalizerLevels[b] ? 18f : 6f;
                equalizerLevels[b] += (barVal - equalizerLevels[b]) * (1f - (float)Math.exp(-dt * speed));
                equalizerPeaks[b] = Math.max(equalizerLevels[b], equalizerPeaks[b] - dt * .45f);
                float hBar = Math.max(1.2f, equalizerLevels[b] * eqMaxH);
                int barCol = ColorUtil.lerpColor(accentA, accentB, b / 11f);
                float bx = eqX + b * 3.2f;
                Render2D.rect(bx, eqY + eqMaxH - hBar, 1.8f, hBar, .6f, ColorUtil.multAlpha(barCol, state.isPlaying() ? .90f * alpha : .40f * alpha));
                if (equalizerPeaks[b] > .08f) {
                    Render2D.rect(bx, eqY + eqMaxH - equalizerPeaks[b] * eqMaxH - 0.5f, 1.8f, .6f, .3f, ColorUtil.rgba(255, 255, 255, (int)(140 * alpha)));
                }
            }
        }

        // 5. Текст: Название трека и Исполнитель
        float maxTitleW = (mod.equalizer.getValue() && state.isActive() ? eqX - 6.0f : x + rw - 8.0f) - contentStartX;
        float titleY = y + 8.5f;
        float artistY = titleY + 11.0f;

        String title = state.isActive() ? state.title() : "Музыка не играет";
        String artist = state.isActive() ? state.artist() : "Запустите трек в плеере";

        String dispTitle = truncate(title, maxTitleW, 6.4f, true);
        Fonts.MONTSERRAT_BOLD.msdf(dispTitle, contentStartX, titleY, 6.4f, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float artistW = Fonts.MONTSERRAT_MEDIUM.width(artist, 5.2f);
        if (artistW > maxTitleW) {
            String truncArtist = truncate(artist, maxTitleW - 3.0f, 5.2f, false);
            Fonts.MONTSERRAT_MEDIUM.msdf(truncArtist, contentStartX, artistY, 5.2f, ColorUtil.rgba(175, 180, 195, (int) (210 * alpha)));
        } else {
            Fonts.MONTSERRAT_MEDIUM.msdf(artist, contentStartX, artistY, 5.2f, ColorUtil.rgba(175, 180, 195, (int) (210 * alpha)));
        }

        // 6. Таймлайн (Полоса прогресса со скруббингом)
        float barY = y + 32.0f;
        float barH = 3.0f;
        float barW = x + rw - contentStartX - 8.0f;

        MinecraftClient mc = MinecraftClient.getInstance();
        float mx = Position.mouseX();
        float my = Position.mouseY();
        float scale = this.getScale();
        float localMx = (mx - x) / scale + x;
        float localMy = (my - y) / scale + y;

        boolean inScreen = mc != null && mc.currentScreen != null;
        boolean hoverTimeline = inScreen && localMy >= barY - 4.5f && localMy <= barY + barH + 4.5f
            && localMx >= contentStartX && localMx <= contentStartX + barW;

        if (hoverTimeline || this.isScrubbing) {
            this.scrubHover = Math.min(1.0f, this.scrubHover + 0.15f);
        } else {
            this.scrubHover = Math.max(0.0f, this.scrubHover - 0.12f);
        }

        if (this.isScrubbing && inScreen) {
            long handle = mc.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                this.scrubProgress = Math.max(0.0f, Math.min(1.0f, (localMx - contentStartX) / barW));
                this.lastUserInteractMs = System.currentTimeMillis();
            } else {
                this.isScrubbing = false;
                double targetSeconds = this.scrubProgress * (state.durationMs() / 1000.0);
                MusicManager.get().seekToSeconds(targetSeconds);
                this.lastUserInteractMs = System.currentTimeMillis();
            }
        }

        if (mod.timeline.getValue()) {
            float curBarH = barH + this.scrubHover * 1.5f;
            float curBarY = barY - this.scrubHover * 0.75f;
            int barBg = ColorUtil.rgba(255, 255, 255, (int) (28 * alpha));
            Render2D.rect(contentStartX, curBarY, barW, curBarH, isSquare ? 0.0f : 1.5f, barBg);

            float currentProgress = this.isScrubbing ? this.scrubProgress : state.progress();
            if (currentProgress > 0.0f) {
                float fillW = Math.max(2.5f, barW * currentProgress);
                int fillCol = dominant != 0 ? ColorUtil.lerpColor(dominant, accentA, 0.25f) : ColorUtil.lerpColor(accentA, accentB, currentProgress);
                Render2D.rect(contentStartX, curBarY, fillW, curBarH, isSquare ? 0.0f : 1.5f, ColorUtil.multAlpha(fillCol, alpha));

                // Glowing Scrubber Knob
                if (this.scrubHover > 0.05f || this.isScrubbing) {
                    float knobX = contentStartX + barW * currentProgress;
                    float knobY = curBarY + curBarH * 0.5f;
                    float knobRad = 3.0f + this.scrubHover * 0.8f;
                    Render2D.circle(knobX, knobY, knobRad, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
                    Render2D.circle(knobX, knobY, knobRad + 1.2f, ColorUtil.multAlpha(fillCol, 0.7f * alpha));

                    // Floating Tooltip Badge when dragging
                    if (this.isScrubbing) {
                        long scrubMs = (long) (this.scrubProgress * state.durationMs());
                        String scrubTime = TrackState.formatTime(scrubMs);
                        float tipW = Fonts.MONTSERRAT_BOLD.width(scrubTime, 4.5f) + 6.0f;
                        float tipH = 9.5f;
                        float tipX = knobX - tipW * 0.5f;
                        float tipY = knobY - knobRad - tipH - 2.5f;
                        Render2D.rect(tipX, tipY, tipW, tipH, isSquare ? 0.0f : 2.0f, ColorUtil.rgba(14, 16, 22, (int) (245 * alpha)));
                        Render2D.outline(tipX, tipY, tipW, tipH, isSquare ? 0.0f : 2.0f, 0.8f, fillCol);
                        Fonts.MONTSERRAT_BOLD.msdf(scrubTime, tipX + 3.0f, tipY + 1.5f, 4.5f, 0xFFFFFFFF);
                    }
                }
            }
        }

        // 7. Кнопки управления (Previous, Play/Pause, Next, Favorite)
        float btnRowY = y + 39.5f;
        if (hasControls) {
            this.updateButtonHovers(x, y, this.getScale(), btnRowY, contentStartX);

            float btnRad = isSquare ? 0.0f : 3.5f;
            this.renderButton(contentStartX, btnRowY, HudBtnType.PREV, this.hoverPrev, alpha, accentA, btnRad);
            HudBtnType playType = state.isPlaying() ? HudBtnType.PAUSE : HudBtnType.PLAY;
            this.renderButton(contentStartX + 18.0f, btnRowY, playType, this.hoverPlay, alpha, accentA, btnRad);
            this.renderButton(contentStartX + 36.0f, btnRowY, HudBtnType.NEXT, this.hoverNext, alpha, accentA, btnRad);

            boolean isFav = state.isActive() && MusicManager.get().getFavorites().isFavorite(state.artist(), state.title());
            int favColor = isFav ? 0xFFFF4477 : accentA;
            this.renderButton(contentStartX + 54.0f, btnRowY, HudBtnType.FAV, this.hoverFav, alpha, favColor, btnRad);
        }

        // 8. Таймер воспроизведения (справа в строке кнопок)
        long posMs = this.isScrubbing ? (long) (this.scrubProgress * state.durationMs()) : state.currentPositionMs();
        String posStr = TrackState.formatTime(posMs);
        String slashStr = " / ";
        String durStr = state.formattedDuration();

        float timeSize = 5.2f;
        float posW = Fonts.MONTSERRAT_BOLD.width(posStr, timeSize);
        float slashW = Fonts.MONTSERRAT_MEDIUM.width(slashStr, timeSize);
        float durW = Fonts.MONTSERRAT_BOLD.width(durStr, timeSize);
        float totalTimeW = posW + slashW + durW;

        float badgeW = totalTimeW + 8.0f;
        float badgeH = 12.0f;
        float badgeX = x + rw - badgeW - 8.0f;
        float badgeY = hasControls ? btnRowY + 0.5f : (barY + 6.0f);

        if (isSquare) {
            Render2D.rect(badgeX, badgeY, badgeW, badgeH, 0.0f, ColorUtil.rgba(11, 14, 20, (int) (245 * alpha)));
            Render2D.outline(badgeX, badgeY, badgeW, badgeH, 0.0f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (50 * alpha)));
        } else {
            Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.0f, ColorUtil.rgba(14, 18, 26, (int) (180 * alpha)));
            Render2D.outline(badgeX, badgeY, badgeW, badgeH, 3.0f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));
        }

        float[] timeBounds = Fonts.MONTSERRAT_BOLD.msdfBounds(posStr + slashStr + durStr, timeSize);
        float timeTextY = badgeY + badgeH * .5f - (timeBounds[1] + timeBounds[3]) * .5f;
        float curX = badgeX + 4.0f;
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (170 * alpha));
        Fonts.MONTSERRAT_BOLD.msdf(posStr, curX + 0.5f, timeTextY + 0.5f, timeSize, shadowCol);
        Fonts.MONTSERRAT_BOLD.msdf(posStr, curX, timeTextY, timeSize, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        Fonts.MONTSERRAT_MEDIUM.msdf(slashStr, curX + posW + 0.5f, timeTextY + 0.5f, timeSize, shadowCol);
        Fonts.MONTSERRAT_MEDIUM.msdf(slashStr, curX + posW, timeTextY, timeSize, ColorUtil.rgba(180, 190, 210, (int) (230 * alpha)));

        Fonts.MONTSERRAT_BOLD.msdf(durStr, curX + posW + slashW + 0.5f, timeTextY + 0.5f, timeSize, shadowCol);
        Fonts.MONTSERRAT_BOLD.msdf(durStr, curX + posW + slashW, timeTextY, timeSize, ColorUtil.rgba(235, 240, 250, (int) (255 * alpha)));
    }

    private void renderStandardCard(DrawContext drawContext, float x, float y, float rw, float rh, float alpha,
                                    TrackState state, MusicHudModule mod, String style, boolean hasControls,
                                    int dominant, int accentA, int accentB) {
        boolean isSquare = "Квадратный".equals(style);
        float cornerRadius = isSquare ? 4.0f : 9.0f;

        // Vivid neon glow
        if (mod.glow.getValue()) {
            float glowInt = mod.glowIntensity.getFloat();
            int glowCol = boostGlowColor(dominant, accentA);
            Render2D.glow(new BuiltGlow(
                x - 2.0f, y - 2.0f, rw + 4.0f, rh + 4.0f,
                new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius},
                glowCol, 0.60f * glowInt, 6.0f * glowInt, 0.18f * alpha
            ));
        }

        // Card background
        if (isSquare) {
            int bg = ColorUtil.rgba(11, 13, 19, (int) (248 * alpha));
            Render2D.rect(x, y, rw, rh, cornerRadius, bg);
            Render2D.rect(x + 1, y + 7, 1.2f, rh - 14, .6f, ColorUtil.multAlpha(accentA, .65f));
            Render2D.rect(x + 7, y + 1, rw - 14, .6f, .3f, ColorUtil.rgba(255, 255, 255, (int)(24 * alpha)));
            int borderCol = ColorUtil.rgba(255, 255, 255, (int) (24 * alpha));
            Render2D.outline(x, y, rw, rh, cornerRadius, .65f, borderCol);
        } else {
            // Стеклянный
            RectUtil.drawClientRectFixedRadius(x, y, rw, rh, cornerRadius, alpha, 0.0f);
            Render2D.rect(x + 8, y + 1, rw - 16, .6f, .3f, ColorUtil.rgba(255, 255, 255, (int)(46 * alpha)));
            Render2D.rect(x + 3, y + 8, .55f, rh - 16, .25f, ColorUtil.multAlpha(accentA, .16f));
            int borderCol = ColorUtil.rgba(255, 255, 255, (int) (28 * alpha));
            Render2D.outline(x, y, rw, rh, cornerRadius, 1.0f, borderCol);
        }

        // Cover artwork (clean, full, crisp without obstructing rectangle)
        float coverSize = 36.0f;
        float coverX = x + 6.0f;
        float coverY = y + 6.0f;
        float contentStartX = x + 8.0f;

        if (mod.showCover.getValue()) {
            CoverTextureManager coverMgr = MusicManager.get().getCoverManager();
            String coverTex = coverMgr.hasCustomCover() ? "nv:dynamic/album_cover" : "nv:textures/gui/default_cover.png";
            float coverRad = isSquare ? 2.0f : 6.0f;

            Render2D.image(coverTex, coverX, coverY, coverSize, coverSize, coverRad, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));
            Render2D.outline(coverX, coverY, coverSize, coverSize, coverRad, 1.0f, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));

            contentStartX = coverX + coverSize + 8.0f;
        }

        if (hasControls && mod.showCover.getValue() && state.isActive()) {
            String status = state.isPaused() ? "Пауза" : "Играет";
            int statusColor = state.isPaused() ? ColorUtil.rgba(190, 193, 206, (int)(210 * alpha)) : accentA;
            float[] statusBounds = Fonts.MONTSERRAT_SEMIBOLD.msdfBounds(status, 5.2f);
            Fonts.MONTSERRAT_SEMIBOLD.msdf(status, coverX+coverSize*.5f-(statusBounds[0]+statusBounds[2])*.5f, y+rh-8f-(statusBounds[1]+statusBounds[3])*.5f, 5.2f, statusColor);
        }

        // Twelve smooth bands with independent peak release.
        float eqW = 12 * 2.1f;
        float eqX = x + rw - eqW - 8.0f;
        float eqY = y + 7.0f;

        if (mod.equalizer.getValue() && state.isActive()) {
            long time = System.currentTimeMillis();
            long now = System.nanoTime();
            float dt = equalizerNanos == 0 ? .016f : Math.min(.1f, (now - equalizerNanos) / 1_000_000_000f);
            equalizerNanos = now;
            for (int b = 0; b < 12; b++) {
                float barVal = state.isPlaying() ? computeEqualizerBar(b, 12, time) : 0f;
                float speed = barVal > equalizerLevels[b] ? 17f : 6f;
                equalizerLevels[b] += (barVal - equalizerLevels[b]) * (1f - (float)Math.exp(-dt * speed));
                equalizerPeaks[b] = Math.max(equalizerLevels[b], equalizerPeaks[b] - dt * .45f);
                float hBar = 1f + equalizerLevels[b] * 9f;
                int barCol = ColorUtil.lerpColor(accentA, accentB, b / 11f);
                float bx = eqX + b * 2.1f;
                Render2D.rect(bx, eqY + 11f - hBar, 1.25f, hBar, .55f, ColorUtil.multAlpha(barCol, state.isPlaying() ? .88f : .38f));
                if (equalizerPeaks[b] > .08f)
                    Render2D.rect(bx, eqY + 10f - equalizerPeaks[b] * 9f, 1.25f, .55f, .25f, ColorUtil.rgba(255, 255, 255, (int)(100 * alpha)));
            }
        }

        float maxTitleW = (mod.equalizer.getValue() && state.isActive() ? eqX - 6.0f : x + rw - 8.0f) - contentStartX;
        float titleY = y + 6.0f;
        float artistY = titleY + 9.5f;

        String title = state.isActive() ? state.title() : "Музыка не играет";
        String artist = state.isActive() ? state.artist() : "Запустите трек в плеере";

        String dispTitle = truncate(title, maxTitleW, 6.4f, true);
        Fonts.MONTSERRAT_BOLD.msdf(dispTitle, contentStartX, titleY, 6.4f, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float artistW = Fonts.MONTSERRAT_MEDIUM.width(artist, 5.2f);
        if (artistW > maxTitleW) {
            String truncArtist = truncate(artist, maxTitleW - 3.0f, 5.2f, false);
            Fonts.MONTSERRAT_MEDIUM.msdf(truncArtist, contentStartX, artistY, 5.2f, ColorUtil.rgba(175, 180, 195, (int) (210 * alpha)));
        } else {
            Fonts.MONTSERRAT_MEDIUM.msdf(artist, contentStartX, artistY, 5.2f, ColorUtil.rgba(175, 180, 195, (int) (210 * alpha)));
        }

        // Timeline and Interactive Scrubber
        float barY = artistY + 9.5f;
        float barH = 3.0f;
        float barW = x + rw - contentStartX - 8.0f;

        MinecraftClient mc = MinecraftClient.getInstance();
        float mx = Position.mouseX();
        float my = Position.mouseY();
        float scale = this.getScale();
        float localMx = (mx - x) / scale + x;
        float localMy = (my - y) / scale + y;

        boolean inScreen = mc != null && mc.currentScreen != null;
        boolean hoverTimeline = inScreen && localMy >= barY - 4.0f && localMy <= barY + barH + 4.0f
            && localMx >= contentStartX && localMx <= contentStartX + barW;

        if (hoverTimeline || this.isScrubbing) {
            this.scrubHover = Math.min(1.0f, this.scrubHover + 0.15f);
        } else {
            this.scrubHover = Math.max(0.0f, this.scrubHover - 0.12f);
        }

        if (this.isScrubbing && inScreen) {
            long handle = mc.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                this.scrubProgress = Math.max(0.0f, Math.min(1.0f, (localMx - contentStartX) / barW));
                this.lastUserInteractMs = System.currentTimeMillis();
            } else {
                this.isScrubbing = false;
                double targetSeconds = this.scrubProgress * (state.durationMs() / 1000.0);
                MusicManager.get().seekToSeconds(targetSeconds);
                this.lastUserInteractMs = System.currentTimeMillis();
            }
        }

        if (mod.timeline.getValue()) {
            float curBarH = barH + this.scrubHover * 1.5f;
            float curBarY = barY - this.scrubHover * 0.75f;
            int barBg = ColorUtil.rgba(255, 255, 255, (int) (28 * alpha));
            Render2D.rect(contentStartX, curBarY, barW, curBarH, isSquare ? 0.0f : 1.5f, barBg);

            float currentProgress = this.isScrubbing ? this.scrubProgress : state.progress();
            if (currentProgress > 0.0f) {
                float fillW = Math.max(2.5f, barW * currentProgress);
                int fillCol = ColorUtil.lerpColor(accentA, accentB, currentProgress);
                Render2D.rect(contentStartX, curBarY, fillW, curBarH, isSquare ? 0.0f : 1.5f, ColorUtil.multAlpha(fillCol, alpha));

                // Glowing Scrubber Knob
                if (this.scrubHover > 0.05f || this.isScrubbing) {
                    float knobX = contentStartX + barW * currentProgress;
                    float knobY = curBarY + curBarH * 0.5f;
                    float knobRad = 3.0f + this.scrubHover * 0.8f;
                    Render2D.circle(knobX, knobY, knobRad, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
                    Render2D.circle(knobX, knobY, knobRad + 1.2f, ColorUtil.multAlpha(fillCol, 0.7f * alpha));

                    // Floating Tooltip Badge when dragging
                    if (this.isScrubbing) {
                        long scrubMs = (long) (this.scrubProgress * state.durationMs());
                        String scrubTime = TrackState.formatTime(scrubMs);
                        float tipW = Fonts.MONTSERRAT_BOLD.width(scrubTime, 4.5f) + 6.0f;
                        float tipH = 9.5f;
                        float tipX = knobX - tipW * 0.5f;
                        float tipY = knobY - knobRad - tipH - 2.5f;
                        Render2D.rect(tipX, tipY, tipW, tipH, isSquare ? 0.0f : 2.0f, ColorUtil.rgba(14, 16, 22, (int) (245 * alpha)));
                        Render2D.outline(tipX, tipY, tipW, tipH, isSquare ? 0.0f : 2.0f, 0.8f, fillCol);
                        Fonts.MONTSERRAT_BOLD.msdf(scrubTime, tipX + 3.0f, tipY + 1.5f, 4.5f, 0xFFFFFFFF);
                    }
                }
            }
        }

        // Controls row
        float btnRowY = y + rh - 17.0f;
        if (hasControls) {
            this.updateButtonHovers(x, y, this.getScale(), btnRowY, contentStartX);

            float btnRad = isSquare ? 0.0f : 3.5f;
            this.renderButton(contentStartX, btnRowY, HudBtnType.PREV, this.hoverPrev, alpha, accentA, btnRad);
            HudBtnType playType = state.isPlaying() ? HudBtnType.PAUSE : HudBtnType.PLAY;
            this.renderButton(contentStartX + 18.0f, btnRowY, playType, this.hoverPlay, alpha, accentA, btnRad);
            this.renderButton(contentStartX + 36.0f, btnRowY, HudBtnType.NEXT, this.hoverNext, alpha, accentA, btnRad);

            boolean isFav = state.isActive() && MusicManager.get().getFavorites().isFavorite(state.artist(), state.title());
            int favColor = isFav ? 0xFFFF4477 : accentA;
            this.renderButton(contentStartX + 54.0f, btnRowY, HudBtnType.FAV, this.hoverFav, alpha, favColor, btnRad);

            this.renderButton(contentStartX + 72.0f, btnRowY, HudBtnType.MENU, this.hoverMenu, alpha, accentA, btnRad);
        }

        // High-contrast, bright time display badge matching button row height
        long posMs = this.isScrubbing ? (long) (this.scrubProgress * state.durationMs()) : state.currentPositionMs();
        String posStr = TrackState.formatTime(posMs);
        String slashStr = " / ";
        String durStr = state.formattedDuration();

        float timeSize = 5.8f;
        float posW = Fonts.MONTSERRAT_BOLD.width(posStr, timeSize);
        float slashW = Fonts.MONTSERRAT_MEDIUM.width(slashStr, timeSize);
        float durW = Fonts.MONTSERRAT_BOLD.width(durStr, timeSize);
        float totalTimeW = posW + slashW + durW;

        float badgeW = totalTimeW + 10.0f;
        float badgeH = 13.0f;
        float badgeX = x + rw - badgeW - 8.0f;
        float badgeY = hasControls ? btnRowY : (barY + 6.0f);

        if (isSquare) {
            // High-tech Cyberplate
            Render2D.rect(badgeX, badgeY, badgeW, badgeH, 0.0f, ColorUtil.rgba(11, 14, 20, (int) (245 * alpha)));
            Render2D.outline(badgeX, badgeY, badgeW, badgeH, 0.0f, 1.0f, ColorUtil.rgba(255, 255, 255, (int) (60 * alpha)));
        } else {
            // Frosted glass capsule with dark backing for contrast
            Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.5f, ColorUtil.rgba(14, 18, 26, (int) (190 * alpha)));
            Render2D.outline(badgeX, badgeY, badgeW, badgeH, 3.5f, 0.8f, ColorUtil.rgba(255, 255, 255, (int) (45 * alpha)));
        }

        float[] timeBounds = Fonts.MONTSERRAT_BOLD.msdfBounds(posStr + slashStr + durStr, timeSize);
        float timeTextY = badgeY + badgeH * .5f - (timeBounds[1] + timeBounds[3]) * .5f;
        float curX = badgeX + 5.0f;
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (170 * alpha));
        Fonts.MONTSERRAT_BOLD.msdf(posStr, curX + 0.5f, timeTextY + 0.5f, timeSize, shadowCol);
        Fonts.MONTSERRAT_BOLD.msdf(posStr, curX, timeTextY, timeSize, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        Fonts.MONTSERRAT_MEDIUM.msdf(slashStr, curX + posW + 0.5f, timeTextY + 0.5f, timeSize, shadowCol);
        Fonts.MONTSERRAT_MEDIUM.msdf(slashStr, curX + posW, timeTextY, timeSize, ColorUtil.rgba(180, 190, 210, (int) (230 * alpha)));

        Fonts.MONTSERRAT_BOLD.msdf(durStr, curX + posW + slashW + 0.5f, timeTextY + 0.5f, timeSize, shadowCol);
        Fonts.MONTSERRAT_BOLD.msdf(durStr, curX + posW + slashW, timeTextY, timeSize, ColorUtil.rgba(235, 240, 250, (int) (255 * alpha)));
    }

    public static int boostGlowColor(int color, int fallbackAccent) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);

        float sat = Math.max(0.68f, hsb[1]);
        float bri = Math.max(0.85f, hsb[2]);

        if (hsb[1] < 0.15f) {
            int boosted = java.awt.Color.HSBtoRGB(hsb[0], sat, bri);
            return ColorUtil.lerpColor(boosted, fallbackAccent, 0.70f);
        }

        int rgb = java.awt.Color.HSBtoRGB(hsb[0], sat, bri);
        return (color & 0xFF000000) | (rgb & 0x00FFFFFF);
    }

    public static float computeEqualizerBar(int barIndex, int totalBars, long timeMs) {
        return rtx.nv.api.music.AudioSpectrum.level(barIndex, totalBars);
    }

    private enum HudBtnType {
        PREV, PLAY, PAUSE, NEXT, FAV, MENU
    }

    private void renderButton(float bx, float by, HudBtnType type, float hover, float alpha, int accent, float radius) {
        float bw = 15.0f;
        float bh = 13.0f;

        int bg = ColorUtil.lerpColor(
            ColorUtil.rgba(255, 255, 255, (int) (14 * alpha)),
            ColorUtil.rgba(255, 255, 255, (int) (38 * alpha)),
            hover
        );
        if (type == HudBtnType.PLAY || type == HudBtnType.PAUSE) bg = ColorUtil.multAlpha(accent, .24f + .16f * hover);
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
        String icon = switch (type) {
            case PREV -> NvIcons.PREVIOUS;
            case PLAY -> NvIcons.PLAY;
            case PAUSE -> NvIcons.PAUSE;
            case NEXT -> NvIcons.NEXT;
            case FAV -> NvIcons.HEART;
            case MENU -> NvIcons.MORE;
        };

        float[] bounds = Fonts.NV.msdfBounds(icon, iconSize);
        Fonts.NV.msdf(icon, cx - (bounds[0] + bounds[2]) * .5f,
            cy - (bounds[1] + bounds[3]) * .5f, iconSize, textCol);
    }

    private void updateButtonHovers(float x, float y, float scale, float btnRowY, float contentStartX) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen == null) {
            this.hoverPrev = Math.max(0.0f, this.hoverPrev - 0.15f);
            this.hoverPlay = Math.max(0.0f, this.hoverPlay - 0.15f);
            this.hoverNext = Math.max(0.0f, this.hoverNext - 0.15f);
            this.hoverFav = Math.max(0.0f, this.hoverFav - 0.15f);
            this.hoverMenu = Math.max(0.0f, this.hoverMenu - 0.15f);
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
        boolean hpl = inRow && localMx >= contentStartX + 18.0f && localMx <= contentStartX + 18.0f + bw;
        boolean hn = inRow && localMx >= contentStartX + 36.0f && localMx <= contentStartX + 36.0f + bw;
        boolean hf = inRow && localMx >= contentStartX + 54.0f && localMx <= contentStartX + 54.0f + bw;
        boolean hm = inRow && localMx >= contentStartX + 72.0f && localMx <= contentStartX + 72.0f + bw;

        this.hoverPrev = Math.max(0.0f, Math.min(1.0f, this.hoverPrev + (hp ? 0.18f : -0.15f)));
        this.hoverPlay = Math.max(0.0f, Math.min(1.0f, this.hoverPlay + (hpl ? 0.18f : -0.15f)));
        this.hoverNext = Math.max(0.0f, Math.min(1.0f, this.hoverNext + (hn ? 0.18f : -0.15f)));
        this.hoverFav = Math.max(0.0f, Math.min(1.0f, this.hoverFav + (hf ? 0.18f : -0.15f)));
        this.hoverMenu = Math.max(0.0f, Math.min(1.0f, this.hoverMenu + (hm ? 0.18f : -0.15f)));
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.action != MouseButtonEvent.Action.PRESS) {
            return;
        }
        if (!this.shouldShow() || this.visibility.get() < 0.2f) {
            return;
        }

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
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

        // Check if mouse inside widget
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return;
        }

        this.lastUserInteractMs = System.currentTimeMillis();



        boolean isMode2 = "Режим 2".equals(mod.mode.getValue());
        TrackState state = MusicManager.get().getClient().getState();
        float localMx = (mx - x) / scale + x;
        float localMy = (my - y) / scale + y;
        float rw = BASE_W;
        float contentStartX = mod.showCover.getValue() ? (isMode2 ? x + 8.0f + 42.0f + 8.0f : x + 6.0f + 36.0f + 8.0f) : (isMode2 ? x + 10.0f : x + 8.0f);

        // Check timeline scrub click
        if (mod.timeline.getValue() && event.button == 0 && state.durationMs() > 0L) {
            float barY;
            float barH = 3.0f;
            float barW = x + rw - contentStartX - 8.0f;
            if (isMode2) {
                barY = y + 32.0f;
            } else {
                float artistY = y + 6.0f + 9.5f;
                barY = artistY + 9.5f;
            }
            if (localMy >= barY - 4.5f && localMy <= barY + barH + 4.5f && localMx >= contentStartX && localMx <= contentStartX + barW) {
                this.isScrubbing = true;
                this.scrubProgress = Math.max(0.0f, Math.min(1.0f, (localMx - contentStartX) / barW));
                event.cancel();
                return;
            }
        }

        if (!mod.controls.getValue()) {
            return;
        }

        if (event.button != 0) {
            return;
        }

        float rh = (float) this.animatedH.get();
        float btnRowY = isMode2 ? (y + 39.5f) : (y + rh - 17.0f);
        float bh = 13.0f;
        float bw = 15.0f;

        if (localMy < btnRowY - 2.0f || localMy > btnRowY + bh + 2.0f) {
            return;
        }

        if (localMx >= contentStartX - 2.0f && localMx <= contentStartX + bw + 2.0f) {
            MusicManager.get().previousTrack();
            event.cancel();
            return;
        }
        if (localMx >= contentStartX + 18.0f - 2.0f && localMx <= contentStartX + 18.0f + bw + 2.0f) {
            MusicManager.get().playPause();
            event.cancel();
            return;
        }
        if (localMx >= contentStartX + 36.0f - 2.0f && localMx <= contentStartX + 36.0f + bw + 2.0f) {
            MusicManager.get().nextTrack();
            event.cancel();
            return;
        }
        if (localMx >= contentStartX + 54.0f - 2.0f && localMx <= contentStartX + 54.0f + bw + 2.0f) {
            MusicManager.get().toggleFavorite();
            event.cancel();
            return;
        }
        if (!isMode2 && localMx >= contentStartX + 72.0f - 2.0f && localMx <= contentStartX + 72.0f + bw + 2.0f) {
            MusicManager.get().openMusicDial();
            event.cancel();
        }
    }

    public static String truncate(String text, float maxW, float size, boolean bold) {
        if (text == null || maxW <= 0) return "";
        var font = bold ? Fonts.MONTSERRAT_BOLD : Fonts.MONTSERRAT_MEDIUM;
        if (font.width(text, size) <= maxW) return text;
        String suffix = "…";
        if (font.width(suffix, size) > maxW) return "";
        int low = 0, high = text.codePointCount(0, text.length());
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            int end = text.offsetByCodePoints(0, middle);
            if (font.width(text.substring(0, end) + suffix, size) <= maxW) low = middle;
            else high = middle - 1;
        }
        return text.substring(0, text.offsetByCodePoints(0, low)) + suffix;
    }
}
