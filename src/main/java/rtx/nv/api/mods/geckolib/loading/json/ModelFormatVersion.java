package rtx.nv.api.mods.geckolib.loading.json;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.Util;

public enum ModelFormatVersion {
    V_1_12_0("1.12.0"),
    V_1_14_0("1.14.0"),
    V_1_21_0("1.21.0");

    private static final Supplier<Map<String, ModelFormatVersion>> LOOKUP;
    private final String serializedName;
    private final boolean supported;
    private final String errorMessage;

    ModelFormatVersion(String serializedName, String errorMessage) {
        this.serializedName = serializedName;
        this.supported = errorMessage == null;
        this.errorMessage = errorMessage;
    }

    ModelFormatVersion(String serializedName) {
        this(serializedName, null);
    }

    static {
        LOOKUP = Suppliers.memoize(() -> Util.make(new Object2ObjectOpenHashMap<>(), map -> {
            for (ModelFormatVersion modelFormatVersion : ModelFormatVersion.values()) {
                map.put(modelFormatVersion.serializedName, modelFormatVersion);
                map.put(modelFormatVersion.name(), modelFormatVersion);
            }
        }));
    }

    public static ModelFormatVersion match(String string) {
        return LOOKUP.get().get(string);
    }

    public boolean isSupported() {
        return this.supported;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
