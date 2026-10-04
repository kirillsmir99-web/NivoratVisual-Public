package rtx.nv.utils.sounds;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.sound.SoundEvent;
import rtx.nv.api.modules.impl.Utils.ClientSounds;
import rtx.nv.utils.sounds.SoundManager;

public final class Sounds {
    private static final Map<String, Sounds.Entry> REG = new LinkedHashMap<String, Sounds.Entry>();
    private static final Map<String, Long> COOLDOWN_MS;
    private static final Map<String, Long> LAST_PLAY_MS;

    private Sounds() {
    }

    static {
        Sounds.register("command_error", SoundManager.COMMAND_ERROR, 1.0f, 1.0f);
        Sounds.register("slider", SoundManager.SERENE_SLIDER, 1.0f, 1.0f);
        Sounds.register("gui_open", SoundManager.SERENE_OPEN, 1.0f, 1.0f);
        Sounds.register("gui_close", SoundManager.SERENE_CLOSE, 1.0f, 1.0f);
        Sounds.register("settings_open", SoundManager.SERENE_DROPDOWN_OPEN, 1.0f, 1.0f);
        Sounds.register("settings_close", SoundManager.SERENE_DROPDOWN_CLOSE, 1.0f, 1.0f);
        Sounds.register("module_settings_open", SoundManager.SERENE_OPEN, 1.0f, 1.0f);
        Sounds.register("module_settings_close", SoundManager.SERENE_CLOSE, 1.0f, 1.0f);
        Sounds.register("search_typing", SoundManager.SERENE_SLIDER, 0.7f, 1.1f);
        Sounds.register("select_category", SoundManager.SERENE_CATEGORY, 1.0f, 1.0f);
        Sounds.register("buttonclick", SoundManager.SERENE_BUTTON, 1.0f, 1.0f);
        Sounds.register("button_click", SoundManager.SERENE_BUTTON, 1.0f, 1.0f);
        Sounds.register("pin", SoundManager.SERENE_PIN, 1.0f, 1.0f);
        Sounds.register("unpin", SoundManager.SERENE_UNPIN, 1.0f, 1.0f);
        COOLDOWN_MS = Map.of("slider", 40L, "search_typing", 40L);
        LAST_PLAY_MS = new LinkedHashMap<String, Long>();
    }

    public static void register(String string, SoundEvent soundEvent, float f, float f2) {
        REG.put(string, new Sounds.Entry(soundEvent, f, f2));
    }

    public static void play(String string) {
        if ("click".equals(string) || "btn_click".equals(string)) {
            string = "buttonclick";
        }
        ClientSounds clientSounds;
        if (!ClientSounds.isAllowed(string)) {
            return;
        }
        Sounds.Entry entry = REG.get(string);
        if (entry == null) {
            return;
        }
        Long l = COOLDOWN_MS.get(string);
        if (l != null) {
            long l2 = System.currentTimeMillis();
            Long l3 = LAST_PLAY_MS.get(string);
            if (l3 != null && l2 - l3 < l) {
                return;
            }
            LAST_PLAY_MS.put(string, l2);
        }
        float f = (clientSounds = ClientSounds.getInstance()) != null ? clientSounds.getVolumeFor(string) : 1.0f;
        SoundEvent soundEvent = clientSounds != null ? clientSounds.resolveSoundEvent(string, entry.event) : entry.event;
        SoundManager.playSoundDirect(soundEvent, entry.volume * f, entry.pitch);
    }


    public static final class Entry {
        final SoundEvent event;
        final float volume;
        final float pitch;
    
        Entry(SoundEvent soundEvent, float f, float f2) {
            this.event = soundEvent;
            this.volume = f;
            this.pitch = f2;
        }
    }
}

