package rtx.nv.api.ui.window;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import rtx.nv.utils.render.voronoi.VoronoiOfQuad;

public final class GuiShatterAnimation {
    public static final int MAX_SHARDS = 8;
    public static final int VERTEX_BYTES = 24;
    private static final int MAX_POLY_VERTICES = 24;
    public static final int MAX_VERTICES = 576;
    private static final int MIN_COUNT = 5;
    private static final int MAX_COUNT = 7;
    private static final float RELAX_STRENGTH = 0.45f;
    private static final float SEED_JITTER = 0.32f;
    private static final float MIN_DISTANCE = 0.68f;
    private static final float EXPAND = 0.34f;
    private static final float SPREAD = 0.028f;
    private static final float MARGIN = 0.004f;
    private static final float CURL_DEGREES = 26.0f;
    private static final float CURL_DEPTH = 0.1f;
    private static final float SCREEN_PERSPECTIVE = 0.9f;
    private static final long GATHER_NS = 360000000L;
    private static final float MAX_BLUR_RADIUS = 9.0f;
    private static final Random RANDOM = new Random();
    private static final State LOCAL = new State();

    private GuiShatterAnimation() {
    }

    public static void begin(float f, float f2, float f3, float f4, float f5, float f6, float f7, long l) {
        LOCAL.begin(f, f2, f3, f4, f5, f6, f7, l);
    }

    public static void begin(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        LOCAL.begin(f, f2, f3, f4, f5, f6, f7, RANDOM.nextLong());
    }

    public static void cancel() {
        LOCAL.cancel();
    }

    public static State create() {
        return new State();
    }

    public static boolean isActive() {
        return LOCAL.isActive();
    }

    public static long seed() {
        return LOCAL.seed();
    }

    public static State local() {
        return LOCAL;
    }

    public static void gather(long l) {
        LOCAL.gather(l);
    }

    public static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    private static float easeOutCubic(float f) {
        float f2 = 1.0f - f;
        return 1.0f - f2 * f2 * f2;
    }

    public static float blurRadius() {
        return LOCAL.blurRadius();
    }

    public static float[] beginRect() {
        return LOCAL.beginRect();
    }

    public static boolean resume() {
        return LOCAL.resume();
    }

    public static float progress(float f) {
        return LOCAL.progress(f);
    }

    public static int buildGeometry(ByteBuffer byteBuffer, float f, float f2, float f3, boolean bl) {
        return LOCAL.buildGeometry(byteBuffer, f, f2, f3, bl, null);
    }

    public static int buildGeometry(ByteBuffer byteBuffer, float f, float f2, float f3, boolean bl, Remap remap) {
        return LOCAL.buildGeometry(byteBuffer, f, f2, f3, bl, remap);
    }

    public static float clampSigned(float f) {
        return Math.max(-1.0f, Math.min(1.0f, f));
    }

    public static boolean isGathering() {
        return LOCAL.isGathering();
    }

    public record Remap(float posOffsetX, float posOffsetY, float posScaleX, float posScaleY, float uvOffsetX, float uvOffsetY, float uvScaleX, float uvScaleY) {
    }

    public static final class State {
        private final List<VoronoiOfQuad.Polygon> shards = new ArrayList<>();
        private float[] baseAngle = new float[0];
        private float[] baseRadius = new float[0];
        private float[] fade = new float[0];
        private float[] radial = new float[0];
        private boolean gathering;
        private long gatherStartNs;
        private long gatherNanos = 360000000L;
        private float gatherFrom;
        private float resumeFrom;
        private float lastProgress;
        private float centerX;
        private float centerY;
        private float halfExtentX = 1.0f;
        private float halfExtentY = 1.0f;
        private float aspect = 1.0f;
        private boolean active;
        private long currentSeed;
        private float beginX;
        private float beginY;
        private float beginWidth;
        private float beginHeight;
        private float beginPad;
        private float beginScreenWidth;
        private float beginScreenHeight;

