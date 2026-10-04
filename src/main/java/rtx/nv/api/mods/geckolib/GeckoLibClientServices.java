package rtx.nv.api.mods.geckolib;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.service.GeckoLibClient;
import rtx.nv.api.mods.geckolib.service.GeckoLibEvents;

public class GeckoLibClientServices {
    public static final GeckoLibEvents EVENTS = GeckoLibClientServices.load(GeckoLibEvents.class);
    public static final GeckoLibClient ITEM_RENDERING = GeckoLibClientServices.load(GeckoLibClient.class);

    private static <T> T load(Class<T> clazz, Supplier<T> supplier) {
        T t = ServiceLoader.load(clazz).findFirst().or(supplier == null ? Optional::empty : () -> Optional.of(supplier.get())).orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        GeckoLibConstants.LOGGER.debug("Loaded {} for service {}", t, clazz);
        return t;
    }

    private static <T> T load(Class<T> clazz) {
        return GeckoLibClientServices.load(clazz, null);
    }
}

