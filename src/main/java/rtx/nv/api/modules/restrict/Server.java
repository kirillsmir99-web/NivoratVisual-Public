package rtx.nv.api.modules.restrict;

public enum Server {
    FT("FunTime", new String[]{"funtime"}),
    RW("ReallyWorld", new String[]{"reallyworld"}),
    HW("HolyWorld", new String[]{"holyworld", "hollyworld", "playhw"}),
    SATURN("Saturn-X", new String[]{"saturn-x", "saturnx"}),
    ST("Space Times", new String[]{"space-times", "spacetimes"});

    private final String display;
    private final String[] keywords;

    private Server(String display, String[] keywords) {
        this.display = display;
        this.keywords = keywords;
    }

    boolean matches(String string, String string2, String string3) {
        for (String string4 : this.keywords) {
            if (!string.isEmpty() && string.contains(string4)) {
                return true;
            }
            if (!string2.isEmpty() && string2.contains(string4)) {
                return true;
            }
            if (string3.isEmpty() || !string3.contains(string4)) continue;
            return true;
        }
        return false;
    }

    public String display() {
        return this.display;
    }
}

