package rtx.nv.utils.render.render2d.sectormask;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.DrawContext;

public record BuiltSectorMask(
    float x, float y, float size, float innerRadius, float outerRadius,
    int sectorCount, float gapRadians, float corner, float feather, float alpha,
    int hoverIndex, float hoverGrow, float hoverShrink, float ringRadius,
    float cellWidth, float cellHeight, int columns, int rows, float hoverZoom,
    GpuTextureView textureView
) {
    public BuiltSectorMask(float x, float y, float size, float innerRadius, float outerRadius, int sectorCount, float gapRadians, float corner, float feather, int hoverIndex, float hoverGrow, float hoverShrink, float hoverZoom, float ringRadius, float cellWidth, float cellHeight, int columns, int rows, float alpha, GpuTextureView textureView) {
        this(x, y, size, innerRadius, outerRadius, sectorCount, gapRadians, corner, feather, alpha, hoverIndex, hoverGrow, hoverShrink, ringRadius, cellWidth, cellHeight, columns, rows, hoverZoom, textureView);
    }

    public BuiltSectorMask(float x, float y, float size, float innerRadius, float outerRadius, int sectorCount, float gapRadians, float corner, float feather, float alpha, int hoverIndex, float hoverGrow, float hoverShrink, float ringRadius, float cellWidth, float cellHeight, int columns, int rows, float hoverZoom) {
        this(x, y, size, innerRadius, outerRadius, sectorCount, gapRadians, corner, feather, alpha, hoverIndex, hoverGrow, hoverShrink, ringRadius, cellWidth, cellHeight, columns, rows, hoverZoom, null);
    }

    public BuiltSectorMask(float x, float y, float size, float innerRadius, float outerRadius, int sectorCount) {
        this(x, y, size, innerRadius, outerRadius, sectorCount, 0.05f, 4.0f, 0.5f, 1.0f, -1, 1.0f, 1.0f, outerRadius, size, size, 1, 1, 1.0f, null);
    }

    public boolean visible() {
        return this.size > 0.0f && this.alpha > 0.0f;
    }

    public void render(DrawContext drawContext) {
        SectorMaskRenderer.getInstance().submit(drawContext, this);
    }
}