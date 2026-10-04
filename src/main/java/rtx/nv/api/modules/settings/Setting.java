package rtx.nv.api.modules.settings;
import java.util.function.Supplier;

public abstract class Setting {
    private final String name;
    private final String description;
    private Supplier<Boolean> visibilityCondition;
    private Runnable changeListener;

    protected Setting(String string) {
        this(string, "");
    }

    protected Setting(String string, String string2) {
        this.name = string;
        this.description = string2 == null ? "" : string2;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return rtx.nv.api.localization.Lang.translateSetting(this.name);
    }

    public String getDescription() {
        return rtx.nv.api.localization.Lang.translateSettingDesc(this.name, this.description);
    }

    public boolean isVisible() {
        return this.visibilityCondition == null || Boolean.TRUE.equals(this.visibilityCondition.get());
    }

    protected void setVisibilityCondition(Supplier<Boolean> supplier) {
        this.visibilityCondition = supplier;
    }

    public void setChangeListener(Runnable runnable) {
        this.changeListener = runnable;
    }

    protected final void notifyChanged() {
        if (this.changeListener != null) {
            this.changeListener.run();
        }
    }
}

