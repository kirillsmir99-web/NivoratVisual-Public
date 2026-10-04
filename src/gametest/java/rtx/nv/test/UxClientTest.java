package rtx.nv.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.music.*;
import rtx.nv.api.music.lyrics.*;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.theme.*;

/** User-visible flow verification with a deterministic local music helper and lyrics fixture. */
public final class UxClientTest implements FabricClientGameTest {
    private static Object field(Object owner, String name) {
        try { var f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void scrollTo(ClientGameTestContext context, ThemeEditorRenderer editor, float rowY) {
        context.runOnClient(client -> {
            float center = UI.panelY() + UI.CONTENT_Y_OFFSET + UI.CONTENT_HEIGHT * .5f;
            float x = UI.panelX() + UI.PANEL_W + 30;
            editor.mouseScrolled(x, center, -(rowY - center) / 18f);
        });
        context.waitTicks(25);
    }

    private static void verifySliders(ClientGameTestContext context, ThemeEditorRenderer editor) {
        scrollTo(context, editor, context.computeOnClient(client -> (float)field(editor, "lastAdvBtnY")));
        context.runOnClient(client -> {
            editor.mouseClicked((float)field(editor, "lastAdvBtnX") + 3, (float)field(editor, "lastAdvBtnY") + 3, 0);
            if (!(boolean)field(editor, "advancedOpen")) throw new AssertionError("Advanced foldout ignored click");
        });
        context.waitTicks(15);
        for (int i = 0; i < 6; i++) {
            final int index = i;
            scrollTo(context, editor, context.computeOnClient(client -> ((float[])field(editor, "lastSliderY"))[index]));
            context.runOnClient(client -> {
                float x = ((float[])field(editor, "lastSliderX"))[index];
                float y = ((float[])field(editor, "lastSliderY"))[index] + 1;
                float width = ((float[])field(editor, "lastSliderW"))[index];
                float bodyTop = UI.panelY() + UI.CONTENT_Y_OFFSET + 34;
                float bodyBottom = UI.panelY() + UI.CONTENT_Y_OFFSET + UI.CONTENT_HEIGHT - 28;
                if (width <= 0 || y < bodyTop || y >= bodyBottom) throw new AssertionError("Slider clipped after scrolling: " + index);
                if (editor.mouseClicked(x + width * .25, y, 1)) throw new AssertionError("Right click applied a slider");
                editor.mouseClicked(x + width * .25, y, 0);
                if ((int)field(editor, "draggingMode") != index + 4) throw new AssertionError("Wrong slider hitbox: " + index);
                editor.mouseReleased(x, y, 1);
                if (!editor.mouseDragged(x + width * .75, y, 0)) throw new AssertionError("Slider stopped after right release");
                var draft = editor.getDraft();
                float actual = switch (index) {
                    case 0 -> draft.getAlpha(); case 1 -> draft.getBlur() / 32;
                    case 2 -> draft.getGlow(); case 3 -> draft.getRefraction();
                    case 4 -> draft.getEdgeStrength(); default -> draft.getGradientAngle() / 360;
                };
                if (Math.abs(actual - .75) > .001) throw new AssertionError("Drag failed: " + index + "=" + actual);
                var live = ThemeManager.currentTheme();
                editor.mouseDragged(x + width * .75, y + 10, 0);
                if (ThemeManager.currentTheme() != live) throw new AssertionError("Unchanged slider rebuilt live theme");
                editor.mouseReleased(x, y, 0);
                if (editor.mouseDragged(x, y, 0)) throw new AssertionError("Drag survived release");
            });
        }
        context.runOnClient(client -> rtx.nv.NV.LOGGER.info("[NV-TEST] All six theme sliders: scroll, hitboxes, drag, right button and unchanged-value guards passed"));
    }

    @Override public void runTest(ClientGameTestContext context) {
        context.waitTicks(20);
        context.runOnClient(client -> UxChecks.verify());
        context.runOnClient(client -> IdentityChecks.verify());
        MusicControlChecks.verify();
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/now-playing", exchange -> {
                byte[] data = "{\"isActive\":true,\"title\":\"NV Test Song\",\"artist\":\"NV\",\"status\":\"playing\",\"positionMs\":1000,\"durationMs\":120000}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, data.length);
                try (var out = exchange.getResponseBody()) { out.write(data); }
            });
            server.createContext("/cover", exchange -> { exchange.sendResponseHeaders(204, -1); exchange.close(); });
            server.start(); MusicManager.get().getClient().start(server.getAddress().getPort());
        } catch (Exception e) { throw new AssertionError(e); }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender(); context.waitTicks(30);
            context.runOnClient(client -> {
                UxChecks.verifyHands();
                client.player.setYaw(0); client.player.setPitch(0);
                @SuppressWarnings("unchecked") var cache = (Map<String, List<LyricLine>>) field(LyricsManager.get(), "lyricsCache");
                cache.put("NV___NV Test Song", List.of(new LyricLine(0, 120000, "Твой Minecraft. Твой стиль.")));
                var music = ModuleManager.get().get(MusicHudModule.class);
                music.enable(); music.skyWords.setValue(true); music.style.setSelected("Квадратный");
                LyricsMotionChecks.verify(client);
            });
            for (String mode : new String[]{"Перед игроком · 3D", "На блоке · 3D", "На экране · 2D"}) {
                context.runOnClient(client -> {
                    client.setScreen(null);
                    ModuleManager.get().get(MusicHudModule.class).wordsMode.setSelected(mode);
                    if (mode.equals("На блоке · 3D")) {
                        var origin = client.player.getBlockPos();
                        for (int x = -3; x <= 3; x++) for (int y = 0; y < 5; y++)
                            client.world.setBlockState(origin.add(x, y, 4), net.minecraft.block.Blocks.STONE.getDefaultState());
                    }
                });
                context.waitTicks(20);
                context.runOnClient(client -> {
                    var bubbles = LyricsManager.get().getActiveBubbles();
                    if (bubbles.isEmpty()) throw new AssertionError("No lyrics in " + mode);
                    var bubble = bubbles.getFirst();
                    if (mode.equals("На экране · 2D") != (bubble.worldPos == null)) throw new AssertionError("Wrong lyrics geometry for " + mode);
                    if (mode.equals("На блоке · 3D") && !bubble.isSurfaceAttached) throw new AssertionError("Block lyrics not attached");
                    rtx.nv.NV.LOGGER.info("[NV-TEST] Lyrics mode {} produced {} bubbles", mode, bubbles.size());
                });
                context.takeScreenshot("lyrics-" + Math.abs(mode.hashCode()));
            }
            for (String style : new String[]{"Стеклянный", "Квадратный"}) {
                context.runOnClient(client -> {
                    ModuleManager.get().get(MusicHudModule.class).style.setSelected(style);
                    ModuleManager.get().get(MusicHudModule.class).autoHide.setValue(false);
                    ModuleManager.get().get(MusicHudModule.class).wordsMode.setSelected("На экране · 2D");
                });
                context.waitTicks(15); context.takeScreenshot("music-card-" + Math.abs(style.hashCode()));
            }
            context.runOnClient(client -> ModuleManager.get().get(MusicHudModule.class).disable());
            var follower = context.computeOnClient(client -> {
                var c = new rtx.nv.api.modules.impl.Visuals.custompet.control.CustomPetFollowerController();
                c.tick(client.player, rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant.MASCOT, false);
                c.getPet().snapTo(client.player.getEntityPos().add(.3, .95, .1), 180);
                return c;
            });
            for (int i = 0; i < 30; i++) {
                context.runOnClient(client -> {
                    var before = follower.getPet().getEntityPos();
                    follower.tick(client.player, rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant.MASCOT, false);
                    if (before.distanceTo(follower.getPet().getEntityPos()) > .001)
                        throw new AssertionError("Owner avoidance teleported pet");
                });
                var previous = context.computeOnClient(client -> follower.getPet().getEntityPos());
                context.waitTicks(1);
                context.runOnClient(client -> {
                    if (previous.distanceTo(follower.getPet().getEntityPos()) > .6)
                        throw new AssertionError("Pet avoidance exceeded continuous flight step");
                });
            }
            context.runOnClient(client -> {
                follower.getPet().snapTo(client.player.getEntityPos().add(0, .95, 2.4), 180);
                follower.getPet().setBehavior(follower.getPet().getEntityPos(), 0, false, false, true, 1);
                rtx.nv.NV.LOGGER.info("[NV-TEST] Mascot owner avoidance remained continuous for 30 ticks");
                ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.Crown.class).enable();
                client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
            });
            context.waitTicks(15); context.takeScreenshot("mascot-crystal-crown");
            context.runOnClient(client -> {
                follower.reset();
                client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
                ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.Crown.class).disable();
            });
            context.setScreen(() -> UI.INSTANCE);
            context.runOnClient(client -> UI.INSTANCE.selectCategoryFromWorkspace(Category.ABOUT));
            context.waitTicks(20); context.takeScreenshot("about-nv");
            context.runOnClient(client -> {
                var material = rtx.nv.api.modules.impl.Interface.InterfaceModule.getInstance();
                material.rectStyle.selected(material.STYLE_GLASS);
                material.waveEdgeEnabled.setValue(true);
                material.waveEdgeMotion.setValue(true);
            });
            context.waitTicks(30); context.takeScreenshot("wave-closed-contour");
            context.runOnClient(client -> rtx.nv.api.modules.impl.Interface.InterfaceModule.getInstance().rectStyle.selected(rtx.nv.api.modules.impl.Interface.InterfaceModule.STYLE_MOSAIC));
            context.waitTicks(100); context.takeScreenshot("large-mosaic-a");
            context.waitTicks(40); context.takeScreenshot("large-mosaic-b");
            context.runOnClient(client -> rtx.nv.api.modules.impl.Interface.InterfaceModule.getInstance().rectStyle.selected(rtx.nv.api.modules.impl.Interface.InterfaceModule.STYLE_GLASS));
            context.runOnClient(client -> UI.INSTANCE.selectCategoryFromWorkspace(Category.THEMES));
            context.waitTicks(15);
            var editor = context.computeOnClient(client -> ((ThemesRenderer)field(UI.INSTANCE, "themesRenderer")).getEditor());
            context.runOnClient(client -> editor.openNew()); context.waitTicks(15);
            context.takeScreenshot("theme-editor");
            verifySliders(context, editor);
            context.takeScreenshot("theme-material-sliders");
            context.runOnClient(client -> {
                editor.getDraft().setName("NV Flow Test"); editor.getDraft().setBlur(9); editor.getDraft().setGlow(.25f);
                float x = (float)field(editor, "lastSaveX"), y = (float)field(editor, "lastSaveY");
                if (!editor.mouseClicked(x + 3, y + 3, 0)) throw new AssertionError("Save click ignored");
                if (!(ThemeManager.currentTheme() instanceof CustomTheme theme) || !theme.displayName().equals("NV Flow Test") || theme.profile().blurRadius != 9)
                    throw new AssertionError("Save button lost draft material");
                CustomThemeManager.loadAll();
                CustomTheme restored = CustomThemeManager.getById(ThemeManager.currentThemeId());
                if (restored == null || restored.profile().blurRadius != 9) throw new AssertionError("Theme file lost saved material");
                rtx.nv.NV.LOGGER.info("[NV-TEST] Theme save button and disk reload retained edited material");
            });
        } finally { MusicManager.get().getClient().stop(); server.stop(0); }
    }
}
