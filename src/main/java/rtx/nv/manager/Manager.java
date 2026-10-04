package rtx.nv.manager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.liteapi.LiteApiClient;
import rtx.nv.api.mods.chatanim.ChatAnimationMod;
import rtx.nv.api.mods.chatheads.ChatHeads;
import rtx.nv.api.mods.geckolib.GeckoLibClient;
import rtx.nv.api.mods.shulkerview.ShulkerViewMod;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetWarmup;
import rtx.nv.api.party.PartyClient;
import rtx.nv.utils.animations.AnimationUtil;
import rtx.nv.utils.discord.rpc.DiscordRPCManager;
import rtx.nv.utils.input.GuiMovementHandler;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.gif.GifRenderer;
import rtx.nv.utils.render.warmup.Load;
import rtx.nv.utils.render.warmup.Render2DWarmup;
import rtx.nv.api.music.MusicManager;
import rtx.nv.utils.sounds.SoundManager;
import rtx.nv.utils.storage.macro.MacroHandler;

public final class Manager {
    private Manager() {
    }

    public static void shutdown() {
        ConfigManager.saveAll();
        PartyClient.INSTANCE.stop();
        LiteApiClient.INSTANCE.stop();
        MusicManager.get().shutdown();
        GifRenderer.shutdown();
        rtx.nv.utils.render.post.customsky.CustomSkyRenderer.clear();
        rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer.clear();
        rtx.nv.utils.render.post.FullscreenPostPass.clearAll();
        Render2D.close();
    }

    public static void init() {
        Render2D.init();
        Render2DWarmup.init();
        AnimationUtil.init();
        GuiMovementHandler.init();
        rtx.nv.api.localization.LocalizationManager.init();
        rtx.nv.api.ui.theme.CustomThemeManager.init();
        ModuleManager.get().init();
        ConfigManager.init();
        CommandManager.get().init();
        MacroHandler.init();
        DragSystem.get().init();
        new GeckoLibClient().onInitializeClient();
        SoundManager.init();
        if (!rtx.nv.ClientEdition.isTrial()) {
            MusicManager.get().init();
        }
        LiteApiClient.INSTANCE.start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraftClient -> Manager.shutdown());

        if (!FabricLoader.getInstance().isModLoaded("shulkerboxtooltip")) {
            try {
                ShulkerViewMod.init();
            } catch (Throwable t) {
                // ignore
            }
        }
        if (!FabricLoader.getInstance().isModLoaded("chat_heads") && !FabricLoader.getInstance().isModLoaded("chatheads")) {
            try {
                ChatHeads.init();
            } catch (Throwable t) {
                // ignore
            }
        }
        if (!FabricLoader.getInstance().isModLoaded("chatanimation")) {
            try {
                ChatAnimationMod.init();
            } catch (Throwable t) {
                // ignore
            }
        }
        try {
            rtx.nv.api.mods.accountswitcher.IasService.ensureInitialized();
        } catch (Throwable t) {
            // ignore
        }
        Thread thread = new Thread(DiscordRPCManager::start, "NV-Discord-RPC-Init");
        thread.setDaemon(true);
        thread.start();
        MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
        Runnable runnable = () -> {
            Load.runStartupWarmup();
        };
        if (minecraftClient2 != null) {
            minecraftClient2.execute(runnable);
        } else {
            runnable.run();
        }
        Identifier identifier = Identifier.of((String)"nv", (String)"ui_font_rewarmup");
        ResourceLoader.get((ResourceType)ResourceType.CLIENT_RESOURCES).registerReloader(identifier, (ResourceReloader)((SynchronousResourceReloader)resourceManager -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient == null) {
                return;
            }
            minecraftClient.execute(() -> {
                rtx.nv.utils.render.post.customsky.CustomSkyRenderer.reset();
                rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer.reset();
                rtx.nv.utils.render.post.FullscreenPostPass.resetAll();
                boolean bl = !Load.initialReloadSeen;
                Load.initialReloadSeen = true;
                if (bl) {
                    Load.runStartupWarmup();
                    return;
                }
                Load.warmupFonts();
                try {
                    Render2DWarmup.reset();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    CustomPetWarmup.warmup();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
        }));
    }
}

