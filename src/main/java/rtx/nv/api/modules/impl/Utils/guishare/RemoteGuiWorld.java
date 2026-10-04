package rtx.nv.api.modules.impl.Utils.guishare;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.impl.Utils.Globals;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareController;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareRemoteState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareThemeState;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiPanel;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiPanelRenderer;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteTheme;
import rtx.nv.api.ui.window.WorldGuiCloseAnimation;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;

public final class RemoteGuiWorld {
    private static final float WORLD_WIDTH_BLOCKS = 1.35f;
    private static final int MAX_PANELS = 4;
    private static final int MAX_SLOTS = 4;
    private static final boolean SPLIT_POPUP_SLOTS = false;
    private static final boolean STACK_BACK_ORDER = true;
    private static final float CAPTURE_SCALE_CAP = 1.5f;
    private static final float SIDE_HYSTERESIS = 0.05f;
    private static final int MAX_CARD_BLUR_RECTS = 64;
    private static final float CARD_BLUR_MAX_RADIUS = 22.0f;
    private static final float CARD_BLUR_BOUNDS_PAD = 48.0f;
    private static final List<PanelQuad> quads = new ArrayList<PanelQuad>();
    private static final List<CardBlurJob> cardBlurJobs = new ArrayList<CardBlurJob>();
    private static final Map<RemoteGuiPanel, Boolean> behindSide = new WeakHashMap<RemoteGuiPanel, Boolean>();
    private static boolean captureRequested;
    private static boolean depthSnapshotWanted;
    private static CardBlurTransform cardBlurTransform;
    private static CardBlurJob collectingCardBlurJob;
    private static CardBlurJob activeCardBlurJob;
    private static int cardBlurDrawIndex;
    private static final float POPUP_FORWARD_BLOCKS = -0.02f;

    private RemoteGuiWorld() {
    }

    private static float[] packSlots(List<float[]> list, float f, float f2) {
        float f3;
        int n = list.size();
        float[] fArray = new float[n * 2 + 1];
        if (n == 0) {
            return fArray;
        }
        int n2 = 1 << n - 1;
        float f4 = -1.0f;
        int n3 = 0;
        for (int i = 0; i < n2; ++i) {
            float f5;
            float f6 = Float.MAX_VALUE;
            float f7 = 0.0f;
            int n4 = 0;
            f3 = 0.0f;
            float f8 = 0.0f;
            int n5 = 0;
            for (int j = 0; j < n; ++j) {
                f3 += list.get(j)[2];
                f8 = Math.max(f8, list.get(j)[3]);
                ++n5;
                if (j != n - 1 && (i & 1 << j) == 0) continue;
                f5 = f - 8.0f * (float)n5;
                f6 = Math.min(f6, f5 <= 0.0f || f3 <= 0.0f ? 0.0f : f5 / f3);
                f7 += f8;
                ++n4;
                f3 = 0.0f;
                f8 = 0.0f;
                n5 = 0;
            }
            float f9 = f2 - 8.0f * (float)n4;
            f5 = f9 <= 0.0f || f7 <= 0.0f ? 0.0f : f9 / f7;
            float f10 = Math.min(1.5f, Math.min(f6, f5));
            if (!(f10 > f4 + 1.0E-4f)) continue;
            f4 = f10;
            n3 = i;
        }
        float f11 = 0.0f;
        int n6 = 0;
        for (int i = 0; i < n; ++i) {
            if (i != n - 1 && (n3 & 1 << i) == 0) continue;
            float f12 = 0.0f;
            for (int j = n6; j <= i; ++j) {
                f12 = Math.max(f12, list.get(j)[3]);
            }
            f3 = 0.0f;
            for (int j = n6; j <= i; ++j) {
                fArray[j * 2] = f3 + 4.0f;
                fArray[j * 2 + 1] = f11 + 4.0f;
                f3 += list.get(j)[2] * f4 + 8.0f;
            }
            f11 += f12 * f4 + 8.0f;
            n6 = i + 1;
        }
        fArray[n * 2] = f4;
        return fArray;
    }

