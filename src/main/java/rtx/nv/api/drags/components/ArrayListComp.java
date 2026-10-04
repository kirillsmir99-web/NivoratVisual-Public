package rtx.nv.api.drags.components;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ArrayListModule;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;

public final class ArrayListComp
extends Draggable {
    private static final String ROW_FONT = "montserrat-medium";
    private static final float ROW_SIZE = 7.2f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float PAD_X = 5.0f;
    private static final float PAD_Y_UP = 3.5f;
    private static final float PAD_Y_DOWN = 3.5f;
    private static final float RADIUS = 6.0f;
    private static final float MIN_WIDTH = 1.0f;
    private static final double ROW_IN_SECONDS = 0.16;
    private static final double ROW_OUT_SECONDS = 0.12;
    private static final float MIN_TEXT_ALPHA = 0.003921569f;
    private static final float WAVE_FREQ = 0.05f;
    private static final Easing ROW_EASING = d -> d * d * (3.0 - 2.0 * d);
    private final Map<Module, SmoothAnimation> rowAnimations = new IdentityHashMap<Module, SmoothAnimation>();
    private final Map<Module, Boolean> rowTargets = new IdentityHashMap<Module, Boolean>();
    private final SmoothAnimation visibility = new SmoothAnimation();
    private boolean lastTargetVisible;
    private float currentWidth = 1.0f;
    private float currentHeight = 9.7f;
    private float[] rowWidths = new float[0];
    private float[] rowHeights = new float[0];
    private int rowCountMetric = 0;
    private boolean leftAlignedState = true;
    private boolean bottomAnchoredState = false;
    private double wavePhase = 0.0;
    private long lastWaveNow = 0L;
    private float lastPinnedHeight = Float.NaN;
    private float lastPinnedWidth = Float.NaN;

    public ArrayListComp() {
        super("arraylist", Math.max(100.0f, Position.screenWidth() - 110.0f), 5.0f);
        this.getDrag().setForceDirectDrag(true);
        this.visibility.set(0.0);
    }

    @Override
    public void resetToDefault() {
        float defaultX = Math.max(10.0f, Position.screenWidth() - this.width() - 5.0f);
        float defaultY = 5.0f;
        this.getDrag().setTargetX(defaultX);
        this.getDrag().setTargetY(defaultY);
        this.getDrag().syncToTarget();
        this.lastPinnedWidth = this.currentWidth;
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    @Override
    public float width() {
        return this.currentWidth;
    }

    @Override
    public float height() {
        return this.currentHeight;
    }

    @Override
    public boolean isInteractive() {
        return this.shouldShow();
    }

    private void refreshMetrics(List<Module> list) {
        if (this.rowWidths.length < list.size()) {
            this.rowWidths = new float[list.size()];
            this.rowHeights = new float[list.size()];
        }
        float f = 0.0f;
        float f2 = 0.0f;
        int n = 0;
        for (Module module : list) {
            float f3 = this.rowProgress(module);
            if (f3 <= 0.001f) continue;
            float f4 = Render2D.msdfWidth(ROW_FONT, module.getDisplayName(), ROW_SIZE) + PAD_X * 2.0f;
            float f5 = ROW_HEIGHT * f3;
            f = Math.max(f, f4);
            f2 += f5;
            this.rowWidths[n] = f4;
            this.rowHeights[n] = f5;
            ++n;
        }
        this.rowCountMetric = n;
        this.currentWidth = Math.max(f, 1.0f);
        this.currentHeight = f2 + PAD_Y_UP + PAD_Y_DOWN;
    }

    private float rowProgress(Module module) {
        SmoothAnimation smoothAnimation = this.rowAnimations.get(module);
        return smoothAnimation == null ? 0.0f : ArrayListComp.clamp(smoothAnimation.get(), 0.0f, 1.0f);
    }

    private boolean isBottomHalf() {
        float f = Math.max(1.0f, Position.screenHeight());
        return this.getDrag().getRenderY() + this.currentHeight * 0.5f > f * 0.5f;
    }

    private void updateOrientation() {
        this.leftAlignedState = !this.isRightHalf();
        this.bottomAnchoredState = this.isBottomHalf();
    }

    private void updateAnimations() {
        for (Module module2 : ModuleManager.get().getAll()) {
            boolean bl = ArrayListComp.isListed(module2);
            SmoothAnimation smoothAnimation = this.rowAnimations.computeIfAbsent(module2, m -> {
                SmoothAnimation anim = new SmoothAnimation();
                anim.set(0.0);
                return anim;
            });
            Boolean bl2 = this.rowTargets.get(module2);
            if (bl2 == null || bl2 != bl) {
                smoothAnimation.run(bl ? 1.0 : 0.0, bl ? 0.22 : 0.16, ROW_EASING, false);
                this.rowTargets.put(module2, bl);
            }
            smoothAnimation.update();
        }
    }

    private List<Module> visibleRows() {
        ArrayList<Module> arrayList = new ArrayList<Module>();
        for (Module module2 : ModuleManager.get().getAll()) {
            if (ArrayListComp.isExcluded(module2) || !(this.rowProgress(module2) > 0.001f)) continue;
            arrayList.add(module2);
        }
        arrayList.sort(Comparator.comparingDouble((Module module) -> Render2D.msdfWidth(ROW_FONT, module.getDisplayName(), 7.2f)).reversed());
        return arrayList;
    }

    private static void drawGradientText(String string, float f, float f2, float f3, int n) {
        float f4 = ArrayListComp.clamp(f3, 0.0f, 1.0f);
        if (f4 <= 0.003921569f || string == null || string.isEmpty()) {
            return;
        }
        Render2D.msdfText(ROW_FONT, string, f, f2, 7.2f, n);
    }

    private boolean isRightHalf() {
        float f = Math.max(1.0f, Position.screenWidth());
        return this.getDrag().getRenderX() + this.currentWidth * 0.5f > f * 0.5f;
    }

    private static boolean isExcluded(Module module) {
        return module instanceof ClickGui;
    }

    @Override
    protected void render(DrawContext drawContext) {
        boolean bl;
        float f;
        this.updateAnimations();
        List<Module> list = this.visibleRows();
        this.refreshMetrics(list);
        this.updateOrientation();
        boolean bl2 = this.bottomAnchoredState;
        if (bl2) {
            Collections.reverse(list);
        }
        if (!this.getDrag().isDragging()) {
            if (Float.isNaN(this.lastPinnedWidth) && this.isRightHalf()) {
                this.leftAlignedState = false;
                float targetRightX = Position.screenWidth() - this.currentWidth - 5.0f;
                if (Math.abs((this.getX() + this.currentWidth) - (Position.screenWidth() - 5.0f)) > 1.0f && this.getX() >= Position.screenWidth() * 0.7f) {
                    this.getDrag().setTargetX(targetRightX);
                    this.getDrag().syncToTarget();
                }
            } else if (!Float.isNaN(this.lastPinnedWidth) && !this.leftAlignedState && Math.abs(this.currentWidth - this.lastPinnedWidth) > 1.0E-4f) {
                float dw = this.currentWidth - this.lastPinnedWidth;
                this.getDrag().adjustX(-dw);
                this.getDrag().syncToTarget();
            }
            if (!Float.isNaN(this.lastPinnedHeight) && bl2 && Math.abs(f = this.currentHeight - this.lastPinnedHeight) > 1.0E-4f) {
                this.getDrag().adjustY(-f);
                this.getDrag().syncToTarget();
            }
        }
        this.lastPinnedWidth = this.currentWidth;
        this.lastPinnedHeight = this.currentHeight;
        boolean bl3 = bl = this.shouldShow() && !list.isEmpty();
        if (bl != this.lastTargetVisible) {
            this.visibility.run(bl ? 1.0 : 0.0, bl ? 0.18 : 0.12, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = bl;
        }
        this.visibility.update();
        float f2 = this.visibility.get();
        if (bl && f2 <= 0.01f) {
            f2 = 0.01f;
        }
        if (f2 <= 0.01f && !bl) {
            return;
        }
        float f3 = this.getX();
        float f4 = this.getY();
        boolean bl4 = this.leftAlignedState;
        float f5 = 0.92f + f2 * 0.08f;
        float f6 = f3 + this.currentWidth * 0.5f;
        float f7 = f4 + this.currentHeight * 0.5f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f6, f7);
        drawContext.getMatrices().scale(f5);
        drawContext.getMatrices().translate(-f6, -f7);
        Render2D.beginFrame(drawContext);
        ArrayListModule arrayListModule = ModuleManager.get().get(ArrayListModule.class);
        String arrayStyle = arrayListModule != null ? arrayListModule.style.getValue() : "Планки";
        boolean isBars = !"Минимализм".equalsIgnoreCase(arrayStyle);
        boolean isMinimal = "Минимализм".equalsIgnoreCase(arrayStyle);
        boolean showSideBar = arrayListModule == null || arrayListModule.sideBar.getValue();

        double d = arrayListModule == null ? 0.0 : arrayListModule.waveSpeed();
        long l = System.currentTimeMillis();
        long l2 = this.lastWaveNow == 0L ? 0L : Math.max(0L, Math.min(100L, l - this.lastWaveNow));
        this.lastWaveNow = l;
        this.wavePhase = (this.wavePhase + (double)l2 * d) % (Math.PI * 2);
        float f8 = (float)this.wavePhase;
        int n = 0;
        float f9 = f4 + PAD_Y_UP;
        for (Module module : list) {
            float f10 = this.rowProgress(module);
            if (f10 <= 0.001f) continue;
            float f11 = ROW_HEIGHT * f10;
            float textW = Render2D.msdfWidth(ROW_FONT, module.getDisplayName(), ROW_SIZE);
            float rowW = textW + PAD_X * 2.0f;
            float rowX = bl4 ? f3 : f3 + this.currentWidth - rowW;
            float textX = bl4 ? (showSideBar && isBars ? rowX + PAD_X + 2.0f : rowX + PAD_X) : (showSideBar && isBars ? rowX + PAD_X - 1.0f : rowX + PAD_X);
            float textY = f9 + (f11 - ROW_SIZE) * 0.5f - 0.2f;
            float f15 = ArrayListComp.clamp(f2 * f10, 0.0f, 1.0f);
            float f16 = 0.5f + 0.5f * (float)Math.sin((f9 + f11 * 0.5f) * 0.05f - f8);
            int n3;
            int n4 = ClientAccent.gradientA(f15 * 255.0f);
            if ((n4 & 0xFFFFFF) == ((n3 = ClientAccent.gradientB(f15 * 255.0f)) & 0xFFFFFF)) {
                n3 = ArrayListComp.mixWhite(n4, 0.3f);
            }
            int n2 = ClientAccent.mix(n4, n3, f16);

            if (isBars) {
                int rowBg = ColorUtil.rgba(14, 17, 24, (int)(165 * f15));
                Render2D.rect(rowX, f9 + 0.5f, rowW, f11 - 1.0f, 3.5f, rowBg);
                int rowBorder = ColorUtil.rgba(255, 255, 255, (int)(22 * f15));
                Render2D.outline(rowX, f9 + 0.5f, rowW, f11 - 1.0f, 3.5f, 0.65f, rowBorder);
                if (showSideBar) {
                    float barIndicatorX = bl4 ? rowX : rowX + rowW - 2.0f;
                    Render2D.rect(barIndicatorX, f9 + 1.5f, 2.0f, f11 - 3.0f, 1.0f, n2);
                }
            }

            int shadowCol = ColorUtil.rgba(0, 0, 0, Math.round(f15 * 115.0f));
            Render2D.msdfText(ROW_FONT, module.getDisplayName(), textX + 0.5f, textY + 0.5f, ROW_SIZE, shadowCol);
            ArrayListComp.drawGradientText(module.getDisplayName(), textX, textY, f15, n2);
            ++n;
            f9 += f11;
        }
        Render2D.flush();
        drawContext.getMatrices().popMatrix();
    }

    private boolean shouldShow() {
        ArrayListModule arrayListModule = ModuleManager.get().get(ArrayListModule.class);
        return arrayListModule != null && arrayListModule.isEnabled();
    }

    private static boolean isListed(Module module) {
        if (module == null || !module.isEnabled() || ArrayListComp.isExcluded(module)) {
            return false;
        }
        ArrayListModule arrayListModule = ModuleManager.get().get(ArrayListModule.class);
        return arrayListModule == null || arrayListModule.categoryShown(module.getCategory());
    }

    private static int mixWhite(int n, float f) {
        int n2 = n >>> 24 & 0xFF;
        int n3 = n >> 16 & 0xFF;
        int n4 = n >> 8 & 0xFF;
        int n5 = n & 0xFF;
        n3 += Math.round((float)(255 - n3) * f);
        n4 += Math.round((float)(255 - n4) * f);
        n5 += Math.round((float)(255 - n5) * f);
        return n2 << 24 | n3 << 16 | n4 << 8 | n5;
    }
}

