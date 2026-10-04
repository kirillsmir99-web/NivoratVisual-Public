package rtx.nv.utils.sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
public final class SoundManager {
    public static final SoundEvent NV_TACTICAL = SoundManager.sound("nv_tactical");
    public static final SoundEvent BELL = SoundManager.sound("bell");
    public static final SoundEvent IMPULSE = SoundManager.sound("impulse");
    public static final SoundEvent POP = SoundManager.sound("pop");
    public static final SoundEvent CRYSTAL = SoundManager.sound("crystal");
    public static final SoundEvent RUST = SoundManager.sound("rust");
    public static final SoundEvent BUBBLE = SoundManager.sound("bubble");
    public static final SoundEvent CLICK = SoundManager.sound("click");
    public static final SoundEvent COD = SoundManager.sound("cod");
    public static final SoundEvent COMMAND_ERROR = SoundManager.sound("command_error");
    public static final SoundEvent NV_GLASS_ERROR = SoundManager.sound("nv_glass_error");
    public static final SoundEvent NV_GLASS_NOTIFY = SoundManager.sound("nv_glass_notify");
    public static final SoundEvent NOTIFICATION = SoundManager.sound("welcome");
    public static final SoundEvent NOTIFICATION_LOW = SoundManager.sound("notification_low");
    public static final SoundEvent PLAYER_PING = SoundManager.sound("player_ping");
    public static final SoundEvent NOTIFICATION_DIRECT = SoundManager.sound("notif");
    public static final SoundEvent LOW = SoundManager.sound("low");
    public static final SoundEvent PLAYERPING = SoundManager.sound("playerping");
    public static final SoundEvent FRAG_EFFECT_ECHO_MAIN = SoundManager.sound("frag_effect_echo_main");
    public static final SoundEvent FRAG_EFFECT_KNOCK_MAIN = SoundManager.sound("frag_effect_knock_main");
    public static final SoundEvent FRAG_EFFECT_PULSE = SoundManager.sound("frag_effect_pulse");
    public static final SoundEvent FRAG_EFFECT_SPARKS_COLLISION = SoundManager.sound("frag_effect_sparks_collision");
    public static final SoundEvent RIBBIT_AMBIENT = SoundManager.sound("entity.ribbit.ambient");
    public static final SoundEvent RIBBIT_STEP = SoundManager.sound("entity.ribbit.step");
    public static final SoundEvent RIBBIT_HURT = SoundManager.sound("entity.ribbit.hurt");
    public static final SoundEvent RIBBIT_DEATH = SoundManager.sound("entity.ribbit.death");
    public static final SoundEvent RIBBIT_AMBIENT1 = SoundManager.sound("ribbit_ambient1");
    public static final SoundEvent RIBBIT_AMBIENT2 = SoundManager.sound("ribbit_ambient2");
    public static final SoundEvent RIBBIT_AMBIENT3 = SoundManager.sound("ribbit_ambient3");
    public static final SoundEvent RIBBIT_AMBIENT4 = SoundManager.sound("ribbit_ambient4");
    public static final SoundEvent RIBBIT_AMBIENT5 = SoundManager.sound("ribbit_ambient5");
    public static final SoundEvent RIBBIT_STEP1 = SoundManager.sound("ribbit_step1");
    public static final SoundEvent RIBBIT_STEP2 = SoundManager.sound("ribbit_step2");
    public static final SoundEvent RIBBIT_HURT1 = SoundManager.sound("ribbit_hurt1");
    public static final SoundEvent RIBBIT_HURT2 = SoundManager.sound("ribbit_hurt2");
    public static final SoundEvent RIBBIT_HURT3 = SoundManager.sound("ribbit_hurt3");
    public static final SoundEvent RIBBIT_DEATH1 = SoundManager.sound("ribbit_death1");
    public static final SoundEvent RIBBIT_DEATH2 = SoundManager.sound("ribbit_death2");

    public static final SoundEvent LAUNCHER_OPEN = SoundManager.sound("launcher_open");
    public static final SoundEvent LAUNCHER_CLOSE = SoundManager.sound("launcher_close");
    public static final SoundEvent LAUNCHER_TOGGLE_ON = SoundManager.sound("launcher_toggle_on");
    public static final SoundEvent LAUNCHER_TOGGLE_OFF = SoundManager.sound("launcher_toggle_off");
    public static final SoundEvent LAUNCHER_SLIDER = SoundManager.sound("launcher_slider");
    public static final SoundEvent LAUNCHER_CATEGORY = SoundManager.sound("launcher_category");
    public static final SoundEvent LAUNCHER_BUTTON = SoundManager.sound("launcher_button");
    public static final SoundEvent LAUNCHER_DROPDOWN_OPEN = SoundManager.sound("launcher_dropdown_open");
    public static final SoundEvent LAUNCHER_DROPDOWN_CLOSE = SoundManager.sound("launcher_dropdown_close");

