package rtx.nv.api.drags;

import java.util.*;

/** Temporary collision offsets never alter saved drag positions. */
public final class HudLayoutCoordinator {
    private record Area(float x, float y, float w, float h, long expires) {}
    private static final Map<String, Area> overlays = new HashMap<>();
    private static final Map<Draggable, float[]> offsets = new IdentityHashMap<>();
    private static final List<Area> placed = new ArrayList<>();
    private static final long started = System.nanoTime();
    private HudLayoutCoordinator() {}
    public static void reserve(String id, float x, float y, float w, float h) {
        overlays.put(id, new Area(x, y, w, h, System.currentTimeMillis() + 150));
    }
    public static void beginFrame() {
        long now = System.currentTimeMillis();
        overlays.values().removeIf(a -> a.expires < now);
        placed.clear(); placed.addAll(overlays.values());
    }
    public static void place(Draggable element) {
        float[] offset = offsets.computeIfAbsent(element, e -> new float[3]);
        long now = System.nanoTime() - started;
        float dt = offset[2] == 0 ? 1 : Math.min(.05f, (now / 1e9f) - offset[2]);
        offset[2] = now / 1e9f;
        float x = element.getDrag().getRenderX(), y = element.getDrag().getRenderY();
        float w = element.width(), h = element.height(), tx = 0, ty = 0;
        if (!DragSystem.get().isDragModeActive() && element.isInteractive()) {
            if (!clear(x,y,w,h)) {
                List<Float> xs = new ArrayList<>(List.of(x,5f,Math.max(5,Position.screenWidth()-w-5)));
                List<Float> ys = new ArrayList<>(List.of(y,5f,Math.max(5,Position.screenHeight()-h-5)));
                for (Area a : placed) { xs.add(a.x-w-6);xs.add(a.x+a.w+6);ys.add(a.y-h-6);ys.add(a.y+a.h+6); }
                float best = Float.MAX_VALUE;
                for(float cx : xs) for(float cy : ys) {
                    if(cx<5 || cy<5 || cx+w>Position.screenWidth()-5 || cy+h>Position.screenHeight()-5 || !clear(cx,cy,w,h)) continue;
                    float cost=(cx-x)*(cx-x)+(cy-y)*(cy-y);
                    if(cost<best) { best=cost;tx=cx-x;ty=cy-y; }
                }
            }
        }
        offset[0] = 0.0f;
        offset[1] = 0.0f;
        if (element.isInteractive()) placed.add(new Area(x, y, w, h, 0));
    }
    private static boolean clear(float x,float y,float w,float h) {
        for(Area a : placed) if(x<a.x+a.w+5 && x+w>a.x-5 && y<a.y+a.h+5 && y+h>a.y-5) return false;
        return true;
    }
    public static float x(Draggable element) { return 0.0f; }
    public static float y(Draggable element) { return 0.0f; }
}
