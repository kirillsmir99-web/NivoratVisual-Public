package rtx.nv.api.modules.impl.Utils.guishare;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteAvatarCache {
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static String texture(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return "";
        return CACHE.computeIfAbsent(avatarUrl, k -> k);
    }
}