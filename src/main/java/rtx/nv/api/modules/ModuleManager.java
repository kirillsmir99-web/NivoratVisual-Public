package rtx.nv.api.modules;
import rtx.nv.api.events.EventHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.input.HotBarScrollEvent;
import rtx.nv.api.events.impl.input.KeyPressEvent;
import rtx.nv.api.events.impl.input.KeyPressEvent.Action;
import rtx.nv.api.events.impl.input.MouseButtonEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.Module.BindMode;
import rtx.nv.api.modules.impl.Interface.ArmorModule;
import rtx.nv.api.modules.impl.Interface.ArrayListModule;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.api.modules.impl.Interface.CooldownsModule;
import rtx.nv.api.modules.impl.Interface.CustomHotbar;
import rtx.nv.api.modules.impl.Interface.HPFocus;
import rtx.nv.api.modules.impl.Interface.HotKeysModule;
import rtx.nv.api.modules.impl.Interface.InfoModule;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Interface.InventoryModule;
import rtx.nv.api.modules.impl.Interface.KeyStrokesModule;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.modules.impl.Interface.NotificationsModule;
import rtx.nv.api.modules.impl.Interface.PotionsModule;
import rtx.nv.api.modules.impl.Interface.ScoreboardModule;
import rtx.nv.api.modules.impl.Interface.TargetHudModule;
import rtx.nv.api.modules.impl.Interface.WatermarkModule;
import rtx.nv.api.modules.impl.Utils.AutoCommands;
import rtx.nv.api.modules.impl.Utils.CameraSettings;
import rtx.nv.api.modules.impl.Utils.ClientSounds;
import rtx.nv.api.modules.impl.Utils.DeathCoords;
import rtx.nv.api.modules.impl.Utils.Discord;
import rtx.nv.api.modules.impl.Utils.FastExp;
import rtx.nv.api.modules.impl.Utils.Freelook;
import rtx.nv.api.modules.impl.Utils.Globals;
import rtx.nv.api.modules.impl.Utils.HandSwap;
import rtx.nv.api.modules.impl.Utils.HitSound;
import rtx.nv.api.modules.impl.Utils.ItemScroller;
import rtx.nv.api.modules.impl.Utils.Optimization;
import rtx.nv.api.modules.impl.Utils.Party;
import rtx.nv.api.modules.impl.Utils.Profiler;
import rtx.nv.api.modules.impl.Utils.ShulkerPreview;
import rtx.nv.api.modules.impl.Utils.StreamerMode;
import rtx.nv.api.modules.impl.Utils.TalTracker;
import rtx.nv.api.modules.impl.Utils.TestSettings;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.api.modules.impl.Visuals.AspectRatio;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;
import rtx.nv.api.modules.impl.Visuals.BlockOverlay;
import rtx.nv.api.modules.impl.Visuals.Cape;
import rtx.nv.api.modules.impl.Visuals.Crown;
import rtx.nv.api.modules.impl.Visuals.Wings;
import rtx.nv.api.modules.impl.Visuals.Crosshair;
import rtx.nv.api.modules.impl.Visuals.CustomPet;
import rtx.nv.api.modules.impl.Visuals.Emotions;
import rtx.nv.api.modules.impl.Visuals.FakePlayer;
import rtx.nv.api.modules.impl.Visuals.FogBlur;
import rtx.nv.api.modules.impl.Visuals.GlowEsp;
import rtx.nv.api.modules.impl.Visuals.HitBubbles;
import rtx.nv.api.modules.impl.Visuals.HitColor;
import rtx.nv.api.modules.impl.Visuals.HitParticles;
import rtx.nv.api.modules.impl.Visuals.Hitboxes;
import rtx.nv.api.modules.impl.Visuals.HpCounter;
import rtx.nv.api.modules.impl.Visuals.ItemHighlight;
import rtx.nv.api.modules.impl.Visuals.ItemPhysics;
import rtx.nv.api.modules.impl.Visuals.JumpCircle;
import rtx.nv.api.modules.impl.Visuals.KillEffect;
import rtx.nv.api.modules.impl.Visuals.NameTags;
import rtx.nv.api.modules.impl.Visuals.NoRender;
import rtx.nv.api.modules.impl.Visuals.ProjectileHelper;
import rtx.nv.api.modules.impl.Visuals.ShaderHands;
import rtx.nv.api.modules.impl.Visuals.SwingAnimation;
import rtx.nv.api.modules.impl.Visuals.TargetESP;
import rtx.nv.api.modules.impl.Visuals.Trails;
import rtx.nv.api.modules.impl.Visuals.ViewModel;
import rtx.nv.api.modules.impl.Visuals.WorldParticles;
import rtx.nv.api.modules.restrict.Server;
import rtx.nv.api.modules.restrict.ServerRestrictions;
import rtx.nv.utils.key.KeyBind;

