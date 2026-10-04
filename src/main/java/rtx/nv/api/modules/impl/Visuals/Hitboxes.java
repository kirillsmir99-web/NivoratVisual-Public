package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.ClientPalette;

public final class Hitboxes extends Module {
    private static final String TARGET_PLAYERS = "Игроки";
    private static final String TARGET_MOBS = "Мобы";
    private static final String TARGET_SELF = "Себя";

    private static final String MODE_OUTLINE = "Контур";
    private static final String MODE_FILL = "Заливка";
    private static final String MODE_BOTH = "Оба";

    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private static Hitboxes instance;

    private final MultiSelectSetting targets = this.register(new MultiSelectSetting("Цели", "Чьи хитбоксы оформлять.").value(TARGET_PLAYERS, TARGET_MOBS, TARGET_SELF).selected(TARGET_PLAYERS, TARGET_MOBS));
    private final ModeSetting mode = this.register(new ModeSetting("Режим", "Контур, заливка или оба варианта.", MODE_OUTLINE, MODE_OUTLINE, MODE_FILL, MODE_BOTH));
    private final SliderSetting lineWidth = this.register(new SliderSetting("Толщина линий", "Ширина контура в пикселях.").range(0.5f, 4.0f).increment(0.1f).setValue(1.5f));
    private final SliderSetting fillOpacity = this.register(new SliderSetting("Прозрачность заливки", "Интенсивность цвета заливки.").range(0.0f, 1.0f).increment(0.05f).setValue(0.2f).visible(() -> !this.mode.is(MODE_OUTLINE)));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет корпуса"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Выбор палитры для корпуса.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Пользовательский оттенок корпуса.", new Color(70, 170, 255, 255)).visible(() -> this.colorMode.is(COLOR_CUSTOM)));

    private final SeparatorSetting eyesSeparator = this.register(new SeparatorSetting("Уровень глаз"));
    private final BooleanSetting showEyes = this.register(new BooleanSetting("Уровень глаз", "Линия высоты взгляда.", true));
    private final ColorSetting eyeColor = this.register(new ColorSetting("Цвет уровня глаз", "Оттенок линии взгляда.", new Color(255, 64, 64, 255)).visible(this.showEyes::getValue));

    private final SeparatorSetting lookSeparator = this.register(new SeparatorSetting("Направление взгляда"));
    private final BooleanSetting showLook = this.register(new BooleanSetting("Направление взгляда", "Стрелка направления взгляда.", true));
    private final ColorSetting lookColor = this.register(new ColorSetting("Цвет взгляда", "Оттенок стрелки взгляда.", new Color(70, 100, 255, 255)).visible(this.showLook::getValue));

    public Hitboxes() {
        super("Hitboxes", "Стилизует штатное debug отображение хитбоксов F3+B.", Category.VISUALS);
        instance = this;
    }

    public static Hitboxes getInstance() {
        Hitboxes mod = ModuleManager.get().get(Hitboxes.class);
        return mod != null ? mod : instance;
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null || entity instanceof CustomPetEntity || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        if (entity == this.mc.player) {
            return this.targets.isSelected(TARGET_SELF) && !this.mc.options.getPerspective().isFirstPerson();
        }
        if (entity.isInvisible() && entity.isInvisibleTo(this.mc.player)) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return this.targets.isSelected(TARGET_PLAYERS);
        }
        return entity instanceof MobEntity && this.targets.isSelected(TARGET_MOBS);
    }

    private int resolveBodyColor() {
        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] palette = ClientPalette.colors();
            if (palette != null && palette.length > 0) {
                return palette[0] | 0xFF000000;
            }
            InterfaceModule iface = InterfaceModule.getInstance();
            if (iface != null) {
                return iface.clientPrimaryColorOpaque();
            }
            return ColorUtil.rgba(70, 170, 255, 255);
        }
        return this.customColor.getColor();
    }

    public boolean drawStyledHitbox(Entity entity, float tickProgress, boolean inLocalServer) {
        if (!this.shouldRender(entity)) {
            return true;
        }

        Vec3d offset = entity.getLerpedPos(tickProgress).subtract(entity.getEntityPos());
        Box box = entity.getBoundingBox().offset(offset);

        int strokeColor = this.resolveBodyColor();
        float strokeW = this.lineWidth.getFloat();
        int fillColor = ColorUtil.multAlpha(strokeColor, this.fillOpacity.getFloat());

        DrawStyle bodyStyle;
        if (this.mode.is(MODE_FILL)) {
            bodyStyle = DrawStyle.filled(fillColor);
        } else if (this.mode.is(MODE_BOTH)) {
            bodyStyle = DrawStyle.filledAndStroked(strokeColor, strokeW, fillColor);
        } else {
            bodyStyle = DrawStyle.stroked(strokeColor, strokeW);
        }

        GizmoDrawing.box(box, bodyStyle);

        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            float width = Math.min(vehicle.getWidth(), entity.getWidth());
            Vec3d ridingPos = vehicle.getPassengerRidingPos(entity).add(offset);
            Box seatBox = new Box(
                ridingPos.x - (double) (width / 2.0f), ridingPos.y, ridingPos.z - (double) (width / 2.0f),
                ridingPos.x + (double) (width / 2.0f), ridingPos.y + 0.0625, ridingPos.z + (double) (width / 2.0f)
            );
            GizmoDrawing.box(seatBox, DrawStyle.stroked(ColorUtil.rgba(255, 230, 0, 255), strokeW));
        }

        if (this.showEyes.getValue() && entity instanceof LivingEntity living) {
            double eyeY = box.minY + (double) living.getStandingEyeHeight();
            Box eyeBox = new Box(box.minX, eyeY - 0.01, box.minZ, box.maxX, eyeY + 0.01, box.maxZ);
            GizmoDrawing.box(eyeBox, DrawStyle.stroked(this.eyeColor.getColor(), strokeW));
        }

        if (entity instanceof EnderDragonEntity dragon) {
            EnderDragonPart[] parts = dragon.getBodyParts();
            if (parts != null) {
                int dragonPartColor = ColorUtil.rgba(255, 60, 220, 255);
                DrawStyle partStyle;
                if (this.mode.is(MODE_FILL)) {
                    partStyle = DrawStyle.filled(ColorUtil.multAlpha(dragonPartColor, this.fillOpacity.getFloat()));
                } else if (this.mode.is(MODE_BOTH)) {
                    partStyle = DrawStyle.filledAndStroked(dragonPartColor, strokeW, ColorUtil.multAlpha(dragonPartColor, this.fillOpacity.getFloat()));
                } else {
                    partStyle = DrawStyle.stroked(dragonPartColor, strokeW);
                }

                for (EnderDragonPart part : parts) {
                    Vec3d partOffset = part.getLerpedPos(tickProgress).subtract(part.getEntityPos());
                    Box partBox = part.getBoundingBox().offset(partOffset);
                    GizmoDrawing.box(partBox, partStyle);
                }
            }
        }

        if (this.showLook.getValue()) {
            Vec3d eyePos = entity.getLerpedPos(tickProgress).add(0.0, (double) entity.getStandingEyeHeight(), 0.0);
            Vec3d lookVec = entity.getRotationVec(tickProgress).multiply(2.0);
            GizmoDrawing.arrow(eyePos, eyePos.add(lookVec), this.lookColor.getColor());
        }

        return true;
    }
}
