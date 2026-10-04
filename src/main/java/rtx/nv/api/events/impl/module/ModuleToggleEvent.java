package rtx.nv.api.events.impl.module;
import rtx.nv.api.events.Event;
import rtx.nv.api.modules.Module;

public final class ModuleToggleEvent
extends Event {
    private final Module module;
    private final boolean enabled;

    public ModuleToggleEvent(Module module, boolean bl) {
        this.module = module;
        this.enabled = bl;
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}