public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<Module>();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean initialized;
    private final java.util.Map<Class<?>, Module> byType = new java.util.HashMap<>();
    private final java.util.Set<Module> heldModules = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private ModuleManager() {
    }

    public <T extends Module> T get(Class<T> clazz) {
        Module exact = this.byType.get(clazz);
        if (exact != null) return clazz.cast(exact);
        for (Module module : this.modules) {
            if (!clazz.isInstance(module)) continue;
            return (T)module;
        }
        return null;
    }

    public static ModuleManager get() {
        return INSTANCE;
    }

    public static boolean isProModule(Module module) {
        if (module == null) return false;
        Class<?> clazz = module.getClass();
        return clazz == Emotions.class
            || clazz == KillEffect.class
            || clazz == MusicHudModule.class
            || clazz == ShaderHands.class
            || clazz == Ambience.class
            || clazz == JumpCircle.class
            || clazz == HitBubbles.class
            || clazz == GlowEsp.class
            || clazz == TargetESP.class
            || clazz == Trails.class
            || clazz == ProjectileHelper.class
            || clazz == AutoCommands.class
            || clazz == Profiler.class
            || clazz == Party.class
            || clazz == TalTracker.class
            || clazz == Crosshair.class
            || clazz == BlockOverlay.class
            || clazz == ViewModel.class
            || clazz == KeyStrokesModule.class
            || clazz == CooldownsModule.class
            || clazz == HPFocus.class
            || clazz == WorldParticles.class;
    }

    private void register(Module ... moduleArray) {
        for (Module module : moduleArray) {
            if (module instanceof TestSettings && !net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) continue;
            if (rtx.nv.ClientEdition.isTrial() && isProModule(module)) continue;
            this.modules.add(module);
            this.byType.put(module.getClass(), module);
        }
    }

    public void init() {
        if (this.initialized) return;
        this.initialized = true;
        this.register(new HitSound(), new Ambience(), new BetterMinecraft(), new AspectRatio(), new ClickGui(), new CustomHotbar(), new CustomPet(), new FogBlur(), new HitBubbles(), new HpCounter(), new HitColor(), new Hitboxes(), new InterfaceModule(), new NotificationsModule(), new WatermarkModule(), new MusicHudModule(), new HotKeysModule(), new Profiler(), new TargetHudModule(), new PotionsModule(), new CooldownsModule(), new InfoModule(), new ArrayListModule(), new ArmorModule(), new InventoryModule(), new HPFocus(), new KeyStrokesModule(), new ScoreboardModule(), new ViewModel(), new Freelook(), new HandSwap(), new CameraSettings(), new ItemPhysics(), new JumpCircle(), new Crosshair(), new KillEffect(), new NameTags(), new GlowEsp(), new Crown(), new Wings(), new Cape(), new Emotions(), new BlockOverlay(), new FakePlayer(), new ShaderHands(), new ItemHighlight(), new NoRender(), new WorldParticles(), new HitParticles(), new ProjectileHelper(), new SwingAnimation(), new TargetESP(), new Trails(), new ShulkerPreview(),  new ClientSounds(), new DeathCoords(), new FastExp(), new TalTracker(), new ItemScroller(), new StreamerMode(), new TestSettings(), new AutoCommands(), new Optimization(), new Party(), new Globals(), new Discord());
        EventBus.get().subscribe(this);
    }

    public List<Module> getAll() {
        return Collections.unmodifiableList(this.modules);
    }

    @EventHandler
    public void onKey(KeyPressEvent keyPressEvent) {
        if (keyPressEvent.action == KeyPressEvent.Action.RELEASE) {
            this.applyKeyBound(keyPressEvent.keyCode, false);
            return;
        }
        if (this.shouldIgnoreBinds()) {
            return;
        }
        if (keyPressEvent.action == KeyPressEvent.Action.PRESS) {
            this.applyKeyBound(keyPressEvent.keyCode, true);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.currentScreen != null || this.mc.world == null || !this.mc.isWindowFocused()) {
            for (Module held : java.util.List.copyOf(this.heldModules)) held.disable();
            this.heldModules.clear();
        }
        EnumSet<Server> enumSet = ServerRestrictions.current();
        for (Module module : this.modules) {
            if (!module.isEnabled()) continue;
            if (ServerRestrictions.isHiddenBy(module, enumSet)) {
                module.disable();
                continue;
            }
            String string = ServerRestrictions.blockReason(module, enumSet);
            if (string == null) continue;
            module.disable();
            ServerRestrictions.notify(string);
        }
    }

    private boolean applyBind(Module module, boolean bl) {
        if (module.getBindMode() == Module.BindMode.HOLD) {
            if (bl) {
                module.enable();
                if (module.isEnabled()) this.heldModules.add(module);
            } else if (this.heldModules.remove(module)) {
                module.disable();
            }
            return true;
        }
        if (bl) {
            module.toggle();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onMouse(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.action == MouseButtonEvent.Action.RELEASE) {
            this.applyMouseBound(mouseButtonEvent.button, false);
            return;
        }
        if (this.shouldIgnoreBinds()) {
            return;
        }
        if (mouseButtonEvent.action == MouseButtonEvent.Action.PRESS) {
            this.applyMouseBound(mouseButtonEvent.button, true);
        }
    }

    public List<Module> getEnabled() {
        return this.modules.stream().filter(Module::isEnabled).toList();
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent hotBarScrollEvent) {
        if (this.shouldIgnoreBinds()) {
            return;
        }
        int n = hotBarScrollEvent.getVertical() > 0.0 ? 1000 : (hotBarScrollEvent.getVertical() < 0.0 ? 1001 : -1);
        if (n != -1 && this.toggleBound(n)) {
            hotBarScrollEvent.cancel();
        }
    }

    public List<Module> getByCategory(Category category) {
        return this.modules.stream().filter(module -> module.getCategory() == category).toList();
    }

    private boolean toggleBound(int n) {
        boolean bl = false;
        for (Module module : this.modules) {
            KeyBind keyBind;
            if (module instanceof ClickGui || !(keyBind = module.getBind()).isBound() || keyBind.getCode() != n) continue;
            module.toggle();
            bl = true;
        }
        return bl;
    }

    private boolean applyMouseBound(int n, boolean bl) {
        boolean bl2 = false;
        for (Module module : this.modules) {
            KeyBind keyBind = module.getBind();
            if (!keyBind.isBound() || !keyBind.matchesMouseButton(n)) continue;
            bl2 |= this.applyBind(module, bl);
        }
        return bl2;
    }

    private boolean applyKeyBound(int n, boolean bl) {
        boolean bl2 = false;
        for (Module module : this.modules) {
            KeyBind keyBind;
            if (module instanceof ClickGui || !(keyBind = module.getBind()).isBound() || keyBind.getCode() != n) continue;
            bl2 |= this.applyBind(module, bl);
        }
        return bl2;
    }

    private boolean shouldIgnoreBinds() {
        return this.mc == null || this.mc.currentScreen != null;
    }

    public Module findByName(String string) {
        if ("China Hat".equalsIgnoreCase(string)) string = "Crown";
        for (Module module : this.modules) {
            if (!module.getName().equalsIgnoreCase(string)) continue;
            return module;
        }
        return null;
    }
}

