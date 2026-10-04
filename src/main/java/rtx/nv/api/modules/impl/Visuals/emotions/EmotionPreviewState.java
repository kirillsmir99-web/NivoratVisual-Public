package rtx.nv.api.modules.impl.Visuals.emotions;

import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import org.joml.Matrix3x2f;

public final class EmotionPreviewState implements SpecialGuiElementRenderState {
    public final List<Emotion> emotions;
    public final float time;
    public final float ringRadius;
    public final float modelScale;
    public final float modelDrop;
    public final float innerRadius;
    public final float outerRadius;
    public final float gapRadians;
    public final float corner;
    public final int hoverIndex;
    public final float hoverGrow;
    public final float hoverShrink;
    public final float hoverZoom;
    public final float alpha;
    public final float designX;
    public final float designY;
    public final float designSize;
    public final float cellWidth;
    public final float cellHeight;
    public final float cellWidthDesign;
    public final float cellHeightDesign;
    public final int columns;
    public final int rows;
    private final Matrix3x2f pose;
    private final int x0;
    private final int y0;
    private final int x1;
    private final int y1;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    public EmotionPreviewState(
        List<Emotion> emotions, float time, float ringRadius, float modelScale, float modelDrop,
        float innerRadius, float outerRadius, float gapRadians, float corner, int hoverIndex,
        float hoverGrow, float hoverShrink, float hoverZoom, float alpha, float designX,
        float designY, float designSize, float cellWidth, float cellHeight, float cellWidthDesign,
        float cellHeightDesign, int columns, int rows, Matrix3x2f pose, int x0, int y0,
        int x1, int y1, ScreenRect scissorArea
    ) {
        this.emotions = emotions;
        this.time = time;
        this.ringRadius = ringRadius;
        this.modelScale = modelScale;
        this.modelDrop = modelDrop;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.gapRadians = gapRadians;
        this.corner = corner;
        this.hoverIndex = hoverIndex;
        this.hoverGrow = hoverGrow;
        this.hoverShrink = hoverShrink;
        this.hoverZoom = hoverZoom;
        this.alpha = alpha;
        this.designX = designX;
        this.designY = designY;
        this.designSize = designSize;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.cellWidthDesign = cellWidthDesign;
        this.cellHeightDesign = cellHeightDesign;
        this.columns = columns;
        this.rows = rows;
        this.pose = pose != null ? pose : new Matrix3x2f();
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.scissorArea = scissorArea;
        this.bounds = SpecialGuiElementRenderState.createBounds(x0, y0, x1, y1, scissorArea);
    }

    @Override
    public int x1() {
        return this.x0;
    }

    @Override
    public int x2() {
        return this.x1;
    }

    @Override
    public int y1() {
        return this.y0;
    }

    @Override
    public int y2() {
        return this.y1;
    }

    @Override
    public float scale() {
        return 1.0f;
    }

    @Override
    public Matrix3x2f pose() {
        return this.pose;
    }

    @Override
    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    @Override
    public ScreenRect bounds() {
        return this.bounds;
    }
}
