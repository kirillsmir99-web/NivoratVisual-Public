package rtx.nv.api.modules.settings.impl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;

public class SelectSetting
extends Setting {
    private List<String> options = Collections.emptyList();
    private String selected = "";
    private String defaultSelected = "";
    private boolean defaultCaptured;

    public SelectSetting(String string, String string2) {
        super(string, string2);
    }

    public SelectSetting value(String ... stringArray) {
        this.options = Arrays.asList(stringArray);
        String string = this.selected = this.options.isEmpty() ? "" : this.options.get(0);
        if (!this.defaultCaptured) {
            this.defaultSelected = this.selected;
        }
        return this;
    }

    public String getValue() {
        return this.selected;
    }

    private static String normalizeOption(String string) {
        if (string == null) {
            return "";
        }
        return switch (string) {
            case "Монолит", "GLASS", "Glass", "NV · Стекло", "NV стекло" -> "Стекло";
            case "Осколки", "MOSAIC", "Mosaic" -> "Мозаика";
            case "Темы" -> "Палитра";
            case "Свой" -> "Акцент";
            case "Радуга", "Спектр", "Rainbow", "SPECTRUM" -> "Палитра";
            case "Горизонтальный" -> "Линия";
            case "Жидкие пятна" -> "Поток";
            case "По квадрату" -> "Углы";
            case "Обычный", "Призрак", "Проекция", "GHOST", "Ghost" -> "Свободный";
            case "Классический" -> "Карточка";
            case "Новый" -> "Панель";
            default -> string;
        };
    }

    public boolean is(String string) {
        if (this.selected.equals(string)) {
            return true;
        }
        return this.selected.equals(SelectSetting.normalizeOption(string));
    }

    public SelectSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    private void captureDefault() {
        if (!this.defaultCaptured) {
            this.defaultSelected = this.selected;
            this.defaultCaptured = true;
        }
    }

    public SelectSetting setSelected(String string) {
        return this.selected(string);
    }

    public String getSelected() {
        return this.selected;
    }

    public String getDefaultSelected() {
        return this.defaultSelected;
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(this.options);
    }

    public boolean isSelected(String string) {
        return this.is(string);
    }

    public SelectSetting selected(String string) {
        String normalized = SelectSetting.normalizeOption(string);
        if (this.options.contains(normalized)) {
            string = normalized;
        }
        if (this.options.contains(string)) {
            boolean bl = !this.selected.equals(string);
            this.selected = string;
            this.captureDefault();
            if (bl) {
                this.notifyChanged();
            }
        }
        return this;
    }
}
