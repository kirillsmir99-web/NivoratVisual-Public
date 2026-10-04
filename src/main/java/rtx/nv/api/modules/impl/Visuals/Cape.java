package rtx.nv.api.modules.impl.Visuals;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.utils.storage.friend.FriendUtils;

public class Cape extends Module {
    private static Cape instance;

    private static final String MODE_SELF = "Только себя";
    private static final String MODE_FRIENDS = "Себя и друзей";
    private static final String MODE_ALL = "Всех";

    private static final String STYLE_LAUNCHER = "Лаунчер";
    private static final String STYLE_THEME = "Тема";
    private static final String STYLE_STATIC = "Статичный";

    private final ModeSetting style = this.register(new ModeSetting("Стиль", "Цветовая схема и оформление плаща.", STYLE_LAUNCHER, STYLE_LAUNCHER, STYLE_THEME, STYLE_STATIC));
    private final ModeSetting targetMode = this.register(new ModeSetting("Отображать для", "Выбор игроков для отрисовки плаща.", MODE_SELF, MODE_SELF, MODE_FRIENDS, MODE_ALL));
    private final BooleanSetting smoothPhysics = this.register(new BooleanSetting("Плавная физика", "Плавная физика колыхания ткани плаща при движении.", true));
    private final BooleanSetting emissive = this.register(new BooleanSetting("Свечение эмблемы", "Яркое свечение логотипа Nivorat без затенения.", true));

    public Cape() {
        super("Cape", "Кастомный анимированный плащ с фирменным стилем лаунчера.", Category.VISUALS);
        instance = this;
    }

    @Override
    public String getDefaultDisplayName() {
        return "Плащ";
    }

    public static Cape getInstance() {
        Cape mod = ModuleManager.get().get(Cape.class);
        return mod != null ? mod : instance;
    }

    public boolean shouldRender(AbstractClientPlayerEntity player) {
        if (!this.isEnabled() || player == null || this.mc.player == null) {
            return false;
        }
        if (this.targetMode.is(MODE_ALL)) {
            return true;
        }
        if (this.targetMode.is(MODE_FRIENDS)) {
            return player == this.mc.player || FriendUtils.isFriend(player.getName().getString());
        }
        return player == this.mc.player || player.getUuid().equals(this.mc.player.getUuid());
    }

    public String getStyle() {
        return this.style.getValue();
    }

    public boolean isSmoothPhysics() {
        return this.smoothPhysics.getValue();
    }

    public boolean isEmissive() {
        return this.emissive.getValue();
    }
}
