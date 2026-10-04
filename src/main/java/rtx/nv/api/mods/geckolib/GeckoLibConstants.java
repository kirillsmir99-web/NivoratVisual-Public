package rtx.nv.api.mods.geckolib;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rtx.nv.api.mods.geckolib.GeckoLibServices;

public final class GeckoLibConstants {
    public static final Logger LOGGER = LogManager.getLogger((String)"GeckoLib");
    public static final String MODID = "geckolib";
    public static final Supplier<ComponentType<Long>> STACK_ANIMATABLE_ID_COMPONENT = GeckoLibServices.PLATFORM.registerDataComponent("stack_animatable_id", builder -> builder.codec((Codec)Codec.LONG).packetCodec(PacketCodecs.VAR_LONG));

    public static Identifier id(String string) {
        return Identifier.of((String)MODID, (String)string);
    }

    public static void init() {
    }

    public static RuntimeException exception(Identifier identifier, String string) {
        return new RuntimeException(String.valueOf(identifier) + ": " + string);
    }

    public static RuntimeException exception(Identifier identifier, String string, Throwable throwable) {
        return new RuntimeException(String.valueOf(identifier) + ": " + string, throwable);
    }
}

