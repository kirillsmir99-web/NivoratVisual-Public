package rtx.nv.api.modules.impl.Interface;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.impl.Interface.InterfaceComponentModule;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.CategoryFilterSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;

public final class ArrayListModule
extends InterfaceComponentModule {
    public final rtx.nv.api.modules.settings.impl.ModeSetting style = this.register(new rtx.nv.api.modules.settings.impl.ModeSetting("Стиль", "Стиль оформления списка модулей.", "Планки", "Планки", "Минимализм"));
    public final rtx.nv.api.modules.settings.impl.BooleanSetting sideBar = this.register(new rtx.nv.api.modules.settings.impl.BooleanSetting("Боковая полоса", "Акцентная светящаяся полоска сбоку каждой строки.", true));
    private final SliderSetting waveSpeedSetting = this.register(new SliderSetting("Скорость волны", "Скорость перелива: больше быстрее.").setValue(18.0f).range(1, 100).increment(1));
    private final MultiSelectSetting categories = this.register(new CategoryFilterSetting("Категории", "Модули каких категорий показывать в списке.").value(Category.VISUALS.getDisplayName(), Category.DISPLAY.getDisplayName()).selected(Category.VISUALS.getDisplayName(), Category.DISPLAY.getDisplayName()));

    public ArrayListModule() {
        super("ArrayList", "Перемещаемый список включённых модулей.");
    }

    public double waveSpeed() {
        return (double)this.waveSpeedSetting.getInt() * 1.0E-4;
    }

    public boolean categoryShown(Category category) {
        if (category == null) return false;
        if (this.categories.is(category.getDisplayName()) || this.categories.is(category.toString()) || this.categories.is(category.name())) return true;
        return switch (category) {
            case VISUALS -> categories.is("Визуал") || categories.is("Визуалы") || categories.is("Визуальные") || categories.is("Visuals");
            case DISPLAY -> categories.is("Display") || categories.is("Интерфейс") || categories.is("Отображение");
            default -> false;
        };
    }
}

