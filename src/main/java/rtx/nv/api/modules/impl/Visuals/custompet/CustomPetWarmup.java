package rtx.nv.api.modules.impl.Visuals.custompet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.cache.GeckoLibResources;

public final class CustomPetWarmup {
    private static final String[] PETS = {"spirit", "dragon", "moth"};

    private CustomPetWarmup() {
    }

    public static void warmup() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return;
        }
        for (String pet : PETS) {
        Identifier TEXTURE = Identifier.of("nv", "textures/entity/pets/" + pet + ".png");
        Identifier MODEL = Identifier.of("nv", "pets/" + pet);
        try {
            if (minecraftClient.getTextureManager() != null) {
                minecraftClient.getTextureManager().registerTexture(TEXTURE, (ReloadableTexture)new ResourceTexture(TEXTURE));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            GeckoLibResources.getBakedModels().getModel(MODEL);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        }
    }
}

