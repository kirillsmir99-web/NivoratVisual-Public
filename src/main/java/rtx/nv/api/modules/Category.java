package rtx.nv.api.modules;

public enum Category {
    VISUALS("Visuals"),
    DISPLAY("Display"),
    UTILS("Utils"),
    MEDIA("Media"),
    PINNED("Pinned"),
    THEMES("Themes"),
    ABOUT("About");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return this.displayName;
    }

    public String getDisplayName() {
        return switch (this) {
            case PINNED -> rtx.nv.api.localization.Lang.get("category.pinned", this.displayName);
            case VISUALS -> rtx.nv.api.localization.Lang.get("category.visuals", this.displayName);
            case DISPLAY -> rtx.nv.api.localization.Lang.get("category.display", this.displayName);
            case UTILS -> rtx.nv.api.localization.Lang.get("category.utils", this.displayName);
            case THEMES -> rtx.nv.api.localization.Lang.get("category.themes", this.displayName);
            case MEDIA -> rtx.nv.api.localization.Lang.get("category.media", this.displayName);
            case ABOUT -> rtx.nv.api.localization.Lang.get("category.about", this.displayName);
        };
    }
}
