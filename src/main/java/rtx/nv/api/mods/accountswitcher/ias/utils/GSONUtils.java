package rtx.nv.api.mods.accountswitcher.ias.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class GSONUtils {
    public static <T> T fromJson(Gson gson, JsonElement element, Class<T> clazz) {
  return gson.fromJson(element, clazz);
    }

    public static int getIntOrThrow(JsonObject obj, String memberName) {
        if (!obj.has(memberName)) throw new IllegalArgumentException("Missing property: " + memberName);
  return obj.get(memberName).getAsInt();
    }
}