    public static final SoundEvent SERENE_OPEN = SoundManager.sound("serene_open");
    public static final SoundEvent SERENE_CLOSE = SoundManager.sound("serene_close");
    public static final SoundEvent SERENE_TOGGLE_ON = SoundManager.sound("serene_toggle_on");
    public static final SoundEvent SERENE_TOGGLE_OFF = SoundManager.sound("serene_toggle_off");
    public static final SoundEvent SERENE_SLIDER = SoundManager.sound("serene_slider");
    public static final SoundEvent SERENE_CATEGORY = SoundManager.sound("serene_category");
    public static final SoundEvent SERENE_BUTTON = SoundManager.sound("serene_button");
    public static final SoundEvent SERENE_DROPDOWN_OPEN = SoundManager.sound("serene_dropdown_open");
    public static final SoundEvent SERENE_DROPDOWN_CLOSE = SoundManager.sound("serene_dropdown_close");
    public static final SoundEvent SERENE_PIN = SoundManager.sound("serene_pin");
    public static final SoundEvent SERENE_UNPIN = SoundManager.sound("serene_unpin");

    public static final SoundEvent BUBBLE_OPEN = SoundManager.sound("bubble_open");
    public static final SoundEvent BUBBLE_CLOSE = SoundManager.sound("bubble_close");
    public static final SoundEvent BUBBLE_TOGGLE_ON = SoundManager.sound("bubble_toggle_on");
    public static final SoundEvent BUBBLE_TOGGLE_OFF = SoundManager.sound("bubble_toggle_off");
    public static final SoundEvent BUBBLE_SLIDER = SoundManager.sound("bubble_slider");
    public static final SoundEvent BUBBLE_CATEGORY = SoundManager.sound("bubble_category");
    public static final SoundEvent BUBBLE_BUTTON = SoundManager.sound("bubble_button");

    public static final SoundEvent CYBER_OPEN = SoundManager.sound("cyber_open");
    public static final SoundEvent CYBER_CLOSE = SoundManager.sound("cyber_close");
    public static final SoundEvent CYBER_TOGGLE_ON = SoundManager.sound("cyber_toggle_on");
    public static final SoundEvent CYBER_TOGGLE_OFF = SoundManager.sound("cyber_toggle_off");
    public static final SoundEvent CYBER_SLIDER = SoundManager.sound("cyber_slider");
    public static final SoundEvent CYBER_CATEGORY = SoundManager.sound("cyber_category");
    public static final SoundEvent CYBER_BUTTON = SoundManager.sound("cyber_button");

    public static final SoundEvent NV_GLASS_OPEN = SoundManager.sound("nv_glass_open");
    public static final SoundEvent NV_GLASS_CLOSE = SoundManager.sound("nv_glass_close");
    public static final SoundEvent NV_GLASS_TOGGLE_ON = SoundManager.sound("nv_glass_toggle_on");
    public static final SoundEvent NV_GLASS_TOGGLE_OFF = SoundManager.sound("nv_glass_toggle_off");
    public static final SoundEvent NV_GLASS_SLIDER = SoundManager.sound("nv_glass_slider");
    public static final SoundEvent NV_GLASS_CATEGORY = SoundManager.sound("nv_glass_category");
    public static final SoundEvent NV_GLASS_BUTTON = SoundManager.sound("nv_glass_button");
    public static final SoundEvent NV_GLASS_DROPDOWN_OPEN = SoundManager.sound("nv_glass_dropdown_open");
    public static final SoundEvent NV_GLASS_DROPDOWN_CLOSE = SoundManager.sound("nv_glass_dropdown_close");
    public static final SoundEvent NV_GLASS_PIN = SoundManager.sound("nv_glass_pin");
    public static final SoundEvent NV_GLASS_UNPIN = SoundManager.sound("nv_glass_unpin");

