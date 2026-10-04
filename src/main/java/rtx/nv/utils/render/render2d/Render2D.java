package rtx.nv.utils.render.render2d;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import rtx.nv.NV;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.arc.ArcRenderer;
import rtx.nv.utils.render.render2d.arc.BuiltArc;
import rtx.nv.utils.render.render2d.blur.BlurBuilder;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;
import rtx.nv.utils.render.render2d.circle.BuiltCircle;
import rtx.nv.utils.render.render2d.circle.CircleRenderer;
import rtx.nv.utils.render.render2d.effecticon.BuiltEffectIcon;
import rtx.nv.utils.render.render2d.effecticon.EffectIconRenderer;
import rtx.nv.utils.render.render2d.font.BuiltText;
import rtx.nv.utils.render.render2d.font.TextRenderer;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.utils.render.render2d.glass.GlassRenderer;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.render.render2d.glow.GlowRenderer;
import rtx.nv.utils.render.render2d.image.BuiltImage;
import rtx.nv.utils.render.render2d.image.ImageRenderer;
import rtx.nv.utils.render.render2d.line.BuiltLine;
import rtx.nv.utils.render.render2d.line.LineRenderer;
import rtx.nv.utils.render.render2d.msdf.BuiltMsdfText;
import rtx.nv.utils.render.render2d.msdf.MsdfTextRenderer;
import rtx.nv.utils.render.render2d.outline.outline360.BuiltOutline360;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360Range;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360Renderer;
import rtx.nv.utils.render.render2d.outline.outlinedefault.BuiltOutline;
import rtx.nv.utils.render.render2d.outline.outlinedefault.DefaultOutlineRenderer;
import rtx.nv.utils.render.render2d.outline.outlineglass.BuiltGlassOutline;
import rtx.nv.utils.render.render2d.outline.outlineglass.GlassOutlineRenderer;
import rtx.nv.utils.render.render2d.picker.BuiltPicker;
import rtx.nv.utils.render.render2d.picker.PickerRenderer;
import rtx.nv.utils.render.render2d.radialglass.BuiltRadialGlass;
import rtx.nv.utils.render.render2d.radialglass.RadialGlassRenderer;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.BuiltRectangle;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.DefaultRectangleRenderer;
import rtx.nv.utils.render.render2d.rectangle.recthalficon.BuiltHalfIconRectangle;
import rtx.nv.utils.render.render2d.rectangle.recthalficon.HalfIconRectangleRenderer;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.BuiltHalftoneRectangle;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.HalftoneRectangleRenderer;
import rtx.nv.utils.render.render2d.ripple.BuiltRipple;
import rtx.nv.utils.render.render2d.ripple.RippleRenderer;
import rtx.nv.utils.render.render2d.sectormask.SectorMaskRenderer;
import rtx.nv.utils.render.render2d.shape.BuiltShape;
import rtx.nv.utils.render.render2d.shape.ShapeRenderer;
import rtx.nv.utils.render.render2d.shimmer.BuiltShimmer;
import rtx.nv.utils.render.render2d.shimmer.ShimmerRenderer;
import rtx.nv.utils.render.render2d.zippy.BuiltZippy;
import rtx.nv.utils.render.render2d.zippy.ZippyRenderer;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class Render2D {
    private static boolean reloadListenerRegistered;
    private static boolean frameActive;

    private Render2D() {
    }

    public static void flush() {
        if (!frameActive) {
            return;
        }
        frameActive = false;
        Render2D.blur().flush();
        Render2D.glass().flush();
        Render2D.shape().flush();
        Render2D.glassOutline().flush();
        Render2D.circle().flush();
        Render2D.arc().flush();
        Render2D.radialGlass().flush();
        Render2D.picker().flush();
        Render2D.rectangle().flush();
        Render2D.halfIconRectangle().flush();
        Render2D.halftoneRectangle().flush();
        Render2D.zippy().flush();
        Render2D.outline().flush();
        Render2D.outline360().flush();
        Render2D.text().flush();
        Render2D.msdf().flush();
        Render2D.shimmer().flush();
        Render2D.image().flush();
        Render2D.line().flush();
        Render2D.effectIcon().flush();
        Render2D.ripple().flush();
        Render2D.glow().flush();
    }

    public static void line(float f, float f2, float f3, float f4, float f5, int n) {
        Render2D.line().enqueue(new BuiltLine(f, f2, f3, f4, f5, n));
    }

    public static void line(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7) {
        Render2D.line().enqueue(new BuiltLine(f, f2, f3, f4, f5, n, f6, f7));
    }

    private static LineRenderer line() {
        return LineRenderer.getInstance();
    }

    public static void close() {
        BlurFramebuffer.closeInstance();
        GlassRenderer.closeInstance();
        ShapeRenderer.closeInstance();
        GlassOutlineRenderer.closeInstance();
        CircleRenderer.closeInstance();
        ArcRenderer.closeInstance();
        RadialGlassRenderer.closeInstance();
        SectorMaskRenderer.closeInstance();
        PickerRenderer.closeInstance();
        DefaultRectangleRenderer.closeInstance();
        HalfIconRectangleRenderer.closeInstance();
        HalftoneRectangleRenderer.closeInstance();
        ZippyRenderer.closeInstance();
        DefaultOutlineRenderer.closeInstance();
        Outline360Renderer.closeInstance();
        TextRenderer.closeInstance();
        MsdfTextRenderer.closeInstance();
        ShimmerRenderer.closeInstance();
        ImageRenderer.closeInstance();
        LineRenderer.closeInstance();
        EffectIconRenderer.closeInstance();
        RippleRenderer.closeInstance();
        GlowRenderer.closeInstance();
        ClientPalette.closeBuffer();
        NV.LOGGER.info("[Render2D] Render2D closed");
    }

    public static void init() {
        Render2D.registerReloadListener();
        NV.LOGGER.info("[Render2D] Render2D initialized");
    }

    public static void text(String string, String string2, float f, float f2, float f3, int n, int n2, int n3, int n4, float f4, float f5, float f6) {
        Render2D.imageBarrier();
        Render2D.text().enqueue(new BuiltText(string, string2, f, f2, f3, n, n2, n3, n4, f4, f5, f6, false, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, true));
    }

    public static void text(String string, String string2, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        Render2D.imageBarrier();
        String string3 = Render2D.msdfFontAlias(string);
        if (string3 != null) {
            Render2D.msdf().enqueue(new BuiltMsdfText(string3, string2, f, f2, f3, Render2D.normalizeTextColor(n), Render2D.normalizeTextColor(n2), Render2D.normalizeTextColor(n3), Render2D.normalizeTextColor(n4)));
            return;
        }
        Render2D.text().enqueue(new BuiltText(string, string2, f, f2, f3, n, n2, n3, n4));
    }

    public static void text(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6) {
        Render2D.imageBarrier();
        Render2D.text().enqueue(new BuiltText(string, string2, f, f2, f3, n, n, n, n, f4, f5, f6, false, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, true));
    }

    public static void text(String string, String string2, float f, float f2, float f3, int n) {
        Render2D.imageBarrier();
        String string3 = Render2D.msdfFontAlias(string);
        if (string3 != null) {
            Render2D.msdf().enqueue(new BuiltMsdfText(string3, string2, f, f2, f3, Render2D.normalizeTextColor(n)));
            return;
        }
        Render2D.text().enqueue(new BuiltText(string, string2, f, f2, f3, n));
    }

    public static void text(DrawContext drawContext, String string, String string2, float f, float f2, float f3, int n) {
        Render2D.beginFrame(drawContext);
        Render2D.text(string, string2, f, f2, f3, n);
        Render2D.flush();
    }

    public static void text(BuiltText builtText) {
        String string;
        Render2D.imageBarrier();
        if (builtText != null && !builtText.hasHorizontalFade() && Math.abs(builtText.rotationDegrees()) < 0.001f && (string = Render2D.msdfFontAlias(builtText.fontName())) != null) {
            Render2D.msdf().enqueue(new BuiltMsdfText(string, builtText.text(), builtText.x(), builtText.y(), builtText.size(), Render2D.normalizeTextColor(builtText.colorTopLeft()), Render2D.normalizeTextColor(builtText.colorTopRight()), Render2D.normalizeTextColor(builtText.colorBottomRight()), Render2D.normalizeTextColor(builtText.colorBottomLeft())));
            return;
        }
        Render2D.text().enqueue(builtText);
    }

    private static TextRenderer text() {
        return TextRenderer.getInstance();
    }

    public static void image(String string, float f, float f2, float f3, float f4, float f5, int n) {
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4, f5, n));
    }

    public static void image(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7, int n) {
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4, f5, f6, f7, n, n, n, n));
    }

    public static void image(String string, float f, float f2, float f3, float f4, int ... nArray) {
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4).withColors(nArray));
    }

    public static void image(String string, float f, float f2, float f3, float f4) {
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4));
    }

    private static ImageRenderer image() {
        return ImageRenderer.getInstance();
    }

    private static GlowRenderer glow() {
        return GlowRenderer.getInstance();
    }

    public static void glow(BuiltGlow builtGlow) {
        if (builtGlow != null) {
            Render2D.glow().enqueue(builtGlow);
        }
    }

    public static void halftoneRect(BuiltHalftoneRectangle builtHalftoneRectangle) {
        Render2D.imageBarrier();
        Render2D.halftoneRectangle().enqueue(builtHalftoneRectangle);
    }

    public static void halftoneRect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, int n3, int n4, int n5, float f9, float f10) {
        Render2D.imageBarrier();
        Render2D.halftoneRectangle().enqueue(new BuiltHalftoneRectangle(f, f2, f3, f4, f5, f6, f7, f8, n, n2, n3, n4, 0.0f, n5, f9, f10));
    }

    public static void halftoneRect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, float f9, float f10) {
        Render2D.imageBarrier();
        Render2D.halftoneRectangle().enqueue(new BuiltHalftoneRectangle(f, f2, f3, f4, f5, f6, f7, f8, n, n2, f9, f10));
    }

    public static void halftoneRect(float f, float f2, float f3, float f4, float f5, int n, int n2, int n3, int n4, int n5, float f6, float f7) {
        Render2D.halftoneRect(f, f2, f3, f4, f5, f5, f5, f5, n, n2, n3, n4, n5, f6, f7);
    }

    public static void halftoneRect(float f, float f2, float f3, float f4, float f5, int n, int n2, float f6, float f7) {
        Render2D.imageBarrier();
        Render2D.halftoneRectangle().enqueue(new BuiltHalftoneRectangle(f, f2, f3, f4, f5, n, n2, f6, f7));
    }

    private static void registerReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        Identifier identifier = Identifier.of((String)"nv", (String)"render2d_msdf_cache");
        ResourceLoader.get((ResourceType)ResourceType.CLIENT_RESOURCES).registerReloader(identifier, (ResourceReloader)((SynchronousResourceReloader)resourceManager -> MsdfTextRenderer.clearResourceCaches()));
        reloadListenerRegistered = true;
    }

    public static void liquidGlassOutline(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n) {
        Render2D.glassOutline(f, f2, f3, f4, new float[]{f7 * f5 / 2.0f, f7 * f5 / 2.0f, f7 * f5 / 2.0f, f7 * f5 / 2.0f}, f8, n, (float)(n >>> 24 & 0xFF) / 255.0f, f4 == 240.0f ? 100.0f : 50.0f, n | 0xFF000000, 1.0f, true, 0.0f, f6, f5, 0.0f);
    }

    public static void circleOutline(float f, float f2, float f3, float f4, int n) {
        Render2D.imageBarrier();
        Render2D.circle().enqueue(new BuiltCircle(f, f2, f3, n).withThickness(f4));
    }

    public static void rectHalftone(float f, float f2, float f3, float f4, float f5, int n, int n2, float f6, float f7) {
        Render2D.halftoneRect(f, f2, f3, f4, f5, n, n2, f6, f7);
    }

    public static void pickerAlpha(float f, float f2, float f3, float f4, int n, float f5) {
        Render2D.imageBarrier();
        Render2D.picker().enqueue(BuiltPicker.alpha(f, f2, f3, f4, n, f5));
    }

    private static String msdfFontAlias(String string) {
        String string2;
        if (string == null || string.isBlank()) {
            return null;
        }
        return switch (string2 = string.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_')) {
            case "montserrat", "montserrat_regular" -> "montserrat-regular";
            case "montserrat_medium" -> "montserrat-medium";
            case "montserrat_semibold", "montserrat_semi_bold" -> "montserrat-semibold";
            case "montserrat_bold" -> "montserrat-bold";
            case "montserrat_extrabold", "montserrat_extra_bold" -> "montserrat-extrabold";
            case "montserrat_black" -> "montserrat-black";
            case "montserrat_light" -> "montserrat-light";
            case "montserrat_extralight", "montserrat_extra_light" -> "montserrat-extralight";
            case "montserrat_thin" -> "montserrat-thin";
            case "sf", "sf_regular" -> "sf-regular";
            case "sf_medium" -> "sf-medium";
            case "sf_bold" -> "sf-bold";
            case "nv" -> "nv";
            case "event_icons", "events_icons", "eventicons" -> "event-icons";
            case "inv_icons", "inventory_icons", "invicons" -> "inv-icons";
            default -> null;
        };
    }

    private static int normalizeTextColor(int n) {
        return n;
    }

    private static HalfIconRectangleRenderer halfIconRectangle() {
        return HalfIconRectangleRenderer.getInstance();
    }

    public static void glassOutline(float f, float f2, float f3, float f4, float[] fArray, float f5, int n, float f6, float f7, int n2, float f8, boolean bl, float f9, float f10, float f11, float f12) {
        Render2D.imageBarrier();
        Render2D.glassOutline().enqueue(new BuiltGlassOutline(f, f2, f3, f4, fArray, f5, n, f6, f7, n2, f8, bl, f9, f10, f11, f12));
    }

    public static void glassOutline(BuiltGlassOutline builtGlassOutline) {
        Render2D.imageBarrier();
        Render2D.glassOutline().enqueue(builtGlassOutline);
    }

    private static GlassOutlineRenderer glassOutline() {
        return GlassOutlineRenderer.getInstance();
    }

    public static void radialGlass(BuiltRadialGlass builtRadialGlass) {
        Render2D.imageBarrier();
        Render2D.radialGlass().enqueue(builtRadialGlass);
    }

    private static RadialGlassRenderer radialGlass() {
        return RadialGlassRenderer.getInstance();
    }

    public static void halfIconRect(BuiltHalfIconRectangle builtHalfIconRectangle) {
        Render2D.imageBarrier();
        if (builtHalfIconRectangle != null && builtHalfIconRectangle.backgroundVisible()) {
            Render2D.rect(builtHalfIconRectangle.x(), builtHalfIconRectangle.y(), builtHalfIconRectangle.width(), builtHalfIconRectangle.height(), builtHalfIconRectangle.radiusTopLeft(), builtHalfIconRectangle.radiusTopRight(), builtHalfIconRectangle.radiusBottomRight(), builtHalfIconRectangle.radiusBottomLeft(), builtHalfIconRectangle.colorTopLeft(), builtHalfIconRectangle.colorTopRight(), builtHalfIconRectangle.colorBottomRight(), builtHalfIconRectangle.colorBottomLeft());
        }
        if (builtHalfIconRectangle != null && builtHalfIconRectangle.iconsVisible()) {
            Render2D.halfIconRectangle().enqueue(builtHalfIconRectangle);
        }
    }

    public static void halfIconRect(float f, float f2, float f3, float f4, float f5, int n, int n2, String string, String string2, float f6, float f7, float f8, float f9) {
        Render2D.halfIconRect(new BuiltHalfIconRectangle(f, f2, f3, f4, f5, n, string, string2, f6, n2, f7, f8, f9));
    }

    public static void halfIconRect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, String string, String string2, float f9, float f10, float f11, float f12, float f13, float f14, float f15, float f16, long l) {
        Render2D.halfIconRect(new BuiltHalfIconRectangle(f, f2, f3, f4, f5, f6, f7, f8, n, n, n, n, 0.0f, string, string2, f9, n2, f10, f11, f12, f13, f14, f15, f16, l));
    }

    public static BlurBuilder blurBuilder() {
        return new BlurBuilder();
    }

    private static HalftoneRectangleRenderer halftoneRectangle() {
        return HalftoneRectangleRenderer.getInstance();
    }

    public static void liquidGlass(float f, float f2, float f3, float f4, float f5, float f6, float f7, int n) {
        Render2D.glass(f, f2, f3, f4, new float[]{f7 * f5 / 2.0f, f7 * f5 / 2.0f, f7 * f5 / 2.0f, f7 * f5 / 2.0f}, n, (float)(n >>> 24 & 0xFF) / 255.0f, f4 == 240.0f ? 100.0f : 50.0f, n | 0xFF000000, 1.0f, true, 0.0f, f6, f5, 0.0f);
    }

    public static Outline360Range outline360Range(float f, float f2, int n) {
        return Outline360Range.of((float)f, (float)f2, (int)n);
    }

    private static void imageBarrier() {
        ImageRenderer.getInstance().barrier();
        EffectIconRenderer.getInstance().barrier();
    }

    public static void pushScissor(DrawContext drawContext, float f, float f2, float f3, float f4) {
        if (drawContext == null) {
            ScissorUtil.push(f, f2, f3, f4);
            return;
        }
        ScissorUtil.push(Render2DCoordinateSpace.pose(drawContext), f, f2, f3, f4);
        ScreenRect screenRect = ScissorUtil.current();
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().identity();
        drawContext.enableScissor(screenRect.getLeft(), screenRect.getTop(), screenRect.getRight(), screenRect.getBottom());
        drawContext.getMatrices().popMatrix();
    }

    public static void msdfTextFade(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6, float f7, float f8) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueue(new BuiltMsdfText(string, string2, f, f2, f3, Render2D.normalizeTextColor(n)).withHorizontalFade(f4, f5, f6, f7, f8));
    }

    public static void imageUvNearest(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int n) {
        if (f3 <= 0.0f) {
            return;
        }
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4, f5, f6, f7, n, n, n, n).withSmoothness(f8).withUv(f9, f10, f11, f12).withNearest());
    }

    public static void imageUvNearest(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int n) {
        if (f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4, f5, n).withSmoothness(f6).withUv(f7, f8, f9, f10).withNearest());
    }

    public static void msdfShimmer(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6, float f7) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueueShimmer(new BuiltMsdfText(string, string2, f, f2, f3, n), f4, f5, f6, f7);
    }

    public static void msdfShimmer(String string, String string2, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueueShimmer(new BuiltMsdfText(string, string2, f, f2, f3, -1), f4, f5, f6, f7);
    }

    public static void rotatedImage(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7, int n) {
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4, n).withRotation(f5, f6, f7));
    }

    private static MsdfTextRenderer msdf() {
        return MsdfTextRenderer.getInstance();
    }

    public static void blur(float f, float f2, float f3, float f4, float f5, float f6) {
        Render2D.blur(f, f2, f3, f4, f5, f6, 1.0f, -1);
    }

    public static void blur(float f, float f2, float f3, float f4, float f5, float f6, float f7, int n, int n2, int n3, int n4) {
        Render2D.imageBarrier();
        Render2D.blur().enqueue(new BuiltBlur(f, f2, f3, f4, f5, f7, f6).withColors(n, n2, n3, n4));
    }

    private static BlurFramebuffer blur() {
        return BlurFramebuffer.getInstance();
    }

    public static void blur(float f, float f2, float f3, float f4, float f5) {
        Render2D.blur(f, f2, f3, f4, f5, 16.0f, 1.0f, -1);
    }

    public static void blur(BuiltBlur builtBlur) {
        Render2D.imageBarrier();
        Render2D.blur().enqueue(builtBlur);
    }

    public static void blur(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        Render2D.beginFrame(drawContext);
        Render2D.blur(f, f2, f3, f4, f5, f6);
        Render2D.flush();
    }

    public static void blur(float f, float f2, float f3, float f4, float f5, float f6, float f7, int n) {
        Render2D.imageBarrier();
        Render2D.blur().enqueue(new BuiltBlur(f, f2, f3, f4, f5, f7, f6).withColor(n));
    }

    public static void shimmer(float f, float f2, float f3, float f4, float f5, float f6, float f7, int n) {
        Render2D.shimmer().enqueue(new BuiltShimmer(f, f2, f3, f4, f5, f6, f7, n));
    }

    private static ShimmerRenderer shimmer() {
        return ShimmerRenderer.getInstance();
    }

    private static CircleRenderer circle() {
        return CircleRenderer.getInstance();
    }

    public static void circle(float f, float f2, float f3, float f4, int n) {
        Render2D.imageBarrier();
        Render2D.circle().enqueue(new BuiltCircle(f, f2, f3, n).withSmoothness(f4));
    }

    public static void circle(float f, float f2, float f3, int n) {
        Render2D.imageBarrier();
        Render2D.circle().enqueue(new BuiltCircle(f, f2, f3, n));
    }

    public static void outline360(float f, float f2, float f3, float f4, float f5, float f6, int n, List<Outline360Range> list) {
        Render2D.imageBarrier();
        Render2D.outline360().enqueue(new BuiltOutline360(f, f2, f3, f4, f5, f6, n, list));
    }

    public static void outline360(float f, float f2, float f3, float f4, float f5, float f6, int n, Outline360Range ... outline360RangeArray) {
        Render2D.imageBarrier();
        Render2D.outline360(f, f2, f3, f4, f5, f6, n, Arrays.asList(outline360RangeArray));
    }

    private static Outline360Renderer outline360() {
        return Outline360Renderer.getInstance();
    }

    public static void outline360(BuiltOutline360 builtOutline360) {
        Render2D.imageBarrier();
        Render2D.outline360().enqueue(builtOutline360);
    }

    public static void outline360(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n, List<Outline360Range> list) {
        Render2D.imageBarrier();
        Render2D.outline360().enqueue(new BuiltOutline360(f, f2, f3, f4, f5, f6, f7, f8, f9, n, list));
    }

    public static void outline360(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n, Outline360Range ... outline360RangeArray) {
        Render2D.imageBarrier();
        Render2D.outline360(f, f2, f3, f4, f5, f6, f7, f8, f9, n, Arrays.asList(outline360RangeArray));
    }

    public static void effectIcon(RegistryEntry<StatusEffect> registryEntry, float f, float f2, float f3, int n) {
        Render2D.effectIcon().enqueue(new BuiltEffectIcon(registryEntry, f, f2, f3, n));
    }

    public static void effectIcon(RegistryEntry<StatusEffect> registryEntry, float f, float f2, float f3) {
        Render2D.effectIcon().enqueue(new BuiltEffectIcon(registryEntry, f, f2, f3));
    }

    public static void effectIcon(StatusEffectInstance statusEffectInstance, float f, float f2, float f3) {
        if (statusEffectInstance != null && statusEffectInstance.shouldShowIcon()) {
            Render2D.effectIcon().enqueue(new BuiltEffectIcon(statusEffectInstance, f, f2, f3));
        }
    }

    public static void effectIcon(StatusEffectInstance statusEffectInstance, float f, float f2, float f3, int n) {
        if (statusEffectInstance != null && statusEffectInstance.shouldShowIcon()) {
            Render2D.effectIcon().enqueue(new BuiltEffectIcon(statusEffectInstance, f, f2, f3, n));
        }
    }

    private static EffectIconRenderer effectIcon() {
        return EffectIconRenderer.getInstance();
    }

    public static void ripple(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2) {
        Render2D.ripple(f, f2, f3, f4, f5, 0.5f, f6, f7, f8, 25.0f, n, n2);
    }

    private static RippleRenderer ripple() {
        return RippleRenderer.getInstance();
    }

    public static void ripple(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int n, int n2) {
        Render2D.ripple().enqueue(new BuiltRipple(f, f2, f3, f4, f5, f5, f5, f5, f6, f7, f8, f9, f10, n, n2, false));
    }

    private static ArcRenderer arc() {
        return ArcRenderer.getInstance();
    }

    public static void arc(float f, float f2, float f3, float f4, float f5, int n) {
        Render2D.imageBarrier();
        Render2D.arc().enqueue(new BuiltArc(f, f2, f3, f4, f5, 0.85f, n));
    }

    private static DefaultRectangleRenderer rectangle() {
        return DefaultRectangleRenderer.getInstance();
    }

    private static ZippyRenderer zippy() {
        return ZippyRenderer.getInstance();
    }

    public static void zippy(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n) {
        Render2D.imageBarrier();
        Render2D.zippy().enqueue(new BuiltZippy(f, f2, f3, f4, f5, f6, f7, f8, n));
    }

    public static void zippy(float f, float f2, float f3, float f4, float f5, int n) {
        Render2D.imageBarrier();
        Render2D.zippy().enqueue(new BuiltZippy(f, f2, f3, f4, f5, n));
    }

    public static void zippy(BuiltZippy builtZippy) {
        Render2D.imageBarrier();
        Render2D.zippy().enqueue(builtZippy);
    }

    private static PickerRenderer picker() {
        return PickerRenderer.getInstance();
    }

    public static void rippleIcon(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2) {
        Render2D.ripple().enqueue(new BuiltRipple(f, f2, f3, f4, f5, f5, f5, f5, 0.5f, f6, f7, f8, 25.0f, n, n2, true));
    }

    public static float[] msdfBounds(String string, String string2, float f) {
        return Render2D.msdf().glyphBounds(string, string2, f);
    }

    public static boolean imageReady(String string) {
        return Render2D.image().isTextureResolvable(string);
    }

    public static void textFade(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6, boolean bl, boolean bl2) {
        Render2D.imageBarrier();
        Render2D.text().enqueue(new BuiltText(string, string2, f, f2, f3, n).withHorizontalFade(f4, f5, f6, bl, bl2));
    }

    public static void textFade(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6, float f7, float f8) {
        Render2D.imageBarrier();
        Render2D.text().enqueue(new BuiltText(string, string2, f, f2, f3, n).withHorizontalFade(f4, f5, f6, f7, f8));
    }

    public static void msdfText(String string, String string2, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueue(new BuiltMsdfText(string, string2, f, f2, f3, Render2D.normalizeTextColor(n), Render2D.normalizeTextColor(n2), Render2D.normalizeTextColor(n3), Render2D.normalizeTextColor(n4)));
    }

    public static void msdfText(BuiltMsdfText builtMsdfText) {
        Render2D.imageBarrier();
        if (builtMsdfText == null) {
            return;
        }
        Render2D.msdf().enqueue(new BuiltMsdfText(builtMsdfText.fontName(), builtMsdfText.text(), builtMsdfText.x(), builtMsdfText.y(), builtMsdfText.size(), Render2D.normalizeTextColor(builtMsdfText.colorTopLeft()), Render2D.normalizeTextColor(builtMsdfText.colorTopRight()), Render2D.normalizeTextColor(builtMsdfText.colorBottomRight()), Render2D.normalizeTextColor(builtMsdfText.colorBottomLeft()), builtMsdfText.rotationDegrees(), builtMsdfText.rotationOriginX(), builtMsdfText.rotationOriginY()).withHorizontalFade(builtMsdfText.fadeLeftX(), builtMsdfText.fadeRightX(), builtMsdfText.fadeWidth(), builtMsdfText.fadeLeftStrength(), builtMsdfText.fadeRightStrength()));
    }

    public static void msdfText(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueue(new BuiltMsdfText(string, string2, f, f2, f3, Render2D.normalizeTextColor(n), f4, f5, f6));
    }

    public static void msdfText(String string, String string2, float f, float f2, float f3, int n) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueue(new BuiltMsdfText(string, string2, f, f2, f3, Render2D.normalizeTextColor(n)));
    }

    public static float msdfWidth(String string, String string2, float f) {
        return Render2D.msdf().width(string, string2, f);
    }

    private static float endFade(float f, float f2, float f3) {
        float f4 = (f = Math.max(0.0f, Math.min(1.0f, f))) < 0.5f ? f2 : f3;
        float f5 = 1.0f - Math.abs(f - 0.5f) * 2.0f;
        return f4 + (1.0f - f4) * f5;
    }

    public static void msdfWave(String string, String string2, float f, float f2, float f3, int n, int n2, float f4) {
        Render2D.imageBarrier();
        int n3 = Render2D.normalizeTextColor(n);
        int n4 = Render2D.normalizeTextColor(n2);
        Render2D.msdf().enqueueWave(new BuiltMsdfText(string, string2, f, f2, f3, n3, n3, n4, n4), f4);
    }

    public static void msdfWave(String string, String string2, float f, float f2, float f3, int n, float f4) {
        Render2D.imageBarrier();
        Render2D.msdf().enqueueWave(new BuiltMsdfText(string, string2, f, f2, f3, Render2D.normalizeTextColor(n)), f4);
    }

    public static void pickerHue(float f, float f2, float f3, float f4, float f5) {
        Render2D.imageBarrier();
        Render2D.picker().enqueue(BuiltPicker.hue(f, f2, f3, f4, f5));
    }

    public static void dashedLine(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7, int n2) {
        float f8 = f3 - f;
        float f9 = f4 - f2;
        float f10 = (float)Math.sqrt(f8 * f8 + f9 * f9);
        if (f10 < 0.01f || n >>> 24 == 0 || f5 <= 0.0f) {
            return;
        }
        float f11 = f8 / f10;
        float f12 = f9 / f10;
        float f13 = Math.max(0.5f, f6 + f7);
        int n3 = (int)Math.ceil(f10 / f13);
        if (n2 > 0 && n3 > n2) {
            n3 = n2;
        }
        for (int i = 0; i < n3; ++i) {
            float f14 = (float)i * f13;
            float f15 = Math.min(f14 + f6, f10);
            if (f15 <= f14) continue;
            Render2D.line(f + f11 * f14, f2 + f12 * f14, f + f11 * f15, f2 + f12 * f15, f5, n);
        }
    }

    public static void dashedLine(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7, int n2, float f8, float f9) {
        float f10 = f3 - f;
        float f11 = f4 - f2;
        float f12 = (float)Math.sqrt(f10 * f10 + f11 * f11);
        if (f12 < 0.01f || n >>> 24 == 0 || f5 <= 0.0f) {
            return;
        }
        float f13 = f10 / f12;
        float f14 = f11 / f12;
        float f15 = Math.max(0.5f, f6 + f7);
        int n3 = (int)Math.ceil(f12 / f15);
        if (n2 > 0 && n3 > n2) {
            n3 = n2;
        }
        for (int i = 0; i < n3; ++i) {
            float f16 = (float)i * f15;
            float f17 = Math.min(f16 + f6, f12);
            if (f17 <= f16) continue;
            float f18 = Render2D.endFade(f16 / f12, f8, f9);
            float f19 = Render2D.endFade(f17 / f12, f8, f9);
            Render2D.line(f + f13 * f16, f2 + f14 * f16, f + f13 * f17, f2 + f14 * f17, f5, n, f18, f19);
        }
    }

    public static void imageUv(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int n) {
        if (f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        Render2D.image().enqueue(new BuiltImage(string, f, f2, f3, f4, f5, n).withSmoothness(f6).withUv(f7, f8, f9, f10));
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, int n, int n2, int n3, int n4) {
        Render2D.rect(f, f2, f3, f4, f5, f5, f5, f5, n, n2, n3, n4);
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n) {
        Render2D.imageBarrier();
        Render2D.rectangle().enqueue(new BuiltRectangle(f, f2, f3, f4, f5, f6, f7, f8, n));
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, int n) {
        Render2D.imageBarrier();
        Render2D.rectangle().enqueue(new BuiltRectangle(f, f2, f3, f4, f5, n));
    }

    public static void rect(float f, float f2, float f3, float f4, int n) {
        Render2D.rect(f, f2, f3, f4, 0.0f, n);
    }

    public static void rect(BuiltRectangle builtRectangle) {
        Render2D.rectangle().enqueue(builtRectangle);
    }

    public static void rect(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int n) {
        Render2D.beginFrame(drawContext);
        Render2D.rect(f, f2, f3, f4, f5, n);
        Render2D.flush();
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, int n3, int n4) {
        Render2D.imageBarrier();
        Render2D.rectangle().enqueue(new BuiltRectangle(f, f2, f3, f4, f5, f6, f7, f8, n, n2, n3, n4, 0.7f));
    }

    public static void popScissor(DrawContext drawContext) {
        ScissorUtil.pop();
        if (drawContext == null) {
            return;
        }
        drawContext.disableScissor();
    }

    public static float textWidth(String string, String string2, float f) {
        String string3 = Render2D.msdfFontAlias(string);
        if (string3 != null) {
            return Render2D.msdf().width(string3, string2, f);
        }
        return Render2D.text().width(string, string2, f);
    }

    public static void glass(float f, float f2, float f3, float f4, float[] fArray, int n, float f5, float f6, int n2, float f7, boolean bl, float f8, float f9, float f10, float f11) {
        Render2D.imageBarrier();
        Render2D.glass().enqueue(new BuiltGlass(f, f2, f3, f4, fArray, n, f5, f6, n2, f7, bl, f8, f9, f10, f11));
    }

    public static void glass(BuiltGlass builtGlass) {
        Render2D.imageBarrier();
        Render2D.glass().enqueue(builtGlass);
    }

    private static GlassRenderer glass() {
        return GlassRenderer.getInstance();
    }

    public static void glass(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7, int n2, float f8, boolean bl, float f9, float f10, float f11, float f12) {
        Render2D.glass(f, f2, f3, f4, f5, f5, f5, f5, n, f6, f7, n2, f8, bl, f9, f10, f11, f12);
    }

    public static void glass(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, int n2, float f11, boolean bl, float f12, float f13, float f14, float f15) {
        Render2D.imageBarrier();
        Render2D.glass().enqueue(new BuiltGlass(f, f2, f3, f4, f5, f6, f7, f8, n, f9, f10, n2, f11, bl, f12, f13, f14, f15));
    }

    public static void beginFrame(DrawContext drawContext) {
        if (frameActive) {
            return;
        }
        frameActive = true;
        Render2D.blur().beginFrame(drawContext);
        Render2D.glass().beginFrame(drawContext);
        Render2D.shape().beginFrame(drawContext);
        Render2D.glassOutline().beginFrame(drawContext);
        Render2D.circle().beginFrame(drawContext);
        Render2D.arc().beginFrame(drawContext);
        Render2D.radialGlass().beginFrame(drawContext);
        Render2D.picker().beginFrame(drawContext);
        Render2D.rectangle().beginFrame(drawContext);
        Render2D.halfIconRectangle().beginFrame(drawContext);
        Render2D.halftoneRectangle().beginFrame(drawContext);
        Render2D.zippy().beginFrame(drawContext);
        Render2D.outline().beginFrame(drawContext);
        Render2D.outline360().beginFrame(drawContext);
        Render2D.text().beginFrame(drawContext);
        Render2D.msdf().beginFrame(drawContext);
        Render2D.shimmer().beginFrame(drawContext);
        Render2D.image().beginFrame(drawContext);
        Render2D.line().beginFrame(drawContext);
        Render2D.effectIcon().beginFrame(drawContext);
        Render2D.ripple().beginFrame(drawContext);
        Render2D.glow().beginFrame(drawContext);
    }

    public static void shape(BuiltShape builtShape) {
        Render2D.imageBarrier();
        Render2D.shape().enqueue(builtShape);
    }

    private static ShapeRenderer shape() {
        return ShapeRenderer.getInstance();
    }

    public static void outline(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n) {
        Render2D.imageBarrier();
        Render2D.outline().enqueue(new BuiltOutline(f, f2, f3, f4, f5, f6, f7, f8, f9, n));
    }

    public static void outline(float f, float f2, float f3, float f4, float f5, float f6, int n, int n2, int n3, int n4) {
        Render2D.outline(f, f2, f3, f4, f5, f5, f5, f5, f6, n, n2, n3, n4);
    }

    public static void outline(float f, float f2, float f3, float f4, float f5, float f6, int n) {
        Render2D.imageBarrier();
        Render2D.outline().enqueue(new BuiltOutline(f, f2, f3, f4, f5, f6, n));
    }

    private static DefaultOutlineRenderer outline() {
        return DefaultOutlineRenderer.getInstance();
    }

    public static void outline(BuiltOutline builtOutline) {
        Render2D.imageBarrier();
        Render2D.outline().enqueue(builtOutline);
    }

    public static void outline(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, int n) {
        Render2D.beginFrame(drawContext);
        Render2D.outline(f, f2, f3, f4, f5, f6, n);
        Render2D.flush();
    }

    public static void outline(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n, int n2, int n3, int n4) {
        Render2D.imageBarrier();
        Render2D.outline().enqueue(new BuiltOutline(f, f2, f3, f4, f5, f6, f7, f8, f9, n, n2, n3, n4, 0.5f));
    }
}

