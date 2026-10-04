package rtx.nv.utils.storage.macro;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import rtx.nv.utils.storage.RepositoryStorage;
import rtx.nv.utils.storage.macro.Macro;

public final class MacroRepository {
    private static final String KEY = "macros";
    private static MacroRepository instance;
    private final List<Macro> macros = new ArrayList<Macro>();

    private MacroRepository() {
        this.load();
    }

    public int size() {
        return this.macros.size();
    }

    private void load() {
        this.macros.clear();
        JsonObject jsonObject = RepositoryStorage.readObject(KEY);
        if (jsonObject.has(KEY)) {
            for (JsonElement jsonElement : jsonObject.getAsJsonArray(KEY)) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                this.macros.add(new Macro(jsonObject2.get("name").getAsString(), jsonObject2.get("message").getAsString(), jsonObject2.get("key").getAsInt()));
            }
        }
    }

    public static MacroRepository getInstance() {
        if (instance == null) {
            instance = new MacroRepository();
        }
        return instance;
    }

    private void save() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for (Macro macro : this.macros) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("name", macro.name());
            jsonObject2.addProperty("message", macro.message());
            jsonObject2.addProperty("key", (Number)macro.key());
            jsonArray.add((JsonElement)jsonObject2);
        }
        jsonObject.add(KEY, (JsonElement)jsonArray);
        RepositoryStorage.write(KEY, jsonObject);
    }

    public List<String> getMacroNames() {
        return this.macros.stream().map(Macro::name).toList();
    }

    public void clearListAndSave() {
        this.macros.clear();
        this.save();
    }

    public void addMacroAndSave(String string, String string2, int n) {
        this.macros.removeIf(macro -> macro.name().equalsIgnoreCase(string));
        this.macros.add(new Macro(string, string2, n));
        this.save();
    }

    public List<Macro> getMacroList() {
        return new ArrayList<Macro>(this.macros);
    }

    public void deleteMacroAndSave(String string) {
        this.macros.removeIf(macro -> macro.name().equalsIgnoreCase(string));
        this.save();
    }
}

