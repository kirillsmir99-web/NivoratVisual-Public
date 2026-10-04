package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ButtonSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.modules.settings.impl.StringSetting;
import rtx.nv.api.modules.settings.impl.TextSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.Render2D;

public class ItemHighlight extends Module {
    private static ItemHighlight instance;

    private final SliderSetting opacity = this.register(new SliderSetting("\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", "\u041d\u0435\u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0444\u043e\u043d\u0430.").range(0, 100).increment(1).setValue(80.0f));

    // Раздел пользовательских предметов
    private final SeparatorSetting customSeparator = this.register(new SeparatorSetting("\u0421\u0432\u043e\u0438 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b"));
    private final TextSetting itemSearch = this.register(new TextSetting("\u041f\u043e\u0438\u0441\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430", "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 ID \u0438\u043b\u0438 \u0447\u0430\u0441\u0442\u044c \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044f \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430.").setPlaceholder("minecraft:diamond"));
    private final ButtonSetting addFromSearch = this.register(new ButtonSetting("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043f\u043e \u043f\u043e\u0438\u0441\u043a\u0443", "\u041d\u0430\u0439\u0442\u0438 \u0438 \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0432 \u0441\u043f\u0438\u0441\u043e\u043a.").onClick(this::addFromSearchAction));
    private final ButtonSetting addFromHand = this.register(new ButtonSetting("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0438\u0437 \u0440\u0443\u043a\u0438", "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0438\u0437 \u0433\u043b\u0430\u0432\u043d\u043e\u0439 \u0440\u0443\u043a\u0438.").onClick(this::addFromHandAction));
    private final SelectSetting selectedCustomItem = this.register(new SelectSetting("\u0412\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043f\u0440\u0435\u0434\u043c\u0435\u0442", "\u0421\u043f\u0438\u0441\u043e\u043a \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0434\u043b\u044f \u0443\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u044f.").value("\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442"));
    private final ButtonSetting removeSelected = this.register(new ButtonSetting("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442", "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430.").onClick(this::removeSelectedAction));
    private final ColorSetting customItemColor = this.register(new ColorSetting("\u0426\u0432\u0435\u0442 \u0441\u0432\u043e\u0438\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432", "\u0426\u0432\u0435\u0442 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438 \u0434\u043b\u044f \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432.", new Color(160, 100, 255)));
    private final ButtonSetting clearAll = this.register(new ButtonSetting("\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a", "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0432\u0441\u0435 \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0432\u0440\u0443\u0447\u043d\u0443\u044e \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b.").onClick(this::clearAllAction));
    private final StringSetting customItems = this.register(new StringSetting("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043d\u044b\u0435 ID", "\u0421\u043f\u0438\u0441\u043e\u043a ID \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u0437\u0430\u043f\u044f\u0442\u0443\u044e.", "", 4096));

    private final Set<String> customIdSet = new LinkedHashSet<>();
    private final Set<Identifier> customIdentifiers = new HashSet<>();
    private String cachedCustomSerialized = "";

    private final Map<Item, ItemHighlight.ItemEntry> itemEntries = new LinkedHashMap<>();

