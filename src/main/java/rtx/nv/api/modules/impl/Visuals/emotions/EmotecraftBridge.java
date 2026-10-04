package rtx.nv.api.modules.impl.Visuals.emotions;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EmotecraftBridge {
    private static Boolean available = null;
    private static final Map<String, String[]> EMOTE_MAPPINGS = new HashMap<>();

    static {
        EMOTE_MAPPINGS.put("Помахать", new String[]{"here", "wave", "spe_show", "show"});
        EMOTE_MAPPINGS.put("Салют", new String[]{"salute", "spe_zoro", "zoro"});
        EMOTE_MAPPINGS.put("Стеснение", new String[]{"shy", "waiting", "waiting for a boyfriend"});
        EMOTE_MAPPINGS.put("Танец", new String[]{"club_penguin_dance", "dance", "wednesday", "wednesdaydance"});
        EMOTE_MAPPINGS.put("Аплодисменты", new String[]{"clap"});
        EMOTE_MAPPINGS.put("Поклон", new String[]{"bow", "gats", "the gats pose"});
        EMOTE_MAPPINGS.put("Фейспалм", new String[]{"palm", "facepalm"});
        EMOTE_MAPPINGS.put("Указать", new String[]{"point", "tracer"});
        EMOTE_MAPPINGS.put("Радость", new String[]{"cheer", "spooky", "spe_spooky"});
        EMOTE_MAPPINGS.put("Смех", new String[]{"laugh"});
        EMOTE_MAPPINGS.put("Плач", new String[]{"crying", "cry"});
        EMOTE_MAPPINGS.put("Раздумье", new String[]{"think", "thinking"});
        EMOTE_MAPPINGS.put("Недоумение", new String[]{"shrug", "unknown"});
        EMOTE_MAPPINGS.put("Сила", new String[]{"flex", "hero"});
        EMOTE_MAPPINGS.put("Сердечко", new String[]{"heart", "love"});
        EMOTE_MAPPINGS.put("Сдаюсь", new String[]{"surrender"});
        EMOTE_MAPPINGS.put("Диско", new String[]{"disco", "roblox_potion_dance"});
        EMOTE_MAPPINGS.put("Зомби", new String[]{"zombie", "the curse of vecna", "vecna"});
        EMOTE_MAPPINGS.put("Боевая стойка", new String[]{"fight", "spider-man", "the killer's stance"});
        EMOTE_MAPPINGS.put("Медитация", new String[]{"meditate", "sit by the fire"});
        EMOTE_MAPPINGS.put("Победа", new String[]{"victory", "backflip"});
        EMOTE_MAPPINGS.put("Угроза", new String[]{"threaten", "zweihander"});
        EMOTE_MAPPINGS.put("Зевота", new String[]{"sleepy", "yawn"});
        EMOTE_MAPPINGS.put("Отдых", new String[]{"rest", "sit"});
        EMOTE_MAPPINGS.put("Даб", new String[]{"dab"});
        EMOTE_MAPPINGS.put("Т поза", new String[]{"t_pose", "tpose"});
        EMOTE_MAPPINGS.put("Флосс", new String[]{"floss", "tectonic wave"});
        EMOTE_MAPPINGS.put("Вращение", new String[]{"spin"});
        EMOTE_MAPPINGS.put("Привал", new String[]{"sit by the fire", "sit"});
        EMOTE_MAPPINGS.put("Герой", new String[]{"hero", "vai stand"});
        EMOTE_MAPPINGS.put("Удар с ноги", new String[]{"kazotsky_kick", "the deadly leg"});
        EMOTE_MAPPINGS.put("Шаффл", new String[]{"kazotsky_kick", "shuffle"});
        EMOTE_MAPPINGS.put("Приветствие", new String[]{"greeting", "wave", "hello", "spe_show"});
        EMOTE_MAPPINGS.put("Победная поза", new String[]{"victory", "victory_pose", "backflip", "hero"});
        EMOTE_MAPPINGS.put("Спокойный танец", new String[]{"calm_dance", "slow_dance", "waltz", "dance"});
    }

    public static boolean isAvailable() {
        if (available == null) {
            try {
                Class.forName("io.github.kosmx.emotes.api.events.client.ClientEmoteAPI");
                available = true;
            } catch (Throwable t) {
                available = false;
            }
        }
        return available;
    }

    public static boolean isPlayerPlayingEmote(UUID uuid) {
        if (!isAvailable() || uuid == null) {
            return false;
        }
        try {
            Class<?> playClass = Class.forName("io.github.kosmx.emotes.main.network.ClientEmotePlay");
            Method getEmoteMethod = playClass.getMethod("getEmoteForUUID", UUID.class);
            Object result = getEmoteMethod.invoke(null, uuid);
            return result != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean playEmote(Emotion emotion) {
        if (!isAvailable() || emotion == null) {
            return false;
        }
        String[] keywords = EMOTE_MAPPINGS.get(emotion.displayName());
        if (keywords == null) {
            keywords = new String[]{emotion.name().toLowerCase()};
        }
        return playEmoteWithKeywords(keywords);
    }

    public static boolean playEmoteWithKeywords(String... keywords) {
        if (!isAvailable() || keywords == null || keywords.length == 0) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("io.github.kosmx.emotes.api.events.client.ClientEmoteAPI");
            Method listMethod = apiClass.getMethod("clientEmoteList");
            Collection<?> list = (Collection<?>) listMethod.invoke(null);
            if (list == null || list.isEmpty()) {
                return false;
            }

            Object matchedAnimation = null;
            for (String kw : keywords) {
                String cleanKw = kw.toLowerCase().replaceAll("[^a-z0-9]", "");
                for (Object anim : list) {
                    Method nameMethod = anim.getClass().getMethod("getNameOrId");
                    String name = (String) nameMethod.invoke(anim);
                    if (name != null) {
                        String cleanName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
                        if (cleanName.contains(cleanKw) || cleanKw.contains(cleanName)) {
                            matchedAnimation = anim;
                            break;
                        }
                    }
                }
                if (matchedAnimation != null) {
                    break;
                }
            }

            if (matchedAnimation != null) {
                Class<?> animClass = Class.forName("com.zigythebird.playeranimcore.animation.Animation");
                Method playMethod = apiClass.getMethod("playEmote", animClass);
                Object success = playMethod.invoke(null, matchedAnimation);
                return Boolean.TRUE.equals(success);
            }
        } catch (Throwable t) {
            // Emotecraft invocation failure, fallback to internal playback
        }
        return false;
    }

    public static boolean stop() {
        if (!isAvailable()) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("io.github.kosmx.emotes.api.events.client.ClientEmoteAPI");
            Method stopMethod = apiClass.getMethod("stopEmote");
            Object success = stopMethod.invoke(null);
            return Boolean.TRUE.equals(success);
        } catch (Throwable t) {
            return false;
        }
    }
}
