package rtx.nv.api.modules.settings.impl;

import rtx.nv.api.modules.Category;
import java.util.Locale;

/** Migrates category names stored by older versions and other UI languages. */
public final class CategoryFilterSetting extends MultiSelectSetting {
    private static final Category[] CATEGORIES = {Category.VISUALS, Category.DISPLAY, Category.UTILS, Category.MEDIA};
    public CategoryFilterSetting(String name, String description) {
        super(name, description);
        minSelectedCount(1);
    }
    public String resolveOption(String stored) {
        if (stored == null) return null;
        if (getOptions().contains(stored)) return stored;
        String normalized=stored.toLowerCase(Locale.ROOT);
        Category category = switch(normalized) {
            case "visuals", "visual", "визуал", "визуалы", "визуальные" -> Category.VISUALS;
            case "display", "interface", "интерфейс", "отображение" -> Category.DISPLAY;
            case "utils", "утилиты", "разное", "твики" -> Category.UTILS;
            case "media", "медиа" -> Category.MEDIA;
            default -> null;
        };
        for(int i=0;i<CATEGORIES.length && i<getOptions().size();i++) if(category==CATEGORIES[i]) return getOptions().get(i);
        return null;
    }
}
