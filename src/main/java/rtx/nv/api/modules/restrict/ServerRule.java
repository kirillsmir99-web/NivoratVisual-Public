package rtx.nv.api.modules.restrict;
import rtx.nv.api.modules.restrict.Server;

public @interface ServerRule {
    public ServerRule.Mode mode();

    public Server[] servers();


    public static enum Mode {
        ONLY,
        BLOCK,
        HIDE;
    
    }
}

