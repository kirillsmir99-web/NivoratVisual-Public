package rtx.nv.api.mods.geckolib.cache.model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtx.nv.api.mods.geckolib.cache.model.GeoVertex;
import rtx.nv.api.mods.geckolib.loading.json.raw.FaceUV;
import rtx.nv.api.mods.geckolib.loading.json.raw.FaceUV.Rotation;

public record GeoQuad(GeoVertex[] vertices, float normalX, float normalY, float normalZ, Direction direction) {
    public static GeoQuad build(GeoVertex[] geoVertexArray, double[] dArray, double[] dArray2, FaceUV.Rotation rotation, float f, float f2, boolean bl, Direction direction) {
        return GeoQuad.build(geoVertexArray, (float)dArray[0], (float)dArray[1], (float)dArray2[0], (float)dArray2[1], rotation, f, f2, bl, direction);
    }

    public static GeoQuad build(GeoVertex[] geoVertexArray, float f, float f2, float f3, float f4, FaceUV.Rotation rotation, float f5, float f6, boolean bl, Direction direction) {
        float f7 = (f + f3) / f5;
        float f8 = (f2 + f4) / f6;
        f /= f5;
        f2 /= f6;
        Vector3f vector3f = direction.getUnitVector();
        if (!bl) {
            float f9 = f7;
            f7 = f;
            f = f9;
        } else {
            vector3f.mul(-1.0f, 1.0f, 1.0f);
        }
        float[] fArray = rotation.rotateUvs(f, f2, f7, f8);
        geoVertexArray[0] = geoVertexArray[0].withUVs(fArray[0], fArray[1]);
        geoVertexArray[1] = geoVertexArray[1].withUVs(fArray[2], fArray[3]);
        geoVertexArray[2] = geoVertexArray[2].withUVs(fArray[4], fArray[5]);
        geoVertexArray[3] = geoVertexArray[3].withUVs(fArray[6], fArray[7]);
        return new GeoQuad(geoVertexArray, vector3f.x, vector3f.y, vector3f.z, direction);
    }

    public Vector3f normalVec() {
        return new Vector3f(this.normalX, this.normalY, this.normalZ);
    }

    public void render(Matrix4f matrix4f, Vector3f vector3f, VertexConsumer vertexConsumer, int n, int n2, int n3) {
        for (GeoVertex geoVertex : this.vertices()) {
            Vector4f vector4f = matrix4f.transform(new Vector4f(geoVertex.posX(), geoVertex.posY(), geoVertex.posZ(), 1.0f));
            vertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), n3, geoVertex.texU(), geoVertex.texV(), n2, n, vector3f.x(), vector3f.y(), vector3f.z());
        }
    }
}

