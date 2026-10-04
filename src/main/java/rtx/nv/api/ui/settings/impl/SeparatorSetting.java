package rtx.nv.api.ui.settings.impl;

import rtx.nv.api.ui.settings.NvSectionHeader;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;

public class SeparatorSetting
implements Setting {
    public static final float HEIGHT = 18.0f;
    private final rtx.nv.api.modules.settings.impl.SeparatorSetting backend;

    public SeparatorSetting(rtx.nv.api.modules.settings.impl.SeparatorSetting separatorSetting) {
        this.backend = separatorSetting;
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    @Override
    public float height() {
        return 20.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        NvSectionHeader.render(f, f2, f3, this.backend.getName(), NvSectionHeader.Style.NORMAL, f4);
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        return false;
    }

    @Override
    public float preferredWidth() {
        return Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.0f) + 48.0f;
    }
}

