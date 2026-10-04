package rtx.nv.api.liteapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class LiteApiCodec {
    public record Incoming(String id, boolean hasId, boolean ok, String error, String message, String event, JsonObject payload) {
        public boolean isPushEvent() {
            return this.event != null;
        }
    }
    public record Request(String id, String json) {}

    private LiteApiCodec() {}

    private static JsonObject obj(JsonObject jsonObject, String string) {
        return jsonObject != null && jsonObject.has(string) && jsonObject.get(string).isJsonObject() ? jsonObject.getAsJsonObject(string) : null;
    }

    private static String str(JsonObject jsonObject, String string) {
        return jsonObject != null && jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive() ? jsonObject.get(string).getAsString() : null;
    }

    public static Incoming parse(String string) {
        JsonObject jsonObject;
        JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(string);
            if (!jsonElement.isJsonObject()) {
                return null;
            }
            jsonObject = jsonElement.getAsJsonObject();
        } catch (RuntimeException runtimeException) {
            return null;
        }
        jsonElement = LiteApiCodec.obj(jsonObject, "payload");
        if (!jsonObject.has("id") && jsonObject.has("event")) {
            return new Incoming(null, false, false, null, null, LiteApiCodec.str(jsonObject, "event"), (JsonObject)jsonElement);
        }
        String idStr = LiteApiCodec.str(jsonObject, "id");
        boolean ok = jsonObject.has("ok") && jsonObject.get("ok").isJsonPrimitive() && jsonObject.get("ok").getAsBoolean();
        String errorStr = LiteApiCodec.str(jsonObject, "error");
        String msgStr = LiteApiCodec.str(jsonObject, "message");
        return new Incoming(idStr, true, ok, errorStr, msgStr, null, (JsonObject)jsonElement);
    }

    public static Request checkFeatures(String clientName, Collection<String> collection) {
        String reqId = UUID.randomUUID().toString();
        JsonArray jsonArray = new JsonArray();
        for (String feature : collection) {
            jsonArray.add(feature);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("client", clientName);
        payload.add("features", jsonArray);

        JsonObject root = new JsonObject();
        root.addProperty("id", reqId);
        root.addProperty("method", "checkFeatures");
        root.add("payload", payload);
        return new Request(reqId, root.toString());
    }

    public static List<String> blocklist(JsonObject jsonObject) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (jsonObject == null || !jsonObject.has("blocklist") || !jsonObject.get("blocklist").isJsonArray()) {
            return arrayList;
        }
        for (JsonElement jsonElement : jsonObject.getAsJsonArray("blocklist")) {
            if (jsonElement == null || !jsonElement.isJsonPrimitive()) continue;
            arrayList.add(jsonElement.getAsString());
        }
        return arrayList;
    }
}
