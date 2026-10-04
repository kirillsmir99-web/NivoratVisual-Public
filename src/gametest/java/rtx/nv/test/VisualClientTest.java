package rtx.nv.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import rtx.nv.api.modules.Category;
import rtx.nv.api.ui.UI;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.events.Event;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import net.minecraft.text.Text;
import rtx.nv.api.modules.impl.Visuals.CustomPet;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.modules.impl.Visuals.Ambience;

/** In-process regression checks. Does not interact with a user's launcher profile or servers. */
public final class VisualClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitTicks(20);
        context.runOnClient(client -> verifyLifecycle());
        context.runOnClient(client -> NetworkBoundaryChecks.verify());
        context.runOnClient(client -> ConfigIoChecks.verify());
        context.runOnClient(client -> UxChecks.verify());
        context.runOnClient(client -> IdentityChecks.verify());
        MusicControlChecks.verify();
        context.takeScreenshot("main-menu");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(40);
            context.runOnClient(client -> UxChecks.verifyHands());
            context.runOnClient(client -> {
                Module module = Ambience.getInstance();
                if (module == null) return;
                var originalBind = module.getBind();
                var originalMode = module.getBindMode();
                boolean originalEnabled = module.isEnabled();
                try {
                    client.setScreen(null); module.disable();
                    module.setBind(new rtx.nv.utils.key.KeyBind(290));
                    module.setBindMode(Module.BindMode.HOLD);
                    ModuleManager.get().onKey(new rtx.nv.api.events.impl.input.KeyPressEvent(290,0,0,rtx.nv.api.events.impl.input.KeyPressEvent.Action.PRESS));
                    if (!module.isEnabled()) throw new AssertionError("HOLD press failed");
                    client.setScreen(UI.INSTANCE);
                    ModuleManager.get().onKey(new rtx.nv.api.events.impl.input.KeyPressEvent(290,0,0,rtx.nv.api.events.impl.input.KeyPressEvent.Action.RELEASE));
                    if (module.isEnabled()) throw new AssertionError("HOLD release lost in GUI");
                } finally {
                    module.disable(); module.setBind(originalBind); module.setBindMode(originalMode);
                    if (originalEnabled) module.enable(); client.setScreen(null);
                }
                rtx.nv.NV.LOGGER.info("[NV-TEST] HOLD release while GUI is open passed");
            });
            context.runOnClient(client -> {
                var helmet = new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_HELMET);
                helmet.setDamage(120);
                var chest = new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_CHESTPLATE);
                chest.setDamage(250);
                var legs = new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_LEGGINGS);
                legs.setDamage(80);
                var boots = new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_BOOTS);
                boots.setDamage(190);
                client.player.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, helmet);
                client.player.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, chest);
                client.player.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, legs);
                client.player.equipStack(net.minecraft.entity.EquipmentSlot.FEET, boots);

                var wm = ModuleManager.get().get(rtx.nv.api.modules.impl.Interface.WatermarkModule.class);
                if (wm != null) wm.enable();
                var armorMod = ModuleManager.get().get(rtx.nv.api.modules.impl.Interface.ArmorModule.class);
                if (armorMod != null) armorMod.enable();
                var hotkeysMod = ModuleManager.get().get(rtx.nv.api.modules.impl.Interface.HotKeysModule.class);
                if (hotkeysMod != null) hotkeysMod.enable();
                var arrayListMod = ModuleManager.get().get(rtx.nv.api.modules.impl.Interface.ArrayListModule.class);
                if (arrayListMod != null) {
                    arrayListMod.enable();
                    arrayListMod.style.selected("Планки");
                }
                var hotbarMod = ModuleManager.get().get(rtx.nv.api.modules.impl.Interface.CustomHotbar.class);
                if (hotbarMod != null) hotbarMod.enable();

                var bm = ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.BetterMinecraft.class);
                if (bm != null) bm.setBind(new rtx.nv.utils.key.KeyBind(net.minecraft.client.util.InputUtil.GLFW_KEY_V));
                var trails = ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.Trails.class);
                if (trails != null) trails.setBind(new rtx.nv.utils.key.KeyBind(net.minecraft.client.util.InputUtil.GLFW_KEY_B));
            });
            context.waitTicks(20);
            context.takeScreenshot("hud-redesign");
            context.takeScreenshot("world");
            context.runOnClient(client -> client.inGameHud.getChatHud().addMessage(Text.literal("NV regression: chat and player heads")));
            context.setScreen(() -> UI.INSTANCE);
            context.runOnClient(client -> UI.INSTANCE.selectCategoryFromWorkspace(Category.VISUALS));
            context.waitTicks(30);
            context.takeScreenshot("clickgui");
            context.runOnClient(client -> UI.INSTANCE.openModuleSettings(InterfaceModule.getInstance()));
            context.waitTicks(20);
            context.takeScreenshot("interface-settings");
            context.runOnClient(client -> InterfaceModule.getInstance().rectStyle.selected(InterfaceModule.STYLE_MOSAIC));
            context.waitTicks(120);
            context.takeScreenshot("mosaic");
            context.runOnClient(client -> {
                if (!InterfaceModule.getInstance().isMosaicStyle()) throw new AssertionError("Mosaic setting failed");
                rtx.nv.NV.LOGGER.info("[NV-TEST] Mosaic transition={}", InterfaceModule.getInstance().getStyleTransition());
                verifyPersistence();
            });
            context.runOnClient(client -> InterfaceModule.getInstance().rectStyle.selected(InterfaceModule.STYLE_GLASS));
            context.waitTicks(20);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                rtx.nv.api.modules.impl.Visuals.Cape cape = rtx.nv.api.modules.impl.Visuals.Cape.getInstance();
                if (cape != null) cape.disable();
                client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
            });
            context.waitTicks(20);
            context.takeScreenshot("vanilla-cape");
            context.runOnClient(client -> {
                rtx.nv.api.modules.impl.Visuals.Cape cape = rtx.nv.api.modules.impl.Visuals.Cape.getInstance();
                if (cape != null) cape.enable();
            });
            context.waitTicks(35);
            context.takeScreenshot("animated-cape");
            context.runOnClient(client -> {
                rtx.nv.api.modules.impl.Visuals.Wings wings = rtx.nv.api.modules.impl.Visuals.Wings.getInstance();
                if (wings != null) wings.enable();
            });
            context.waitTicks(20);
            context.takeScreenshot("wings-third-person-back");
            context.runOnClient(client -> client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT));
            context.waitTicks(20);
            context.takeScreenshot("wings-third-person-front");
            context.runOnClient(client -> {
                rtx.nv.api.modules.impl.Visuals.Wings wings = rtx.nv.api.modules.impl.Visuals.Wings.getInstance();
                if (wings != null) wings.disable();
            });
            context.runOnClient(client -> client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON));
            context.runOnClient(client -> {
                rtx.nv.api.modules.impl.Interface.MusicHudModule musicHud = ModuleManager.get().get(rtx.nv.api.modules.impl.Interface.MusicHudModule.class);
                if (musicHud != null) {
                    musicHud.enable();
                    musicHud.skyWords.setValue(true);
                    musicHud.wordsMode.selected("На блоке · 3D");
                    rtx.nv.api.music.lyrics.MusicSubtitlesConfig.INSTANCE.sync();
                    var line = new rtx.nv.api.music.lyrics.LyricLine(0L, 10000000L, "Nivorat Visual 3D Lyrics");
                    rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().clear();
                    client.player.setPitch(25);
                    var bubble = rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.createBlockBubble(line, false);
                    if (bubble != null) {
                        bubble.isLiveCaption = true;
                        bubble.liveDurationMs = 60000L;
                        rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().add(bubble);
                    }
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("lyrics-on-block");
            context.runOnClient(client -> {
                if (rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().isEmpty()) {
                    throw new AssertionError("3D Lyrics on block did not spawn");
                }
                var bubble = rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().get(0);
                if (!bubble.isSurfaceAttached) {
                    throw new AssertionError("3D Lyrics was not surface attached");
                }
                rtx.nv.NV.LOGGER.info("[NV-TEST] 3D Lyrics on block verified at {}", bubble.worldPos);
                rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().clear();
            });
            context.runOnClient(client -> {
                var lineAir = new rtx.nv.api.music.lyrics.LyricLine(0L, 10000000L, "3D Lyrics in Air at Eye Level");
                rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().clear();
                client.player.setPitch(0);
                var airBubble = rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.createWorldBubble(lineAir);
                if (airBubble != null) {
                    airBubble.isLiveCaption = true;
                    airBubble.liveDurationMs = 60000L;
                    rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().add(airBubble);
                    double eyeY = client.player.getEyeY();
                    if (airBubble.worldPos.y < eyeY - 0.35 || airBubble.worldPos.y > eyeY + 0.8) {
                        throw new AssertionError("Air bubble not at eye level: pos.y=" + airBubble.worldPos.y + ", eyeY=" + eyeY);
                    }
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("lyrics-air-eye-level");
            context.runOnClient(client -> {
                rtx.nv.api.music.lyrics.LyricsManager.INSTANCE.getActiveBubbles().clear();
            });
            for (Module module : ModuleManager.get().getAll()) {
                if (module.getName().equalsIgnoreCase("IRC") || module.getName().equalsIgnoreCase("Party") || module.getName().equalsIgnoreCase("Globals") || module.getName().equalsIgnoreCase("ClickGui")) continue;
                context.runOnClient(client -> {
                    module.disable();
                    module.enable();
                    if (!module.isEnabled()) throw new AssertionError("Enable failed: " + module.getName());
                });
                context.waitTicks(2);
                context.runOnClient(client -> {
                    module.disable();
                    if (module.isEnabled()) throw new AssertionError("Disable failed: " + module.getName());
                });
            }
            context.runOnClient(client -> rtx.nv.NV.LOGGER.info("[NV-TEST] Module lifecycle sweep passed (external services excluded)"));
            context.runOnClient(client -> client.inGameHud.getChatHud().clear(false));
            PostEffectChecks.verify(context);
            var target = context.computeOnClient(client -> {
                client.player.setYaw(0); client.player.setPitch(6);
                var pos = client.player.getEntityPos().add(0,0,3);
                var entity = new net.minecraft.entity.decoration.ArmorStandEntity(client.world,pos.x,pos.y,pos.z);
                client.world.addEntity(entity);
                var hit = ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.HitBubbles.class);
                hit.getSettings().all().stream().filter(s -> s.getName().equals("Длительность")).forEach(s -> ((rtx.nv.api.modules.settings.impl.NumberSetting)s).setValue(2000));
                hit.getSettings().all().stream().filter(s -> s.getName().equals("Подкрашивать цветом")).forEach(s -> ((rtx.nv.api.modules.settings.impl.BooleanSetting)s).setValue(true));
                hit.enable();
                var esp = ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.TargetESP.class);
                esp.getSettings().all().stream().filter(s -> s instanceof SelectSetting && s.getName().equals("Стиль")).forEach(s -> ((SelectSetting)s).selected("Окружность"));
                esp.enable();
                return entity;
            });
            context.waitTicks(5);
            long passes = context.computeOnClient(client -> rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer.renderedPasses());
            context.runOnClient(client -> EventBus.get().post(new rtx.nv.api.events.impl.player.AttackEntityEvent(target)));
            context.waitTicks(6);
            context.takeScreenshot("hitbubbles-target-circle");
            context.runOnClient(client -> {
                if (rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer.isDisabledAfterError() || rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer.renderedPasses() <= passes)
                    throw new AssertionError("HitBubbles attack produced no render pass");
                ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.HitBubbles.class).disable();
                ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.TargetESP.class).disable();
                client.world.removeEntity(target.getId(),net.minecraft.entity.Entity.RemovalReason.DISCARDED);
                rtx.nv.NV.LOGGER.info("[NV-TEST] HitBubbles attack render and TargetESP circle completed");
            });
            for (CustomPetVariant variant : new CustomPetVariant[]{CustomPetVariant.SPIRIT, CustomPetVariant.DRAGON, CustomPetVariant.MOTH, CustomPetVariant.MASCOT}) {
                CustomPetEntity pet = context.computeOnClient(client -> {
                    client.player.setYaw(0);
                    client.player.setPitch(6);
                    CustomPetEntity entity = new CustomPetEntity(client.world);
                    entity.setPetVariant(variant);
                    Vec3d pos = client.player.getEntityPos().add(0, .95, 2.4);
                    entity.snapTo(pos, 180);
                    entity.setBehavior(pos, 0, false, false, true, 1);
                    client.world.addEntity(entity);
                    return entity;
                });
                context.waitTicks(45);
                context.runOnClient(client -> {
                    var renderer = (rtx.nv.api.modules.impl.Visuals.custompet.render.CustomPetRenderer)(Object)client.getEntityRenderDispatcher().getRenderer(pet);
                    var state = renderer.getAndUpdateRenderState(pet,1f);
                    rtx.nv.NV.LOGGER.info("[NV-TEST] Pet {} entity yaw={}, body yaw={}, render yaw={}",variant,pet.getYaw(),pet.bodyYaw,state.bodyYaw);
                    float modelYaw = state.getOrDefaultGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.ENTITY_BODY_YAW,Float.NaN);
                    if (!Float.isFinite(modelYaw) || Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(modelYaw-state.bodyYaw))>.01f)
                        throw new AssertionError("Pet orientation missing from model render data");
                });
                context.takeScreenshot("pet-" + variant.name().toLowerCase(java.util.Locale.ROOT));
                context.runOnClient(client -> {
                    if (pet.isRemoved() || !pet.isAirborneMode()) throw new AssertionError("Fantasy pet state failed: " + variant);
                    client.world.removeEntity(pet.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
                });
            }
            context.runOnClient(client -> rtx.nv.NV.LOGGER.info("[NV-TEST] Fantasy pet model render sweep passed"));
            for (String type : new String[]{"Северное сияние", "Чёрная дыра", "Звездопад"}) {
                context.runOnClient(client -> {
                    Ambience ambience = Ambience.getInstance();
                    ambience.getSettings().all().stream().filter(s -> s instanceof rtx.nv.api.modules.settings.impl.BooleanSetting && s.getName().equals("Своё небо"))
                            .forEach(s -> ((rtx.nv.api.modules.settings.impl.BooleanSetting)s).setValue(true));
                    ambience.getSettings().all().stream().filter(s -> s instanceof rtx.nv.api.modules.settings.impl.ModeSetting && s.getName().equals("Тип неба"))
                            .forEach(s -> ((rtx.nv.api.modules.settings.impl.ModeSetting)s).selected(type));
                    ambience.enable();
                    client.player.setPitch(-45);
                });
                context.waitTicks(40);
                context.takeScreenshot("sky-" + Math.abs(type.hashCode()));
            }
            context.runOnClient(client -> Ambience.getInstance().disable());
            var reload = context.computeOnClient(client -> client.reloadResources());
            context.waitFor(client -> reload.isDone(), 1200);
            if (reload.isCompletedExceptionally()) throw new AssertionError("Resource reload failed");
            context.runOnClient(client -> rtx.nv.NV.LOGGER.info("[NV-TEST] Resource reload and three sky modes completed"));
            context.setScreen(() -> UI.INSTANCE);
            context.runOnClient(client -> UI.INSTANCE.openModuleSettings(InterfaceModule.getInstance()));
            for (int[] size : new int[][]{{854,480}, {1280,720}, {1920,1080}}) {
                context.runOnClient(client -> client.getWindow().setWindowedSize(size[0],size[1]));
                context.waitTicks(45);
                context.takeScreenshot("gui-" + size[0] + "x" + size[1]);
                context.runOnClient(client -> {
                    float scale = rtx.nv.utils.render.render2d.Render2DCoordinateSpace.designGuiScale();
                    if (UI.panelW() * scale > client.getWindow().getFramebufferWidth()) throw new AssertionError("GUI overflows viewport");
                });
            }
            context.runOnClient(client -> {
                client.getWindow().setWindowedSize(1280, 720);
                UI.INSTANCE.openModuleSettings(rtx.nv.api.modules.impl.Visuals.Wings.getInstance());
            });
            context.waitTicks(30);
            context.runOnClient(client -> {
                float f2 = UI.panelX();
                float f3 = UI.panelY();
                float prevX = f2 - rtx.nv.api.ui.WingPreviewRenderer.WIDTH - 8.0f;
                float prevY = f3 + 26.0f;
                UI.INSTANCE.mouseScrolled(prevX + 50, prevY + 50, 0, 3.0);
            });
            context.waitTicks(20);
            context.takeScreenshot("preview-wings");
            context.runOnClient(client -> {
                UI.INSTANCE.openModuleSettings(rtx.nv.api.modules.impl.Visuals.CustomPet.getInstance());
            });
            context.waitTicks(30);
            context.runOnClient(client -> {
                float f2 = UI.panelX();
                float f3 = UI.panelY();
                float prevX = f2 - rtx.nv.api.ui.PetPreviewRenderer.WIDTH - 8.0f;
                float prevY = f3 + 26.0f;
                UI.INSTANCE.mouseScrolled(prevX + 50, prevY + 50, 0, 3.0);
            });
            context.waitTicks(20);
            context.takeScreenshot("preview-pet-moth");
            context.runOnClient(client -> {
                SelectSetting petSetting = (SelectSetting)CustomPet.getInstance().getSettings().all().stream()
                        .filter(s -> s instanceof SelectSetting).findFirst().orElseThrow();
                petSetting.selected("Маскот NV");
            });
            context.waitTicks(20);
            context.takeScreenshot("preview-pet-mascot");
            context.runOnClient(client -> {
                SelectSetting petSetting = (SelectSetting)CustomPet.getInstance().getSettings().all().stream()
                        .filter(s -> s instanceof SelectSetting).findFirst().orElseThrow();
                petSetting.selected("Дух");
            });
            context.waitTicks(20);
            context.takeScreenshot("preview-pet-spirit");
            context.setScreen(() -> null);
        }
    }

    private static void verifyPersistence() {
        SelectSetting pet = (SelectSetting)CustomPet.getInstance().getSettings().all().stream()
                .filter(s -> s instanceof SelectSetting).findFirst().orElseThrow();
        pet.selected("Светящийся мотылёк");
        CustomPet.getInstance().enable();
        ConfigManager.saveProfile("NV-regression");
        pet.selected("Дух");
        CustomPet.getInstance().disable();
        InterfaceModule.getInstance().rectStyle.selected(InterfaceModule.STYLE_GLASS);
        ConfigManager.loadProfile("NV-regression");
        if (!pet.is("Светящийся мотылёк") || !CustomPet.getInstance().isEnabled() || !InterfaceModule.getInstance().isMosaicStyle())
            throw new AssertionError("Profile round trip failed");
        CustomPet.getInstance().disable();
        InterfaceModule.getInstance().rectStyle.selected("Монолит");
        if (!InterfaceModule.getInstance().rectStyle.is(InterfaceModule.STYLE_GLASS)) throw new AssertionError("Legacy material migration failed");
        InterfaceModule.getInstance().rectStyle.selected("Осколки");
        if (!InterfaceModule.getInstance().isMosaicStyle()) throw new AssertionError("Legacy Mosaic migration failed");
        rtx.nv.NV.LOGGER.info("[NV-TEST] Profile persistence and legacy material migration passed");
        rtx.nv.api.modules.settings.impl.SliderSetting numeric = new rtx.nv.api.modules.settings.impl.SliderSetting("fixture", "finite values").range(0f, 10f).setValue(5);
        numeric.setValue(Float.NaN);
        if (!Float.isFinite(numeric.getValue())) throw new AssertionError("Nonfinite numeric setting accepted");
    }

    private static void verifyLifecycle() {
        EventBus bus = EventBus.get();
        Probe probe = new Probe();
        bus.subscribe(probe);
        bus.subscribe(probe);
        bus.post(new AuditEvent());
        if (probe.calls != 1) throw new AssertionError("Duplicate event subscription");
        bus.unsubscribe(probe);
        bus.post(new AuditEvent());
        if (probe.calls != 1) throw new AssertionError("Unsubscribe failed");
        FailingModule module = new FailingModule();
        module.failEnable = true;
        module.enable();
        bus.post(new AuditEvent());
        if (module.isEnabled() || module.calls != 0) throw new AssertionError("Failed enable did not roll back");
        module.failEnable = false;
        module.enable();
        bus.post(new AuditEvent());
        module.disable();
        bus.post(new AuditEvent());
        if (module.isEnabled() || module.calls != 1) throw new AssertionError("Failed disable leaked listener");
        rtx.nv.NV.LOGGER.info("[NV-TEST] Failure rollback and event idempotency passed");
    }

    private static final class AuditEvent extends Event {}
    private static class Probe {
        int calls;
        @EventHandler public void onEvent(AuditEvent event) { calls++; }
    }
    private static final class FailingModule extends Module {
        boolean failEnable;
        int calls;
        FailingModule() { super("NV regression fixture", "Lifecycle rollback", Category.UTILS); }
        @Override protected void onEnable() { if (failEnable) throw new IllegalStateException("intentional test failure"); }
        @Override protected void onDisable() { throw new IllegalStateException("intentional test failure"); }
        @EventHandler public void onEvent(AuditEvent event) { calls++; }
    }
}
