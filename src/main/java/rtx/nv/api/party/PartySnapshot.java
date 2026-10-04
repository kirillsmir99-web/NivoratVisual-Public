package rtx.nv.api.party;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rtx.nv.api.party.PartyMember;

public final class PartySnapshot {
    public static final PartySnapshot NONE = new PartySnapshot(null, null, 10, Collections.emptyList());
    private final String name;
    private final String leader;
    private final int max;
    private final List<PartyMember> members;

    public PartySnapshot(String string, String string2, int n, List<PartyMember> list) {
        this.name = string;
        this.leader = string2;
        this.max = n;
        this.members = list;
    }

    public String name() {
        return this.name;
    }

    public int max() {
        return this.max;
    }

    public List<PartyMember> members() {
        return this.members;
    }

    public boolean exists() {
        return this.name != null;
    }

    public boolean inParty() {
        return exists();
    }

    public String leader() {
        return this.leader;
    }

    public boolean isLeader(String string) {
        return this.leader != null && string != null && this.leader.equalsIgnoreCase(string);
    }

    public static PartySnapshot fromJson(JsonObject jsonObject) {
        if (jsonObject == null || jsonObject.isJsonNull() || !jsonObject.has("name") || jsonObject.get("name").isJsonNull()) {
            return NONE;
        }
        String string = jsonObject.get("name").getAsString();
        String string2 = jsonObject.has("leader") ? jsonObject.get("leader").getAsString() : "";
        int n = jsonObject.has("max") && jsonObject.get("max").isJsonPrimitive() ? jsonObject.get("max").getAsInt() : 10;
        ArrayList<PartyMember> arrayList = new ArrayList<PartyMember>();
        if (jsonObject.has("members") && jsonObject.get("members").isJsonArray()) {
            JsonArray jsonArray = jsonObject.getAsJsonArray("members");
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) continue;
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                String string3 = jsonObject2.has("name") ? jsonObject2.get("name").getAsString() : "?";
                boolean bl = jsonObject2.has("leader") && jsonObject2.get("leader").getAsBoolean();
                boolean bl2 = jsonObject2.has("online") && jsonObject2.get("online").getAsBoolean();
                arrayList.add(new PartyMember(string3, bl, bl2));
            }
        }
        return new PartySnapshot(string, string2, n, arrayList);
    }
}

