package rtx.nv.utils.render.cape;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** Original analytic cloth surface. No simulation map, per-player cache or worker. */
public final class NvCapeMesh {
    public static final int SEGMENTS = 12;
    private NvCapeMesh() {}

    public static void draw(MatrixStack.Entry pose, VertexConsumer vertices, float age, float motion, int light) {
        float previousZ = depth(0, age, motion);
        float previousX = drift(0, age);
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = (float)i / SEGMENTS, t1 = (float)(i + 1) / SEGMENTS;
            float nextZ = depth(t1, age, motion), nextX = drift(t1, age);
            float v0 = (1 + t0 * 16) / 32, v1 = (1 + t1 * 16) / 32;
            float half = .3125f, thickness = .035f;
            // Outer and inner faces use the standard Minecraft cape texture regions.
            quad(pose, vertices, previousX-half, t0, previousZ, previousX+half, t0, previousZ,
                    nextX+half, t1, nextZ, nextX-half, t1, nextZ, 11f/64, 1f/64, v0, v1, light, 0, 0, 1);
            quad(pose, vertices, previousX+half, t0, previousZ-thickness, previousX-half, t0, previousZ-thickness,
                    nextX-half, t1, nextZ-thickness, nextX+half, t1, nextZ-thickness, 22f/64, 12f/64, v0, v1, light, 0, 0, -1);
            quad(pose, vertices, previousX-half, t0, previousZ-thickness, previousX-half, t0, previousZ,
                    nextX-half, t1, nextZ, nextX-half, t1, nextZ-thickness, 1f/64, 0, v0, v1, light, -1, 0, 0);
            quad(pose, vertices, previousX+half, t0, previousZ, previousX+half, t0, previousZ-thickness,
                    nextX+half, t1, nextZ-thickness, nextX+half, t1, nextZ, 12f/64, 11f/64, v0, v1, light, 1, 0, 0);
            if (i == 0 || i == SEGMENTS-1) {
                float x = i == 0 ? previousX : nextX, z = i == 0 ? previousZ : nextZ, y = i == 0 ? t0 : t1;
                quad(pose, vertices, x-half, y, z-thickness, x+half, y, z-thickness,
                        x+half, y, z, x-half, y, z, 11f/64, 1f/64, 0, 1f/32, light, 0, i == 0 ? -1 : 1, 0);
            }
            previousZ = nextZ;
            previousX = nextX;
        }
    }

    private static float depth(float t, float age, float motion) {
        return .18f + .06f*t + t*t*(motion*.42f + (float)Math.sin(age*.12f-t*3.1f)*(.035f+motion*.025f));
    }
    private static float drift(float t, float age) { return (float)Math.sin(age*.085f-t*2.2f)*.018f*t*t; }

    private static void quad(MatrixStack.Entry pose, VertexConsumer out,
            float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,
            float u0,float u1,float v0,float v1,int light,float nx,float ny,float nz) {
        vertex(pose,out,x0,y0,z0,u0,v0,light,nx,ny,nz); vertex(pose,out,x1,y1,z1,u1,v0,light,nx,ny,nz);
        vertex(pose,out,x2,y2,z2,u1,v1,light,nx,ny,nz); vertex(pose,out,x3,y3,z3,u0,v1,light,nx,ny,nz);
    }
    private static void vertex(MatrixStack.Entry pose, VertexConsumer out, float x,float y,float z,float u,float v,int light,float nx,float ny,float nz) {
        out.vertex(pose,x,y,z).color(-1).texture(u,v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(pose,nx,ny,nz);
    }
}
