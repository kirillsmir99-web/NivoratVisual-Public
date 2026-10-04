package rtx.nv.utils.render.voronoi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class VoronoiOfQuad {
    private static final Random RANDOM = new Random();
    private static final int CANDIDATES_PER_POINT = 24;
    public final float x;
    public final float y;
    public final float x2;
    public final float y2;
    public final float cx;
    public final float cy;
    private final List<VoronoiOfQuad.Polygon> polygons;

    public VoronoiOfQuad(float f, float f2, float f3, float f4, List<VoronoiOfQuad.Vec2f> list) {
        this.x = f;
        this.y = f2;
        this.x2 = f3;
        this.y2 = f4;
        this.cx = f + (f3 - f) / 2.0f;
        this.cy = f2 + (f4 - f2) / 2.0f;
        this.polygons = this.getVoronoiPolygons(f, f2, f3, f4, list);
    }

    public VoronoiOfQuad(float f, float f2, float f3, float f4, int n) {
        this(f, f2, f3, f4, VoronoiOfQuad.genPointsInBounds(f, f2, f3, f4, n));
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public static VoronoiOfQuad spread(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, float f11, float f12, Random random) {
        VoronoiOfQuad.Vec2f vec2f;
        List<VoronoiOfQuad.Polygon> list;
        float f13 = (float)Math.sqrt((f7 - f5) * f9 * (f8 - f6) / (float)Math.max(1, n));
        float f14 = f13 * f12;
        List<VoronoiOfQuad.Vec2f> list2 = VoronoiOfQuad.genSpreadPointsInBounds(f5, f6, f7, f8, n, f9, f14, random);
        if (f10 > 0.001f && (list = new VoronoiOfQuad(f, f2, f3, f4, list2).getPolygons()).size() == list2.size()) {
            for (int i = 0; i < list2.size(); ++i) {
                VoronoiOfQuad.Vec2f vec2f2 = list2.get(i);
                vec2f = list.get((int)i).center;
                vec2f2.x = VoronoiOfQuad.clamp(vec2f2.x + (vec2f.x - vec2f2.x) * f10, f5, f7);
                vec2f2.y = VoronoiOfQuad.clamp(vec2f2.y + (vec2f.y - vec2f2.y) * f10, f6, f8);
            }
        }
        if (f11 > 0.001f) {
            float f15 = f13 * f11 / f9;
            float f16 = f13 * f11;
            for (int i = 0; i < list2.size(); ++i) {
                float f17;
                vec2f = list2.get(i);
                float f18 = VoronoiOfQuad.clamp(vec2f.x + (random.nextFloat() * 2.0f - 1.0f) * f15, f5, f7);
                if (!(VoronoiOfQuad.nearestDistance(list2, i, f18, f17 = VoronoiOfQuad.clamp(vec2f.y + (random.nextFloat() * 2.0f - 1.0f) * f16, f6, f8), f9) >= f14 * f14)) continue;
                vec2f.x = f18;
                vec2f.y = f17;
            }
        }
        return new VoronoiOfQuad(f, f2, f3, f4, list2);
    }

    public static VoronoiOfQuad spread(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, float f11, float f12) {
        return VoronoiOfQuad.spread(f, f2, f3, f4, f5, f6, f7, f8, n, f9, f10, f11, f12, RANDOM);
    }

    private static List<VoronoiOfQuad.Vec2f> genPointsInBounds(float f, float f2, float f3, float f4, int n) {
        ArrayList<VoronoiOfQuad.Vec2f> arrayList = new ArrayList<VoronoiOfQuad.Vec2f>(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(new VoronoiOfQuad.Vec2f(f + RANDOM.nextFloat() * (f3 - f), f2 + RANDOM.nextFloat() * (f4 - f2)));
        }
        return arrayList;
    }

    public List<VoronoiOfQuad.Polygon> getVoronoiPolygons(float f, float f2, float f3, float f4, List<VoronoiOfQuad.Vec2f> list) {
        ArrayList<VoronoiOfQuad.Polygon> arrayList = new ArrayList<VoronoiOfQuad.Polygon>();
        List<VoronoiOfQuad.Vec2f> list2 = Arrays.asList(new VoronoiOfQuad.Vec2f(f, f2), new VoronoiOfQuad.Vec2f(f3, f2), new VoronoiOfQuad.Vec2f(f3, f4), new VoronoiOfQuad.Vec2f(f, f4));
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            float f5;
            float f6;
            float f7;
            List<VoronoiOfQuad.Vec2f> list3 = new ArrayList<VoronoiOfQuad.Vec2f>(list2);
            for (int j = 0; !(j >= n || j != i && (list3 = this.clipPolygon(list3, f7 = 2.0f * (list.get((int)j).x - list.get((int)i).x), f6 = 2.0f * (list.get((int)j).y - list.get((int)i).y), f5 = list.get((int)i).x * list.get((int)i).x + list.get((int)i).y * list.get((int)i).y - (list.get((int)j).x * list.get((int)j).x + list.get((int)j).y * list.get((int)j).y))).isEmpty()); ++j) {
            }
            if (list3.size() < 3) continue;
            arrayList.add(new VoronoiOfQuad.Polygon(this, new ArrayList<VoronoiOfQuad.Vec2f>(list3)));
        }
        return arrayList;
    }

    private List<VoronoiOfQuad.Vec2f> clipPolygon(List<VoronoiOfQuad.Vec2f> list, float f, float f2, float f3) {
        ArrayList<VoronoiOfQuad.Vec2f> arrayList = new ArrayList<VoronoiOfQuad.Vec2f>();
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            VoronoiOfQuad.Vec2f vec2f;
            VoronoiOfQuad.Vec2f vec2f2 = list.get(i);
            VoronoiOfQuad.Vec2f vec2f3 = list.get((i + 1) % n);
            boolean bl = this.isInside(vec2f2, f, f2, f3);
            boolean bl2 = this.isInside(vec2f3, f, f2, f3);
            if (bl) {
                if (!bl2) {
                    vec2f = this.findIntersection(vec2f2, vec2f3, f, f2, f3);
                    if (vec2f == null) continue;
                    arrayList.add(vec2f);
                    continue;
                }
                arrayList.add(vec2f3);
                continue;
            }
            if (!bl2) continue;
            vec2f = this.findIntersection(vec2f2, vec2f3, f, f2, f3);
            if (vec2f != null) {
                arrayList.add(vec2f);
            }
            arrayList.add(vec2f3);
        }
        return arrayList;
    }

    private VoronoiOfQuad.Vec2f findIntersection(VoronoiOfQuad.Vec2f vec2f, VoronoiOfQuad.Vec2f vec2f2, float f, float f2, float f3) {
        float f4 = vec2f2.x - vec2f.x;
        float f5 = vec2f2.y - vec2f.y;
        float f6 = f * f4 + f2 * f5;
        if ((double)Math.abs(f6) < 1.0E-4) {
            return null;
        }
        float f7 = -(f * vec2f.x + f2 * vec2f.y + f3);
        float f8 = f7 / f6;
        return f8 >= 0.0f && f8 <= 1.0f ? new VoronoiOfQuad.Vec2f(vec2f.x + f8 * f4, vec2f.y + f8 * f5) : null;
    }

    private static float nearestDistance(List<VoronoiOfQuad.Vec2f> list, int n, float f, float f2, float f3) {
        float f4 = Float.MAX_VALUE;
        for (int i = 0; i < list.size(); ++i) {
            if (i == n) continue;
            VoronoiOfQuad.Vec2f vec2f = list.get(i);
            float f5 = (f - vec2f.x) * f3;
            float f6 = f2 - vec2f.y;
            f4 = Math.min(f4, f5 * f5 + f6 * f6);
        }
        return f4;
    }

    private static List<VoronoiOfQuad.Vec2f> genSpreadPointsInBounds(float f, float f2, float f3, float f4, int n, float f5, float f6, Random random) {
        ArrayList<VoronoiOfQuad.Vec2f> arrayList = new ArrayList<VoronoiOfQuad.Vec2f>(n);
        float f7 = f6 * f6;
        for (int i = 0; i < n; ++i) {
            VoronoiOfQuad.Vec2f vec2f = null;
            float f8 = -1.0f;
            for (int j = 0; j < 24; ++j) {
                VoronoiOfQuad.Vec2f vec2f2 = new VoronoiOfQuad.Vec2f(f + random.nextFloat() * (f3 - f), f2 + random.nextFloat() * (f4 - f2));
                if (arrayList.isEmpty()) {
                    vec2f = vec2f2;
                    f8 = Float.MAX_VALUE;
                    break;
                }
                float f9 = VoronoiOfQuad.nearestDistance(arrayList, -1, vec2f2.x, vec2f2.y, f5);
                if (f9 > f8) {
                    f8 = f9;
                    vec2f = vec2f2;
                }
                if (f8 >= f7) break;
            }
            if (vec2f == null || !(f8 >= f7)) continue;
            arrayList.add(vec2f);
        }
        return arrayList;
    }

    private static float wrapDegrees(float f) {
        float f2 = f % 360.0f;
        if (f2 >= 180.0f) {
            f2 -= 360.0f;
        }
        if (f2 < -180.0f) {
            f2 += 360.0f;
        }
        return f2;
    }

    private boolean isInside(VoronoiOfQuad.Vec2f vec2f, float f, float f2, float f3) {
        return f * vec2f.x + f2 * vec2f.y + f3 <= 0.0f;
    }

    public List<VoronoiOfQuad.Polygon> getPolygons() {
        return this.polygons;
    }


    public static class Polygon {
        public final VoronoiOfQuad.Vec2f center;
        public final ArrayList<VoronoiOfQuad.Vec2f> list;
        final /* synthetic */ VoronoiOfQuad this$0;
    
        public Polygon(VoronoiOfQuad voronoiOfQuad, ArrayList<VoronoiOfQuad.Vec2f> arrayList) {
            this.this$0 = voronoiOfQuad;
            this.list = arrayList;
            this.center = this.getCenter();
        }
    
        public Polygon copy() {
            ArrayList<VoronoiOfQuad.Vec2f> arrayList = new ArrayList<VoronoiOfQuad.Vec2f>(this.list.size());
            for (VoronoiOfQuad.Vec2f vec2f : this.list) {
                arrayList.add(new VoronoiOfQuad.Vec2f(vec2f.x, vec2f.y));
            }
            return new Polygon(this.this$0, arrayList);
        }
    
        public Polygon rotateAtYOfAngleAwayPos(float f, float f2, float f3) {
            float f4 = this.center.x - f;
            float f5 = this.center.y - f2;
            return this.rotateAngleOfCenter(VoronoiOfQuad.wrapDegrees(f4 * f3 * (float)(f5 < 0.0f ? 1 : -1)));
        }
    
        public Polygon rotateAngleOfCenter(float f) {
            if (f == 0.0f) {
                return this;
            }
            for (VoronoiOfQuad.Vec2f vec2f : this.list) {
                float f2 = vec2f.x - this.center.x;
                float f3 = vec2f.y - this.center.y;
                float f4 = (float)Math.toRadians(Math.toDegrees(Math.atan2(f3, f2)) + (double)f - 90.0);
                float f5 = (float)Math.sqrt(f2 * f2 + f3 * f3);
                vec2f.x = this.center.x - (float)Math.sin(f4) * f5;
                vec2f.y = this.center.y + (float)Math.cos(f4) * f5;
            }
            return this;
        }
    
        public Polygon setPolygonMidPos(float f, float f2) {
            float f3 = f - this.center.x;
            float f4 = f2 - this.center.y;
            if (f3 == 0.0f && f4 == 0.0f) {
                return this;
            }
            return this.moveXY(f3, f4);
        }
    
        public Polygon twirlAroundCenter(float f, float f2) {
            float f3;
            float f4;
            if (f == 0.0f) {
                return this;
            }
            float f5 = 0.0f;
            for (VoronoiOfQuad.Vec2f vec2f : this.list) {
                f4 = (vec2f.x - this.center.x) * f2;
                f3 = vec2f.y - this.center.y;
                f5 = Math.max(f5, (float)Math.sqrt(f4 * f4 + f3 * f3));
            }
            if (f5 <= 1.0E-6f) {
                return this;
            }
            for (VoronoiOfQuad.Vec2f vec2f : this.list) {
                f4 = (vec2f.x - this.center.x) * f2;
                f3 = vec2f.y - this.center.y;
                float f6 = (float)Math.sqrt(f4 * f4 + f3 * f3);
                float f7 = (float)Math.toRadians(f * (f6 / f5));
                float f8 = (float)Math.sin(f7);
                float f9 = (float)Math.cos(f7);
                float f10 = f4 * f9 - f3 * f8;
                float f11 = f4 * f8 + f3 * f9;
                vec2f.x = this.center.x + f10 / f2;
                vec2f.y = this.center.y + f11;
            }
            return this;
        }
    
        public float distanceToAtCenter(float f, float f2) {
            float f3 = this.center.x - f;
            float f4 = this.center.y - f2;
            return (float)Math.sqrt(f3 * f3 + f4 * f4);
        }
    
        public Polygon translateToPosLoc(float f, float f2, float f3) {
            float f4 = this.center.x - f;
            float f5 = this.center.y - f2;
            float f6 = (float)(Math.atan2(f5, f4) + Math.toRadians(90.0));
            return this.moveXY(-((float)Math.sin(f6)) * f3, (float)Math.cos(f6) * f3);
        }
    
        public Polygon translateAwayPosLoc(float f, float f2, float f3) {
            return this.translateToPosLoc(f, f2, -f3);
        }
    
        public Polygon rotateAtXOfAngleAwayPos(float f, float f2, float f3) {
            float f4 = this.center.x - f;
            float f5 = this.center.y - f2;
            return this.rotateAngleOfCenter(VoronoiOfQuad.wrapDegrees(f5 * f3 * (float)(f4 > 0.0f ? 1 : -1)));
        }
    
        public Polygon rotateAroundCenter(float f, float f2) {
            if (f == 0.0f) {
                return this;
            }
            float f3 = (float)Math.toRadians(f);
            float f4 = (float)Math.sin(f3);
            float f5 = (float)Math.cos(f3);
            for (VoronoiOfQuad.Vec2f vec2f : this.list) {
                float f6 = (vec2f.x - this.center.x) * f2;
                float f7 = vec2f.y - this.center.y;
                vec2f.x = this.center.x + (f6 * f5 - f7 * f4) / f2;
                vec2f.y = this.center.y + (f6 * f4 + f7 * f5);
            }
            return this;
        }
    
        public Polygon moveXY(float f, float f2) {
            this.center.x += f;
            this.center.y += f2;
            for (VoronoiOfQuad.Vec2f vec2f : this.list) {
                vec2f.x += f;
                vec2f.y += f2;
            }
            return this;
        }
    
        public VoronoiOfQuad.Vec2f getCenter() {
            VoronoiOfQuad.Vec2f vec2f = new VoronoiOfQuad.Vec2f(0.0f, 0.0f);
            for (VoronoiOfQuad.Vec2f vec2f2 : this.list) {
                vec2f.x += vec2f2.x;
                vec2f.y += vec2f2.y;
            }
            int n = Math.max(1, this.list.size());
            vec2f.x /= (float)n;
            vec2f.y /= (float)n;
            return vec2f;
        }
    
        public List<VoronoiOfQuad.Vec2f> getAllVertices() {
            return this.list;
        }
    }
    
        public static class Vec2f {
        public float x;
        public float y;
    
        public Vec2f(float f, float f2) {
            this.x = f;
            this.y = f2;
        }
    
        public float getUVTexX(float f, float f2) {
            return (this.x - f) / (f2 - f);
        }
    
        public float getUVTexY(float f, float f2) {
            return 1.0f - (f2 - this.y) / (f2 - f);
        }
    }
}