    public static List<PanelQuad> peekQuads() {
        return quads;
    }

    private static boolean wantsSplit(RemoteGuiPanel remoteGuiPanel) {
        return RemoteGuiWorld.popupVisible(remoteGuiPanel) && !remoteGuiPanel.shatterActive();
    }

    public static void beginCardBlurDraw(Framebuffer framebuffer) {
        CardBlurJob cardBlurJob = activeCardBlurJob = cardBlurDrawIndex < cardBlurJobs.size() ? cardBlurJobs.get(cardBlurDrawIndex++) : null;
        if (activeCardBlurJob != null && RemoteGuiWorld.activeCardBlurJob.ready) {
            GuiMotionBlurRenderer.captureBackground(framebuffer, RemoteGuiWorld.activeCardBlurJob.radius);
        }
    }

    public static void endCardBlurDraw(Framebuffer framebuffer) {
        CardBlurJob cardBlurJob = activeCardBlurJob;
        activeCardBlurJob = null;
        if (cardBlurJob == null || !cardBlurJob.ready || framebuffer == null) {
            return;
        }
        GuiMotionBlurRenderer.applyWithCopy(framebuffer, 1.0f, cardBlurJob.radius, cardBlurJob.boundsX, cardBlurJob.boundsY, cardBlurJob.boundsW, cardBlurJob.boundsH, cardBlurJob.mask, cardBlurJob.cardCount + 1, cardBlurJob.boundsX, cardBlurJob.boundsY, cardBlurJob.boundsW, cardBlurJob.boundsH, 1.0f, cardBlurJob.originX, cardBlurJob.originY);
    }

