package rtx.nv.api.drags.components;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.renderitem.RenderItem;

public abstract class ListHudComp
extends Draggable {
    private static final String TITLE_FONT = "montserrat-semibold";
    private static final String ROW_FONT = "montserrat-medium";
    private static final float HEADER_HEIGHT = 18.0f;
    private static final float CONTENT_TOP = 4.0f;
    private static final float PAD_X = 6.5f;
    private static final float PAD_BOTTOM = 3.5f;
    private static final String ROW_CENTER_REFERENCE = "[H]";
    private static final float ROW_HEIGHT = 9.5f;
    private static final float TITLE_SIZE = 7.2f;
    private static final float ROW_SIZE = 6.0f;
    private static final float VALUE_GAP = 6.0f;
    private static final float HEADER_EXTRA_WIDTH = 7.0f;
    private static final float NAME_SLIDE = 7.0f;
    private static final float VALUE_SLIDE = 7.0f;
    private static final float ICON_SIZE = 8.5f;
    private static final float ICON_GAP = 4.0f;
    private static final String HEADER_ICON_FONT = "nv";
    private static final float HEADER_ICON_SIZE = 7.0f;
    private static final float HEADER_ICON_GAP = 6.0f;
    private static final float HEADER_ICON_SLIDE = 5.0f;
    private static final double HEADER_ICON_IN_SECONDS = 0.25;
    private static final double HEADER_ICON_OUT_SECONDS = 0.2;
    private static final float RADIUS = 5.5f;
    private static final float DIVIDER_HEIGHT = 0.5f;
    private static final float ROW_CLIP_PAD_Y = 2.0f;
    private static final double ROW_IN_SECONDS = 0.22;
    private static final double ROW_OUT_SECONDS = 0.18;
    private static final double WIDTH_SECONDS = 0.24;
    private static final double DIVIDER_IN_SECONDS = 0.24;
    private static final double DIVIDER_OUT_SECONDS = 0.14;
    private static final float MIN_TEXT_ALPHA = 0.003921569f;
    private static final float MIN_DIVIDER_ALPHA = 0.003921569f;
    private static final long FADE_OUT_DELAY_MS = 100L;
    private static final Easing ROW_EASING = d -> d * d * (3.0 - 2.0 * d);
    private static final int ROW_COLOR = -3355444;
    private static final int VALUE_COLOR = -3355444;
    private static final int DIVIDER_COLOR = -1997220937;
    private final String title;
    private final Class<? extends Module> moduleType;
    private final Map<Object, ListHudComp.RowState> rows = new LinkedHashMap<Object, ListHudComp.RowState>();
    private final SmoothAnimation visibility = new SmoothAnimation();
    private final SmoothAnimation widthAnimation = new SmoothAnimation();
    private final SmoothAnimation dividerAlpha = new SmoothAnimation();
    private final SmoothAnimation headerIconAnim = new SmoothAnimation();
    private boolean lastTargetVisible;
    private boolean lastDividerVisible;
    private boolean lastHeaderIconVisible;
    private boolean everHadContent;
    private long sizeCollapsedAtMs;
    private float titleWidthCache = -1.0f;
    private int activeCount;
    private float rowsProgress;
    private float currentWidth = 60.0f;
    private float currentHeight = 22.0f;

    protected ListHudComp(String string, String string2, Class<? extends Module> clazz, float f, float f2) {
        super(string, f, f2);
        this.title = string2;
        this.moduleType = clazz;
        this.visibility.set(0.0);
        this.widthAnimation.set(this.currentWidth);
        this.dividerAlpha.set(0.0);
        this.headerIconAnim.set(0.0);
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

    protected String headerIconGlyph() {
        return null;
    }

    protected abstract List<ListHudComp.Row> collectRows();

    protected float iconSlotSize() {
        return 8.5f;
    }

    protected boolean hasHeader() {
        return true;
    }

    protected float minWidthWithoutHeader() {
        return 50.0f;
    }

    private void updateDividerAlpha() {
        if (!this.hasHeader()) {
            return;
        }
        boolean bl;
        boolean bl2 = bl = this.activeCount > 0;
        if (bl != this.lastDividerVisible) {
            this.dividerAlpha.run(bl ? 1.0 : 0.0, bl ? 0.24 : 0.14, ROW_EASING, false);
            this.lastDividerVisible = bl;
        }
        this.dividerAlpha.update();
    }

    private void refreshMetrics() {
        boolean header = this.hasHeader();
        float f = header ? this.headerMinWidth() : this.minWidthWithoutHeader();
        float f2 = header ? HEADER_HEIGHT : 4.0f;
        float f3 = header ? this.dividerProgress() : 0.0f;
        if (f3 > 0.001f) {
            f2 += 5.5f * f3;
        }
        for (ListHudComp.RowState rowState : this.rows.values()) {
            float f4 = ListHudComp.rowProgress(rowState);
            if (f4 <= 0.001f && !rowState.active) continue;
            float f5 = rowState.icon != null ? this.iconSlotSize() + 4.0f : 0.0f;
            float f6 = Render2D.msdfWidth(ROW_FONT, rowState.name, 6.5f);
            float f7 = Render2D.msdfWidth(ROW_FONT, rowState.value, 6.5f);
            f = Math.max(f, 18.0f + f5 + f6 + 10.0f + f7);
            f2 += 11.5f * f4;
        }
        if (header && f3 > 0.001f) {
            f2 += 5.0f * f3;
        }
        if (!header) {
            f2 += 4.0f;
        }
        float f8 = Math.max(f, header ? this.headerMinWidth() : this.minWidthWithoutHeader());
        if (Math.abs(this.widthAnimation.getToValue() - (double)f8) > 0.25) {
            this.widthAnimation.run(f8, 0.20, ROW_EASING, false);
        }
        if (!header && this.activeCount == 0 && this.rowsProgress <= 0.005f) {
            this.currentHeight = 0.0f;
        } else {
            this.currentHeight = Math.max(header ? HEADER_HEIGHT : 16.0f, f2);
        }
    }

    private static boolean pushRoundedContentClip(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        float f8 = Math.max(f2, f6);
        float f9 = Math.min(f2 + f4, f6 + f7);
        if (f3 <= 0.5f || f4 <= 0.5f || f9 <= f8) {
            return false;
        }
        float f10 = ListHudComp.roundedHorizontalInset(f2, f4, f5, f8, f9);
        float f11 = f + f10;
        float f12 = f3 - f10 * 2.0f;
        if (f12 <= 0.5f) {
            return false;
        }
        Render2D.pushScissor(drawContext, f11, f8, f12, f9 - f8);
        return true;
    }

    private static float rowProgress(ListHudComp.RowState rowState) {
        return ListHudComp.clamp(rowState.animation.get(), 0.0f, 1.0f);
    }

    private static float delayedProgress(float f, float f2) {
        if (f <= f2) {
            return 0.0f;
        }
        return ListHudComp.clamp((f - f2) / (1.0f - f2), 0.0f, 1.0f);
    }

    private float headerIconWidth() {
        String string = this.headerIconGlyph();
        if (string == null) {
            return 0.0f;
        }
        float[] fArray = ListHudComp.textBounds(HEADER_ICON_FONT, string, 7.0f);
        return fArray[2] - fArray[0];
    }

    private void renderDivider(float f, float f2, float f3) {
        if (!this.hasHeader()) {
            return;
        }
        float f4 = ListHudComp.smoothstep(this.dividerProgress());
        float f5 = Math.max(0.0f, this.currentWidth - 18.0f);
        float f6 = f5 * f4;
        if (f4 <= 0.003921569f || f6 <= 0.25f) {
            return;
        }
        Render2D.rect(f + (this.currentWidth - f6) * 0.5f, f2 + HEADER_HEIGHT, f6, 0.5f, 0.0f, ColorUtil.multAlpha(-1997220937, f3 * f4));
    }

    private static float cornerInset(float f, float f2) {
        float f3 = ListHudComp.clamp(f2, 0.0f, f);
        return f - (float)Math.sqrt(Math.max(0.0f, f * f - f3 * f3));
    }

    private float headerMinWidth() {
        String hTitle = this.displayTitle();
        float f = this.titleWidth() + 10.0f;
        float f2 = this.headerIconProgress();
        if (f2 <= 0.001f) {
            return f;
        }
        float[] fArray = ListHudComp.textBounds(TITLE_FONT, hTitle, 8.2f);
        float f3 = 18.0f + (fArray[2] - fArray[0]) + 6.0f + this.headerIconWidth();
        return f + Math.max(0.0f, f3 - f) * f2;
    }

    private float dividerProgress() {
        return ListHudComp.clamp(this.dividerAlpha.get(), 0.0f, 1.0f);
    }

    private static boolean hudIconsEnabled() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        return interfaceModule != null && interfaceModule.hudIcons.getValue();
    }

    private boolean computeTargetVisible() {
        if (!this.shouldShow()) {
            return false;
        }
        if (this.activeCount > 0 || DragSystem.get().isDragModeActive()) {
            this.everHadContent = true;
            this.sizeCollapsedAtMs = 0L;
            return true;
        }
        if (!this.everHadContent) {
            return false;
        }
        if (this.rowsProgress > 0.005f) {
            this.sizeCollapsedAtMs = 0L;
            return true;
        }
        return false;
    }

    private void updateHeaderIcon() {
        if (!this.hasHeader()) {
            return;
        }
        boolean bl;
        boolean bl2 = bl = this.headerIconGlyph() != null && ListHudComp.hudIconsEnabled();
        if (bl != this.lastHeaderIconVisible) {
            this.headerIconAnim.run(bl ? 1.0 : 0.0, bl ? 0.25 : 0.2, ROW_EASING, false);
            this.lastHeaderIconVisible = bl;
        }
        this.headerIconAnim.update();
    }

    private float headerIconProgress() {
        return ListHudComp.clamp(this.headerIconAnim.get(), 0.0f, 1.0f);
    }

    private static float roundedHorizontalInset(float f, float f2, float f3, float f4, float f5) {
        float f6 = Math.max(0.0f, Math.min(f3, f2 * 0.5f));
        if (f6 <= 0.0f) {
            return 0.0f;
        }
        float f7 = f + f2;
        float f8 = 0.0f;
        if (f4 < f + f6) {
            f8 = Math.max(f8, ListHudComp.cornerInset(f6, f + f6 - Math.max(f, f4)));
        }
        if (f5 > f7 - f6) {
            f8 = Math.max(f8, ListHudComp.cornerInset(f6, Math.min(f7, f5) - (f7 - f6)));
        }
        return f8;
    }

    private static float clientRectRadius(float f, float f2) {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f3 = interfaceModule == null ? 7.0f : interfaceModule.rectCornerRadius.getFloat();
        return Math.max(0.0f, Math.min(f3, Math.min(f, f2) * 0.5f));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void render(DrawContext drawContext) {
        this.syncRows();
        this.updateDividerAlpha();
        this.updateHeaderIcon();
        this.refreshMetrics();
        this.widthAnimation.update();
        boolean bl = this.computeTargetVisible();
        if (bl != this.lastTargetVisible) {
            this.visibility.run(bl ? 1.0 : 0.0, bl ? 0.18 : 0.14, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = bl;
        }
        this.visibility.update();
        float f = this.visibility.get();
        if (bl && f <= 0.01f) {
            f = 0.01f;
        }
        if (f <= 0.01f && !bl) {
            return;
        }
        this.currentWidth = Math.max(this.hasHeader() ? this.headerMinWidth() : this.minWidthWithoutHeader(), this.widthAnimation.get());
        float f2 = this.getX();
        float f3 = this.getY();
        float f4 = 0.90f + f * 0.10f;
        float f5 = f2 + this.currentWidth * 0.5f;
        float f6 = f3 + this.currentHeight * 0.5f;
        float offsetY = (1.0f - f) * 6.0f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f5, f6 + offsetY);
        drawContext.getMatrices().scale(f4);
        drawContext.getMatrices().translate(-f5, -(f6 + offsetY));
        RenderItem.beginFrame(drawContext);
        Render2D.beginFrame(drawContext);
        RectUtil.drawClientRect(f2, f3, this.currentWidth, this.currentHeight, 7.0f, f);
        float f7 = ListHudComp.clientRectRadius(this.currentWidth, this.currentHeight);
        if (this.hasHeader()) {
            float f8 = 11.5f;
            float f9 = f3 + (HEADER_HEIGHT - f8) * 0.5f;
            if (ListHudComp.pushRoundedContentClip(drawContext, f2, f3, this.currentWidth, this.currentHeight, f7, f9, f8)) {
                try {
                    float f10 = this.headerIconProgress();
                    String hTitle = this.displayTitle();
                    float[] fArray = ListHudComp.textBounds(TITLE_FONT, hTitle, 8.2f);
                    float f11 = fArray[2] - fArray[0];
                    float f12 = f3 + HEADER_HEIGHT * 0.5f;
                    float f13 = f12 - (fArray[1] + fArray[3]) * 0.5f;
                    float f14 = f2 + (this.currentWidth - f11) * 0.5f - fArray[0];
                    float f15 = f2 + 9.0f - fArray[0];
                    float f16 = f14 + (f15 - f14) * f10;
                    ListHudComp.drawText(TITLE_FONT, hTitle, f16, f13, 8.2f, ListHudComp.titleColor(), f);
                    String string = this.headerIconGlyph();
                    if (string != null && f10 > 0.003921569f) {
                        float[] fArray2 = ListHudComp.textBounds(HEADER_ICON_FONT, string, 7.0f);
                        float f17 = f2 + this.currentWidth - 9.0f - fArray2[2] + 5.0f * (1.0f - f10);
                        float f18 = f12 - (fArray2[1] + fArray2[3]) * 0.5f;
                        Render2D.msdfText(HEADER_ICON_FONT, string, f17, f18, 7.0f, rtx.nv.api.ui.theme.ClientAccent.icon(255f * f * f10));
                    }
                }
                finally {
                    Render2D.popScissor(drawContext);
                }
            }
            this.renderDivider(f2, f3, f);
        }
        this.drawRows(drawContext, f2, f3, f, f7);
        Render2D.flush();
        RenderItem.flush();
        drawContext.getMatrices().popMatrix();
    }

    private boolean shouldShow() {
        Module module = ModuleManager.get().get(this.moduleType);
        return module != null && module.isEnabled();
    }

    private void syncRows() {
        List<ListHudComp.Row> list = this.shouldShow() ? this.collectRows() : List.of();
        java.util.LinkedHashMap<Object, ListHudComp.RowState> orderedRows = new java.util.LinkedHashMap<>();
        this.activeCount = 0;
        for (ListHudComp.Row row : list) {
            if (row == null || row.id() == null) continue;
            ListHudComp.RowState rowState = this.rows.remove(row.id());
            if (rowState == null) {
                rowState = new ListHudComp.RowState();
            }
            rowState.active = true;
            rowState.name = row.name() == null ? "" : row.name();
            rowState.value = row.value() == null ? "" : row.value();
            rowState.icon = row.icon();
            rowState.pulse = row.pulse();
            rowState.color = row.color();
            orderedRows.put(row.id(), rowState);
            ++this.activeCount;
        }
        for (Map.Entry<Object, ListHudComp.RowState> entry : this.rows.entrySet()) {
            ListHudComp.RowState rowState = entry.getValue();
            rowState.active = false;
            orderedRows.put(entry.getKey(), rowState);
        }
        this.rows.clear();
        this.rows.putAll(orderedRows);

        this.rowsProgress = 0.0f;
        Iterator<Map.Entry<Object, ListHudComp.RowState>> iterator = this.rows.entrySet().iterator();
        while (iterator.hasNext()) {
            ListHudComp.RowState rowState = iterator.next().getValue();
            if (rowState.active != rowState.lastActive) {
                rowState.animation.run(rowState.active ? 1.0 : 0.0, rowState.active ? 0.22 : 0.18, ROW_EASING, false);
                rowState.lastActive = rowState.active;
            }
            rowState.animation.update();
            float f = ListHudComp.rowProgress(rowState);
            this.rowsProgress += f;
            if (rowState.active || !(f <= 0.001f)) continue;
            iterator.remove();
        }
        this.rowsProgress = ListHudComp.clamp(this.rowsProgress, 0.0f, 1.0f);
    }

    private static float[] textBounds(String string, String string2, float f) {
        float[] fArray = Render2D.msdfBounds(string, string2, f);
        if (fArray != null && fArray.length >= 4 && fArray[3] - fArray[1] > 0.0f) {
            return fArray;
        }
        return new float[]{0.0f, 0.0f, Render2D.msdfWidth(string, string2, f), f};
    }

    private static void drawText(String string, String string2, float f, float f2, float f3, int n, float f4) {
        float f5 = ListHudComp.clamp(f4, 0.0f, 1.0f);
        if (f5 <= 0.003921569f || string2 == null || string2.isEmpty()) {
            return;
        }
        int shadowCol = ColorUtil.rgba(0, 0, 0, Math.round(f5 * 102.0f));
        Render2D.msdfText(string, string2, f + 0.5f, f2 + 0.5f, f3, shadowCol);
        Render2D.msdfText(string, string2, f, f2, f3, ColorUtil.multAlpha(n, f5));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drawRows(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = this.hasHeader() ? this.dividerProgress() : 0.0f;
        float f6 = f2 + (this.hasHeader() ? (HEADER_HEIGHT + 5.5f * f5) : 4.0f);
        float[] fArray = ListHudComp.textBounds(ROW_FONT, ROW_CENTER_REFERENCE, 6.5f);
        float f7 = (fArray[1] + fArray[3]) * 0.5f;
        for (ListHudComp.RowState rowState : this.rows.values()) {
            float f8 = ListHudComp.rowProgress(rowState);
            if (f8 <= 0.001f) continue;
            float f9 = rowState.pulse != null ? ListHudComp.clamp(rowState.pulse.alpha(), 0.0f, 1.0f) : 1.0f;
            float f10 = f3 * f9;
            float f11 = f8;
            float f12 = ListHudComp.delayedProgress(f8, 0.10f);
            float f13 = rowState.icon != null ? this.iconSlotSize() + 4.0f : 0.0f;
            float f14 = Render2D.msdfWidth(ROW_FONT, rowState.value, 6.5f);
            float f15 = f + 9.0f + f13 - 5.0f * (1.0f - f11);
            float f16 = f + this.currentWidth - 9.0f - f14 + 5.0f * (1.0f - f12);
            float f17 = 11.5f * f8;
            float f18 = f6 + f17 * 0.5f;
            float f19 = 15.5f * f8;
            float f20 = f18 - f19 * 0.5f;
            float f21 = f18 - f7;
            if (ListHudComp.pushRoundedContentClip(drawContext, f, f2, this.currentWidth, this.currentHeight, f4, f20, f19)) {
                try {
                    if (rowState.icon != null) {
                        float f22 = f18 - this.iconSlotSize() * 0.5f;
                        rowState.icon.draw(drawContext, f + 9.0f, f22, this.iconSlotSize(), f10 * f11);
                    }
                    int n = rowState.color != 0 ? rowState.color : -3355444;
                    int n2 = rowState.color != 0 ? rowState.color : -3355444;
                    ListHudComp.drawText(ROW_FONT, rowState.name, f15, f21, 6.5f, n, f10 * f11);
                    ListHudComp.drawText(ROW_FONT, rowState.value, f16, f21, 6.5f, n2, f10 * f12);
                }
                finally {
                    Render2D.popScissor(drawContext);
                }
            }
            f6 += f17;
        }
    }

    private static int titleColor() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        int n = interfaceModule == null ? -1 : interfaceModule.clientPrimaryColorOpaque();
        return ColorUtil.lerpColor(-1, n, 0.1f);
    }

    public String displayTitle() {
        return rtx.nv.api.localization.Lang.get("hud." + this.getId() + ".title", this.title);
    }

    private float titleWidth() {
        return Render2D.msdfWidth(TITLE_FONT, this.displayTitle(), 8.2f);
    }

    private static float smoothstep(float f) {
        float f2 = ListHudComp.clamp(f, 0.0f, 1.0f);
        return f2 * f2 * (3.0f - 2.0f * f2);
    }

    @FunctionalInterface
    public interface IconDrawer {
        void draw(DrawContext drawContext, float x, float y, float size, float alpha);
    }

    @FunctionalInterface
    public interface AlphaPulse {
        float alpha();
    }


    public record Row(Object id, String name, String value, ListHudComp.IconDrawer icon, ListHudComp.AlphaPulse pulse, int color) {
        public Row(Object object, String string, String string2, ListHudComp.IconDrawer iconDrawer, ListHudComp.AlphaPulse alphaPulse) {
            this(object, string, string2, iconDrawer, alphaPulse, 0);
        }
        public Row(Object object, String string, String string2, ListHudComp.IconDrawer iconDrawer) {
            this(object, string, string2, iconDrawer, null, 0);
        }
    }

    public static final class RowState {
        final SmoothAnimation animation = new SmoothAnimation();
        boolean active;
        boolean lastActive;
        String name;
        String value;
        ListHudComp.IconDrawer icon;
        ListHudComp.AlphaPulse pulse;
        int color;
    }
}

