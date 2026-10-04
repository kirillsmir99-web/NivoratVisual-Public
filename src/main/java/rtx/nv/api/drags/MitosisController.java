package rtx.nv.api.drags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.HudSettingsPanel;
import rtx.nv.api.drags.Position;
import rtx.nv.api.drags.SplitRectComp;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.render.render2d.ClientSplits;

public final class MitosisController {
    private static final MitosisController INSTANCE = new MitosisController();
    private static final boolean ENABLED = false;
    private static final double DURATION_MIN = 0.42;
    private static final double DURATION_MAX = 1.1;
    private static final double DURATION_BASE = 0.25;
    private static final double DURATION_PER_PX = 0.00275;
    private static final Easing OUT_QUART = d -> 1.0 - Math.pow(1.0 - d, 4.0);
    private static final float SMOOTHNESS = 40.0f;
    private static final float DEFAULT_RADIUS = 7.0f;
    private static final float MARGIN = (float)Math.ceil(40.0) + 4.0f;
    private final List<Split> active = new ArrayList<Split>();
    private final Set<Draggable> suppressed = new HashSet<Draggable>();
    private final Map<Draggable, ArrayDeque<SplitRectComp>> born = new HashMap<Draggable, ArrayDeque<SplitRectComp>>();
    private final Map<Draggable, MitosisController.SplitDraw> overrides = new HashMap<Draggable, MitosisController.SplitDraw>();
    private int counter;

    private MitosisController() {
    }

    public boolean split(Draggable draggable) {
        return false;
    }

    public static MitosisController get() {
        return INSTANCE;
    }

    public void stage() {
        if (this.active.isEmpty()) {
            return;
        }
        for (Split split : this.active) {
            float f;
            float f2;
            float f3;
            float f4;
            float f5;
            float f6 = Math.max(0.0f, Math.min(1.0f, split.anim.get()));
            float f7 = split.pw * 0.5f;
            float f8 = split.ph * 0.5f;
            float f9 = split.cw * 0.5f;
            float f10 = split.ch * 0.5f;
            if (split.detach == null) {
                f5 = split.parent.getDrag().getRenderX();
                f4 = split.parent.getDrag().getRenderY();
                f3 = f5 + (split.childMaxX - split.px);
                f2 = f4 + (split.childMaxY - split.py);
            } else {
                f5 = split.parent.getDrag().getRenderX();
                f4 = split.parent.getDrag().getRenderY();
                f3 = split.detach.getDrag().getRenderX();
                f2 = split.detach.getDrag().getRenderY();
            }
            float f11 = f5 + f7;
            float f12 = f4 + f8;
            float f13 = f3 + f9;
            float f14 = f2 + f10;
            float f15 = f11 + (f13 - f11) * f6;
            float f16 = f12 + (f14 - f12) * f6;
            float f17 = f9 * f6;
            float f18 = f10 * f6;
            float f19 = Math.min(split.radius, Math.min(f17, f18));
            float f20 = Math.min(f5, f15 - f17) - MARGIN;
            float f21 = Math.min(f4, f16 - f18) - MARGIN;
            float f22 = Math.max(f5 + split.pw, f15 + f17) + MARGIN;
            float f23 = Math.max(f4 + split.ph, f16 + f18) + MARGIN;
            float f24 = f22 - f20;
            float f25 = f23 - f21;
            float f26 = f11 - f20;
            float f27 = f12 - f21;
            float f28 = f15 - f20;
            float f29 = f16 - f21;
            float f30 = MitosisController.smoothstep(0.0f, 0.12f, f6);
            float f31 = 40.0f * f30 * (f = 1.0f - MitosisController.smoothstep(0.7f, 1.0f, f6));
            int n = ClientSplits.add(f26, f27, f7, f8, f28, f29, f17, f18, f19, f31);
            if (n <= 0) continue;
            this.overrides.put(split.parent, new MitosisController.SplitDraw(f20, f21, f24, f25, split.radius, n, f15 - f17, f16 - f18, f17 * 2.0f, f18 * 2.0f, f19));
        }
    }

    public MitosisController.SplitDraw overrideFor(Draggable draggable) {
        return this.overrides.get(draggable);
    }

    public boolean isSplitting(Draggable draggable) {
        return this.suppressed.contains(draggable);
    }

    private void forget(Draggable draggable) {
        for (ArrayDeque<SplitRectComp> arrayDeque : this.born.values()) {
            arrayDeque.remove(draggable);
        }
    }

    public void beginFrame() {
        ClientSplits.reset();
        this.overrides.clear();
        if (this.active.isEmpty()) {
            return;
        }
        Iterator<Split> iterator = this.active.iterator();
        while (iterator.hasNext()) {
            Split split = iterator.next();
            split.anim.update();
            if (!split.anim.isFinished()) continue;
            if (split.detach != null) {
                DragSystem.get().unregister(split.detach);
                this.suppressed.remove(split.detach);
                this.forget(split.detach);
            } else if (!split.reversing) {
                String string = "split_" + ++this.counter;
                float f = split.parent.getDrag().getRenderX() + (split.childMaxX - split.px);
                float f2 = split.parent.getDrag().getRenderY() + (split.childMaxY - split.py);
                SplitRectComp splitRectComp = new SplitRectComp(string, f, f2, split.cw, split.ch, split.radius, split.parent);
                DragSystem.get().register(splitRectComp);
                this.born.computeIfAbsent(split.parent, draggable -> new ArrayDeque()).push(splitRectComp);
            }
            iterator.remove();
        }
    }

