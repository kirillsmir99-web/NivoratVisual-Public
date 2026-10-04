package rtx.nv.api.drags.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.components.ListHudComp;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.HotKeysModule;
import rtx.nv.api.modules.settings.Setting;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.math.MathUtils;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;

public final class HotKeysComp extends ListHudComp {
    private static final String ICON_FONT = "nv";
    private static final int ICON_COLOR = -3355444;
    private static final String EXAMPLE_NAME = "Пример бинда";
    private static final String EXAMPLE_KEY = "[B] • Перекл";

    public HotKeysComp() {
        super("hotkeys", "Горячие клавиши", HotKeysModule.class, 5.0f, 33.0f);
    }

    @Override
    protected boolean hasHeader() {
        return false;
    }

    @Override
    protected String headerIconGlyph() {
        return null;
    }

    private static ListHudComp.IconDrawer categoryIcon(Category category) {
        if (category == null) {
            return null;
        }
        String glyph = NvIcons.forCategory(category);
        return (DrawContext drawContext, float x, float y, float size, float alpha) -> {
            float a = Math.max(0.0f, Math.min(1.0f, alpha));
            if (a <= 0.0039f) {
                return;
            }
            float[] bounds = Render2D.msdfBounds(ICON_FONT, glyph, size);
            if (bounds == null || bounds.length < 4) {
                return;
            }
            float px = x + (size - (bounds[2] - bounds[0])) * 0.5f - bounds[0];
            float py = y + (size - (bounds[3] - bounds[1])) * 0.5f - bounds[1];
            Render2D.msdfText(ICON_FONT, glyph, px, py, size, ColorUtil.multAlpha(ICON_COLOR, a));
        };
    }

    @Override
    protected List<ListHudComp.Row> collectRows() {
        List<ListHudComp.Row> list = new ArrayList<>();
        ModuleManager manager = ModuleManager.get();
        if (manager == null) {
            return list;
        }

        for (Module module : manager.getAll()) {
            if (module == null) continue;

            KeyBind bind = module.getBind();
            if (bind != null && bind.isBound()) {
                String modeTag = module.getBindMode() == Module.BindMode.HOLD ? "Удерж" : "Перекл";
                String val = "[" + MathUtils.shortBind(bind) + "] " + modeTag;
                int rowColor = module.isEnabled() ? -1 : 0x90B0B8C0;
                list.add(new ListHudComp.Row(module, module.getName(), val, null, null, rowColor));
            }

            for (Setting setting : module.getSettings().all()) {
                if (setting instanceof BindSetting bindSetting && bindSetting.isBound()) {
                    String modeTag = bindSetting.getType() == BindSetting.Type.HOLD ? "Удерж" : "Перекл";
                    String val = "[" + MathUtils.shortBind(bindSetting.getValue()) + "] " + modeTag;
                    String actionName = module.getName() + " " + bindSetting.getName();
                    list.add(new ListHudComp.Row(bindSetting, actionName, val, null, null, 0xFFE0E0E0));
                }
            }
        }

        list.sort(Comparator.comparing(r -> r.name().toLowerCase()));

        if (list.isEmpty() && DragSystem.get().isDragModeActive()) {
            list.add(new ListHudComp.Row("preview", EXAMPLE_NAME, "[B] Перекл", null, null, -1));
        }

        return list;
    }

    @Override
    protected float iconSlotSize() {
        return 5.5f;
    }
}
