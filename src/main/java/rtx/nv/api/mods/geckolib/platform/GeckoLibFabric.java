package rtx.nv.api.mods.geckolib.platform;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.service.GeckoLibPlatform;

public final class GeckoLibFabric
implements GeckoLibPlatform {
    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public <T> Supplier<ComponentType<T>> registerDataComponent(String string, UnaryOperator<ComponentType.Builder<T>> unaryOperator) {
        net.minecraft.util.Identifier id = GeckoLibConstants.id(string);
        ComponentType existing = (ComponentType)Registries.DATA_COMPONENT_TYPE.get(id);
        if (existing != null) {
            return () -> existing;
        }
        try {
            ComponentType componentType = (ComponentType)Registry.register((Registry)Registries.DATA_COMPONENT_TYPE, (String)id.toString(), (Object)((ComponentType.Builder)unaryOperator.apply(ComponentType.builder())).build());
            return () -> componentType;
        } catch (Exception e) {
            ComponentType fallback = (ComponentType)Registries.DATA_COMPONENT_TYPE.get(id);
            if (fallback != null) {
                return () -> fallback;
            }
            throw e;
        }
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}

