package rtx.nv.utils.render.cape;

public final class CapeVertexColor {
    private CapeVertexColor() {}

    public static int at(float u, float v, float alpha) {
        int a = (int)(alpha * 255.0f) & 0xFF;
        return (a << 24) | 0xFFFFFF;
    }
}
