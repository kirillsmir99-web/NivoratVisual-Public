package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;

public abstract class InterfaceComponentModule
extends Module {
    protected InterfaceComponentModule(String string, String string2) {
        this(string, string2, Category.DISPLAY);
    }

    protected InterfaceComponentModule(String name, String description, Category category) {
        super(name, description, category);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }
}
