package rtx.nv.api.mods.accountswitcher.ias.config;

public enum TextAlign {
    LEFT("ias.config.textAlign.left"),
    CENTER("ias.config.textAlign.center"),
    RIGHT("ias.config.textAlign.right");

    private final String key;

    TextAlign(String key) {
        this.key = key;
    }

    public String toString() {
        return this.key;
    }
}
