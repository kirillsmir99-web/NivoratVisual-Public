package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.math.MathUtils;
import rtx.nv.utils.render.pipeline.ClientPipelines;
import rtx.nv.utils.render.render2d.ClientPalette;

public class Trails extends Module {
    private static final Identifier GLOW_TEXTURE = Trails.id("textures/particle/glow.png");
    private static final String MODE_RIBBON = "Лента";
    private static final String MODE_LINE = "Линия";
    private static final String MODE_LIGHT_NODES = "Световые узлы";

    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private static final double MAX_TELEPORT_DIST_SQ = 64.0; // 8 blocks squared

    private final SeparatorSetting generalSeparator = this.register(new SeparatorSetting("Основное"));
    private final ModeSetting mode = this.register(new ModeSetting("Режим", "Режим отрисовки следа.", MODE_RIBBON, MODE_RIBBON, MODE_LINE, MODE_LIGHT_NODES));
    private final SliderSetting length = this.register(new SliderSetting("Длина", "Максимальная длина хвоста в точках.").range(10.0f, 100.0f).increment(5.0f).setValue(40.0f));
    private final SliderSetting width = this.register(new SliderSetting("Ширина", "Ширина полосы или размер узлов.").range(0.05f, 1.5f).increment(0.05f).setValue(0.45f));
    private final SliderSetting lifetime = this.register(new SliderSetting("Время жизни", "Длительность угасания следа в секундах.").range(0.2f, 4.0f).increment(0.1f).setValue(1.2f));
    private final SliderSetting density = this.register(new SliderSetting("Плотность", "Частота фиксации точек при движении.").range(1.0f, 10.0f).increment(1.0f).setValue(6.0f));
    private final SliderSetting alpha = this.register(new SliderSetting("Прозрачность", "Интенсивность видимости следа.").range(0.1f, 1.0f).increment(0.05f).setValue(0.85f));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Выбор палитры оформления.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать градиент со вторым оттенком.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет следа.", new Color(255, 255, 255, 255)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй оттенок для градиента.", new Color(130, 80, 255, 255)));

    private final List<TailPoint> points = new ArrayList<>();
    private Vec3d lastAdded;

    public Trails() {
        super("Trails", "Оставляет плавный визуальный след за вами при перемещении.", Category.VISUALS);
        this.useSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    private static Identifier id(String path) {
        return Identifier.of("nv", path);
    }

    @Override
    protected void onDisable() {
        this.points.clear();
        this.lastAdded = null;
    }

    private void addPoint(long now) {
        if (this.mc.player == null) return;
        Vec3d currentPos = MathUtils.interpolate((Entity) this.mc.player).add(0.0, (double) this.mc.player.getHeight() * 0.5, 0.0);
        if (!Double.isFinite(currentPos.x) || !Double.isFinite(currentPos.y) || !Double.isFinite(currentPos.z)) {
            return;
        }

        if (this.lastAdded != null) {
            double distSq = this.lastAdded.squaredDistanceTo(currentPos);
            // Teleport detection: distance jumped more than 8 blocks in a frame
            if (distSq > MAX_TELEPORT_DIST_SQ) {
                this.points.clear();
                this.lastAdded = currentPos;
                this.points.add(new TailPoint(currentPos, now));
                return;
            }

            // Min distance based on density setting
            double minSpacing = Math.max(0.02, 0.3 / Math.max(1.0, this.density.getValue()));
            if (distSq < minSpacing * minSpacing) {
                return;
            }
        }

        this.points.add(new TailPoint(currentPos, now));
        this.lastAdded = currentPos;

        int maxPts = (int) this.length.getValue();
        if (this.points.size() > maxPts) {
            this.points.subList(0, this.points.size() - maxPts).clear();
        }
    }

    private int resolveColor(int index, float factorAlpha) {
        float globalAlpha = this.alpha.getFloat() * factorAlpha;
        if (globalAlpha <= 0.001f) return 0;

        int primary;
        int secondary;

        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] palette = ClientPalette.colors();
            if (palette != null && palette.length >= 2) {
                int col = paletteFade(8, index * 6, palette);
                return ColorUtil.multAlpha(col, globalAlpha);
            }
            InterfaceModule iface = InterfaceModule.getInstance();
            if (iface != null) {
                primary = iface.clientPrimaryColorOpaque();
                secondary = iface.usesSecondClientColor() ? iface.clientSecondaryColorOpaque() : primary;
            } else {
                primary = ColorUtil.rgba(127, 242, 255, 255);
                secondary = ColorUtil.rgba(255, 50, 150, 255);
            }
        } else {
            primary = this.customColor.getColor();
            secondary = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : primary;
        }

        if (primary == secondary) {
            return ColorUtil.multAlpha(primary, globalAlpha);
        }

        int col = fade(8, index * 6, primary, secondary);
        return ColorUtil.multAlpha(col, globalAlpha);
    }

    private static int fade(int speed, int offset, int col1, int col2) {
        int step = (int) ((System.currentTimeMillis() / (long) Math.max(1, speed) + (long) offset) % 360L);
        step = step >= 180 ? 360 - step : step;
        return ColorUtil.lerpColor(col1, col2, (float) step / 180.0f);
    }

    private static int paletteFade(int speed, int offset, int[] palette) {
        int len = palette.length;
        int step = (int) ((System.currentTimeMillis() / (long) Math.max(1, speed) + (long) offset) % 360L);
        float frac = (float) step / 360.0f * (float) len;
        int idx1 = (int) frac % len;
        int idx2 = (idx1 + 1) % len;
        int c1 = palette[idx1] | 0xFF000000;
        int c2 = palette[idx2] | 0xFF000000;
        return ColorUtil.lerpColor(c1, c2, frac - (float) Math.floor(frac)) | 0xFF000000;
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (this.mc.player == null || this.mc.world == null) {
            this.points.clear();
            this.lastAdded = null;
            return;
        }

        long now = System.currentTimeMillis();
        long maxLifetimeMs = Math.max(100L, (long) (this.lifetime.getValue() * 1000.0f));

        this.addPoint(now);

        // Remove expired points
        for (int i = this.points.size() - 1; i >= 0; --i) {
            if (now - this.points.get(i).spawnMs > maxLifetimeMs) {
                this.points.remove(i);
            }
        }

        if (this.points.size() < 2) {
            return;
        }

        Vec3d camPos = event.getCamera() != null ? event.getCamera().getCameraPos() : this.mc.gameRenderer.getCamera().getCameraPos();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        RenderLayer renderLayer = ClientPipelines.WORLD_PARTICLES_GLOW;
        VertexConsumer consumer = immediate.getBuffer(renderLayer);
        MatrixStack.Entry entry = event.getStack().peek();

        String currentMode = this.mode.getValue();
        boolean rendered = false;

        float baseWidth = this.width.getFloat();

        if (MODE_RIBBON.equals(currentMode)) {
            // Render smooth tapered ribbon
            for (int i = 0; i < this.points.size() - 1; i++) {
                TailPoint p1 = this.points.get(i);
                TailPoint p2 = this.points.get(i + 1);

                long age1 = now - p1.spawnMs;
                long age2 = now - p2.spawnMs;
                if (age1 > maxLifetimeMs || age2 > maxLifetimeMs) continue;

                float prog1 = MathHelper.clamp((float) age1 / (float) maxLifetimeMs, 0.0f, 1.0f);
                float prog2 = MathHelper.clamp((float) age2 / (float) maxLifetimeMs, 0.0f, 1.0f);

                float alpha1 = (1.0f - prog1) * Math.min((float) age1 / 60.0f, 1.0f);
                float alpha2 = (1.0f - prog2) * Math.min((float) age2 / 60.0f, 1.0f);

                int col1 = this.resolveColor(i, alpha1);
                int col2 = this.resolveColor(i + 1, alpha2);

                float h1 = baseWidth * (0.25f + 0.75f * (1.0f - prog1)) * 0.5f;
                float h2 = baseWidth * (0.25f + 0.75f * (1.0f - prog2)) * 0.5f;

                float x1 = (float) (p1.pos.x - camPos.x);
                float y1 = (float) (p1.pos.y - camPos.y);
                float z1 = (float) (p1.pos.z - camPos.z);

                float x2 = (float) (p2.pos.x - camPos.x);
                float y2 = (float) (p2.pos.y - camPos.y);
                float z2 = (float) (p2.pos.z - camPos.z);

                // Front side
                consumer.vertex(entry, x1, y1 - h1, z1).color(col1);
                consumer.vertex(entry, x1, y1 + h1, z1).color(col1);
                consumer.vertex(entry, x2, y2 + h2, z2).color(col2);
                consumer.vertex(entry, x2, y2 - h2, z2).color(col2);

                // Back side
                consumer.vertex(entry, x2, y2 - h2, z2).color(col2);
                consumer.vertex(entry, x2, y2 + h2, z2).color(col2);
                consumer.vertex(entry, x1, y1 + h1, z1).color(col1);
                consumer.vertex(entry, x1, y1 - h1, z1).color(col1);

                rendered = true;
            }
        } else if (MODE_LINE.equals(currentMode)) {
            // Camera-oriented smooth 3D line strip
            for (int i = 0; i < this.points.size() - 1; i++) {
                TailPoint p1 = this.points.get(i);
                TailPoint p2 = this.points.get(i + 1);

                long age1 = now - p1.spawnMs;
                long age2 = now - p2.spawnMs;
                if (age1 > maxLifetimeMs || age2 > maxLifetimeMs) continue;

                float prog1 = MathHelper.clamp((float) age1 / (float) maxLifetimeMs, 0.0f, 1.0f);
                float prog2 = MathHelper.clamp((float) age2 / (float) maxLifetimeMs, 0.0f, 1.0f);

                float alpha1 = (1.0f - prog1) * Math.min((float) age1 / 60.0f, 1.0f);
                float alpha2 = (1.0f - prog2) * Math.min((float) age2 / 60.0f, 1.0f);

                int col1 = this.resolveColor(i, alpha1);
                int col2 = this.resolveColor(i + 1, alpha2);

                float w1 = baseWidth * 0.4f * (0.3f + 0.7f * (1.0f - prog1));
                float w2 = baseWidth * 0.4f * (0.3f + 0.7f * (1.0f - prog2));

                float x1 = (float) (p1.pos.x - camPos.x);
                float y1 = (float) (p1.pos.y - camPos.y);
                float z1 = (float) (p1.pos.z - camPos.z);

                float x2 = (float) (p2.pos.x - camPos.x);
                float y2 = (float) (p2.pos.y - camPos.y);
                float z2 = (float) (p2.pos.z - camPos.z);

                // Segment direction
                float dx = x2 - x1;
                float dy = y2 - y1;
                float dz = z2 - z1;

                // View vector from midpoint to camera
                float midX = (x1 + x2) * 0.5f;
                float midY = (y1 + y2) * 0.5f;
                float midZ = (z1 + z2) * 0.5f;

                // Perpendicular vector = dir x view
                float px = dy * (-midZ) - dz * (-midY);
                float py = dz * (-midX) - dx * (-midZ);
                float pz = dx * (-midY) - dy * (-midX);
                float pLen = (float) Math.sqrt(px * px + py * py + pz * pz);
                if (pLen > 1.0E-4f) {
                    px /= pLen;
                    py /= pLen;
                    pz /= pLen;
                } else {
                    px = 0.0f;
                    py = 1.0f;
                    pz = 0.0f;
                }

                consumer.vertex(entry, x1 - px * w1, y1 - py * w1, z1 - pz * w1).color(col1);
                consumer.vertex(entry, x1 + px * w1, y1 + py * w1, z1 + pz * w1).color(col1);
                consumer.vertex(entry, x2 + px * w2, y2 + py * w2, z2 + pz * w2).color(col2);
                consumer.vertex(entry, x2 - px * w2, y2 - py * w2, z2 - pz * w2).color(col2);

                consumer.vertex(entry, x2 - px * w2, y2 - py * w2, z2 - pz * w2).color(col2);
                consumer.vertex(entry, x2 + px * w2, y2 + py * w2, z2 + pz * w2).color(col2);
                consumer.vertex(entry, x1 + px * w1, y1 + py * w1, z1 + pz * w1).color(col1);
                consumer.vertex(entry, x1 - px * w1, y1 - py * w1, z1 - pz * w1).color(col1);

                rendered = true;
            }
        } else if (MODE_LIGHT_NODES.equals(currentMode)) {
            // Light nodes billboards with connecting beam
            Quaternionf camRot = this.mc.gameRenderer.getCamera().getRotation();
            Vector3f right = camRot.transform(new Vector3f(1.0f, 0.0f, 0.0f));
            Vector3f up = camRot.transform(new Vector3f(0.0f, 1.0f, 0.0f));

            // Connecting thin beam
            float beamWidth = baseWidth * 0.12f;
            for (int i = 0; i < this.points.size() - 1; i++) {
                TailPoint p1 = this.points.get(i);
                TailPoint p2 = this.points.get(i + 1);

                long age1 = now - p1.spawnMs;
                long age2 = now - p2.spawnMs;
                if (age1 > maxLifetimeMs || age2 > maxLifetimeMs) continue;

                float prog1 = MathHelper.clamp((float) age1 / (float) maxLifetimeMs, 0.0f, 1.0f);
                float prog2 = MathHelper.clamp((float) age2 / (float) maxLifetimeMs, 0.0f, 1.0f);
                float alpha1 = (1.0f - prog1) * 0.45f;
                float alpha2 = (1.0f - prog2) * 0.45f;

                int col1 = this.resolveColor(i, alpha1);
                int col2 = this.resolveColor(i + 1, alpha2);

                float x1 = (float) (p1.pos.x - camPos.x);
                float y1 = (float) (p1.pos.y - camPos.y);
                float z1 = (float) (p1.pos.z - camPos.z);

                float x2 = (float) (p2.pos.x - camPos.x);
                float y2 = (float) (p2.pos.y - camPos.y);
                float z2 = (float) (p2.pos.z - camPos.z);

                consumer.vertex(entry, x1, y1 - beamWidth, z1).color(col1);
                consumer.vertex(entry, x1, y1 + beamWidth, z1).color(col1);
                consumer.vertex(entry, x2, y2 + beamWidth, z2).color(col2);
                consumer.vertex(entry, x2, y2 - beamWidth, z2).color(col2);

                consumer.vertex(entry, x2, y2 - beamWidth, z2).color(col2);
                consumer.vertex(entry, x2, y2 + beamWidth, z2).color(col2);
                consumer.vertex(entry, x1, y1 + beamWidth, z1).color(col1);
                consumer.vertex(entry, x1, y1 - beamWidth, z1).color(col1);
                rendered = true;
            }

            // Light nodes billboards
            for (int i = 0; i < this.points.size(); i++) {
                TailPoint pt = this.points.get(i);
                long age = now - pt.spawnMs;
                if (age > maxLifetimeMs) continue;

                float prog = MathHelper.clamp((float) age / (float) maxLifetimeMs, 0.0f, 1.0f);
                float nodeAlpha = (1.0f - prog) * Math.min((float) age / 40.0f, 1.0f);
                if (nodeAlpha <= 0.01f) continue;

                int col = this.resolveColor(i, nodeAlpha);
                float nodeSize = baseWidth * (0.35f + 0.65f * (1.0f - prog)) * 0.5f;

                float cx = (float) (pt.pos.x - camPos.x);
                float cy = (float) (pt.pos.y - camPos.y);
                float cz = (float) (pt.pos.z - camPos.z);

                float rx = right.x() * nodeSize;
                float ry = right.y() * nodeSize;
                float rz = right.z() * nodeSize;

                float ux = up.x() * nodeSize;
                float uy = up.y() * nodeSize;
                float uz = up.z() * nodeSize;

                // Core glow quad
                consumer.vertex(entry, cx - rx - ux, cy - ry - uy, cz - rz - uz).color(col);
                consumer.vertex(entry, cx - rx + ux, cy - ry + uy, cz - rz + uz).color(col);
                consumer.vertex(entry, cx + rx + ux, cy + ry + uy, cz + rz + uz).color(col);
                consumer.vertex(entry, cx + rx - ux, cy + ry - uy, cz + rz - uz).color(col);

                // Soft halo quad
                int haloCol = ColorUtil.multAlpha(col, 0.35f);
                float haloSize = nodeSize * 1.8f;
                float hrx = right.x() * haloSize;
                float hry = right.y() * haloSize;
                float hrz = right.z() * haloSize;
                float hux = up.x() * haloSize;
                float huy = up.y() * haloSize;
                float huz = up.z() * haloSize;

                consumer.vertex(entry, cx - hrx - hux, cy - hry - huy, cz - hrz - huz).color(haloCol);
                consumer.vertex(entry, cx - hrx + hux, cy - hry + huy, cz - hrz + huz).color(haloCol);
                consumer.vertex(entry, cx + hrx + hux, cy + hry + huy, cz + hrz + huz).color(haloCol);
                consumer.vertex(entry, cx + hrx - hux, cy + hry - huy, cz + hrz - huz).color(haloCol);

                rendered = true;
            }
        }

        if (rendered) {
            immediate.draw(renderLayer);
        }
    }

    public static record TailPoint(Vec3d pos, long spawnMs) {}
}
