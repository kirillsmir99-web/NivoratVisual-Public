package rtx.nv.api.mods.geckolib.cache.model;

public record GeoVertex(float posX, float posY, float posZ, float texU, float texV) {
    public GeoVertex(double d, double d2, double d3) {
        this((float)d, (float)d2, (float)d3, 0.0f, 0.0f);
    }

    public GeoVertex withUVs(float f, float f2) {
        return new GeoVertex(this.posX, this.posY, this.posZ, f, f2);
    }
}

