package rtx.nv.utils.render.render2d.blur;
import net.minecraft.client.texture.TextureSetup;

public final class BlurCapture {
    public float regionX;
    public float regionY;
    public float regionW = 1.0f;
    public float regionH = 1.0f;
    public TextureSetup setup = TextureSetup.empty();

    public void reset() {
        this.regionX = 0.0f;
        this.regionY = 0.0f;
        this.regionW = 1.0f;
        this.regionH = 1.0f;
        this.setup = TextureSetup.empty();
    }
}

