package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.color.ColorUtil;

public final class HitColor extends Module {
    public static final String COLOR_CLIENT = "Клиент";
    public static final String COLOR_CUSTOM = "Свой";

    private static HitColor instance;
    private static final Map<LivingEntityRenderState, Integer> renderTints = Collections.synchronizedMap(new WeakHashMap<>());

    private final ModeSetting colorMode = this.register(new ModeSetting(
        "Режим цвета",
        "Источник оттенка подкрашивания при получении урона.",
        COLOR_CLIENT,
        COLOR_CLIENT,
        COLOR_CUSTOM
    ));
    private final ColorSetting color = this.register(new ColorSetting(
        "Свой цвет",
        "Пользовательский цвет игрока при получении урона.",
        new Color(255, 90, 90, 255)
    ).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final SliderSetting intensity = this.register(new SliderSetting(
        "Интенсивность",
        "Степень насыщенности цвета при ударе."
    ).range(0.20f, 1.0f).increment(0.05f).setValue(0.75f));
    private final BooleanSetting playersOnly = this.register(new BooleanSetting(
        "Только игроки",
        "Окрашивать только игроков при получении урона.",
        true
    ));

    public HitColor() {
        super("Hit Color", "Меняет цвет существа при получении урона", Category.VISUALS);
        instance = this;
    }

    private static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    public static int color() {
        if (instance == null) return -1;
        int base;
        if (instance.colorMode.is(COLOR_CLIENT)) {
            base = ClientAccent.accentOpaque();
        } else {
            base = instance.color.getColor();
        }
        float alpha = instance.intensity.getFloat();
        int a = Math.round(alpha * 255.0f);
        return (base & 0x00FFFFFF) | (a << 24);
    }

    public static boolean shouldTint(LivingEntity livingEntity) {
        if (!HitColor.isActive() || livingEntity == null) {
            return false;
        }
        if (livingEntity.hurtTime <= 0 || livingEntity.isDead()) {
            return false;
        }
        if (instance.playersOnly.getValue()) {
            return livingEntity instanceof PlayerEntity;
        }
        return true;
    }

    public static Integer tintFor(LivingEntityRenderState state) {
        return HitColor.isActive() ? renderTints.get(state) : null;
    }

    @Override
    protected void onDisable() {
        renderTints.clear();
    }

    public static void captureTint(LivingEntityRenderState state, LivingEntity entity) {
        if (!HitColor.isActive()) {
            renderTints.remove(state);
            return;
        }
        if (HitColor.shouldTint(entity)) {
            renderTints.put(state, HitColor.color());
        } else {
            renderTints.remove(state);
        }
    }
}
