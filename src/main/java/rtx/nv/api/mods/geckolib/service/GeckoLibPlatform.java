package rtx.nv.api.mods.geckolib.service;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;

public interface GeckoLibPlatform {
    public boolean isDevelopmentEnvironment();

    public Path getGameDir();

    public <T> Supplier<ComponentType<T>> registerDataComponent(String var1, UnaryOperator<ComponentType.Builder<T>> var2);

    public boolean isPhysicalClient();

    default public boolean isInSwimmableFluid(Entity entity) {
        return entity.isTouchingWater();
    }
}