    public static boolean captureRequested() {
        return captureRequested;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void renderPanels(DrawContext drawContext) {
        float f;
        captureRequested = false;
        depthSnapshotWanted = false;
        quads.clear();
        cardBlurJobs.clear();
        cardBlurTransform = null;
        collectingCardBlurJob = null;
        activeCardBlurJob = null;
        cardBlurDrawIndex = 0;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (drawContext == null || minecraftClient == null || minecraftClient.world == null || minecraftClient.player == null) {
            return;
        }
        if (!Globals.remoteGuisEnabled()) {
            return;
        }
        if (!WorldGuiCloseAnimation.hasCapturedWorldMatrices() || !GuiLayerBlurRenderer.available()) {
            return;
        }
        List<RemoteGuiPanel> list = GuiShareController.renderablePanels(4);
        if (list.isEmpty()) {
            return;
        }
        float f2 = Position.screenWidth();
        float f3 = Position.screenHeight();
        if (f2 <= 32.0f || f3 <= 32.0f) {
            return;
        }
        int n = list.size();
        int n2 = 0;
        for (RemoteGuiPanel object2 : list) {
            if (!RemoteGuiWorld.wantsSplit(object2)) continue;
            ++n2;
        }
        boolean bl = false;
        ArrayList<float[]> arrayList = new ArrayList<float[]>();
        int[] nArray = new int[n];
        int[] nArray2 = new int[n];
        for (int fArray = 0; fArray < n; ++fArray) {
            float[] fArray2;
            RemoteGuiPanel f4 = list.get(fArray);
            float[] l = bl && RemoteGuiWorld.wantsSplit(f4) ? RemoteGuiWorld.tightPopupCell(f4) : null;
            nArray[fArray] = arrayList.size();
            if (l != null) {
                float[] fArray3 = new float[4];
                fArray3[0] = 0.0f;
                fArray3[1] = 0.0f;
                fArray3[2] = 590.0f;
                fArray2 = fArray3;
                fArray3[3] = 472.0f;
            } else {
                fArray2 = RemoteGuiWorld.computeCell(f4);
            }
            arrayList.add(fArray2);
            if (l != null) {
                nArray2[fArray] = arrayList.size();
                arrayList.add(l);
                continue;
            }
            nArray2[fArray] = -1;
        }
        float[] fArray = RemoteGuiWorld.packSlots(arrayList, f2, f3);
        float f4 = fArray[arrayList.size() * 2];
        if (f4 <= 0.05f) {
            return;
        }
        captureRequested = true;
        depthSnapshotWanted = true;
        Render2D.beginFrame(drawContext);
        GuiLayerBlurRenderer.markRemoteBegin(drawContext);
        long l = System.currentTimeMillis();
        float f5 = f = Render2DCoordinateSpace.designGuiScale();
        BlurFramebuffer.beginWorldScope();
        try {
            for (int i = 0; i < n; ++i) {
                RemoteGuiPanel remoteGuiPanel = list.get(i);
                int n3 = nArray[i];
                float[] fArray4 = (float[])arrayList.get(n3);
                float f6 = fArray[n3 * 2];
                float f7 = fArray[n3 * 2 + 1];
                RemoteTheme remoteTheme = remoteGuiPanel.theme();
                remoteTheme.update(l);
                ClientPalette.writeRemoteSlot(n3 + 1, remoteTheme.palette6(), remoteTheme.phase(), remoteTheme.styleId(), remoteTheme.closed() ? 1.0f : 0.0f, 0.0f, remoteTheme.styleId());
                boolean bl2 = nArray2[i] >= 0;
                boolean bl3 = !bl2 && RemoteGuiWorld.wantsSplit(remoteGuiPanel) && RemoteGuiWorld.viewerBehind(remoteGuiPanel);
                float f8 = fArray4[2];
                float f9 = fArray4[3];
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(f6, f7);
                drawContext.getMatrices().scale(f4, f4);
                Render2D.pushScissor(drawContext, 0.0f, 0.0f, f8, f9);
                drawContext.getMatrices().translate(-fArray4[0], -fArray4[1]);
                RemoteGuiWorld.beginCardBlurSlot(f6, f7, f4, fArray4[0], fArray4[1], f, f5);
                try {
                    if (bl2) {
                        RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n3 + 1, 1);
                    } else if (bl3) {
                        RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n3 + 1, 2, 1.0f, true);
                        GuiLayerBlurRenderer.markPopupBoundary(drawContext);
                        RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n3 + 1, 1, 1.0f, false);
                    } else {
                        RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n3 + 1, 0);
                    }
                }
                finally {
                    RemoteGuiWorld.endCardBlurSlot();
                }
                Render2D.popScissor(drawContext);
                drawContext.getMatrices().popMatrix();
                remoteGuiPanel.resolveAnchor(GuiShareController.liveAnchorFor(remoteGuiPanel.state(), remoteGuiPanel.smoothYaw(), remoteGuiPanel.smoothPitch()));
                boolean bl4 = RemoteGuiWorld.updateViewerSide(remoteGuiPanel);
                float f10 = 295.0f - fArray4[0];
                float f11 = 236.0f - fArray4[1];
                float f12 = f6 * f;
                float f13 = f7 * f5;
                float f14 = f8 * f4 * f;
                float f15 = f9 * f4 * f5;
                Matrix4f matrix4f = RemoteGuiWorld.buildMatrix(remoteGuiPanel, f10, f11, 0.0f);
                if (matrix4f != null) {
                    quads.add(new PanelQuad(matrix4f, f8, f9, f12, f13, f14, f15, RemoteGuiWorld.buildShatter(remoteGuiPanel, -fArray4[0], -fArray4[1], f8, f9), remoteGuiPanel, f10, f11, 0.0f, null, 1.0f));
                }
                if (!bl2) continue;
                int n4 = nArray2[i];
                float[] fArray5 = (float[])arrayList.get(n4);
                float f16 = fArray[n4 * 2];
                float f17 = fArray[n4 * 2 + 1];
                float f18 = fArray5[2];
                float f19 = fArray5[3];
                ClientPalette.writeRemoteSlot(n4 + 1, remoteTheme.palette6(), remoteTheme.phase(), remoteTheme.styleId(), remoteTheme.closed(), 0.0f, remoteTheme.styleId());
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(f16, f17);
                drawContext.getMatrices().scale(f4, f4);
                Render2D.pushScissor(drawContext, 0.0f, 0.0f, f18, f19);
                drawContext.getMatrices().translate(-fArray5[0], -fArray5[1]);
                RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n4 + 1, 2);
                Render2D.popScissor(drawContext);
                drawContext.getMatrices().popMatrix();
                float f20 = 295.0f - fArray5[0];
                float f21 = 236.0f - fArray5[1];
                Matrix4f matrix4f2 = RemoteGuiWorld.buildMatrix(remoteGuiPanel, f20, f21, -0.02f);
                if (matrix4f2 == null) continue;
                float f22 = f4 * f;
                float f23 = f4 * f5;
                SlotBlit slotBlit = new SlotBlit(f12 + (fArray5[0] - fArray4[0]) * f22, f13 + (fArray5[1] - fArray4[1]) * f23, f18 * f22, f19 * f23, f12, f13, f14, f15);
                quads.add(new PanelQuad(matrix4f2, f18, f19, f16 * f, f17 * f5, f18 * f22, f19 * f23, null, remoteGuiPanel, f20, f21, -0.02f, slotBlit, 1.0f));
            }
        }
        finally {
            BlurFramebuffer.endWorldScope();
        }
        Render2D.flush();
        GuiLayerBlurRenderer.markPanelEndRemote(drawContext);
    }

    private static float[] tightPopupCell(RemoteGuiPanel remoteGuiPanel) {
        float[] fArray = RemoteGuiPanelRenderer.popupDrawRect(remoteGuiPanel);
        if (fArray == null || fArray[2] <= 1.0f || fArray[3] <= 1.0f || fArray[2] > 2360.0f || fArray[3] > 1888.0f) {
            return null;
        }
        return fArray;
    }

    private static float[] computeCell(RemoteGuiPanel remoteGuiPanel) {
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 590.0f;
        float f4 = 472.0f;
        if (RemoteGuiWorld.popupVisible(remoteGuiPanel)) {
            float[] fArray = RemoteGuiWorld.tightPopupCell(remoteGuiPanel);
            if (fArray != null) {
                f = Math.min(f, fArray[0]);
                f2 = Math.min(f2, fArray[1]);
                f3 = Math.max(f3, fArray[0] + fArray[2]);
                f4 = Math.max(f4, fArray[1] + fArray[3]);
            } else {
                float f5 = 80.0f + remoteGuiPanel.popupHoldX();
                float f6 = 80.0f + remoteGuiPanel.popupHoldY();
                float f7 = 8.0f;
                float f8 = 100.0f;
                f = Math.min(f, f5 - f8 - f7);
                f2 = Math.min(f2, f6 - f8 - f7);
                f3 = Math.max(f3, f5 + 180.0f + f8 + f7);
                f4 = Math.max(f4, f6 + 290.0f + f8 + f7);
            }
        }
        return new float[]{f, f2, f3 - f, f4 - f2};
    }

    static boolean beginCardBlurJob(DrawContext drawContext, float f, float f2, float f3, float f4) {
        CardBlurTransform cardBlurTransform = RemoteGuiWorld.cardBlurTransform;
        if (drawContext == null || cardBlurTransform == null || collectingCardBlurJob != null || f3 <= 0.5f || f4 <= 0.5f) {
            return false;
        }
        CardBlurJob cardBlurJob = new CardBlurJob(cardBlurTransform.offsetX() + f * cardBlurTransform.scaleX(), cardBlurTransform.offsetY() + f2 * cardBlurTransform.scaleY(), f3 * cardBlurTransform.scaleX(), f4 * cardBlurTransform.scaleY(), cardBlurTransform.radiusScale());
        cardBlurJobs.add(cardBlurJob);
        collectingCardBlurJob = cardBlurJob;
        GuiLayerBlurRenderer.markRemoteCardBegin(drawContext);
        return true;
    }

    private static Vec3d offsetForward(Vec3d vec3d, float f, float f2, float f3) {
        if (vec3d == null || f3 == 0.0f) {
            return vec3d;
        }
        return vec3d.add(Vec3d.fromPolar((float)f, (float)f2).multiply((double)f3));
    }

    public static boolean depthSnapshotWanted() {
        return depthSnapshotWanted;
    }

    static boolean recordCardBlurRect(float f, float f2, float f3, float f4, float f5, float f6) {
        CardBlurTransform cardBlurTransform = RemoteGuiWorld.cardBlurTransform;
        CardBlurJob cardBlurJob = collectingCardBlurJob;
        if (cardBlurTransform == null || cardBlurJob == null) {
            return false;
        }
        return cardBlurJob.add(cardBlurTransform.offsetX() + f * cardBlurTransform.scaleX(), cardBlurTransform.offsetY() + f2 * cardBlurTransform.scaleY(), f3 * cardBlurTransform.scaleX(), f4 * cardBlurTransform.scaleY(), f5, f6);
    }

    static void endCardBlurJob(DrawContext drawContext) {
        CardBlurJob cardBlurJob = collectingCardBlurJob;
        if (cardBlurJob == null) {
            return;
        }
        collectingCardBlurJob = null;
        cardBlurJob.finish();
        GuiLayerBlurRenderer.markRemoteCardEnd(drawContext);
    }

    private static Shatter buildShatter(RemoteGuiPanel remoteGuiPanel, float f, float f2, float f3, float f4) {
        if (!remoteGuiPanel.shatterActive()) {
            return null;
        }
        float[] fArray = remoteGuiPanel.shatterRect();
        if (fArray == null || fArray.length < 7 || fArray[5] <= 1.0f || fArray[6] <= 1.0f || fArray[2] <= 1.0f || fArray[3] <= 1.0f) {
            return null;
        }
        float f5 = fArray[0] / fArray[5];
        float f6 = fArray[1] / fArray[6];
        float f7 = fArray[2] / fArray[5];
        float f8 = fArray[3] / fArray[6];
        if (f7 <= 1.0E-5f || f8 <= 1.0E-5f) {
            return null;
        }
        float f9 = 430.0f;
        float f10 = 290.0f;
        float f11 = 80.0f;
        float f12 = 22.0f;
        float f13 = f9 / f3 / f7;
        float f14 = (f + f11) / f3 - f9 / f3 * (f5 / f7);
        float f15 = f10 / f4 / f8;
        float f16 = (f2 + f11 + f12) / f4 - f10 / f4 * (f6 / f8);
        return new Shatter(remoteGuiPanel.shatterState(), remoteGuiPanel.shatterProgressForRender(), remoteGuiPanel.closeAlpha(), f14, f16, f13, f15);
    }

    public static List<PanelQuad> consumeQuads() {
        ArrayList<PanelQuad> arrayList = new ArrayList<PanelQuad>(quads);
        quads.clear();
        captureRequested = false;
        RemoteGuiWorld.sortFarthestFirst(arrayList);
        return arrayList;
    }

    private static Vec3d panelWorldPos(PanelQuad panelQuad) {
        RemoteGuiPanel remoteGuiPanel = panelQuad.panel();
        return remoteGuiPanel == null ? null : remoteGuiPanel.resolvedAnchor();
    }

    private static boolean updateViewerSide(RemoteGuiPanel remoteGuiPanel) {
        Vec3d vec3d;
        boolean bl = RemoteGuiWorld.viewerBehind(remoteGuiPanel);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Vec3d vec3d2 = vec3d = remoteGuiPanel == null ? null : remoteGuiPanel.resolvedAnchor();
        if (minecraftClient == null || minecraftClient.player == null || vec3d == null) {
            return bl;
        }
        Vec3d vec3d3 = minecraftClient.player.getEyePos().subtract(vec3d);
        double d = vec3d3.length();
        if (d < 1.0E-4) {
            return bl;
        }
        Vec3d vec3d4 = Vec3d.fromPolar((float)remoteGuiPanel.resolvedPitch(), (float)remoteGuiPanel.resolvedYaw());
        double d2 = vec3d3.dotProduct(vec3d4) / d;
        boolean bl2 = bl ? d2 > (double)-0.05f : d2 > (double)0.05f;
        behindSide.put(remoteGuiPanel, bl2);
        return bl2;
    }

    private static void sortFarthestFirst(List<PanelQuad> list) {
        if (list.size() < 2) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.player == null) {
            return;
        }
        Vec3d vec3d = minecraftClient.player.getEyePos();
        list.sort((panelQuad, panelQuad2) -> {
            if (panelQuad.panel() == panelQuad2.panel()) {
                if (!RemoteGuiWorld.viewerBehind(panelQuad.panel())) {
                    int n;
                    int n2 = panelQuad.blit() != null ? 1 : 0;
                    int n3 = n = panelQuad2.blit() != null ? 1 : 0;
                    if (n2 != n) {
                        return Integer.compare(n2, n);
                    }
                }
                Vec3d vec3d2 = RemoteGuiWorld.quadWorldPos(panelQuad);
                Vec3d vec3d3 = RemoteGuiWorld.quadWorldPos(panelQuad2);
                if (vec3d2 == null || vec3d3 == null) {
                    return 0;
                }
                return Double.compare(vec3d3.squaredDistanceTo(vec3d), vec3d2.squaredDistanceTo(vec3d));
            }
            Vec3d vec3d4 = RemoteGuiWorld.panelWorldPos(panelQuad);
            Vec3d vec3d5 = RemoteGuiWorld.panelWorldPos(panelQuad2);
            if (vec3d4 == null || vec3d5 == null) {
                return 0;
            }
            return Double.compare(vec3d5.squaredDistanceTo(vec3d), vec3d4.squaredDistanceTo(vec3d));
        });
    }

    public static void prepareCardBlurDraw() {
        cardBlurDrawIndex = 0;
        activeCardBlurJob = null;
    }

    private static void beginCardBlurSlot(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        cardBlurTransform = new CardBlurTransform((f - f4 * f3) * f6, (f2 - f5 * f3) * f7, f3 * f6, f3 * f7, f3);
    }

    private static void endCardBlurSlot() {
        cardBlurTransform = null;
        collectingCardBlurJob = null;
    }

    private static Vec3d quadWorldPos(PanelQuad panelQuad) {
        RemoteGuiPanel remoteGuiPanel = panelQuad.panel();
        if (remoteGuiPanel == null || remoteGuiPanel.resolvedAnchor() == null) {
            return null;
        }
        return RemoteGuiWorld.offsetForward(remoteGuiPanel.resolvedAnchor(), remoteGuiPanel.resolvedPitch(), remoteGuiPanel.resolvedYaw(), panelQuad.forwardOffset());
    }

    public static Matrix4f freshMatrix(PanelQuad panelQuad) {
        Vec3d vec3d;
        if (panelQuad == null || panelQuad.panel() == null) {
            return panelQuad == null ? null : panelQuad.matrix();
        }
        RemoteGuiPanel remoteGuiPanel = panelQuad.panel();
        GuiShareRemoteState guiShareRemoteState = remoteGuiPanel.state();
        if (guiShareRemoteState == null || !WorldGuiCloseAnimation.hasCapturedWorldMatrices()) {
            return panelQuad.matrix();
        }
        Vec3d vec3d2 = vec3d = remoteGuiPanel.closeActive() ? remoteGuiPanel.resolvedAnchor() : GuiShareController.liveAnchorFor(guiShareRemoteState, remoteGuiPanel.smoothYaw(), remoteGuiPanel.smoothPitch());
        if (vec3d == null) {
            return panelQuad.matrix();
        }
        Matrix4f matrix4f = WorldGuiCloseAnimation.buildRemoteMatrix(RemoteGuiWorld.offsetForward(vec3d, remoteGuiPanel.resolvedPitch(), remoteGuiPanel.resolvedYaw(), panelQuad.forwardOffset()), remoteGuiPanel.resolvedYaw(), remoteGuiPanel.resolvedPitch(), 0.003139535f, remoteGuiPanel.animScale(), panelQuad.pivotX(), panelQuad.pivotY());
        return matrix4f != null ? matrix4f : panelQuad.matrix();
    }

    private static boolean viewerBehind(RemoteGuiPanel remoteGuiPanel) {
        return remoteGuiPanel != null && Boolean.TRUE.equals(behindSide.get(remoteGuiPanel));
    }

    private static Matrix4f buildMatrix(RemoteGuiPanel remoteGuiPanel, float f, float f2, float f3) {
        float f4 = 0.003139535f;
        return WorldGuiCloseAnimation.buildRemoteMatrix(RemoteGuiWorld.offsetForward(remoteGuiPanel.resolvedAnchor(), remoteGuiPanel.resolvedPitch(), remoteGuiPanel.resolvedYaw(), f3), remoteGuiPanel.resolvedYaw(), remoteGuiPanel.resolvedPitch(), f4, remoteGuiPanel.animScale(), f, f2);
    }

    static boolean popupVisible(RemoteGuiPanel remoteGuiPanel) {
        String string = remoteGuiPanel.popupDisplayed();
        return string != null && !string.isBlank() && remoteGuiPanel.popupT() > 0.01f;
    }

    public record PanelQuad(
        Matrix4f matrix,
        float width,
        float height,
        float u,
        float v,
        float uWidth,
        float vHeight,
        Shatter shatter,
        RemoteGuiPanel panel,
        float pivotX,
        float pivotY,
        float forwardOffset,
        SlotBlit blit,
        float alpha
    ) {
        public float opacity() { return alpha; }
    }

    public record SlotBlit(
        float srcX,
        float srcY,
        float srcW,
        float srcH,
        float clipX,
        float clipY,
        float clipW,
        float clipH
    ) {
        public float u() { return srcX; }
        public float v() { return srcY; }
        public float uWidth() { return srcW; }
        public float vHeight() { return srcH; }
        public float bgU() { return clipX; }
        public float bgV() { return clipY; }
        public float bgUWidth() { return clipW; }
        public float bgVHeight() { return clipH; }
    }

    public record Shatter(
        Object state,
        float progress,
        float alpha,
        float x,
        float y,
        float width,
        float height
    ) {}

    public record CardBlurTransform(
        float offsetX,
        float offsetY,
        float scaleX,
        float scaleY,
        float radiusScale
    ) {}

    public static final class CardBlurJob {
        public boolean ready;
        public float radius;
        public float boundsX, boundsY, boundsW, boundsH;
        public float originX, originY;
        public int cardCount;
        public int mask;

        public CardBlurJob(float x, float y, float w, float h, float radiusScale) {
            this.boundsX = x;
            this.boundsY = y;
            this.boundsW = w;
            this.boundsH = h;
            this.originX = x;
            this.originY = y;
            this.radius = radiusScale;
            this.ready = true;
        }

        public boolean add(float x, float y, float w, float h, float r, float a) {
            this.cardCount++;
            return true;
        }

        public void finish() {
            this.ready = true;
        }
    }
}

