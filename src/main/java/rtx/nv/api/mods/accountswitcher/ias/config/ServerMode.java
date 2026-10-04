package rtx.nv.api.mods.accountswitcher.ias.config;

public enum ServerMode {
    ALWAYS("ias.config.server.always"),
    AVAILABLE("ias.config.server.available"),
    NEVER("ias.config.server.never");

    private final String key;

    ServerMode(String key) {
        this.key = key;
    }

    public String toString() {
        return this.key;
    }
}
