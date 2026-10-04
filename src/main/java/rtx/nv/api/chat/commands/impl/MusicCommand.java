package rtx.nv.api.chat.commands.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.music.MusicManager;
import rtx.nv.api.music.TrackState;

public final class MusicCommand extends Command {
    public MusicCommand() {
        super("music", "Управление музыкальным плеером и виджетом.", "m", "плеер");
    }

    @Override
    public void execute(String raw, String[] args) {
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0]) || "dial".equalsIgnoreCase(args[0])) {
            MusicManager.get().openMusicDial();
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "play", "pause" -> {
                MusicManager.get().playPause();
                this.logDirect("Воспроизведение переключено.", Formatting.GREEN);
            }
            case "next" -> {
                MusicManager.get().nextTrack();
                this.logDirect("Переключение на следующий трек.", Formatting.AQUA);
            }
            case "prev", "previous" -> {
                MusicManager.get().previousTrack();
                this.logDirect("Переключение на предыдущий трек.", Formatting.AQUA);
            }
            case "fav", "favorite" -> {
                MusicManager.get().toggleFavorite();
            }
            case "hud" -> {
                MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
                if (mod != null) {
                    mod.toggle();
                    this.logDirect("Music HUD: " + (mod.isEnabled() ? "включен" : "выключен"), Formatting.YELLOW);
                }
            }
            case "info" -> {
                TrackState state = MusicManager.get().getClient().getState();
                if (state.isActive()) {
                    this.logDirect("Текущий трек: " + state.artist() + " / " + state.title() + " (" + state.status() + ")", Formatting.GOLD);
                } else {
                    this.logDirect("Сейчас ничего не играет.", Formatting.GRAY);
                }
            }
            default -> this.usage();
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
            "Управление музыкальным проигрывателем.",
            "> .music menu : открыть радиальный переключатель",
            "> .music play : пауза или возобновление",
            "> .music next : следующий трек",
            "> .music prev : предыдущий трек",
            "> .music fav : добавить в избранное",
            "> .music hud : переключить видимость HUD",
            "> .music info : информация о текущем треке"
        );
    }

    @Override
    public Stream<String> tabComplete(String current, String[] args) {
        if (args.length == 1) {
            return Stream.of("menu", "play", "next", "prev", "fav", "hud", "info")
                .filter(s -> s.startsWith(args[0].toLowerCase()));
        }
        return Stream.empty();
    }
}
