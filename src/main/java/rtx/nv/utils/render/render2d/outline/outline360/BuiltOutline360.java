package rtx.nv.utils.render.render2d.outline.outline360;

import java.util.List;
import net.minecraft.client.gui.DrawContext;

public record BuiltOutline360(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, float thickness, int defaultColor, float smoothness, float blendDegrees, float angleOffsetDegrees, List<Outline360Range> ranges) {
    public static final float DEFAULT_SMOOTHNESS = 0.5f;
    public static final float DEFAULT_BLEND_DEGREES = 14.0f;
    public static final int DEFAULT_COLOR = -13619152;

    public BuiltOutline360(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n, List<Outline360Range> list) {
        this(f, f2, f3, f4, f5, f6, f7, f8, f9, n, 0.5f, 14.0f, 0.0f, list);
    }

    public BuiltOutline360(float f, float f2, float f3, float f4, float f5, float f6, int n, List<Outline360Range> list) {
        this(f, f2, f3, f4, f5, f5, f5, f5, f6, n, 0.5f, 14.0f, 0.0f, list);
    }

    public BuiltOutline360 {
        ranges = ranges == null ? List.of() : List.copyOf(ranges);
    }

    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && this.thickness > 0.0f && (this.defaultColor >>> 24) != 0;
    }

    public BuiltOutline360 withAngleOffset(float f) {
        return new BuiltOutline360(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.thickness, this.defaultColor, this.smoothness, this.blendDegrees, f, this.ranges);
    }

    public BuiltOutline360 withBlendDegrees(float f) {
        return new BuiltOutline360(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.thickness, this.defaultColor, this.smoothness, f, this.angleOffsetDegrees, this.ranges);
    }

    public void render(DrawContext drawContext) {
        Outline360Renderer.getInstance().submit(drawContext, this);
    }

    public BuiltOutline360 withSmoothness(float f) {
        return new BuiltOutline360(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.thickness, this.defaultColor, f, this.blendDegrees, this.angleOffsetDegrees, this.ranges);
    }
}
