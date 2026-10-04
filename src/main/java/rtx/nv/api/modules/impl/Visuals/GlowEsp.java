package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.post.glowesp.GlowEspRenderer;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.storage.friend.FriendUtils;

public class GlowEsp extends Module {
    private static final String TARGET_PLAYERS = "Игроки";
    private static final String TARGET_MOBS = "Мобы";
    private static final String TARGET_SELF = "Себя";
    private static final String TARGET_FRIENDS = "Друзья";

    private static final String SHAPE_OUTLINE = "Контур";
    private static final String SHAPE_SILHOUETTE = "Силуэт";
    private static final String SHAPE_CORNERS = "Углы";

    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private static final int DARK_SECOND = new Color(16, 16, 16, 75).getRGB();
    private static GlowEsp instance;

    private final SeparatorSetting targetsSeparator = this.register(new SeparatorSetting("Фильтрация"));
    private final MultiSelectSetting targets = this.register(new MultiSelectSetting("Цели", "Кого подсвечивать в мире.")
        .value(TARGET_PLAYERS, TARGET_MOBS, TARGET_SELF, TARGET_FRIENDS)
        .selected(TARGET_PLAYERS, TARGET_MOBS, TARGET_FRIENDS));
    private final SliderSetting maxDistance = this.register(new SliderSetting("Дистанция", "Максимальное расстояние для отображения подсветки.")
        .range(8.0f, 128.0f).increment(2.0f).setValue(48.0f));

    private final SeparatorSetting shapeSeparator = this.register(new SeparatorSetting("Геометрия"));
    private final ModeSetting shapeMode = this.register(new ModeSetting("Форма", "Стиль геометрии подсветки.", SHAPE_OUTLINE, SHAPE_OUTLINE, SHAPE_SILHOUETTE, SHAPE_CORNERS));
    private final SliderSetting thickness = this.register(new SliderSetting("Толщина", "Толщина линий подсветки.")
        .range(0.5f, 5.0f).increment(0.1f).setValue(1.5f));
    private final SliderSetting opacity = this.register(new SliderSetting("Прозрачность", "Базовая прозрачность элементов подсветки.")
        .range(0.1f, 1.0f).increment(0.05f).setValue(0.85f));
    private final BooleanSetting throughWalls = this.register(new BooleanSetting("За препятствиями", "Отображать подсветку сквозь стены и препятствия.", true));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Режим окрашивания подсветки.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать второй цвет для градиента.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет подсветки.", new Color(120, 170, 255, 220)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет подсветки.", new Color(ColorUtil.lerpColor(new Color(120, 170, 255, 220).getRGB(), DARK_SECOND, 0.7f), true)));

    public GlowEsp() {
        super("ESP", "Подсвечивает сущности контуром, силуэтом или углами.", Category.VISUALS);
        instance = this;
        this.useSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    public static GlowEsp getInstance() {
        GlowEsp glowEsp = ModuleManager.get().get(GlowEsp.class);
        return glowEsp != null ? glowEsp : instance;
    }

    @Override
    protected void onDisable() {
        GlowEspRenderer.clear();
    }

    private int resolvePrimaryColor() {
        if (this.colorMode.is(COLOR_CLIENT)) {
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            int styleId = interfaceModule != null ? interfaceModule.gradientStyleId() : 0;
            float phase = ClientPalette.phase() * (styleId == 2 ? 20.0f : 1.0f);
            return ClientPalette.loopColor(phase) | 0xFF000000;
        }
        return this.customColor.getColor();
    }

    private int resolveSecondaryColor() {
        if (this.colorMode.is(COLOR_CLIENT)) {
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            int styleId = interfaceModule != null ? interfaceModule.gradientStyleId() : 0;
            float phase = ClientPalette.phase() * (styleId == 2 ? 20.0f : 1.0f);
            return ClientPalette.loopColor(phase + 0.35f) | 0xFF000000;
        }
        return this.useSecondColor.getValue() ? this.customSecondColor.getColor() : this.customColor.getColor();
    }

    private boolean shouldRenderPlayer(AbstractClientPlayerEntity player) {
        if (player.isSpectator()) {
            return false;
        }
        if (player == this.mc.player) {
            return this.targets.isSelected(TARGET_SELF) && this.mc.options.getPerspective() != Perspective.FIRST_PERSON;
        }
        if (!this.targets.isSelected(TARGET_PLAYERS)) {
            return false;
        }
        return this.targets.isSelected(TARGET_FRIENDS) || !FriendUtils.isFriend(player.getName().getString());
    }

    private boolean isOccluded(Vec3d cameraPos, LivingEntity entity) {
        if (this.mc.world == null || this.mc.player == null) return false;
        Vec3d targetEye = entity.getEyePos();
        BlockHitResult hit = this.mc.world.raycast(new RaycastContext(
            cameraPos, targetEye,
            RaycastContext.ShapeType.VISUAL,
            RaycastContext.FluidHandling.NONE,
            this.mc.player
        ));
        return hit != null && hit.getType() != HitResult.Type.MISS;
    }

    private boolean shouldRenderEntity(LivingEntity entity, Vec3d cameraPos) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        double maxDist = this.maxDistance.getFloat();
        if (entity.squaredDistanceTo(cameraPos) > maxDist * maxDist) {
            return false;
        }

        if (!this.throughWalls.getValue() && isOccluded(cameraPos, entity)) {
            return false;
        }

        if (entity instanceof AbstractClientPlayerEntity) {
            return this.shouldRenderPlayer((AbstractClientPlayerEntity) entity);
        }
        if (entity instanceof MobEntity) {
            return this.targets.isSelected(TARGET_MOBS) && !entity.isSpectator();
        }
        return false;
    }

    private List<LivingEntity> collectTargets(Frustum frustum, Vec3d cameraPos) {
        EntityRenderManager renderManager = this.mc.getEntityRenderDispatcher();
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : this.mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!this.shouldRenderEntity(living, cameraPos)) continue;
            if (!renderManager.shouldRender(living, frustum, cameraPos.x, cameraPos.y, cameraPos.z)) continue;
            result.add(living);
        }
        return result;
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }
        Vec3d cameraPos = event.getCamera() != null ? event.getCamera().getCameraPos() : this.mc.gameRenderer.getCamera().getCameraPos();
        Frustum frustum = new Frustum(event.getPositionMatrix(), event.getProjectionMatrix());
        frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);

        List<LivingEntity> targets = this.collectTargets(frustum, cameraPos);
        if (targets.isEmpty()) {
            return;
        }

        int primary = this.resolvePrimaryColor();
        int secondary = this.resolveSecondaryColor();
        boolean useSec = this.colorMode.is(COLOR_CLIENT) || this.useSecondColor.getValue();

        VertexConsumerProvider.Immediate consumers = this.mc.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrixStack = event.getStack();
        float tickDelta = (float) event.getPartialTicks();

        GlowEspRenderer.render(
            consumers,
            matrixStack,
            cameraPos,
            targets,
            tickDelta,
            this.shapeMode.getValue(),
            this.thickness.getFloat(),
            this.opacity.getFloat(),
            this.throughWalls.getValue(),
            primary,
            secondary,
            useSec
        );
    }
}