    private static final SoundEvent[] ALL_SOUNDS = new SoundEvent[]{NV_TACTICAL, NV_GLASS_OPEN, NV_GLASS_CLOSE, NV_GLASS_TOGGLE_ON, NV_GLASS_TOGGLE_OFF, NV_GLASS_SLIDER, NV_GLASS_CATEGORY, NV_GLASS_BUTTON, NV_GLASS_DROPDOWN_OPEN, NV_GLASS_DROPDOWN_CLOSE, NV_GLASS_PIN, NV_GLASS_UNPIN, NV_GLASS_ERROR, NV_GLASS_NOTIFY, BELL, IMPULSE, POP, CRYSTAL, RUST, BUBBLE, CLICK, COD, COMMAND_ERROR, NOTIFICATION, NOTIFICATION_LOW, PLAYER_PING, NOTIFICATION_DIRECT, LOW, PLAYERPING, FRAG_EFFECT_ECHO_MAIN, FRAG_EFFECT_KNOCK_MAIN, FRAG_EFFECT_PULSE, FRAG_EFFECT_SPARKS_COLLISION, RIBBIT_AMBIENT, RIBBIT_STEP, RIBBIT_HURT, RIBBIT_DEATH, RIBBIT_AMBIENT1, RIBBIT_AMBIENT2, RIBBIT_AMBIENT3, RIBBIT_AMBIENT4, RIBBIT_AMBIENT5, RIBBIT_STEP1, RIBBIT_STEP2, RIBBIT_HURT1, RIBBIT_HURT2, RIBBIT_HURT3, RIBBIT_DEATH1, RIBBIT_DEATH2, LAUNCHER_OPEN, LAUNCHER_CLOSE, LAUNCHER_TOGGLE_ON, LAUNCHER_TOGGLE_OFF, LAUNCHER_SLIDER, LAUNCHER_CATEGORY, LAUNCHER_BUTTON, LAUNCHER_DROPDOWN_OPEN, LAUNCHER_DROPDOWN_CLOSE, SERENE_OPEN, SERENE_CLOSE, SERENE_TOGGLE_ON, SERENE_TOGGLE_OFF, SERENE_SLIDER, SERENE_CATEGORY, SERENE_BUTTON, SERENE_DROPDOWN_OPEN, SERENE_DROPDOWN_CLOSE, SERENE_PIN, SERENE_UNPIN, BUBBLE_OPEN, BUBBLE_CLOSE, BUBBLE_TOGGLE_ON, BUBBLE_TOGGLE_OFF, BUBBLE_SLIDER, BUBBLE_CATEGORY, BUBBLE_BUTTON, CYBER_OPEN, CYBER_CLOSE, CYBER_TOGGLE_ON, CYBER_TOGGLE_OFF, CYBER_SLIDER, CYBER_CATEGORY, CYBER_BUTTON};
    private static boolean initialized;

    private SoundManager() {
    }

    private static void register(SoundEvent soundEvent) {
        if (!Registries.SOUND_EVENT.containsId(soundEvent.id())) {
            Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)soundEvent.id(), (Object)soundEvent);
        }
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        for (SoundEvent soundEvent : ALL_SOUNDS) {
            SoundManager.register(soundEvent);
        }
    }

    public static void playSound(SoundEvent soundEvent, float f, float f2) {
        var profile = rtx.nv.api.modules.impl.Utils.ClientSounds.getInstance();
        if (profile != null && profile.isEnabled() && (soundEvent == NOTIFICATION || soundEvent == NOTIFICATION_LOW || soundEvent == NOTIFICATION_DIRECT)) {
            soundEvent = profile.resolveSoundEvent("notification", soundEvent);
            f *= profile.getMasterVolume();
        }
        playSoundDirect(soundEvent, f, f2);
    }

    public static void playSoundDirect(SoundEvent soundEvent, float f, float f2) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null && minecraftClient.getSoundManager() != null && soundEvent != null && f > 0 && Float.isFinite(f) && Float.isFinite(f2)) {
            minecraftClient.getSoundManager().play(
                new PositionedSoundInstance(
                    soundEvent.id(),
                    SoundCategory.MASTER,
                    Math.clamp(f, 0.0f, 2.0f),
                    Math.clamp(f2, 0.5f, 2.0f),
                    SoundInstance.createRandom(),
                    false,
                    0,
                    SoundInstance.AttenuationType.NONE,
                    0.0,
                    0.0,
                    0.0,
                    true
                )
            );
        }
    }

    private static SoundEvent sound(String string) {
        return SoundEvent.of((Identifier)Identifier.of((String)"nv", (String)string));
    }
}

