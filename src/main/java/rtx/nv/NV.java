package rtx.nv;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.nv.api.mods.geckolib.GeckoLib;
import rtx.nv.manager.Manager;

public class NV
implements ModInitializer,
ClientModInitializer {
    public static final String MOD_ID = "nv";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"nv");

    public void onInitializeClient() {
        Manager.init();
    }

    public void onInitialize() {
        new GeckoLib().onInitialize();
    }
}

