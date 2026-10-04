package rtx.nv.utils.render.render2d.glow;
import net.minecraft.client.texture.TextureSetup;

final class GlowCapture {
    public float regionU0 = 0.0f;
    public float regionV0 = 0.0f;
    public float regionUW = 1.0f;
    public float regionVH = 1.0f;
    public int index = 0;
    public TextureSetup setup = TextureSetup.empty();

    GlowCapture() {
    }

    public void reset() {
        this.regionU0 = 0.0f;
        this.regionV0 = 0.0f;
        this.regionUW = 1.0f;
        this.regionVH = 1.0f;
        this.setup = TextureSetup.empty();
    }
}