    public ItemHighlight() {
        super("ItemHighlight", "\u041f\u043e\u0434\u0441\u0432\u0435\u0447\u0438\u0432\u0430\u0435\u0442 \u0444\u043e\u043d \u044f\u0447\u0435\u0435\u043a \u0434\u043b\u044f \u043d\u0443\u0436\u043d\u044b\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432.", Category.VISUALS);
        instance = this;

        this.register(new SeparatorSetting("\u0411\u0430\u0437\u043e\u0432\u044b\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b"));
        this.addItem(Items.ENDER_PEARL, "Жемчуг Эндера", new Color(0, 150, 0));
        this.addItem(Items.SNOWBALL, "\u0421\u043d\u0435\u0436\u043e\u043a", new Color(100, 200, 255));
        this.addItem(Items.NETHERITE_SCRAP, "Незеритовый лом", new Color(200, 180, 150));
        this.addItem(Items.LANTERN, "\u0421\u0432\u0435\u0442\u0438\u043b\u044c\u043d\u0438\u043a", new Color(255, 150, 0));
        this.addItem(Items.FIRE_CHARGE, "Огненный заряд", new Color(255, 0, 0));
        this.addItem(Items.TOTEM_OF_UNDYING, "\u0422\u043e\u0442\u0435\u043c", new Color(255, 200, 0));
        this.addItem(Items.CROSSBOW, "\u0410\u0440\u0431\u0430\u043b\u0435\u0442", new Color(150, 100, 50));
        this.addItem(Items.NETHERITE_SWORD, "\u041d\u0435\u0437\u0435\u0440\u0438\u0442\u043e\u0432\u044b\u0439 \u043c\u0435\u0447", new Color(50, 50, 50));
        this.addItem(Items.CHORUS_FRUIT, "\u0425\u043e\u0440\u0443\u0441", new Color(200, 100, 200));
        this.addItem(Items.SUGAR, "\u0421\u0430\u0445\u0430\u0440", new Color(255, 255, 255));
        this.addItem(Items.PHANTOM_MEMBRANE, "\u041c\u0435\u043c\u0431\u0440\u0430\u043d\u0430", new Color(200, 180, 150));
        this.addItem(Items.ENDER_EYE, "\u041e\u043a\u043e \u044d\u043d\u0434\u0435\u0440\u0430", new Color(100, 0, 200));
        this.addItem(Items.DRIED_KELP, "\u041b\u0430\u043c\u0438\u043d\u0430\u0440\u0438\u044f", new Color(100, 150, 50));
        this.addItem(Items.EXPERIENCE_BOTTLE, "\u041f\u0443\u0437\u044b\u0440\u0435\u043a \u043e\u043f\u044b\u0442\u0430", new Color(0, 200, 100));
        this.addItem(Items.GOLDEN_APPLE, "\u0417\u043e\u043b\u043e\u0442\u043e\u0435 \u044f\u0431\u043b\u043e\u043a\u043e", new Color(255, 200, 0));
        this.addItem(Items.ENCHANTED_GOLDEN_APPLE, "\u0427\u0430\u0440. \u044f\u0431\u043b\u043e\u043a\u043e", new Color(255, 150, 0));
    }

    public static ItemHighlight getInstance() {
        ItemHighlight itemHighlight = ModuleManager.get().get(ItemHighlight.class);
        return itemHighlight != null ? itemHighlight : instance;
    }