        public void begin(float f, float f2, float f3, float f4, float f5, float f6, float f7, long l) {
            this.cancel();
            if (f3 <= 1.0f || f4 <= 1.0f || f6 <= 1.0f || f7 <= 1.0f) {
                return;
            }
            Random random = new Random(l);
            this.currentSeed = l;
            this.beginX = f;
            this.beginY = f2;
            this.beginWidth = f3;
            this.beginHeight = f4;
            this.beginPad = f5;
            this.beginScreenWidth = f6;
            this.beginScreenHeight = f7;
            float f8 = GuiShatterAnimation.clamp01(f / f6);
            float f9 = GuiShatterAnimation.clamp01(f2 / f7);
            float f10 = GuiShatterAnimation.clamp01((f + f3) / f6);
            float f11 = GuiShatterAnimation.clamp01((f2 + f4) / f7);
            this.centerX = (f8 + f10) * 0.5f;
            this.centerY = (f9 + f11) * 0.5f;
            this.halfExtentX = Math.max(0.001f, (f10 - f8) * 0.5f);
            this.halfExtentY = Math.max(0.001f, (f11 - f9) * 0.5f);
            this.aspect = Math.max(0.001f, f6 / f7);
            int n = 5 + random.nextInt(3);
            VoronoiOfQuad voronoiOfQuad = VoronoiOfQuad.spread(f8, f9, f10, f11, f8 - 0.004f, f9 - 0.004f, f10 + 0.004f, f11 + 0.004f, n, this.aspect, 0.45f, 0.32f, 0.68f, random);
            this.shards.clear();
            this.shards.addAll(voronoiOfQuad.getPolygons());
            int n2 = this.shards.size();
            this.baseAngle = new float[n2];
            this.baseRadius = new float[n2];
            this.fade = new float[n2];
            this.radial = new float[n2];
            for (int i = 0; i < n2; ++i) {
                VoronoiOfQuad.Polygon polygon = this.shards.get(i);
                float f12 = (polygon.center.x - this.centerX) * this.aspect;
                float f13 = polygon.center.y - this.centerY;
                float f14 = (float)Math.sqrt(f12 * f12 + f13 * f13);
                this.baseAngle[i] = (float)Math.atan2(f13, f12) + (random.nextFloat() - 0.5f) * 0.35f;
                this.baseRadius[i] = f14;
                this.fade[i] = 0.55f + 0.45f * random.nextFloat();
                this.radial[i] = 0.65f + 0.7f * random.nextFloat();
            }
            this.active = true;
        }

        public void cancel() {
            this.active = false;
            this.gathering = false;
            this.lastProgress = 0.0f;
            this.shards.clear();
        }

        public boolean isActive() {
            return this.active;
        }

        public long seed() {
            return this.currentSeed;
        }

        public void gather(long l) {
            if (!this.active) {
                return;
            }
            this.gathering = true;
            this.gatherStartNs = System.nanoTime();
            this.gatherNanos = Math.max(1000000L, l);
            this.gatherFrom = this.lastProgress;
        }

        public boolean isGathering() {
            return this.active && this.gathering;
        }

        public float blurRadius() {
            if (!this.active) {
                return 0.0f;
            }
            return 9.0f * (1.0f - this.lastProgress);
        }

        public float lastProgress() {
            return this.lastProgress;
        }

        public float[] beginRect() {
            return new float[]{this.beginX, this.beginY, this.beginWidth, this.beginHeight, this.beginPad, this.beginScreenWidth, this.beginScreenHeight};
        }

        public boolean resume() {
            if (!this.active) {
                return false;
            }
            this.gathering = false;
            this.resumeFrom = this.lastProgress;
            return true;
        }

        public float progress(float f) {
            if (!this.active) {
                return 0.0f;
            }
            if (this.gathering) {
                long l = System.nanoTime() - this.gatherStartNs;
                float f2 = GuiShatterAnimation.clamp01((float)l / (float)this.gatherNanos);
                this.lastProgress = this.gatherFrom * (1.0f - GuiShatterAnimation.easeOutCubic(f2));
                if (f2 >= 1.0f) {
                    this.cancel();
                    return 0.0f;
                }
                return this.lastProgress;
            }
            this.lastProgress = f;
            return f;
        }