    private boolean startMerge(SplitRectComp splitRectComp) {
        if (this.active.size() >= 16) {
            return false;
        }
        this.forget(splitRectComp);
        Draggable draggable = splitRectComp.origin();
        float f = draggable.width();
        float f2 = draggable.height();
        if (f <= 0.0f || f2 <= 0.0f) {
            return false;
        }
        float f3 = draggable.getDrag().getRenderX();
        float f4 = draggable.getDrag().getRenderY();
        float f5 = this.resolveRadius(f, f2);
        Split split = new Split(draggable, (Draggable)splitRectComp, f3, f4, f, f2, f5, splitRectComp.width(), splitRectComp.height(), splitRectComp.getDrag().getRenderX(), splitRectComp.getDrag().getRenderY());
        split.anim.setValue(1.0);
        split.anim.run(0.0, split.duration, OUT_QUART, false);
        split.reversing = true;
        this.active.add(split);
        this.suppressed.add(splitRectComp);
        return true;
    }

    public boolean isAnimating(Draggable draggable) {
        for (Split split : this.active) {
            if (split.parent != draggable && split.detach != draggable) continue;
            return true;
        }
        return false;
    }

    private static float smoothstep(float f, float f2, float f3) {
        float f4 = Math.max(0.0f, Math.min(1.0f, (f3 - f) / (f2 - f)));
        return f4 * f4 * (3.0f - 2.0f * f4);
    }

    private SplitRectComp newestLiveChild(Draggable draggable) {
        ArrayDeque<SplitRectComp> arrayDeque = this.born.get(draggable);
        if (arrayDeque == null) {
            return null;
        }
        List<Draggable> list = DragSystem.get().getAll();
        while (!arrayDeque.isEmpty()) {
            SplitRectComp splitRectComp = arrayDeque.peek();
            if (list.contains(splitRectComp)) {
                return splitRectComp;
            }
            arrayDeque.pop();
        }
        this.born.remove(draggable);
        return null;
    }

    private float resolveRadius(float f, float f2) {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f3 = interfaceModule == null ? 7.0f : interfaceModule.rectCornerRadius.getFloat();
        return Math.max(0.0f, Math.min(f3, Math.min(f, f2) * 0.5f));
    }

    private boolean startForward(Draggable draggable) {
        float f;
        float f2;
        float f3;
        float f4;
        float f5;
        if (this.active.size() >= 16) {
            return false;
        }
        float f6 = draggable.width();
        float f7 = draggable.height();
        if (f6 <= 0.0f || f7 <= 0.0f) {
            return false;
        }
        float f8 = draggable.getDrag().getRenderX();
        float f9 = draggable.getDrag().getRenderY();
        float f10 = Position.screenWidth();
        float f11 = f8 + f6 * 0.5f < f10 * 0.5f ? 1.0f : -1.0f;
        List<Setting> list = draggable.hudSettings();
        if (list.isEmpty()) {
            f5 = f6;
            f4 = f7;
            f3 = f6 * 1.25f + 10.0f;
            f2 = f8 + f3 * f11;
            f = f9;
        } else {
            f5 = 132.0f;
            f4 = HudSettingsPanel.height((int)list.size());
            f3 = 8.0f;
            f2 = f11 > 0.0f ? f8 + f6 + f3 : f8 - f5 - f3;
            f = f9;
        }
        f3 = list.isEmpty() ? this.resolveRadius(f6, f7) : this.resolveRadius(f5, f4);
        Split split = new Split(draggable, null, f8, f9, f6, f7, f3, f5, f4, f2, f);
        split.anim.run(1.0, split.duration, OUT_QUART, false);
        this.active.add(split);
        return true;
    }

    private static double durationFor(float f, float f2) {
        double d = Math.max(f, f2);
        return Math.max(0.42, Math.min(1.1, 0.25 + d * 0.00275));
    }

    private static class Split {
        final Draggable parent;
        final Draggable detach;
        final float px;
        final float py;
        final float pw;
        final float ph;
        final float radius;
        final float cw;
        final float ch;
        final float childMaxX;
        final float childMaxY;
        final rtx.nv.utils.animations.SmoothAnimation anim = new rtx.nv.utils.animations.SmoothAnimation();
        final double duration;
        boolean reversing;

        Split(Draggable parent, Draggable detach, float px, float py, float pw, float ph, float radius, float cw, float ch, float childMaxX, float childMaxY) {
            this.parent = parent;
            this.detach = detach;
            this.px = px;
            this.py = py;
            this.pw = pw;
            this.ph = ph;
            this.radius = radius;
            this.cw = cw;
            this.ch = ch;
            this.childMaxX = childMaxX;
            this.childMaxY = childMaxY;
            this.duration = MitosisController.durationFor(Math.abs(childMaxX - px), Math.abs(childMaxY - py));
        }
    }


    public static final class SplitDraw {
        public final float x;
        public final float y;
        public final float width;
        public final float height;
        public final float radius;
        public final int index;
        public final float childX;
        public final float childY;
        public final float childW;
        public final float childH;
        public final float childRadius;
    
        private SplitDraw(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7, float f8, float f9, float f10) {
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
            this.radius = f5;
            this.index = n;
            this.childX = f6;
            this.childY = f7;
            this.childW = f8;
            this.childH = f9;
            this.childRadius = f10;
        }
    }
}

