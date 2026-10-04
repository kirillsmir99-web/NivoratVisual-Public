package rtx.nv.api.music;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import rtx.nv.NV;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.input.KeyPressEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.utils.sounds.Sounds;

public final class MusicManager {
    private static final MusicManager INSTANCE = new MusicManager();

    private final CoverTextureManager coverManager = new CoverTextureManager();
    private final NowPlayingClient client = new NowPlayingClient(this.coverManager);
    private final MusicFavoritesManager favorites = MusicFavoritesManager.get();
    private boolean initialized = false;
    private volatile boolean stopped;
    private Thread helperStarter;
    private volatile String lastSourceRequest = "";
    private volatile boolean sourcePending;
    private long lastSourceAttempt;

    private MusicManager() {
    }

    public static MusicManager get() {
        return INSTANCE;
    }

    public CoverTextureManager getCoverManager() {
        return this.coverManager;
    }

    public NowPlayingClient getClient() {
        return this.client;
    }

    public MusicFavoritesManager getFavorites() {
        return this.favorites;
    }

    public void init() {
        if (rtx.nv.ClientEdition.isTrial()) {
            return;
        }
        if (this.initialized) {
            return;
        }
        this.initialized = true;

        this.favorites.init();
        rtx.nv.api.music.lyrics.LyricsManager.get().init();
        MusicPopupBanner.get().init();
        rtx.nv.api.music.subtitles.MusicSubtitlesRenderer.init();

        this.helperStarter = new Thread(() -> {
            try {
                MusicHelperManager.get().start();
            } catch (RuntimeException failure) {
                NV.LOGGER.warn("[MusicManager] Failed to start music helper", failure);
            } finally {
                if (this.stopped) MusicHelperManager.get().stop();
            }
        }, "NV-MusicHelper-Init");
        this.helperStarter.setDaemon(true);
        this.helperStarter.start();

        EventBus.get().subscribe(this);
    }

    public void shutdown() {
        this.stopped = true;
        EventBus.get().unsubscribe(this);
        if (this.helperStarter != null) this.helperStarter.interrupt();
        rtx.nv.api.music.lyrics.LyricsManager.get().shutdown();
        this.client.stop();
        this.coverManager.shutdown();
        MusicHelperManager.get().stop();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.stopped || !event.isPre()) {
            return;
        }

        if (!this.client.isStarted()) {
            int port = MusicHelperManager.get().getPort();
            if (port > 0) {
                this.client.start(port);
            }
        }

        MusicHudModule music = ModuleManager.get().get(MusicHudModule.class);
        if (music != null && this.client.isStarted() && System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            String filter = switch (music.source.getValue()) {
                case "Яндекс Музыка" -> "yandex"; case "VK Музыка" -> "vk";
                case "Spotify" -> "spotify"; case "Браузер" -> "browser"; default -> "auto";
            };
            String request = this.client.getPort() + ":" + filter;
            if (!request.equals(this.lastSourceRequest) && !sourcePending && System.currentTimeMillis() - lastSourceAttempt >= 2000) {
                sourcePending = true;
                lastSourceAttempt = System.currentTimeMillis();
                this.client.setPreferredSource(filter).whenComplete((ok, failure) -> {
                    if (failure == null && Boolean.TRUE.equals(ok)) this.lastSourceRequest = request;
                    sourcePending = false;
                });
            }
        }
        rtx.nv.api.music.subtitles.MusicSubtitlesManager.get().onTick();
    }

    @EventHandler
    public void onKey(KeyPressEvent event) {
        if (event.action != KeyPressEvent.Action.PRESS) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen != null) {
            return;
        }

        MusicHudModule module = ModuleManager.get().get(MusicHudModule.class);
        if (module == null || !module.isEnabled()) {
            return;
        }

        int dialKey = module.dialKey.isBound() ? module.dialKey.getKey() : GLFW.GLFW_KEY_R;
        if (event.keyCode == dialKey) {
            this.openMusicDial();
            event.cancel();
            return;
        }

        if (module.playPauseKey.isBound() && event.keyCode == module.playPauseKey.getKey()) {
            this.playPause();
            event.cancel();
            return;
        }

        if (module.nextKey.isBound() && event.keyCode == module.nextKey.getKey()) {
            this.nextTrack();
            event.cancel();
            return;
        }

        if (module.prevKey.isBound() && event.keyCode == module.prevKey.getKey()) {
            this.previousTrack();
            event.cancel();
        }
    }

    public void playPause() {
        this.client.sendPlayPause();
        Sounds.play("buttonclick");
    }

    public void nextTrack() {
        this.client.sendNext();
        Sounds.play("buttonclick");
    }

    public void previousTrack() {
        this.client.sendPrevious();
        Sounds.play("buttonclick");
    }

    public void seekToSeconds(double seconds) {
        long posMs = (long) (Math.max(0.0, seconds) * 1000.0);
        this.client.sendSeek(posMs);
        Sounds.play("buttonclick");
    }

    public void toggleFavorite() {
        TrackState state = this.client.getState();
        if (!state.isActive()) {
            return;
        }
        boolean added = this.favorites.toggleFavorite(state.artist(), state.title());
        Sounds.play(added ? "pin" : "unpin");

        MusicPopupBanner.get().showFavoriteToast(state.artist(), state.title(), added);
    }

    public void openMusicDial() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.setScreen(new MusicDialScreen());
        }
    }
}
