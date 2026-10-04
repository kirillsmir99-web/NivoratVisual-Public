package rtx.nv.api.modules.settings.impl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;

public class MultiSelectSetting
extends Setting {
    private List<String> options = Collections.emptyList();
    private List<String> selected = new ArrayList<String>();
    private List<String> defaultSelected = new ArrayList<String>();
    private boolean defaultCaptured;
    private int minSelectedCount;

    public MultiSelectSetting(String string, String string2) {
        super(string, string2);
    }

    public MultiSelectSetting value(String ... stringArray) {
        this.options = Arrays.asList(stringArray);
        return this;
    }

    public Set<String> getValue() {
        return new LinkedHashSet<String>(this.selected);
    }

    public boolean is(String string) {
        return this.selected.contains(string);
    }

    public MultiSelectSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public boolean toggle(String string) {
        if (string == null || !this.options.contains(string)) {
            return false;
        }
        if (this.selected.contains(string)) {
            if (this.selected.size() <= this.minSelectedCount) {
                return false;
            }
            this.selected.remove(string);
        } else {
            this.selected.add(string);
        }
        this.notifyChanged();
        return true;
    }

    public List<String> getSelected() {
        return Collections.unmodifiableList(this.selected);
    }

    public List<String> getDefaultSelected() {
        return Collections.unmodifiableList(this.defaultSelected);
    }

    public int getMinSelectedCount() {
        return this.minSelectedCount;
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(this.options);
    }

    public boolean isSelected(String string) {
        return this.is(string);
    }

    public MultiSelectSetting selected(String ... stringArray) {
        ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(stringArray));
        boolean bl = !this.selected.equals(arrayList);
        this.selected = arrayList;
        if (!this.defaultCaptured) {
            this.defaultSelected = new ArrayList<String>(this.selected);
            this.defaultCaptured = true;
        }
        if (bl) {
            this.notifyChanged();
        }
        return this;
    }

    public MultiSelectSetting minSelectedCount(int n) {
        this.minSelectedCount = Math.max(0, n);
        return this;
    }
}

