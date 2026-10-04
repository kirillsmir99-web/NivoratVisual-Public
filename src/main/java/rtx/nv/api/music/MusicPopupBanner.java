package rtx.nv.api.music;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.Position;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.components.MusicHudComp;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class MusicPopupBanner {
    private static final MusicPopupBanner INSTANCE = new MusicPopupBanner();

    private final SmoothAnimation slideAnim = new SmoothAnimation();
    private TrackState displayState = TrackState.INACTIVE;
    private long startTime = 0L;
    private String lastTitle = "";
    private String lastArtist = "";
    private boolean wasPlaying = false;

    // Favorite toast notification
    private final SmoothAnimation favAnim = new SmoothAnimation();
    private final SmoothAnimation favWidthAnim = new SmoothAnimation();
    private final SmoothAnimation favYAnim = new SmoothAnimation();
    private String favTitle = "";
    private String favArtist = "";
    private boolean favAdded = false;
    private long favStartTime = 0L;

    private MusicPopupBanner() {
        this.slideAnim.set(0.0);
        this.favAnim.set(0.0);
        this.favWidthAnim.set(160.0);
        this.favYAnim.set(Position.screenHeight() - 72.0f);
    }

    public static MusicPopupBanner get() {
        return INSTANCE;
    }

    public void init() {
        EventBus.get().subscribe(this);
    }

    public void showFavoriteToast(String artist, String title, boolean added) {
        this.favArtist = artist != null ? artist.trim() : "";
        this.favTitle = title != null ? title.trim() : "";
        this.favAdded = added;
        this.favStartTime = System.currentTimeMillis();
        this.favAnim.run(1.0, 0.36, Easings.BACK_OUT, false);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!event.isPre()) {
            return;
        }

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null || !mod.isEnabled() || !mod.screenPopup.getValue()) {
            return;
        }

        TrackState state = MusicManager.get().getClient().getState();
        if (!state.isActive()) {
            this.wasPlaying = false;
            return;
        }

        boolean nowPlaying = state.isPlaying();
        boolean trackChanged = !state.title().equalsIgnoreCase(this.lastTitle) || !state.artist().equalsIgnoreCase(this.lastArtist);
        boolean unpaused = !this.wasPlaying && nowPlaying;

        if (nowPlaying && (trackChanged || unpaused)) {
            this.trigger(state);
        }

        this.lastTitle = state.title();
        this.lastArtist = state.artist();
        this.wasPlaying = nowPlaying;
    }

    public void trigger(TrackState state) {
        this.displayState = state;
        this.startTime = System.currentTimeMillis();
        this.slideAnim.run(1.0, 0.38, Easings.BACK_OUT, false);
    }

    @EventHandler
    public void onHudRender(HudRenderEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden) {
            return;
        }

        DrawContext drawContext = event.getGraphics();
        if (drawContext == null) {
            return;
        }

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null || !mod.isEnabled()) {
            return;
        }

        this.renderFavoriteToast(drawContext);

        if (!mod.screenPopup.getValue()) {
            return;
        }

        long elapsed = System.currentTimeMillis() - this.startTime;
        long durationMs = (long) (mod.popupDuration.getFloat() * 1000L);
        if (elapsed > durationMs && this.slideAnim.getToValue() > 0.0) {
            this.slideAnim.run(0.0, 0.28, Easings.EXPO_IN, false);
        }

        this.slideAnim.update();
        float slide = (float) this.slideAnim.get();
        if (slide <= 0.005f) {
            return;
        }

        String style = mod.style.getValue();
        boolean isSquare = "Квадратный".equals(style);

        float bw = 210.0f;
        float bh = isSquare ? 40.0f : 42.0f;
        float screenW = Position.screenWidth();
        float screenH = Position.screenHeight();
        float bx = (screenW - bw) * 0.5f;

        boolean isAboveHotbar = "Над хотбаром".equals(mod.popupPosition.getValue());
        float targetY = isAboveHotbar ? (screenH - 72.0f) : 28.0f;
        float startY = isAboveHotbar ? (screenH + 25.0f) : (-bh - 14.0f);
        float by = startY + (targetY - startY) * slide;
        float alpha = Math.min(1.0f, slide * 1.25f);
        float radius = isSquare ? 3.0f : 7.0f;
        rtx.nv.api.drags.HudLayoutCoordinator.reserve("music-banner", bx, by, bw, bh);

        int dominant = MusicManager.get().getCoverManager().getDominantColor();
        int accentA = ClientAccent.gradientA((int) (240 * alpha));
        int accentB = ClientAccent.gradientB((int) (240 * alpha));

        Render2D.beginFrame(drawContext);

        // Soft dominant glow
        if (mod.glow.getValue()) {
            float glowInt = mod.glowIntensity.getFloat();
            int glowCol = MusicHudComp.boostGlowColor(dominant, accentA);
            Render2D.glow(new BuiltGlow(
                bx - 2.0f, by - 2.0f, bw + 4.0f, bh + 4.0f,
                new float[]{radius, radius, radius, radius},
                glowCol, 0.60f * glowInt, 6.0f * glowInt, 0.18f * alpha
            ));
        }

        // Background based on selected style
        if (isSquare) {
            int bg = ColorUtil.rgba(11, 13, 19, (int) (248 * alpha));
            Render2D.rect(bx, by, bw, bh, 0.0f, bg);
            Render2D.rect(bx, by, bw, 1.5f, 0.0f, accentA);
            int borderCol = ColorUtil.rgba(255, 255, 255, (int) (55 * alpha));
            Render2D.outline(bx, by, bw, bh, 0.0f, 1.0f, borderCol);
        } else {
            // Стеклянный
            RectUtil.drawClientRectFixedRadius(bx, by, bw, bh, radius, alpha, 0.0f);
            int borderCol = ColorUtil.rgba(255, 255, 255, (int) (35 * alpha));
            Render2D.outline(bx, by, bw, bh, radius, 1.0f, borderCol);
        }

        // Album cover
        float coverSize = isSquare ? 28.0f : 30.0f;
        float coverX = bx + (6.0f);
        float coverY = by + (bh - coverSize) * 0.5f;
        float coverRad = isSquare ? 2.0f : 4.0f;
        CoverTextureManager coverMgr = MusicManager.get().getCoverManager();
        String coverTex = coverMgr.hasCustomCover() ? "nv:dynamic/album_cover" : "nv:textures/gui/default_cover.png";
        Render2D.image(coverTex, coverX, coverY, coverSize, coverSize, coverRad, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha)));
        Render2D.outline(coverX, coverY, coverSize, coverSize, coverRad, 1.0f, ColorUtil.rgba(255, 255, 255, (int) (45 * alpha)));

        float contentX = coverX + coverSize + (8.0f);
        float contentW = bw - (contentX - bx) - (24.0f);


            // Header label
            Render2D.circle(contentX + 2.0f, by + 8.5f, 1.8f, accentA);
            Fonts.MONTSERRAT_BOLD.msdf("СЕЙЧАС ИГРАЕТ", contentX + 6.5f, by + 6.0f, 4.2f, accentA);

            // Title
            String title = this.displayState.title();
            float titleY = by + 14.5f;
            String dispTitle = MusicHudComp.truncate(title, contentW, 5.8f, true);
            Fonts.MONTSERRAT_BOLD.msdf(dispTitle, contentX, titleY, 5.8f, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

            // Artist
            String artist = this.displayState.artist();
            float artistY = titleY + 8.5f;
            String dispArtist = MusicHudComp.truncate(artist, contentW, 4.8f, false);
            Fonts.MONTSERRAT_MEDIUM.msdf(dispArtist, contentX, artistY, 4.8f, ColorUtil.rgba(170, 175, 190, (int) (210 * alpha)));

        // Animated sound wave bars on right (5 bars with rich dynamics)
        int eqBars = 5;
        float eqW = eqBars * (3.4f);
        float eqX = bx + bw - eqW - (8.0f);
        float eqY = by + (bh - 12.0f) * 0.5f;
        long time = System.currentTimeMillis();
        for (int b = 0; b < eqBars; b++) {
            float barVal = MusicHudComp.computeEqualizerBar(b, eqBars, time);
            float hBar = (2.5f) + barVal * (8.5f);
            int barCol = ColorUtil.lerpColor(accentA, accentB, b / (float) (eqBars - 1));
            Render2D.rect(eqX + b * (3.4f), eqY + (11.0f) - hBar, 2.0f, hBar, isSquare ? 0.0f : 0.8f, ColorUtil.multAlpha(barCol, alpha));
        }

        Render2D.flush();
    }

    private void renderFavoriteToast(DrawContext drawContext) {
        long elapsed = System.currentTimeMillis() - this.favStartTime;
        if (elapsed > 3200L && this.favAnim.getToValue() > 0.0) {
            this.favAnim.run(0.0, 0.25, Easings.EXPO_IN, false);
        }

        this.favAnim.update();
        float slide = (float) this.favAnim.get();
        if (slide <= 0.005f) {
            return;
        }

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        String style = mod != null ? mod.style.getValue() : "Стеклянный";
        boolean isSquare = "Квадратный".equals(style);

        float screenW = Position.screenWidth();
        float screenH = Position.screenHeight();

        String actionText = this.favAdded ? "Добавлено в избранное" : "Удалено из избранного";
        String trackText = this.favArtist.isEmpty() ? this.favTitle : this.favArtist + " • " + this.favTitle;

        float titleW = Fonts.MONTSERRAT_BOLD.width(actionText, 5.0f);
        float subW = Fonts.MONTSERRAT_MEDIUM.width(trackText, 4.3f);
        float contentW = Math.max(titleW, subW);
        float targetFw = Math.max(120.0f, Math.min(270.0f, contentW + 36.0f));

        this.favWidthAnim.run(targetFw, 0.18, Easings.CUBIC_OUT, false);
        this.favWidthAnim.update();
        float fw = (float) this.favWidthAnim.get();
        float fh = 26.0f;
        float fx = (screenW - fw) * 0.5f;

        // Anti-collision queueing: if track banner is active above hotbar, stack favorite toast right above it
        float bannerSlide = (float) this.slideAnim.get();
        boolean bannerAboveHotbar = mod != null && mod.screenPopup.getValue() && "Над хотбаром".equals(mod.popupPosition.getValue());
        float targetY = screenH - 72.0f;
        if (bannerAboveHotbar && bannerSlide > 0.01f) {
            float bannerBh = isSquare ? 40.0f : 42.0f;
            float bannerTargetY = screenH - 72.0f;
            float bannerStartY = screenH + 25.0f;
            float curBannerY = bannerStartY + (bannerTargetY - bannerStartY) * bannerSlide;
            targetY = curBannerY - fh - 6.0f;
        }

        this.favYAnim.run(targetY, 0.22, Easings.CUBIC_OUT, false);
        this.favYAnim.update();
        float animTargetY = (float) this.favYAnim.get();

        float startY = screenH + 20.0f;
        float fy = startY + (animTargetY - startY) * slide;
        float alpha = Math.min(1.0f, slide * 1.3f);
        float rad = isSquare ? 3.0f : 6.0f;

        int glowCol = this.favAdded ? 0xFFFF3366 : 0xFF708090;
        int accentA = ClientAccent.gradientA((int) (240 * alpha));

        Render2D.beginFrame(drawContext);

        if (mod != null && mod.glow.getValue()) {
            float glowInt = mod.glowIntensity.getFloat();
            Render2D.glow(new BuiltGlow(
                fx - 2.0f, fy - 2.0f, fw + 4.0f, fh + 4.0f,
                new float[]{rad, rad, rad, rad},
                glowCol, 0.85f * glowInt, 6.0f * glowInt, 0.35f * alpha
            ));
        }

        if (isSquare) {
            Render2D.rect(fx, fy, fw, fh, 0.0f, ColorUtil.rgba(11, 13, 19, (int) (248 * alpha)));
            Render2D.rect(fx, fy, fw, 1.5f, 0.0f, this.favAdded ? 0xFFFF4477 : accentA);
            int borderCol = this.favAdded
                ? ColorUtil.rgba(255, 68, 120, (int) (140 * alpha))
                : ColorUtil.rgba(255, 255, 255, (int) (55 * alpha));
            Render2D.outline(fx, fy, fw, fh, 0.0f, 1.0f, borderCol);
        } else {
            // Стеклянный
            RectUtil.drawClientRectFixedRadius(fx, fy, fw, fh, rad, alpha, 0.0f);
            int borderCol = this.favAdded
                ? ColorUtil.rgba(255, 68, 120, (int) (150 * alpha))
                : ColorUtil.rgba(255, 255, 255, (int) (32 * alpha));
            Render2D.outline(fx, fy, fw, fh, rad, 1.0f, borderCol);
        }

        float iconSize = 7.5f;
        float iconX = fx + (7.5f);
        float iconY = fy + (5.5f);
        int iconCol = this.favAdded
            ? ColorUtil.rgba(255, 68, 120, (int) (255 * alpha))
            : ColorUtil.rgba(160, 165, 180, (int) (200 * alpha));
        float[] heartBounds = Fonts.NV.msdfBounds(rtx.nv.utils.render.fonts.NvIcons.HEART, iconSize);
        Fonts.NV.msdf(rtx.nv.utils.render.fonts.NvIcons.HEART, fx+12-(heartBounds[0]+heartBounds[2])*.5f, fy+fh*.5f-(heartBounds[1]+heartBounds[3])*.5f, iconSize, iconCol);
        rtx.nv.api.drags.HudLayoutCoordinator.reserve("music-favorite", fx, fy, fw, fh);

        float textX = iconX + iconSize + (7.5f);
        float maxTextW = fw - (textX - fx) - (10.0f);
        float actionSize = 5.0f;
        float trackSize = 4.3f;
        Fonts.MONTSERRAT_BOLD.msdf(actionText, textX, fy + (4.5f), actionSize, ColorUtil.rgba(255, 255, 255, (int) (250 * alpha)));
        String dispSub = truncate(trackText, maxTextW, trackSize);
        Fonts.MONTSERRAT_MEDIUM.msdf(dispSub, textX, fy + (13.5f), trackSize, ColorUtil.rgba(180, 185, 200, (int) (215 * alpha)));

        Render2D.flush();
    }

    private static String truncate(String text, float maxW, float size) {
        if (text == null) return "";
        float w = Fonts.MONTSERRAT_MEDIUM.width(text, size);
        if (w <= maxW) return text;
        String cur = text;
        while (cur.length() > 3 && Fonts.MONTSERRAT_MEDIUM.width(cur + "...", size) > maxW) {
            cur = cur.substring(0, cur.length() - 1);
        }
        return cur + "...";
    }
}
