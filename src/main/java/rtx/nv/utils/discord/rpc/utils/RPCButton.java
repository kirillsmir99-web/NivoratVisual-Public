package rtx.nv.utils.discord.rpc.utils;
import java.io.Serializable;

public class RPCButton
implements Serializable {
    private final String url;
    private final String label;

    protected RPCButton(String string, String string2) {
        this.label = string;
        this.url = string2;
    }

    public static RPCButton create(String string, String string2) {
        string = string.substring(0, Math.min(string.length(), 31));
        return new RPCButton(string, string2);
    }

    public String getLabel() {
        return this.label;
    }

    public String getUrl() {
        return this.url;
    }
}

