package rtx.nv.utils.render.render2d.image;

final class ImageQuad {
    final float x;
    final float y;
    final float width;
    final float height;
    final float radiusTL;
    final float radiusTR;
    final float radiusBR;
    final float radiusBL;
    final float smoothness;
    final int colorTopLeft;
    final int colorTopRight;
    final int colorBottomRight;
    final int colorBottomLeft;
    final float u0;
    final float v0;
    final float u1;
    final float v1;
    final float rotationDegrees;
    final float rotationOriginX;
    final float rotationOriginY;

    ImageQuad(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n, int n2, int n3, int n4, float f10, float f11, float f12, float f13, float f14, float f15, float f16) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.radiusTL = f5;
        this.radiusTR = f6;
        this.radiusBR = f7;
        this.radiusBL = f8;
        this.smoothness = f9;
        this.colorTopLeft = n;
        this.colorTopRight = n2;
        this.colorBottomRight = n3;
        this.colorBottomLeft = n4;
        this.u0 = f10;
        this.v0 = f11;
        this.u1 = f12;
        this.v1 = f13;
        this.rotationDegrees = f14;
        this.rotationOriginX = f15;
        this.rotationOriginY = f16;
    }

    ImageQuad(float f, float f2, float f3, float f4, float f5, int n, int n2, int n3, int n4) {
        this(f, f2, f3, f4, f5, f5, f5, f5, 0.0f, n, n2, n3, n4, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f);
    }
}

