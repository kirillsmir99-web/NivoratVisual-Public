package rtx.nv.utils.net;

public final class Endpoints {
    public static final String DEFAULT_BASE = "https://virion.185-56-162-195.sslip.io/nv";
    private static final String BASE = System.getProperty("nv.social.base", DEFAULT_BASE);
    private Endpoints() {}
    public static String funtime() { return ""; }
    public static String irc() { return BASE; }
    public static String party() { return websocket("/party"); }
    public static String sync(String channel) { return websocket("/sync/"+channel); }
    private static String websocket(String path) {
        if (BASE.startsWith("https://")) return "wss://"+BASE.substring(8)+path;
        if (BASE.startsWith("http://127.0.0.1:")) return "ws://"+BASE.substring(7)+path;
        throw new IllegalStateException("Social service requires HTTPS");
    }
}