    private void syncFromConfigIfNeeded() {
        String val = this.customItems.getValue();
        if (val == null) val = "";
        if (!val.equals(this.cachedCustomSerialized)) {
            this.cachedCustomSerialized = val;
            this.customIdSet.clear();
            this.customIdentifiers.clear();
            for (String part : val.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    this.customIdSet.add(trimmed);
                    Identifier id = Identifier.tryParse(trimmed);
                    if (id != null) {
                        this.customIdentifiers.add(id);
                    }
                }
            }
            this.refreshDropdownOptions();
        }
    }

    private void refreshDropdownOptions() {
        if (this.customIdSet.isEmpty()) {
            this.selectedCustomItem.value("\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442");
            this.selectedCustomItem.setSelected("\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442");
            return;
        }
        List<String> options = new ArrayList<>();
        for (String idStr : this.customIdSet) {
            Identifier id = Identifier.tryParse(idStr);
            if (id != null && Registries.ITEM.containsId(id)) {
                Item item = Registries.ITEM.get(id);
                String name = item.getName().getString();
                options.add(name + " [" + idStr + "]");
            } else {
                options.add(idStr);
            }
        }
        this.selectedCustomItem.value(options.toArray(new String[0]));
        if (!options.isEmpty()) {
            this.selectedCustomItem.setSelected(options.get(0));
        }
    }

    private void addCustomId(String idStr) {
        if (idStr == null || idStr.isBlank()) return;
        idStr = idStr.trim();
        this.syncFromConfigIfNeeded();
        if (this.customIdSet.contains(idStr)) return; // Исключить дубли
        this.customIdSet.add(idStr);
        Identifier id = Identifier.tryParse(idStr);
        if (id != null) {
            this.customIdentifiers.add(id);
        }
        this.cachedCustomSerialized = String.join(",", this.customIdSet);
        this.customItems.setText(this.cachedCustomSerialized);
        this.refreshDropdownOptions();
    }

    private void addFromHandAction() {
        if (this.mc.player == null) return;
        ItemStack stack = this.mc.player.getMainHandStack();
        if (stack.isEmpty()) return;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id != null && !id.equals(Registries.ITEM.getDefaultId())) {
            this.addCustomId(id.toString());
        }
    }

    private void addFromSearchAction() {
        String query = this.itemSearch.getValue();
        if (query == null || query.isBlank()) return;
        query = query.trim();

        // 1. Прямая попытка парсинга как Identifier
        Identifier direct = Identifier.tryParse(query.contains(":") ? query : "minecraft:" + query.toLowerCase());
        if (direct != null && Registries.ITEM.containsId(direct)) {
            this.addCustomId(direct.toString());
            this.itemSearch.setText("");
            return;
        }

        // 2. Поиск по путям и локализованным названиям в реестре
        for (Identifier regId : Registries.ITEM.getIds()) {
            Item item = Registries.ITEM.get(regId);
            String name = item.getName().getString();
            if (regId.getPath().equalsIgnoreCase(query) || name.equalsIgnoreCase(query) || name.toLowerCase().contains(query.toLowerCase())) {
                this.addCustomId(regId.toString());
                this.itemSearch.setText("");
                return;
            }
        }

        // 3. Если синтаксически корректный ID стороннего мода
        if (direct != null) {
            this.addCustomId(direct.toString());
            this.itemSearch.setText("");
        }
    }

    private void removeSelectedAction() {
        this.syncFromConfigIfNeeded();
        String sel = this.selectedCustomItem.getSelected();
        if (sel == null || sel.equals("\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442")) return;
        String idToRemove = sel;
        if (sel.contains("[") && sel.contains("]")) {
            idToRemove = sel.substring(sel.indexOf('[') + 1, sel.indexOf(']'));
        }
        if (this.customIdSet.remove(idToRemove)) {
            Identifier id = Identifier.tryParse(idToRemove);
            if (id != null) {
                this.customIdentifiers.remove(id);
            }
            this.cachedCustomSerialized = String.join(",", this.customIdSet);
            this.customItems.setText(this.cachedCustomSerialized);
            this.refreshDropdownOptions();
        }
    }

    private void clearAllAction() {
        this.customIdSet.clear();
        this.customIdentifiers.clear();
        this.cachedCustomSerialized = "";
        this.customItems.setText("");
        this.refreshDropdownOptions();
    }

    public int backgroundFor(ItemStack itemStack, boolean isHotbar) {
        if (!this.isEnabled() || itemStack == null || itemStack.isEmpty()) {
            return 0;
        }
        this.syncFromConfigIfNeeded();

        Identifier itemId = Registries.ITEM.getId(itemStack.getItem());
        if (itemId != null && (this.customIdentifiers.contains(itemId) || this.customIdSet.contains(itemId.toString()))) {
            return this.withOpacity(this.customItemColor.getColorOpaque() & 0xFFFFFF, this.customItemColor.getAlpha());
        }

        ItemHighlight.ItemEntry itemEntry = this.itemEntries.get(itemStack.getItem());
        if (itemEntry == null || !itemEntry.enabled().getValue()) {
            return 0;
        }
        int n2 = itemEntry.color().getColorOpaque() & 0xFFFFFF;
        return this.withOpacity(n2, itemEntry.color().getAlpha());
    }

    public void drawSlotBackground(DrawContext drawContext, int n, int n2, int n3) {
        if (drawContext == null || n3 == 0) {
            return;
        }
        drawContext.fill(n, n2, n + 16, n2 + 16, n3);
    }

    private void addItem(Item item, String string, Color color) {
        BooleanSetting booleanSetting = this.register(new BooleanSetting(string, "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 \u00ab" + string + "\u00bb.", true));
        ColorSetting colorSetting = this.register(new ColorSetting(string + " \u0446\u0432\u0435\u0442", "\u0426\u0432\u0435\u0442 \u0444\u043e\u043d\u0430 \u0434\u043b\u044f \u00ab" + string + "\u00bb.", new Color(color.getRGB() | 0xFF000000, true)).visibleWhen(() -> booleanSetting.getValue()));
        this.itemEntries.put(item, new ItemHighlight.ItemEntry(booleanSetting, colorSetting));
    }

    private int withOpacity(int n, float f) {
        float f2 = Math.max(0.0f, Math.min(1.0f, this.opacity.getFloat() / 100.0f));
        int n2 = Math.round(255.0f * f * f2);
        if (n2 <= 0) {
            return 0;
        }
        return n2 << 24 | n;
    }

    public void drawRoundedSlotBackground(float f, float f2, float f3, int n, float f4) {
        if (n == 0 || f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f5 = interfaceModule == null ? 2.0f : interfaceModule.rectCornerRadius.getFloat();
        f5 = Math.min(f5, f3 * 0.5f);
        this.drawRoundedSlotBackground(f, f2, f3, n, f4, f5, f5, f5, f5);
    }

    public void drawRoundedSlotBackground(float f, float f2, float f3, int n, float f4, float f5, float f6, float f7, float f8) {
        if (n == 0 || f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        float f9 = f3 * 0.5f;
        Render2D.rect(f, f2, f3, f3, Math.min(f5, f9), Math.min(f6, f9), Math.min(f7, f9), Math.min(f8, f9), ColorUtil.multAlpha(n, f4));
    }

    public static record ItemEntry(BooleanSetting enabled, ColorSetting color) {
    }
}
