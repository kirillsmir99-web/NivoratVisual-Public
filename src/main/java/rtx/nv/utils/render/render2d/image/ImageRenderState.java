package rtx.nv.utils.render.render2d.image;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.image.ImageQuad;
import rtx.nv.utils.render.render2d.image.ImageRenderer;
import rtx.nv.utils.render.render2d.image.ImageTexture;

final class ImageRenderState
implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final ImageTexture texture;
    private final ScreenRect scissorArea;
    private final boolean nearest;
    private final List<ImageQuad> images = new ArrayList<ImageQuad>(16);
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -3.4028235E38f;
    private float maxY = -3.4028235E38f;
    private final boolean additive;

    ImageRenderState(Matrix3x2f matrix3x2f, ImageTexture imageTexture, ScreenRect screenRect, boolean bl) {
        this(matrix3x2f, imageTexture, screenRect, bl, false);
    }

    ImageRenderState(Matrix3x2f matrix3x2f, ImageTexture imageTexture, ScreenRect screenRect, boolean bl, boolean bl2) {
        this.additive = bl2;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.texture = imageTexture;
        this.scissorArea = screenRect;
        this.nearest = bl;
    }

    void add(ImageQuad imageQuad) {
        this.images.add(imageQuad);
        this.includeBounds(imageQuad);
    }

    private void include(float f, float f2) {
        this.minX = Math.min(this.minX, f);
        this.minY = Math.min(this.minY, f2);
        this.maxX = Math.max(this.maxX, f);
        this.maxY = Math.max(this.maxY, f2);
    }

    public ScreenRect bounds() {
        if (this.images.isEmpty()) {
            return new ScreenRect(0, 0, 1, 1);
        }
        int n = (int)Math.floor(this.minX);
        int n2 = (int)Math.floor(this.minY);
        int n3 = Math.max(1, (int)Math.ceil(this.maxX - this.minX));
        int n4 = Math.max(1, (int)Math.ceil(this.maxY - this.minY));
        ScreenRect screenRect = new ScreenRect(n, n2, n3, n4).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        return this.scissorArea == null ? screenRect : this.scissorArea.intersection(screenRect);
    }

    public RenderPipeline pipeline() {
        return this.additive ? ImageRenderer.IMAGE_ADDITIVE_PIPELINE : ImageRenderer.IMAGE_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.texture.setup(this.nearest);
    }

    public void setupVertices(VertexConsumer vertices) {
        for (ImageQuad imageQuad : this.images) {
            int n = ImageRenderer.getInstance().reserve(imageQuad);
            if (n < 0) continue;
            float f = imageQuad.x;
            float f2 = imageQuad.y;
            float f3 = imageQuad.x + imageQuad.width;
            float f4 = imageQuad.y + imageQuad.height;
            this.vertex(vertices, imageQuad, f, f2, imageQuad.u0, imageQuad.v0, 0, 0, n);
            this.vertex(vertices, imageQuad, f, f4, imageQuad.u0, imageQuad.v1, 0, 255, n);
            this.vertex(vertices, imageQuad, f3, f4, imageQuad.u1, imageQuad.v1, 255, 255, n);
            this.vertex(vertices, imageQuad, f3, f2, imageQuad.u1, imageQuad.v0, 255, 0, n);
        }
    }

    private void vertex(VertexConsumer vertexConsumer, ImageQuad imageQuad, float f, float f2, float f3, float f4, int n, int n2, int n3) {
        float f5 = ImageRenderState.rotatedX(imageQuad, f, f2);
        float f6 = ImageRenderState.rotatedY(imageQuad, f, f2);
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f5, f6).texture(f3, f4).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }

    private void includeBounds(ImageQuad imageQuad) {
        float f = imageQuad.x;
        float f2 = imageQuad.y;
        float f3 = imageQuad.x + imageQuad.width;
        float f4 = imageQuad.y + imageQuad.height;
        this.include(ImageRenderState.rotatedX(imageQuad, f, f2), ImageRenderState.rotatedY(imageQuad, f, f2));
        this.include(ImageRenderState.rotatedX(imageQuad, f, f4), ImageRenderState.rotatedY(imageQuad, f, f4));
        this.include(ImageRenderState.rotatedX(imageQuad, f3, f4), ImageRenderState.rotatedY(imageQuad, f3, f4));
        this.include(ImageRenderState.rotatedX(imageQuad, f3, f2), ImageRenderState.rotatedY(imageQuad, f3, f2));
    }

    private static float rotatedX(ImageQuad imageQuad, float f, float f2) {
        float f3 = imageQuad.rotationDegrees;
        if (f3 == 0.0f || !Float.isFinite(f3)) {
            return f;
        }
        double d = Math.toRadians(f3);
        float f4 = f - imageQuad.rotationOriginX;
        float f5 = f2 - imageQuad.rotationOriginY;
        return imageQuad.rotationOriginX + (float)((double)f4 * Math.cos(d) - (double)f5 * Math.sin(d));
    }

    private static float rotatedY(ImageQuad imageQuad, float f, float f2) {
        float f3 = imageQuad.rotationDegrees;
        if (f3 == 0.0f || !Float.isFinite(f3)) {
            return f2;
        }
        double d = Math.toRadians(f3);
        float f4 = f - imageQuad.rotationOriginX;
        float f5 = f2 - imageQuad.rotationOriginY;
        return imageQuad.rotationOriginY + (float)((double)f4 * Math.sin(d) + (double)f5 * Math.cos(d));
    }
}

