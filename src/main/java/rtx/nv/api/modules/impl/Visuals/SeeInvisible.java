package rtx.nv.api.modules.impl.Visuals;

import net.minecraft.entity.Entity;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;

public final class SeeInvisible extends Module {
    private static SeeInvisible instance;
    private final BooleanSetting solid = this.register(new BooleanSetting("Solid", "Делает невидимок полностью видимыми", false));

    public SeeInvisible() {
        super("SeeInvisible", "Позволяет видеть невидимых игроков и сущностей.", Category.VISUALS);
        instance = this;
    }

    public static SeeInvisible getInstance() {
        SeeInvisible module = ModuleManager.get().get(SeeInvisible.class);
        return module != null ? module : instance;
    }

    public boolean shouldReveal(Entity entity) {
        return this.isEnabled();
    }

    public boolean isSolid() {
        return this.solid.getValue();
    }

    public int ghostTint() {
        return 0x26FFFFFF;
    }
}