        public int buildGeometry(ByteBuffer byteBuffer, float f, float f2, float f3, boolean bl, Remap remap) {
            if (!this.active || this.shards.isEmpty()) {
                return 0;
            }
            byteBuffer.clear();
            int n = 0;
            float f4 = this.progress(f);
            float f5 = GuiShatterAnimation.easeOutCubic(f4);
            for (int i = 0; i < this.shards.size(); ++i) {
                VoronoiOfQuad.Polygon polygon = this.shards.get(i);
                List<VoronoiOfQuad.Vec2f> list = polygon.getAllVertices();
                if (list.size() < 3) continue;
                float f6 = this.baseAngle[i];
                float f7 = this.baseRadius[i] + f5 * 0.16f * this.radial[i];
                float f8 = this.centerX + (float)Math.cos(f6) * f7 / this.aspect;
                float f9 = this.centerY + (float)Math.sin(f6) * f7;
                float f10 = f8 - polygon.center.x;
                float f11 = f9 - polygon.center.y;
                float f12 = 1.0f - f5 * this.fade[i];
                if (f12 <= 0.0f) continue;
                float rot = (i % 2 == 0 ? 1.0f : -1.0f) * f5 * 0.06f;
                float cosR = (float)Math.cos(rot);
                float sinR = (float)Math.sin(rot);
                VoronoiOfQuad.Vec2f v0 = list.get(0);
                for (int j = 1; j < list.size() - 1; ++j) {
                    VoronoiOfQuad.Vec2f v1 = list.get(j);
                    VoronoiOfQuad.Vec2f v2 = list.get(j + 1);
                    this.putRotatedVertex(byteBuffer, v0, polygon.center, cosR, sinR, f10, f11, f12, f2, f3, remap);
                    this.putRotatedVertex(byteBuffer, v1, polygon.center, cosR, sinR, f10, f11, f12, f2, f3, remap);
                    this.putRotatedVertex(byteBuffer, v2, polygon.center, cosR, sinR, f10, f11, f12, f2, f3, remap);
                    n += 3;
                }
            }
            byteBuffer.flip();
            return n;
        }

        private void putRotatedVertex(ByteBuffer byteBuffer, VoronoiOfQuad.Vec2f vec, VoronoiOfQuad.Vec2f center, float cosR, float sinR, float ox, float oy, float alpha, float sw, float sh, Remap remap) {
            float rx = (vec.x - center.x) * this.aspect;
            float ry = vec.y - center.y;
            float rotX = (rx * cosR - ry * sinR) / this.aspect;
            float rotY = rx * sinR + ry * cosR;
            float px = (center.x + rotX + ox) * sw;
            float py = (center.y + rotY + oy) * sh;
            float u = vec.x;
            float v = vec.y;
            if (remap != null) {
                px = remap.posOffsetX() + px * remap.posScaleX();
                py = remap.posOffsetY() + py * remap.posScaleY();
                u = remap.uvOffsetX() + u * remap.uvScaleX();
                v = remap.uvOffsetY() + v * remap.uvScaleY();
            }
            byteBuffer.putFloat(px);
            byteBuffer.putFloat(py);
            byteBuffer.putFloat(0.0f);
            byteBuffer.putFloat(u);
            byteBuffer.putFloat(v);
            byteBuffer.putFloat(alpha);
        }

        private void putVertex(ByteBuffer byteBuffer, VoronoiOfQuad.Vec2f vec, float ox, float oy, float alpha, float sw, float sh, Remap remap) {
            float px = (vec.x + ox) * sw;
            float py = (vec.y + oy) * sh;
            float u = vec.x;
            float v = vec.y;
            if (remap != null) {
                px = remap.posOffsetX() + px * remap.posScaleX();
                py = remap.posOffsetY() + py * remap.posScaleY();
                u = remap.uvOffsetX() + u * remap.uvScaleX();
                v = remap.uvOffsetY() + v * remap.uvScaleY();
            }
            byteBuffer.putFloat(px);
            byteBuffer.putFloat(py);
            byteBuffer.putFloat(0.0f);
            byteBuffer.putFloat(u);
            byteBuffer.putFloat(v);
            byteBuffer.putFloat(alpha);
        }
    }
}
