package rtx.nv.api.modules.impl.Visuals.custompet;
import java.util.ArrayList;
import java.util.Locale;

public enum CustomPetVariant {
    MASCOT("Маскот"),
    SPIRIT("Дух"),
    DRAGON("Астральный дракон"),
    MOTH("Светящийся мотылёк"),
    DEFAULT("\u041e\u0431\u044b\u0447\u043d\u0430\u044f"),
    NITWIT("\u041d\u0438\u0442\u0432\u0438\u0442"),
    GARDENER("\u0424\u0435\u0440\u043c\u0435\u0440"),
    FISHERMAN("\u0420\u044b\u0431\u0430\u043a"),
    MERCHANT("\u041f\u0443\u0442\u0435\u0448\u0435\u0441\u0442\u0432\u0435\u043d\u043d\u0438\u043a"),
    SORCERER("\u0412\u0435\u0434\u044c\u043c\u0430"),
    ROBOT("\u0420\u043e\u0431\u043e\u0442");

    private final String settingValue;

    private CustomPetVariant(String settingValue) {
        this.settingValue = settingValue;
    }

    public boolean isRobot() {
        return this == ROBOT;
    }

    public boolean isFantasy() {
        return this == MASCOT || this == SPIRIT || this == DRAGON || this == MOTH;
    }

    public boolean isFreeVariant() {
        return this == MASCOT;
    }

    public boolean alwaysFlies() {
        return isFantasy() || isRobot();
    }

    public static String[] settingValues() {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (CustomPetVariant customPetVariant : CustomPetVariant.values()) {
            if (customPetVariant == DEFAULT || customPetVariant == ROBOT) continue;
            arrayList.add(customPetVariant.settingValue);
        }
        return arrayList.toArray(new String[0]);
    }

    public String getSettingValue() {
        return this.settingValue;
    }

    public static CustomPetVariant fromSettingValue(String string) {
        if (string == null || string.isBlank()) {
            return NITWIT;
        }
        for (CustomPetVariant customPetVariant : CustomPetVariant.values()) {
            if (!customPetVariant.settingValue.equalsIgnoreCase(string)) continue;
            return customPetVariant;
        }
        return switch (string.trim().toLowerCase(Locale.ROOT)) {
            case "dragon", "астральный дракон", "мини-дракон", "мини дракон" -> DRAGON;
            case "mascot", "маскот nv", "маскот" -> MASCOT;
            case "spirit", "дух" -> SPIRIT;
            case "moth", "светящийся мотылёк", "мотылёк", "мотылек" -> MOTH;
            case "default", "\u043e\u0431\u044b\u0447\u043d\u0430\u044f" -> NITWIT;
            case "nitwit" -> NITWIT;
            case "gardener", "farmer", "\u0441\u0430\u0434\u043e\u0432\u043d\u0438\u043a" -> GARDENER;
            case "fisherman" -> FISHERMAN;
            case "merchant", "traveler", "traveller", "\u0442\u043e\u0440\u0433\u043e\u0432\u0435\u0446" -> MERCHANT;
            case "sorcerer", "witch", "\u043a\u043e\u043b\u0434\u0443\u043d" -> SORCERER;
            default -> NITWIT;
        };
    }

    public static CustomPetVariant fromSerializedName(String string) {
        if (string == null || string.isBlank()) {
            return NITWIT;
        }
        try {
            CustomPetVariant customPetVariant = CustomPetVariant.valueOf(string.toUpperCase());
            return customPetVariant == DEFAULT ? NITWIT : customPetVariant;
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return CustomPetVariant.fromSettingValue(string);
        }
    }

    public boolean usesMerchantBody() {
        return this == MERCHANT;
    }

    public boolean usesMerchantLeaf() {
        return this == MERCHANT;
    }

    public boolean usesSorcererHat() {
        return this == SORCERER;
    }

    public boolean usesFishermanGear() {
        return this == FISHERMAN;
    }

    public boolean usesGardenerGear() {
        return this == GARDENER;
    }
}

