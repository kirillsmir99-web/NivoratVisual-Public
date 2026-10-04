package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;

public class ItemPhysics
extends Module {
    private static ItemPhysics instance;
    private final ModeSetting mode = this.register(new ModeSetting("\u0424\u0438\u0437\u0438\u043a\u0430", "\u0420\u0435\u0436\u0438\u043c \u0444\u0438\u0437\u0438\u043a\u0438 \u0432\u044b\u0431\u0440\u043e\u0448\u0435\u043d\u043d\u044b\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432.", "\u041e\u0431\u044b\u0447\u043d\u0430\u044f", "\u041e\u0431\u044b\u0447\u043d\u0430\u044f"));
    private final SeparatorSetting groundSeparator = this.register(new SeparatorSetting("\u041d\u0430 \u0437\u0435\u043c\u043b\u0435"));
    private final NumberSetting groundSize = this.register(new NumberSetting("\u0420\u0430\u0437\u043c\u0435\u0440", "\u041c\u0430\u0441\u0448\u0442\u0430\u0431 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0441 \u0444\u0438\u0437\u0438\u043a\u043e\u0439 \u0432 \u043f\u043e\u043b\u0451\u0442\u0435 \u0438 \u043d\u0430 \u0437\u0435\u043c\u043b\u0435.", 1.0, 0.5, 2.5, 0.05));

    public ItemPhysics() {
        super("Item Physics", "\u0414\u043e\u0431\u0430\u0432\u043b\u044f\u0435\u0442 \u0444\u0438\u0437\u0438\u043a\u0443 \u043f\u0430\u0434\u0435\u043d\u0438\u044f \u0432\u044b\u0431\u0440\u043e\u0448\u0435\u043d\u043d\u044b\u043c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430\u043c.", Category.VISUALS);
        instance = this;
    }

    public static ItemPhysics getInstance() {
        ItemPhysics itemPhysics = ModuleManager.get().get(ItemPhysics.class);
        return itemPhysics != null ? itemPhysics : instance;
    }

    public boolean isNormalMode() {
        return this.mode.is("\u041e\u0431\u044b\u0447\u043d\u0430\u044f");
    }

    public float groundItemScale() {
        return this.isEnabled() ? this.groundSize.getFloat() : 1.0f;
    }
}

