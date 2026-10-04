package rtx.nv.api.mods.geckolib;
import net.fabricmc.api.ModInitializer;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.service.GeckoLibNetworking;

public final class GeckoLib
implements ModInitializer {
    public void onInitialize() {
        GeckoLibConstants.init();
        GeckoLibNetworking.init();
    }
}

