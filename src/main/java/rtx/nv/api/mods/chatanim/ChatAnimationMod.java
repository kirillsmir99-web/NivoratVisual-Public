package rtx.nv.api.mods.chatanim;
import java.io.File;
import net.fabricmc.loader.api.FabricLoader;
import rtx.nv.api.mods.chatanim.config.ModConfig;

public final class ChatAnimationMod {
    public static final String MOD_ID = "chatanimation";
    public static final String CONFIG_FILE = "chatanimation.json";

    private ChatAnimationMod() {
    }

    public static void init() {
        File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE);
        ModConfig.getConfig().load(file);
    }
}

