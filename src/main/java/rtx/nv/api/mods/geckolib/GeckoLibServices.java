package rtx.nv.api.mods.geckolib;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.service.GeckoLibNetworking;
import rtx.nv.api.mods.geckolib.service.GeckoLibPlatform;

public final class GeckoLibServices {
    public static final GeckoLibPlatform PLATFORM = GeckoLibServices.load(GeckoLibPlatform.class);
    public static final GeckoLibNetworking NETWORK = GeckoLibServices.load(GeckoLibNetworking.class);

    private static <T> T load(Class<T> clazz, Supplier<T> supplier) {
        T t = ServiceLoader.load(clazz).findFirst().or(supplier == null ? Optional::empty : () -> Optional.of(supplier.get())).orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        GeckoLibConstants.LOGGER.debug("Loaded {} for service {}", t, clazz);
        return t;
    }

    private static <T> T load(Class<T> clazz) {
        return GeckoLibServices.load(clazz, null);
    }
}

