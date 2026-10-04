package rtx.nv.utils.render.renderitem;

import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

public record ItemTexture(Identifier identifier, TextureSetup textureSetup) {
    public Identifier id() {
        return this.identifier;
    }
}