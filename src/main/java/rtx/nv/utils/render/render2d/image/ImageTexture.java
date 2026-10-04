package rtx.nv.utils.render.render2d.image;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

record ImageTexture(Identifier id, TextureSetup linearSetup, TextureSetup nearestSetup, int width, int height) {
    TextureSetup setup(boolean bl) {
        return bl ? this.nearestSetup : this.linearSetup;
    }

    float drawHeight(float f) {
        if (this.width <= 0 || this.height <= 0 || this.height >= this.width) {
            return f;
        }
        return f * ((float)this.height / (float)this.width);
    }

    float drawWidth(float f) {
        if (this.width <= 0 || this.height <= 0 || this.width >= this.height) {
            return f;
        }
        return f * ((float)this.width / (float)this.height);
    }
}

