package rtx.nv.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import rtx.nv.ClientEdition;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Visuals.Hitboxes;
import rtx.nv.api.modules.impl.Visuals.ItemHighlight;
import rtx.nv.api.modules.impl.Visuals.ViewModel;
import rtx.nv.api.modules.impl.Visuals.Wings;
import rtx.nv.api.modules.impl.Interface.TargetHudModule;
import rtx.nv.api.ui.UI;
import rtx.nv.api.modules.settings.impl.StringSetting;
import rtx.nv.utils.render.wings.WingAnimation;
import rtx.nv.utils.render.wings.WingModelRegistry;
import java.util.List;
import java.util.Map;

public final class ProductClientTest implements FabricClientGameTest {
    private net.minecraft.util.math.BlockPos breakingPos;
    @Override public void runTest(ClientGameTestContext context) {
        // InteractionManager clears synthetic mining input each tick. Keep the isolated
        // fixture alive after that update, then dispatch the module's normal tick event.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (breakingPos == null || client.interactionManager == null) return;
            var interaction = (rtx.nv.mixin.accessor.MultiPlayerGameModeAccessor)client.interactionManager;
            interaction.nv_setDestroyBlockPos(breakingPos);
            interaction.nv_setDestroyProgress(.6f);
            interaction.nv_setDestroying(true);
            try {
                var tick = rtx.nv.api.modules.impl.Visuals.BlockOverlay.class.getDeclaredMethod("onTick", rtx.nv.api.events.impl.game.TickEvent.class);
                tick.setAccessible(true);
                tick.invoke(ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.BlockOverlay.class), new rtx.nv.api.events.impl.game.TickEvent(rtx.nv.api.events.impl.game.TickEvent.Phase.PRE));
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
        context.waitTicks(20);
        context.runOnClient(client -> {
            var manager = ModuleManager.get();
            if (manager.getAll().isEmpty()) throw new AssertionError("Module registry empty");
            for (var module : manager.getAll()) {
                if (ClientEdition.isTrial() && ModuleManager.isProModule(module)) throw new AssertionError("Pro module registered in Free");
            }
            Wings wings = manager.get(Wings.class);
            if (wings == null || WingModelRegistry.getModel(wings.getSelectedSkinId()) == null) throw new AssertionError("Default wings missing");
            if (ClientEdition.isTrial() && (!wings.getSelectedSkinId().equals("crystal") || WingModelRegistry.getModel("angel") != null)) throw new AssertionError("Free cosmetic restriction failed");
            WingAnimation animation = new WingAnimation(2, Map.of("wing", List.of(new WingAnimation.Keyframe(.25f, 0, 170, 0), new WingAnimation.Keyframe(1.75f, 0, -170, 0))));
            float before = animation.sampleBone("wing", 1.9999f)[1], after = animation.sampleBone("wing", .0001f)[1];
            float jump = net.minecraft.util.math.MathHelper.wrapDegrees(before-after);
            if (Math.abs(jump) > .1f) throw new AssertionError("Animation loop discontinuity: " + jump);
            var items = manager.get(ItemHighlight.class);
            items.enable();
            ((StringSetting)items.getSettings().all().stream().filter(s -> s instanceof StringSetting).findFirst().orElseThrow()).setText("minecraft:diamond,invalid id");
            if (items.backgroundFor(new ItemStack(Items.DIAMOND), false) == 0) throw new AssertionError("Custom item not highlighted");
            if (items.backgroundFor(new ItemStack(Items.DIRT), false) != 0) throw new AssertionError("Unselected item highlighted");
            rtx.nv.NV.LOGGER.info("[NV-PRODUCT] Edition, registry, cosmetic, loop and custom item checks passed: {}", ClientEdition.getEditionName());
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(30);
            context.runOnClient(client -> {
                var view = ViewModel.getInstance();
                view.enable(); view.mainX.setValue(.45f); view.mainY.setValue(-.2f);
                MatrixStack matrices = new MatrixStack();
                if (!ViewModel.apply(matrices, Hand.MAIN_HAND) || Math.abs(matrices.peek().getPositionMatrix().m30()-.45f) > .001f) throw new AssertionError("Hand translation not applied");
                UxChecks.verifyHands();
                view.enable();
                client.player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
                ModuleManager.get().get(Hitboxes.class).enable();
                ModuleManager.get().get(TargetHudModule.class).enable();
                client.player.setYaw(0); client.player.setPitch(0);
                var golem = EntityType.IRON_GOLEM.create(client.world, SpawnReason.COMMAND);
                Vec3d position = client.player.getEntityPos().add(0, 0, 3);
                golem.refreshPositionAndAngles(position.x, position.y, position.z, 180, 0);
                client.world.addEntity(golem);
                Wings.getInstance().enable();
            });
            context.waitTicks(25);
            context.takeScreenshot("product-hitbox-golem-hand");
            context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.ChatScreen("", false)));
            context.waitTicks(3);
            var cursor = context.computeOnClient(client -> new double[]{client.getWindow().getWidth()*.82, client.getWindow().getHeight()*.78});
            context.getInput().setCursorPos(cursor[0], cursor[1]);
            float handBefore = context.computeOnClient(client -> ViewModel.getInstance().mainX.getValue());
            context.getInput().holdShift();
            // Fabric TestInput constructs MouseInput with modifiers=0 even while
            // Shift is held. Dispatch the real modifier payload through Mouse.
            context.runOnClient(client -> ((net.fabricmc.fabric.mixin.client.gametest.input.MouseHandlerAccessor)client.mouse)
                .invokeOnMouseButton(client.getWindow().getHandle(), new net.minecraft.client.input.MouseInput(0, org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT), org.lwjgl.glfw.GLFW.GLFW_PRESS));
            context.runOnClient(client -> {
                try {
                var holding = ViewModel.class.getDeclaredField("holding"); holding.setAccessible(true);
                rtx.nv.NV.LOGGER.info("[NV-PRODUCT] Shift drag begin: holding={}, cursor=({},{}), screen=({},{}), setting={}", holding.get(ViewModel.getInstance()), rtx.nv.api.drags.Position.mouseX(), rtx.nv.api.drags.Position.mouseY(), rtx.nv.api.drags.Position.screenWidth(), rtx.nv.api.drags.Position.screenHeight(), ViewModel.getInstance().mainX.getValue());
                if (!holding.getBoolean(ViewModel.getInstance())) throw new AssertionError("Shift click did not grab hand");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            context.waitTicks(2);
            context.getInput().moveCursor(-45, -30);
            context.waitTicks(8);
            context.takeScreenshot("product-shift-hand-drag");
            context.runOnClient(client -> ((net.fabricmc.fabric.mixin.client.gametest.input.MouseHandlerAccessor)client.mouse)
                .invokeOnMouseButton(client.getWindow().getHandle(), new net.minecraft.client.input.MouseInput(0, org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT), org.lwjgl.glfw.GLFW.GLFW_RELEASE));
            context.getInput().releaseShift();
            context.runOnClient(client -> {
                if (Math.abs(ViewModel.getInstance().mainX.getValue()-handBefore) < .005f) throw new AssertionError("Shift hand drag did not move main hand");
                client.setScreen(null);
                rtx.nv.NV.LOGGER.info("[NV-PRODUCT] Shift mouse hand dragging passed");
            });
            context.runOnClient(client -> {
                client.setScreen(UI.INSTANCE);
                UI.INSTANCE.selectCategoryFromWorkspace(Category.VISUALS);
            });
            context.waitTicks(20);
            context.runOnClient(client -> UI.INSTANCE.getInspector().open(Wings.getInstance()));
            context.waitTicks(30);
            context.takeScreenshot("product-wing-preview");
            var skins = context.computeOnClient(client -> (rtx.nv.api.modules.settings.impl.SelectSetting)Wings.getInstance().getSettings().all().stream().filter(s -> s.getName().equals("Скин крыльев")).findFirst().orElseThrow());
            for (String skin : skins.getOptions()) {
                context.runOnClient(client -> {
                    skins.selected(skin);
                    if (WingModelRegistry.getModel(Wings.getInstance().getSelectedSkinId()) == null) throw new AssertionError("Wing model missing: " + skin);
                });
                context.waitTicks(3);
            }
            context.runOnClient(client -> rtx.nv.NV.LOGGER.info("[NV-PRODUCT] Cosmetic render sweep completed: {} variants", skins.getOptions().size()));
            if (!ClientEdition.isTrial()) {
                context.runOnClient(client -> {
                    rtx.nv.api.modules.impl.Visuals.CustomPet.getInstance().disable();
                    UI.INSTANCE.getInspector().open(rtx.nv.api.modules.impl.Visuals.CustomPet.getInstance());
                });
                var petKinds = context.computeOnClient(client -> (rtx.nv.api.modules.settings.impl.SelectSetting)rtx.nv.api.modules.impl.Visuals.CustomPet.getInstance().getSettings().all().stream().filter(s -> s.getName().equals("Питомец") && s instanceof rtx.nv.api.modules.settings.impl.SelectSetting).findFirst().orElseThrow());
                int entitiesBefore = context.computeOnClient(client -> { int count=0; for (var e : client.world.getEntities()) count++; return count; });
                for (String kind : petKinds.getOptions()) {
                    context.runOnClient(client -> petKinds.selected(kind));
                    context.waitTicks(25);
                    context.takeScreenshot("pet-preview-" + rtx.nv.api.modules.impl.Visuals.CustomPet.getInstance().getSelectedVariant().name().toLowerCase(java.util.Locale.ROOT));
                    context.runOnClient(client -> {
                        if (UI.layoutWidth() * rtx.nv.utils.render.render2d.Render2DCoordinateSpace.designGuiScale() > client.getWindow().getFramebufferWidth()+1) throw new AssertionError("Pet preview outside viewport");
                    });
                }
                context.runOnClient(client -> {
                    int count=0; for (var e : client.world.getEntities()) count++;
                    if (count != entitiesBefore) throw new AssertionError("Pet preview registered a world entity");
                    rtx.nv.NV.LOGGER.info("[NV-PRODUCT] Four detached pet previews passed with module disabled");
                });
            }
            context.runOnClient(client -> {
                client.setScreen(null);
                rtx.nv.NV.LOGGER.info("[NV-PRODUCT] World, hand render, hitbox, mob portrait and wing preview render completed");
            });
            var blockPos = context.computeOnClient(client -> client.player.getBlockPos().add(0, 1, 3));
            context.runOnClient(client -> {
                for (var entity : client.world.getEntities()) if (entity instanceof net.minecraft.entity.passive.IronGolemEntity) entity.discard();
                breakingPos = blockPos;
            });
            for (var block : new net.minecraft.block.Block[]{net.minecraft.block.Blocks.OAK_STAIRS, net.minecraft.block.Blocks.STONE_SLAB, net.minecraft.block.Blocks.OAK_FENCE}) {
                for (String style : new String[]{"Контур", "Космос", "Бездна"}) {
                    context.runOnClient(client -> {
                        client.world.setBlockState(blockPos, block.getDefaultState());
                        var interaction = (rtx.nv.mixin.accessor.MultiPlayerGameModeAccessor)client.interactionManager;
                        interaction.nv_setDestroyBlockPos(blockPos); interaction.nv_setDestroyProgress(.6f); interaction.nv_setDestroying(true);
                        var overlay = ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.BlockOverlay.class);
                        overlay.enable();
                        ((rtx.nv.api.modules.settings.impl.SelectSetting)overlay.getSettings().all().stream().filter(s -> s.getName().equals("Поверхность")).findFirst().orElseThrow()).selected(style);
                    });
                    context.waitTicks(8);
                    context.runOnClient(client -> {
                        try {
                            var field = rtx.nv.api.modules.impl.Visuals.BlockOverlay.class.getDeclaredField("lastShapeBoxes"); field.setAccessible(true);
                            var boxes = (List<?>)field.get(ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.BlockOverlay.class));
                            if (boxes.isEmpty()) throw new AssertionError("Block shape not rendered");
                            var expected = client.world.getBlockState(blockPos).getOutlineShape(client.world, blockPos).getBoundingBoxes();
                            if (boxes.size() != expected.size()) throw new AssertionError("Block shape component count mismatch");
                            for (int i=0; i<boxes.size(); i++) {
                                var box = (net.minecraft.util.math.Box)boxes.get(i);
                                var bounds = expected.get(i).offset(blockPos).expand(0.005);
                                if (box.minX < bounds.minX || box.minY < bounds.minY || box.minZ < bounds.minZ || box.maxX > bounds.maxX || box.maxY > bounds.maxY || box.maxZ > bounds.maxZ) throw new AssertionError("Block surface outside shape");
                            }
                        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                    });
                    context.takeScreenshot("block-" + net.minecraft.registry.Registries.BLOCK.getId(block).getPath() + "-" + style.hashCode());
                }
            }
            context.runOnClient(client -> {
                breakingPos = null;
                ((rtx.nv.mixin.accessor.MultiPlayerGameModeAccessor)client.interactionManager).nv_setDestroying(false);
                ModuleManager.get().get(rtx.nv.api.modules.impl.Visuals.BlockOverlay.class).disable();
                rtx.nv.NV.LOGGER.info("[NV-PRODUCT] Nine block surface render and geometry checks passed");
            });
        }
    }
}
