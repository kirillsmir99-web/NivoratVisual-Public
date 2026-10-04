package rtx.nv.api.modules.impl.Utils.guishare;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Utils.guishare.GuiSharePopupRow;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareRemoteState;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteAvatarCache;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiPanel;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiWorld;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteTheme;
import rtx.nv.api.modules.restrict.Server;
import rtx.nv.api.modules.restrict.ServerRestrictions;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.theme.Theme;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.others.RoundedScissor;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.gif.GifRenderer;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class RemoteGuiPanelRenderer {
    public static final float NAME_H = 22.0f;
    public static final float PANEL_W = 430.0f;
    public static final float PANEL_H = 290.0f;
    public static final float GLOW_PAD = 80.0f;
    public static final float CONTENT_W = 590.0f;
    public static final float CONTENT_H = 472.0f;
    private static final float SIDEBAR_W = 110.0f;
    private static final float CONTENT_X_OFF = 117.0f;
    private static final float CONTENT_INSET = 122.0f;
    private static final float CONTENT_Y_OFFSET = 5.0f;
    private static final float CONTENT_HEIGHT = 280.0f;
    private static final float HEADER_OFFSET = 30.0f;
    private static final float CAT_COL_TOP = 34.0f;
    private static final float CAT_HEADER_H = 20.0f;
    private static final float CAT_SUB_GAP = 4.0f;
    private static final float CAT_SUB_ROW_H = 18.0f;
    private static final float CAT_OTHERS_GAP = 12.0f;
    private static final float CAT_EVENTS_GAP = 3.0f;
    private static final float CAT_OTHER_ROW_H = 19.0f;
    private static final float CARD_RADIUS = 6.0f;
    private static final float CARD_GAP = 5.0f;
    private static final float CARD_PAD = 5.0f;
    private static final float PAD_X = 8.0f;
    private static final float NAME_SIZE = 7.5f;
    private static final float NAME_TOP = 7.0f;
    private static final float TITLE_CY = 10.75f;
    private static final float DESC_SIZE = 6.0f;
    private static final float DESC_LINE_H = 7.0f;
    private static final float DESC_TOP = 19.0f;
    private static final float DESC_RIGHT_PAD = 4.0f;
    private static final float DESC_BOTTOM_PAD = 7.0f;
    private static final float NO_DESC_H = 22.0f;
    private static final float TOGGLE_RIGHT_PAD = 8.0f;
    private static final float GEAR_GAP = 7.0f;
    private static final float GEAR_SIZE = 7.0f;
    private static final String GEAR_GLYPH = NvIcons.SETTINGS;
    private static final float BADGE_H = 10.0f;
    private static final float TOGGLE_W = 14.692308f;
    private static final float TOGGLE_H = 8.0f;
    public static final float POPUP_MIN_WIDTH = 110.0f;
    public static final float POPUP_MAX_WIDTH = 180.0f;
    public static final float POPUP_MAX_HEIGHT = 290.0f;
    public static final float POPUP_OVERLAY_REACH = 100.0f;
    private static final float POPUP_RADIUS = 8.0f;
    private static final float POPUP_SIDE_PAD = 5.0f;
    private static final float POPUP_TOP_PAD = 7.0f;
    private static final float POPUP_BOTTOM_PAD = 7.0f;
    private static final float POPUP_EDGE = 4.0f;
    private static final float DROP_PAD = 6.0f;
    private static final float SV_W = 70.0f;
    private static final float SV_H = 50.0f;
    private static final float BAR_H = 4.0f;
    private static final float SV_GAP = 4.0f;
    private static final float DROP_RADIUS = 2.0f;
    private static final float LIST_ITEM_H = 12.0f;
    private static final float LIST_PAD = 3.0f;
    private static final float LIST_TEXT_SIZE = 5.5f;
    private static final int LIST_MAX_VISIBLE = 7;
    private static final float LIST_EDGE_FADE = 4.0f;
    private static final float LIST_EDGE_INSET = 1.0f;
    private static final float SERVER_ROW_H = 31.0f;
    private static final float SERVER_GAP = 3.0f;
    private static final float SERVER_BOTTOM_PAD = 18.0f;
    private static final long SERVER_SHIMMER_MS = 4400L;
    private static final String EVENT_ICON_FONT = "event-icons";
    private static final String EVENT_ICON_PICKAXE = "i";
    private static final float EVENT_ICON_SIZE = 14.5f;
    private static final Category[] MAIN_CATEGORIES = new Category[]{Category.VISUALS, Category.DISPLAY, Category.MEDIA, Category.UTILS};
    private static final Category[] OTHER_CATEGORIES = new Category[]{Category.THEMES};
    private static final String RU_LAYOUT = "\u0439\u0446\u0443\u043a\u0435\u043d\u0433\u0448\u0449\u0437\u0445\u044a\u0444\u044b\u0432\u0430\u043f\u0440\u043e\u043b\u0434\u0436\u044d\u044f\u0447\u0441\u043c\u0438\u0442\u044c\u0431\u044e\u0451";
    private static final String EN_LAYOUT = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";
    private static final Map<String, List<String>> descCache = new HashMap<String, List<String>>();
    private static final Map<String, Float> heightCache = new HashMap<String, Float>();
    private static final Map<String, List<Module>> filterCache = new HashMap<String, List<Module>>();
    private static final Map<String, Decelerate> enableAnims = new HashMap<String, Decelerate>();
    private static final Map<String, Decelerate> themeSelectAnims = new HashMap<String, Decelerate>();
    private static final Map<String, List<Setting>> popupWidgetsCache = new HashMap<String, List<Setting>>();
    private static long filterCacheResetAt;
    public static final int MODE_FULL = 0;
    public static final int MODE_PANEL_ONLY = 1;
    public static final int MODE_POPUP_ONLY = 2;

    private RemoteGuiPanelRenderer() {
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    private static Module findModule(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        for (Module module : ModuleManager.get().getAll()) {
            if (!module.getName().equalsIgnoreCase(string)) continue;
            return module;
        }
        return null;
    }

    private static int color(int n, int n2, int n3, int n4, float f) {
        int n5 = Math.max(0, Math.min(255, Math.round((float)n4 * f)));
        if (n5 <= 0) {
            return 0;
        }
        return new Color(n, n2, n3, n5).getRGB();
    }

    private static void renderNoCategoryPlaceholder(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = f + 117.0f;
        float f5 = f2 + 5.0f;
        float f6 = 308.0f;
        float f7 = 280.0f;
        float f8 = 60.0f;
        float f9 = f4 + (f6 - f8) * 0.5f;
        float f10 = f5 + (f7 - f8) * 0.5f - 10.0f;
        Render2D.image("nv:textures/logo.png", f9, f10, f8, f8, RemoteGuiPanelRenderer.color(255, 255, 255, 255, f3));
        String string = "\u041e\u0442\u043a\u0440\u043e\u0439\u0442\u0435 \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u044e \u0447\u0442\u043e\u0431\u044b \u043d\u0430\u0447\u0430\u0442\u044c";
        float f11 = 6.0f;
        float f12 = Fonts.MONTSERRAT_MEDIUM.width(string, f11);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f4 + (f6 - f12) * 0.5f, f10 + f8 + 8.0f, f11, RemoteGuiPanelRenderer.color(255, 255, 255, 100, f3));
    }

    private static float panelRadius(RemoteTheme remoteTheme) {
        return Math.max(0.0f, Math.min(remoteTheme.cornerRadius() + 6.0f, Math.min(430.0f, 290.0f) * 0.5f));
    }

    public static void render(DrawContext drawContext, RemoteGuiPanel remoteGuiPanel, int n) {
        RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n, 0);
    }

    public static void render(DrawContext drawContext, RemoteGuiPanel remoteGuiPanel, int n, int n2) {
        RemoteGuiPanelRenderer.render(drawContext, remoteGuiPanel, n, n2, 1.0f, true);
    }

    public static void render(DrawContext drawContext, RemoteGuiPanel remoteGuiPanel, int n, int n2, float f, boolean bl) {
        float f2;
        if (n2 == 2) {
            float f3;
            GuiShareRemoteState guiShareRemoteState = remoteGuiPanel.state();
            if (bl) {
                remoteGuiPanel.updateSmoothing(0L);
            }
            if ((f3 = remoteGuiPanel.contentAlpha() * f) > 0.01f) {
                RemoteGuiPanelRenderer.renderPopup(drawContext, 80.0f, 102.0f, remoteGuiPanel, guiShareRemoteState, remoteGuiPanel.theme(), n, f3);
            }
            return;
        }
        GuiShareRemoteState guiShareRemoteState = remoteGuiPanel.state();
        RemoteTheme remoteTheme = remoteGuiPanel.theme();
        if (bl) {
            remoteGuiPanel.updateSmoothing(0L);
        }
        if ((f2 = remoteGuiPanel.contentAlpha() * f) <= 0.01f) {
            return;
        }
        long l = System.currentTimeMillis();
        if (l >= filterCacheResetAt) {
            filterCache.clear();
            filterCacheResetAt = l + 3000L;
        }
        float f4 = 80.0f;
        float f5 = 102.0f;
        RemoteGuiPanelRenderer.renderNameplate(guiShareRemoteState, remoteTheme, f2);
        RemoteGuiPanelRenderer.renderPanelBackground(remoteTheme, n, f4, f5, f2);
        RenderHelper.drawPanelBg(f4 + 5.0f, f5 + 5.0f, 110.0f, 280.0f, 12.0f, 0.0f, 0.0f, 12.0f, f2);
        RenderHelper.drawPanelBg(f4 + 117.0f, f5 + 5.0f, 308.0f, 280.0f, 0.0f, 12.0f, 12.0f, 0.0f, f2);
        RemoteGuiPanelRenderer.renderSidebar(f4 + 5.0f, f5 + 2.0f, remoteGuiPanel, remoteTheme, f2);
        Category category = remoteGuiPanel.contentCategory();
        boolean bl2 = category != null && category != Category.THEMES;
        float f6 = f2 * remoteGuiPanel.categoryT();
        if (bl2 && f6 > 0.01f) {
            List<Module> list = RemoteGuiPanelRenderer.filteredModules(category, guiShareRemoteState.search());
            RemoteGuiPanelRenderer.renderModuleCards(drawContext, f4, f5, remoteGuiPanel, guiShareRemoteState, remoteTheme, list, f6);
            if (list.isEmpty() && guiShareRemoteState.search() != null && !guiShareRemoteState.search().isBlank()) {
                RemoteGuiPanelRenderer.renderNoResults(f4, f5, f6);
            }
        }
        if (category == Category.THEMES && f6 > 0.01f) {
            RemoteGuiPanelRenderer.renderThemesGrid(drawContext, f4, f5, remoteGuiPanel, guiShareRemoteState, remoteTheme, f6);
        }
        float f7 = remoteGuiPanel.modulesHeaderT();
        if (bl2 && f7 > 0.004f) {
            RemoteGuiPanelRenderer.renderHeader(drawContext, f4, f5, guiShareRemoteState, remoteTheme, f2 * f7);
        }
        float f8 = remoteGuiPanel.placeholderT();
        if (category == null && remoteGuiPanel.targetCategory() == null && f8 > 0.01f) {
            RemoteGuiPanelRenderer.renderNoCategoryPlaceholder(drawContext, f4, f5, f2 * f8);
        }
        if (n2 == 0) {
            RemoteGuiPanelRenderer.renderPopup(drawContext, f4, f5, remoteGuiPanel, guiShareRemoteState, remoteTheme, n, f2);
        }
    }

    private static int rgba(int n, int n2, int n3, float f) {
        int n4 = Math.max(0, Math.min(255, (int)f));
        if (n4 <= 0) {
            return 0;
        }
        return new Color(n, n2, n3, n4).getRGB();
    }

    private static Decelerate createAnim(int n) {
        Decelerate decelerate = (Decelerate)new Decelerate().setMs(n).setValue(1.0);
        decelerate.setDirection(Direction.BACKWARDS);
        decelerate.counter.setTime(System.currentTimeMillis() - 10000L);
        return decelerate;
    }

    private static char iconChar(Category category) {
        return NvIcons.charForCategory(category);
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }

    private static List<String> descLines(Module module, float f) {
        String string = module.getName() + "|" + (int)f;
        List<String> list = descCache.get(string);
        if (list != null) {
            return list;
        }
        String string2 = module.getDescription() != null ? module.getDescription() : "";
        ArrayList<String> arrayList = new ArrayList<String>();
        if (!string2.isEmpty()) {
            float f2 = f - 16.0f - 4.0f;
            StringBuilder stringBuilder = new StringBuilder();
            for (String string3 : string2.split(" ")) {
                String string4;
                String string5 = string4 = stringBuilder.length() > 0 ? String.valueOf(stringBuilder) + " " + string3 : string3;
                if (Fonts.MONTSERRAT_MEDIUM.width(string4, 6.0f) > f2 && stringBuilder.length() > 0) {
                    arrayList.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder(string3);
                    continue;
                }
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(' ');
                }
                stringBuilder.append(string3);
            }
            if (stringBuilder.length() > 0) {
                arrayList.add(stringBuilder.toString());
            }
        }
        descCache.put(string, arrayList);
        return arrayList;
    }

    private static List<Module> filteredModules(Category category, String string) {
        String string2;
        String string3 = (category == null ? "" : category.name()) + "|" + (string2 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT));
        List<Module> list = filterCache.get(string3);
        if (list != null) {
            return list;
        }
        ArrayList<Module> arrayList = new ArrayList<Module>();
        EnumSet<Server> enumSet = ServerRestrictions.current();
        if (string2.isEmpty()) {
            if (category != null) {
                for (Module module : ModuleManager.get().getByCategory(category)) {
                    if (ServerRestrictions.isHiddenBy(module, enumSet)) continue;
                    arrayList.add(module);
                }
            }
        } else {
            String string4 = RemoteGuiPanelRenderer.layoutNormalize(string2);
            String string5 = string2.replace(" ", "");
            String string6 = string4.replace(" ", "");
            for (Module module : ModuleManager.get().getAll()) {
                if (ServerRestrictions.isHiddenBy(module, enumSet)) continue;
                String string7 = module.getName().toLowerCase(Locale.ROOT);
                String disp = module.getDisplayName().toLowerCase(Locale.ROOT);
                String desc = module.getDescription() != null ? module.getDescription().toLowerCase(Locale.ROOT) : "";
                String string8 = string7.replace(" ", "");
                if (string7.contains(string2) || string7.contains(string4) || string8.contains(string5) || string8.contains(string6) ||
                    disp.contains(string2) || disp.contains(string4) || desc.contains(string2) || desc.contains(string4)) {
                    arrayList.add(module);
                }
            }
        }
        if (filterCache.size() > 32) {
            filterCache.clear();
        }
        filterCache.put(string3, arrayList);
        return arrayList;
    }

    static boolean isMainCategory(Category category) {
        if (category == null) {
            return false;
        }
        for (Category category2 : MAIN_CATEGORIES) {
            if (category2 != category) continue;
            return true;
        }
        return false;
    }

    private record PopupLayout(
        List<GuiSharePopupRow> rows,
        List<Setting> widgets,
        float px,
        float py,
        float width,
        float height,
        float bodyViewH,
        float contentH,
        float t
    ) {}

    private static void renderNoResults(float f, float f2, float f3) {
        float f4 = f + 117.0f;
        float f5 = 308.0f;
        float f6 = f2 + 5.0f + 30.0f;
        float f7 = 250.0f;
        String string = "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e";
        float f8 = 6.5f;
        float f9 = Fonts.MONTSERRAT_MEDIUM.width(string, f8);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f4 + (f5 - f9) * 0.5f, f6 + f7 * 0.5f - 3.0f, f8, RemoteGuiPanelRenderer.color(255, 255, 255, 110, f3));
    }

    private static String layoutNormalize(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            int n = RU_LAYOUT.indexOf(c);
            stringBuilder.append(n >= 0 ? EN_LAYOUT.charAt(n) : c);
        }
        return stringBuilder.toString();
    }

    static void pruneIdentities(Set<String> set) {
        enableAnims.keySet().removeIf(string -> {
            int n = string.lastIndexOf(124);
            return n <= 0 || !set.contains(string.substring(0, n));
        });
        themeSelectAnims.keySet().removeIf(string -> {
            int n = string.lastIndexOf(124);
            return n <= 0 || !set.contains(string.substring(0, n));
        });
    }

    private static float baseCardHeight(Module module, float f) {
        String string = module.getName() + "|" + (int)f;
        Float f2 = heightCache.get(string);
        if (f2 != null) {
            return f2.floatValue();
        }
        List<String> list = RemoteGuiPanelRenderer.descLines(module, f);
        float f3 = list.isEmpty() ? 22.0f : 19.0f + (float)list.size() * 7.0f + 7.0f;
        heightCache.put(string, Float.valueOf(f3));
        return f3;
    }

    private static float totalContentH(List<Module> list, float f) {
        float f2 = 0.0f;
        float f3 = 0.0f;
        for (int i = 0; i < list.size(); ++i) {
            float f4 = RemoteGuiPanelRenderer.baseCardHeight(list.get(i), f) + 5.0f;
            if ((i & 1) == 0) {
                f2 += f4;
                continue;
            }
            f3 += f4;
        }
        float f5 = Math.max(f2, f3);
        if (f5 > 0.0f) {
            f5 -= 5.0f;
        }
        return 10.0f + f5;
    }

    private static float edgeAlpha(float f, float f2, float f3) {
        return Math.min(RemoteGuiPanelRenderer.clamp((f - f2) / 4.0f, 0.0f, 1.0f), RemoteGuiPanelRenderer.clamp((f3 - f) / 4.0f, 0.0f, 1.0f));
    }

    private static boolean hasOverlay(GuiSharePopupRow guiSharePopupRow) {
        return switch (guiSharePopupRow.type()) {
            case "c" -> true;
            case "m", "n" -> {
                if (!guiSharePopupRow.options().isEmpty()) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private static float listScroll(GuiSharePopupRow guiSharePopupRow, int n) {
        int n2;
        int n3 = 0;
        for (n2 = 0; n2 < n; ++n2) {
            if ((guiSharePopupRow.selectedMask() & 1 << n2) == 0) continue;
            n3 = n2;
            break;
        }
        n2 = Math.max(0, n - 7);
        int n4 = Math.max(0, Math.min(n3 - 3, n2));
        return (float)n4 * 12.0f;
    }

    private static int mixRgb(int n, int n2, float f) {
        f = f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
        int n3 = n >> 16 & 0xFF;
        int n4 = n >> 8 & 0xFF;
        int n5 = n & 0xFF;
        int n6 = n2 >> 16 & 0xFF;
        int n7 = n2 >> 8 & 0xFF;
        int n8 = n2 & 0xFF;
        int n9 = Math.round((float)n3 + (float)(n6 - n3) * f);
        int n10 = Math.round((float)n4 + (float)(n7 - n4) * f);
        int n11 = Math.round((float)n5 + (float)(n8 - n5) * f);
        return n9 << 16 | n10 << 8 | n11;
    }

    private static String badgeLabel(KeyBind keyBind) {
        String string = keyBind.getDisplayName();
        return string == null || string.isEmpty() ? "?" : string;
    }

    private static void renderNameplate(GuiShareRemoteState guiShareRemoteState, RemoteTheme remoteTheme, float f) {
        String string = guiShareRemoteState.minecraftUsername();
        float f2 = 7.5f;
        float f3 = 8.0f;
        float f4 = 4.0f;
        float f5 = Fonts.MONTSERRAT_MEDIUM.width(string, f2);
        float f6 = Fonts.NV.msdfWidth(NvIcons.CLOSE, f3);
        float f7 = f6 + f4 + f5;
        float f8 = 80.0f + (430.0f - f7) * 0.5f;
        float f9 = 88.0f;
        Render2D.rect(f8 - 7.0f, f9 - 7.0f, f7 + 14.0f, 14.0f, 7.0f, RemoteGuiPanelRenderer.color(0, 0, 0, 130, f));
        RemoteGuiPanelRenderer.remoteMsdfIcon(remoteTheme, "nv", NvIcons.CLOSE, f8, f9 - f3 * 0.5f + 0.5f, f3, 235.0f * f, 0.1f);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f8 + f6 + f4, f9 - f2 + 3.5f, f2, RemoteGuiPanelRenderer.color(255, 255, 255, 235, f));
    }

    private static void renderPopup(DrawContext drawContext, float f, float f2, RemoteGuiPanel remoteGuiPanel, GuiShareRemoteState guiShareRemoteState, RemoteTheme remoteTheme, int n, float f3) {
        float f4;
        float f5;
        PopupLayout popupLayout = RemoteGuiPanelRenderer.popupLayout(f, f2, remoteGuiPanel);
        if (popupLayout == null) {
            return;
        }
        float f6 = f3 * popupLayout.t();
        List<GuiSharePopupRow> list = popupLayout.rows();
        List<Setting> list2 = popupLayout.widgets();
        float f7 = popupLayout.contentH();
        float f8 = popupLayout.width();
        float f9 = popupLayout.bodyViewH();
        float f10 = popupLayout.height();
        float f11 = popupLayout.px();
        float f12 = popupLayout.py();
        GuiLayerBlurRenderer.markPopupBoundary(drawContext);
        RemoteGuiPanelRenderer.drawThemeGlow(remoteTheme, f11, f12, f8, f10, 8.0f, f6);
        RemoteGuiPanelRenderer.drawThemeGlass(remoteTheme, n, f11, f12, f8, f10, 8.0f, f6);
        Render2D.outline(f11, f12, f8, f10, 8.0f, 0.6f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 24.0f * f6));
        float f13 = Math.max(0.0f, f7 - f9);
        float f14 = RemoteGuiPanelRenderer.clamp(remoteGuiPanel.smoothPopupScroll(), 0.0f, f13);
        float f15 = f12 + 7.0f;
        Render2D.pushScissor(drawContext, f11, f15, f8, f9);
        float f16 = f15 - f14;
        if (!list.isEmpty()) {
            f5 = f11 + 5.0f;
            f4 = f8 - 10.0f;
            for (GuiSharePopupRow guiSharePopupRow : list) {
                float f17 = RemoteGuiPanelRenderer.popupRowHeight(guiSharePopupRow);
                if (f16 + f17 >= f15 - 6.0f && f16 <= f15 + f9 + 6.0f) {
                    RemoteGuiPanelRenderer.renderPopupRow(remoteTheme, guiSharePopupRow, f5, f16, f4, f6);
                }
                f16 += f17 + 4.0f;
            }
        } else {
            for (Setting setting : list2) {
                if (!setting.isVisible()) continue;
                float f18 = setting.height();
                if (f16 + f18 >= f15 - 6.0f && f16 <= f15 + f9 + 6.0f) {
                    Fonts.MONTSERRAT_MEDIUM.draw(setting.name(), f11 + 5.0f + 6.0f, f16 + 5.0f, 6.5f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 200.0f * f6));
                    float f19 = f11 + f8 - 5.0f - 4.0f - 19.1f - 6.0f;
                    Render2D.rect(f19, f16 + 6.0f, 30.0f, 4.0f, 2.0f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 26.0f * f6));
                    RemoteGuiPanelRenderer.remoteFillHorizontal(remoteTheme, f19, f16 + 6.0f, 17.0f, 4.0f, 2.0f, 140.0f * f6);
                }
                f16 += f18 + 4.0f;
            }
        }
        Render2D.popScissor(drawContext);
        if (f7 > f9 + 0.5f) {
            f5 = f11 + f8 - 4.5f;
            f4 = Math.max(12.0f, f9 * (f9 / f7));
            float f20 = f15 + (f9 - f4) * (f14 / f13);
            Render2D.rect(f5, f15, 1.5f, f9, 0.75f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 20.0f * f6));
            RemoteGuiPanelRenderer.remoteFillVertical(remoteTheme, f5, f20, 1.5f, f4, 0.75f, 160.0f * f6);
        }
        if (!list.isEmpty()) {
            f5 = f11 + 5.0f;
            f4 = f8 - 10.0f;
            float f21 = f15 - f14;
            boolean bl = false;
            for (GuiSharePopupRow guiSharePopupRow : list) {
                if (guiSharePopupRow.open() && RemoteGuiPanelRenderer.hasOverlay(guiSharePopupRow)) {
                    if (!bl) {
                        GuiLayerBlurRenderer.markPopupBoundary(drawContext);
                        bl = true;
                    }
                    RemoteGuiPanelRenderer.renderPopupOverlay(remoteTheme, n, guiSharePopupRow, f5, f21, f4, f6);
                }
                f21 += RemoteGuiPanelRenderer.popupRowHeight(guiSharePopupRow) + 4.0f;
            }
        }
    }

    private static void renderPopupToggle(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        float f5 = f + f3 - 19.1f - 4.0f;
        float f6 = f2 + 2.8000002f;
        RenderHelper.drawName(guiSharePopupRow.name(), f, f2, f5 - (f + 6.0f) - 4.0f, f4);
        RemoteGuiPanelRenderer.drawToggleRemote(remoteTheme, f5, f6, 19.1f, 10.4f, RemoteGuiPanelRenderer.clamp(guiSharePopupRow.fraction(), 0.0f, 1.0f), f4);
    }

    private static float[] overlayRect(GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3) {
        switch (guiSharePopupRow.type()) {
            case "c": {
                float f4 = 82.0f;
                float f5 = 78.0f;
                return new float[]{f + f3 - f4 - 4.0f, f2 + 16.0f + 1.0f, f4, f5};
            }
            case "m": 
            case "n": {
                boolean bl;
                List<String> list = guiSharePopupRow.options();
                if (list.isEmpty()) {
                    return null;
                }
                boolean bl2 = "m".equals(guiSharePopupRow.type());
                float f6 = 0.0f;
                for (String string : list) {
                    f6 = Math.max(f6, Fonts.MONTSERRAT_MEDIUM.width(string, 5.5f));
                }
                boolean bl3 = bl = bl2 && list.size() > 7;
                float f7 = bl2 ? f6 + 16.0f + (bl ? 5.0f : 0.0f) : f6 + 20.0f;
                int n = bl2 ? Math.min(list.size(), 7) : list.size();
                float f8 = (float)n * 12.0f + 6.0f;
                return new float[]{f + f3 - f7 - 4.0f, f2 + 16.0f + 1.0f, f7, f8};
            }
        }
        return null;
    }

    private static void renderPopupText(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        float f5 = 92.0f;
        float f6 = 12.0f;
        float f7 = f + f3 - f5 - 4.0f;
        float f8 = f2 + (16.0f - f6) * 0.5f;
        RenderHelper.drawName(guiSharePopupRow.name(), f, f2, f7 - (f + 6.0f) - 4.0f, f4);
        Render2D.rect(f7, f8, f5, f6, 3.0f, RemoteGuiPanelRenderer.rgba(0, 0, 0, 60.0f * f4));
        String string = guiSharePopupRow.value();
        float f9 = f5 - 10.0f;
        while (string.length() > 1 && Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) > f9) {
            string = string.substring(1);
        }
        int n = guiSharePopupRow.fraction() > 0.5f ? remoteTheme.accentSoft(220.0f * f4) : RemoteGuiPanelRenderer.rgba(255, 255, 255, 110.0f * f4);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f7 + 5.0f, f8 + 2.5f, 6.0f, n);
    }


    private static void renderModuleCards(DrawContext drawContext, float f, float f2, RemoteGuiPanel remoteGuiPanel, GuiShareRemoteState guiShareRemoteState, RemoteTheme remoteTheme, List<Module> list, float f3) {
        float f4;
        float f5;
        float f6;
        float f7;
        float f8 = f + 117.0f;
        float f9 = f2 + 5.0f + 30.0f;
        float f10 = 308.0f;
        float f11 = 250.0f;
        float f12 = (f10 - 5.0f - 10.0f) * 0.5f;
        float f13 = f8 + 5.0f;
        float f14 = f13 + f12 + 5.0f;
        float f15 = RemoteGuiPanelRenderer.totalContentH(list, f12);
        float f16 = Math.max(0.0f, f15 - f11);
        float f17 = RemoteGuiPanelRenderer.clamp(remoteGuiPanel.smoothScroll(), 0.0f, f16);
        Set set = remoteGuiPanel.enabledSet();
        EnumSet<Server> enumSet = ServerRestrictions.current();
        String string = guiShareRemoteState.identityKey();
        Render2D.pushScissor(drawContext, f8, f9, f10, f11);
        RoundedScissor.push(drawContext, f8, f9, f10, f11, 0.0f, 0.0f, 12.0f, 0.0f);
        boolean bl = RemoteGuiWorld.beginCardBlurJob(drawContext, f8, f9, f10, f11);
        float f18 = (1.0f - remoteGuiPanel.categoryT()) * 8.0f;
        float f19 = 0.0f;
        float f20 = 0.0f;
        for (int i = 0; i < list.size(); ++i) {
            float f21;
            float f22;
            float f23;
            float f24;
            float f25;
            float f26;
            float f27;
            float f28;
            float f29;
            float f30;
            Module module = list.get(i);
            int n = i % 2;
            int n2 = i / 2;
            f7 = RemoteGuiPanelRenderer.baseCardHeight(module, f12);
            float f31 = f6 = n == 0 ? f19 : f20;
            if (n == 0) {
                f19 += f7 + 5.0f;
            } else {
                f20 += f7 + 5.0f;
            }
            f5 = n == 0 ? f13 : f14;
            f4 = f9 + 5.0f + f6 - f17 + f18;
            if (f4 + f7 < f9 - 5.0f || f4 > f9 + f11 + 5.0f || (f30 = remoteGuiPanel.rowAppear(n2)) < 0.001f) continue;
            float f32 = Math.min(1.0f, f30 / 0.6f);
            float f33 = f32 * f32;
            float f34 = f30 < 0.6f ? 0.0f : (f30 - 0.6f) / 0.4f;
            float f35 = 1.0f - f34 * f34 * (3.0f - 2.0f * f34);
            boolean bl2 = f30 < 0.999f;
            float f36 = Math.max(f4 += (1.0f - f32) * 6.0f, f9);
            float f37 = Math.min(f4 + f7, f9 + f11);
            boolean bl3 = bl2 && bl && f37 - f36 > 0.5f && RemoteGuiWorld.recordCardBlurRect(f5, f36, f12, f37 - f36, f33, f35);
            boolean bl4 = ServerRestrictions.isBlockedBy(module, enumSet);
            float f38 = f29 = bl4 ? f3 * 0.35f : f3;
            if (bl2) {
                if (!bl3) {
                    f29 *= f33;
                }
                f28 = 0.85f + 0.15f * f32;
                f27 = f5 + f12 * 0.5f;
                f26 = f4 + f7 * 0.5f;
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(f27, f26);
                drawContext.getMatrices().scale(f28, f28);
                drawContext.getMatrices().translate(-f27, -f26);
            }
            f28 = f29;
            f27 = 6.0f;
            f26 = 6.0f;
            if (n == 1 && (f25 = f4 + f7) > (f24 = f9 + f11) - 16.0f) {
                float f39 = Math.max(0.0f, Math.min(1.0f, (f25 - (f24 - 16.0f)) / 16.0f));
                f27 = 6.0f + 6.0f * f39;
            }
            f25 = RemoteGuiPanelRenderer.enableAnim(string, module.getName(), set.contains(module.getName()));
            Render2D.rect(f5, f4, f12, f7, 6.0f, f26, f27, 6.0f, RemoteGuiPanelRenderer.rgba(10, 12, 16, 72.0f * f28));
            if (f25 > 0.01f) {
                int n3 = remoteTheme.gradientA(30.0f * f25 * f28);
                int n4 = remoteTheme.gradientB(30.0f * f25 * f28);
                Render2D.rect(f5, f4, f12, f7, 6.0f, f26, f27, 6.0f, n3, n4, n4, n3);
            }
            int n5 = RemoteGuiPanelRenderer.rgba(255, 255, 255, 14.0f * f28);
            int n6 = ColorUtil.lerpColor(n5, remoteTheme.gradientA(65.0f * f28), f25);
            int n7 = ColorUtil.lerpColor(n5, remoteTheme.gradientB(65.0f * f28), f25);
            Render2D.outline(f5, f4, f12, f7, 6.0f, f26, f27, 6.0f, 0.6f, n6, n7, n7, n6);
            KeyBind keyBind = module.getBind();
            float f40 = 0.0f;
            if (keyBind != null && keyBind.isBound()) {
                String string2 = RemoteGuiPanelRenderer.badgeLabel(keyBind);
                float f41 = Math.max(10.0f, Fonts.MONTSERRAT_MEDIUM.width(string2, 5.5f) + 6.0f);
                f23 = f5 + 8.0f;
                f22 = f4 + 10.75f - 5.0f;
                Render2D.rect(f23, f22, f41, 10.0f, 3.0f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 16.0f * f29));
                Render2D.outline(f23, f22, f41, 10.0f, 3.0f, 0.5f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 30.0f * f29));
                f21 = Fonts.MONTSERRAT_MEDIUM.width(string2, 5.5f);
                Fonts.MONTSERRAT_MEDIUM.draw(string2, f23 + (f41 - f21) * 0.5f, f22 + 1.75f, 5.5f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 205.0f * f29));
                f40 = f41 + 5.0f;
            }
            float f42 = f5 + 8.0f + f40;
            Fonts.MONTSERRAT_MEDIUM.draw(module.getDisplayName(), f42, f4 + 7.0f, 7.5f, RemoteGuiPanelRenderer.rgba(255, 255, 255, (160.0f + 95.0f * f25) * f29));
            List<String> list2 = RemoteGuiPanelRenderer.descLines(module, f12);
            if (!list2.isEmpty()) {
                int n8 = RemoteGuiPanelRenderer.rgba(255, 255, 255, (78.0f + 27.0f * f25) * f29);
                f22 = f4 + 19.0f;
                for (String string3 : list2) {
                    Fonts.MONTSERRAT_MEDIUM.draw(string3, f5 + 8.0f, f22, 6.0f, n8);
                    f22 += 7.0f;
                }
            }
            f23 = f5 + f12 - 14.692308f - 8.0f;
            f22 = f4 + 10.75f - 4.0f;
            RemoteGuiPanelRenderer.drawToggleRemote(remoteTheme, f23, f22, 14.692308f, 8.0f, f25, f29);
            if (!module.getSettings().all().isEmpty()) {
                f21 = module.getName().equals(guiShareRemoteState.popupModule()) ? 1.0f : 0.0f;
                float f43 = Fonts.NV.msdfWidth(GEAR_GLYPH, 7.0f);
                float f44 = f23 - 7.0f - f43;
                float f45 = f4 + 10.75f - 3.5f;
                int n9 = ColorUtil.lerpColor(RemoteGuiPanelRenderer.rgba(255, 255, 255, 120.0f * f29), remoteTheme.accentSoft(235.0f * f29), f21);
                Fonts.NV.msdf(GEAR_GLYPH, f44, f45, 7.0f, n9);
            }
            if (!bl2) continue;
            drawContext.getMatrices().popMatrix();
        }
        remoteGuiPanel.markAppearFrameDone();
        RoundedScissor.pop();
        Render2D.popScissor(drawContext);
        if (bl) {
            RemoteGuiWorld.endCardBlurJob(drawContext);
        }
        if (f16 > 0.5f) {
            float f46 = f + 430.0f - 7.5f;
            float f47 = RenderHelper.effectiveCornerRadius(12.0f, f10, 280.0f);
            float f48 = Math.max(0.0f, RenderHelper.cornerEdgeInset(f47, 1.0f) + 1.5f - 3.0f);
            float f49 = f9 + 3.0f;
            f7 = f11 - 6.0f - f48;
            f6 = Math.max(12.0f, f7 * (f11 / f15));
            f5 = Math.max(1.0f, f7 - f6);
            f4 = f49 + f5 * (f17 / f16);
            Render2D.rect(f46, f49, 1.5f, f7, 0.75f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 20.0f * f3));
            RemoteGuiPanelRenderer.remoteFillVertical(remoteTheme, f46, f4, 1.5f, f6, 0.75f, 160.0f * f3);
        }
    }

    private static void renderHeader(DrawContext drawContext, float f, float f2, GuiShareRemoteState guiShareRemoteState, RemoteTheme remoteTheme, float f3) {
        float f4 = f + 117.0f;
        float f5 = f2 + 5.0f;
        float f6 = 308.0f;
        float f7 = 6.0f;
        float f8 = 14.0f;
        float f9 = 26.0f;
        float f10 = f5 + (f9 - f8) * 0.5f;
        RenderHelper.drawPanelBg(f4, f5, f6, f9, 0.0f, 12.0f, 0.0f, 0.0f, f3);
        Render2D.pushScissor(drawContext, f4, f5, f6, f9);
        float f11 = 18.0f;
        float f12 = f4 + f6 - f7 - f11;
        float f13 = f5 + (f9 - f11) * 0.5f;
        RemoteGuiPanelRenderer.renderAvatar(guiShareRemoteState, f12, f13, f11, f3);
        String string = guiShareRemoteState.profileUsername();
        String string2 = guiShareRemoteState.role() == null ? "" : guiShareRemoteState.role();
        float f14 = 6.5f;
        float f15 = 5.0f;
        float f16 = Fonts.MONTSERRAT_MEDIUM.width(string, f14);
        float f17 = Fonts.MONTSERRAT_MEDIUM.width(string2, f15);
        float f18 = f12 - 5.0f;
        float f19 = f13 + (f11 - (f14 + 1.0f + f15)) * 0.5f;
        Fonts.MONTSERRAT_MEDIUM.draw(string, f18 - f16, f19, f14, RemoteGuiPanelRenderer.color(255, 255, 255, 230, f3));
        Fonts.MONTSERRAT_MEDIUM.draw(string2, f18 - f17, f19 + f14 + 1.0f, f15, remoteTheme.accentSoft(195.0f * f3));
        float f20 = f18 - Math.max(f16, f17);
        float f21 = f4 + f7;
        float f22 = Math.max(60.0f, f20 - f7 - f21);
        RemoteGuiPanelRenderer.renderSearchField(drawContext, f21, f10, f22, f8, guiShareRemoteState.search(), remoteTheme, f3);
        Render2D.popScissor(drawContext);
    }



    private static void renderAvatar(GuiShareRemoteState guiShareRemoteState, float f, float f2, float f3, float f4) {
        float f5 = f3 * 0.5f;
        int n = Math.max(0, Math.min(255, Math.round(f4 * 255.0f))) << 24 | 0xFFFFFF;
        String string = RemoteAvatarCache.texture((String)guiShareRemoteState.avatarUrl());
        if (string != null && Render2D.imageReady(string)) {
            Render2D.image(string, f, f2, f3, f3, f5, n);
            return;
        }
        Render2D.rect(f, f2, f3, f3, f5, RemoteGuiPanelRenderer.color(0, 0, 0, 90, f4));
        Render2D.outline(f, f2, f3, f3, f5, 0.8f, RemoteGuiPanelRenderer.color(255, 255, 255, 30, f4));
    }

    private static List<Setting> popupWidgets(Module module) {
        List<Setting> list;
        String string = module.getName().toLowerCase(Locale.ROOT);
        List<Setting> list2 = popupWidgetsCache.get(string);
        if (list2 != null) {
            return list2;
        }
        try {
            list = SettingsFactory.build(module);
        }
        catch (Throwable throwable) {
            list = List.of();
        }
        popupWidgetsCache.put(string, list);
        return list;
    }

    private static void renderListOverlay(RemoteTheme remoteTheme, int n, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4, boolean bl) {
        float f5;
        float f6;
        float f7;
        float f8;
        boolean bl2;
        List<String> list = guiSharePopupRow.options();
        if (list.isEmpty()) {
            return;
        }
        float f9 = 0.0f;
        for (String string : list) {
            f9 = Math.max(f9, Fonts.MONTSERRAT_MEDIUM.width(string, 5.5f));
        }
        boolean bl3 = bl2 = bl && list.size() > 7;
        float f10 = bl ? f9 + 16.0f + (bl2 ? 5.0f : 0.0f) : f9 + 20.0f;
        float f11 = f + f3 - f10 - 4.0f;
        float f12 = f2 + 16.0f + 1.0f;
        int n2 = bl ? Math.min(list.size(), 7) : list.size();
        float f13 = (float)n2 * 12.0f + 6.0f;
        float f14 = Math.max(0.0f, (float)(list.size() - 7)) * 12.0f;
        float f15 = bl2 ? RemoteGuiPanelRenderer.listScroll(guiSharePopupRow, list.size()) : 0.0f;
        RemoteGuiPanelRenderer.drawThemeGlass(remoteTheme, n, f11, f12, f10, f13, 2.0f, f4);
        float f16 = f12 + 1.0f;
        float f17 = f12 + f13 - 1.0f;
        for (int i = 0; i < list.size(); ++i) {
            f8 = f12 + 3.0f + (float)i * 12.0f - f15;
            f7 = f8 + 2.0f;
            f6 = f8 + 12.0f - 2.0f;
            if (f6 <= f16 || f7 >= f17) continue;
            float f18 = f5 = bl2 ? Math.min(RemoteGuiPanelRenderer.edgeAlpha(f7, f16, f17), RemoteGuiPanelRenderer.edgeAlpha(f6, f16, f17)) : 1.0f;
            if (f5 <= 0.01f) continue;
            boolean bl4 = (guiSharePopupRow.selectedMask() & 1 << i) != 0;
            int n3 = bl4 ? remoteTheme.accentSoft(230.0f * f4 * f5) : RemoteGuiPanelRenderer.rgba(255, 255, 255, 140.0f * f4 * f5);
            Fonts.MONTSERRAT_MEDIUM.draw(list.get(i), f11 + 6.0f, f8 + 2.5f, 5.5f, n3);
            if (!bl4) continue;
            float f19 = f11 + f10 - (bl && !bl2 ? 6.0f : 8.0f);
            RemoteGuiPanelRenderer.remoteFillVertical(remoteTheme, f19, f8 + 6.0f - 1.0f, 2.0f, 2.0f, 1.0f, 200.0f * f4 * f5);
        }
        if (bl2) {
            float f20 = f11 + f10 - 3.0f;
            f8 = f12 + 2.0f;
            f7 = f13 - 4.0f;
            Render2D.rect(f20, f8, 1.6f, f7, 0.8f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 28.0f * f4));
            f6 = Math.max(10.0f, f7 * 7.0f / (float)list.size());
            f5 = f8 + (f14 <= 0.0f ? 0.0f : f15 / f14 * (f7 - f6));
            Render2D.rect(f20, f5, 1.6f, f6, 0.8f, remoteTheme.accentSoft(190.0f * f4));
        }
    }

    private static void renderPopupColor(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        String string = guiSharePopupRow.value();
        float f5 = Fonts.MONTSERRAT_MEDIUM.width(string, 5.5f);
        float f6 = 8.0f;
        float f7 = f6 + 4.0f + f5 + 10.0f;
        float f8 = f + f3 - f7 - 4.0f;
        float f9 = f2 + 2.0f;
        RenderHelper.drawName(guiSharePopupRow.name(), f, f2, f8 - (f + 6.0f) - 4.0f, f4);
        RemoteGuiPanelRenderer.drawPopupBtnBg(remoteTheme, f8, f9, f7, 12.0f, f4);
        int n = RemoteGuiPanelRenderer.clamp255(255.0f * f4) << 24 | guiSharePopupRow.rgb() & 0xFFFFFF;
        Render2D.rect(f8 + 3.0f, f9 + (12.0f - f6) * 0.5f, f6, f6, 2.0f, n);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f8 + 3.0f + f6 + 4.0f, f9 + 3.0f, 5.5f, remoteTheme.accentSoft(200.0f * f4));
    }

    private static void drawPopupBtnBg(RemoteTheme remoteTheme, float f, float f2, float f3, float f4, float f5) {
        float f6 = remoteTheme.cornerRadius() / 7.0f;
        float f7 = Math.min(Math.min(f3, f4) * 0.5f, 3.0f * f6);
        Render2D.rect(f, f2, f3, f4, f7, RemoteGuiPanelRenderer.rgba(0, 0, 0, 40.0f * f5));
    }

    private static void drawThemeGlass(RemoteTheme remoteTheme, int n, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        BuiltGlass builtGlass = new BuiltGlass(f, f2, f3, f4, f5, f5, f5, f5, remoteTheme.primaryColor(), RemoteGuiPanelRenderer.clamp(f6, 0.0f, 1.0f), remoteTheme.edgeSharpness(), remoteTheme.primaryColor(), 1.0f, true, remoteTheme.edgeStrength(), f7, 0.5f, 0.0f).withBlurRadius(remoteTheme.backdropBlur()).withSecondColor(remoteTheme.secondaryColor(), remoteTheme.colorOffset()).withPaletteSlot(n);
        Render2D.glass(builtGlass);
    }

    private static void drawThemeGlass(RemoteTheme remoteTheme, int n, float f, float f2, float f3, float f4, float f5, float f6) {
        RemoteGuiPanelRenderer.drawThemeGlass(remoteTheme, n, f, f2, f3, f4, f5, f6, remoteTheme.refraction());
    }

    private static PopupLayout popupLayout(float f, float f2, RemoteGuiPanel remoteGuiPanel) {
        String string = remoteGuiPanel.popupDisplayed();
        if (string == null || string.isBlank()) {
            return null;
        }
        float f3 = remoteGuiPanel.popupT();
        if (f3 <= 0.01f) {
            return null;
        }
        List<GuiSharePopupRow> list = remoteGuiPanel.popupRows();
        List<Setting> list2 = List.of();
        float f4 = 0.0f;
        float f5 = 0.0f;
        if (!list.isEmpty()) {
            for (GuiSharePopupRow guiSharePopupRow : list) {
                f4 = Math.max(f4, RemoteGuiPanelRenderer.popupRowPreferredWidth(guiSharePopupRow));
                f5 += RemoteGuiPanelRenderer.popupRowHeight(guiSharePopupRow) + 4.0f;
            }
            f5 -= 4.0f;
        } else {
            Module module = RemoteGuiPanelRenderer.findModule(string);
            if (module == null) {
                return null;
            }
            list2 = RemoteGuiPanelRenderer.popupWidgets(module);
            if (list2.isEmpty()) {
                return null;
            }
            int n = 0;
            for (Setting setting : list2) {
                f4 = Math.max(f4, setting.preferredWidth());
                if (!setting.isVisible()) continue;
                f5 += setting.height() + 4.0f;
                ++n;
            }
            if (n == 0) {
                return null;
            }
            f5 -= 4.0f;
        }
        float f6 = RemoteGuiPanelRenderer.clamp(f4 + 10.0f + 4.0f, 110.0f, 180.0f);
        float f7 = 250.0f;
        float f8 = f7 - 7.0f - 7.0f - 8.0f;
        float f9 = Math.min(f5, f8);
        float f10 = 7.0f + f9 + 7.0f;
        float f11 = f + remoteGuiPanel.popupHoldX();
        float f12 = f2 + remoteGuiPanel.popupHoldY() - 22.0f + (1.0f - f3) * 4.0f;
        return new PopupLayout(list, list2, f11, f12, f6, f10, f9, f5, f3);
    }

    private static float popupRowHeight(GuiSharePopupRow guiSharePopupRow) {
        return switch (guiSharePopupRow.type()) {
            case "s" -> 22.0f;
            case "p" -> 18.0f;
            default -> 16.0f;
        };
    }



    private static void renderColorOverlay(RemoteTheme remoteTheme, int n, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        float f5 = 82.0f;
        float f6 = 78.0f;
        float f7 = f + f3 - f5 - 4.0f;
        float f8 = f2 + 16.0f + 1.0f;
        float f9 = f7 + 6.0f;
        float f10 = f8 + 6.0f;
        float f11 = f10 + 50.0f + 4.0f;
        float f12 = f11 + 4.0f + 4.0f;
        int n2 = guiSharePopupRow.rgb();
        float[] fArray = Color.RGBtoHSB(n2 >>> 16 & 0xFF, n2 >>> 8 & 0xFF, n2 & 0xFF, null);
        float f13 = fArray[0];
        float f14 = fArray[1];
        float f15 = fArray[2];
        float f16 = RemoteGuiPanelRenderer.clamp((float)guiSharePopupRow.alpha() / 255.0f, 0.0f, 1.0f);
        RemoteGuiPanelRenderer.drawThemeGlass(remoteTheme, n, f7, f8, f5, f6, 2.0f, f4);
        int n3 = RemoteGuiPanelRenderer.clamp255(255.0f * f4) << 24;
        int n4 = Color.HSBtoRGB(f13, 1.0f, 1.0f) & 0xFFFFFF | n3;
        int n5 = n3 | 0xFFFFFF;
        int n6 = RemoteGuiPanelRenderer.clamp255(220.0f * f4) << 24 | 0xFFFFFF;
        Render2D.rect(f9, f10, 70.0f, 50.0f, 2.0f, n5, n4, n4, n5);
        Render2D.rect(f9, f10, 70.0f, 50.0f, 2.0f, 0, 0, n3, n3);
        float f17 = f9 + 70.0f * f14;
        float f18 = f10 + 50.0f * (1.0f - f15);
        Render2D.outline(f17 - 3.0f, f18 - 3.0f, 5.0f, 5.0f, 3.0f, 1.0f, n6);
        Render2D.pickerHue(f9, f11, 70.0f, 4.0f, f4);
        Render2D.rect(f9 + 70.0f * f13 - 1.0f, f11 - 1.0f, 2.0f, 6.0f, 1.0f, n6);
        Render2D.pickerAlpha(f9, f12, 70.0f, 4.0f, 0xFF000000 | Color.HSBtoRGB(f13, f14, f15) & 0xFFFFFF, f4);
        Render2D.rect(f9 + 70.0f * f16 - 1.0f, f12 - 1.0f, 2.0f, 6.0f, 1.0f, n6);
    }

    private static void renderPopupOverlay(RemoteTheme remoteTheme, int n, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        switch (guiSharePopupRow.type()) {
            case "c": {
                RemoteGuiPanelRenderer.renderColorOverlay(remoteTheme, n, guiSharePopupRow, f, f2, f3, f4);
                break;
            }
            case "m": {
                RemoteGuiPanelRenderer.renderListOverlay(remoteTheme, n, guiSharePopupRow, f, f2, f3, f4, true);
                break;
            }
            case "n": {
                RemoteGuiPanelRenderer.renderListOverlay(remoteTheme, n, guiSharePopupRow, f, f2, f3, f4, false);
                break;
            }
        }
    }

    private static void renderPopupRow(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        switch (guiSharePopupRow.type()) {
            case "s": {
                RemoteGuiPanelRenderer.renderPopupSlider(remoteTheme, guiSharePopupRow, f, f2, f3, f4);
                break;
            }
            case "b": {
                RemoteGuiPanelRenderer.renderPopupToggle(remoteTheme, guiSharePopupRow, f, f2, f3, f4);
                break;
            }
            case "m": 
            case "n": 
            case "k": {
                RemoteGuiPanelRenderer.renderPopupChip(remoteTheme, guiSharePopupRow, f, f2, f3, f4, false);
                break;
            }
            case "u": {
                RemoteGuiPanelRenderer.renderPopupChip(remoteTheme, guiSharePopupRow, f, f2, f3, f4, true);
                break;
            }
            case "p": {
                RemoteGuiPanelRenderer.renderPopupSeparator(remoteTheme, guiSharePopupRow, f, f2, f3, f4);
                break;
            }
            case "c": {
                RemoteGuiPanelRenderer.renderPopupColor(remoteTheme, guiSharePopupRow, f, f2, f3, f4);
                break;
            }
            case "x": {
                RemoteGuiPanelRenderer.renderPopupText(remoteTheme, guiSharePopupRow, f, f2, f3, f4);
                break;
            }
            default: {
                RenderHelper.drawName(guiSharePopupRow.name(), f, f2, f3 - 12.0f, f4);
            }
        }
    }

    private static void drawToggleRemote(RemoteTheme remoteTheme, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f4 * 0.4615385f;
        int n = RemoteGuiPanelRenderer.rgba(96, 99, 105, 165.0f * f6 * (1.0f - f5));
        int n2 = RemoteGuiPanelRenderer.rgba(78, 81, 87, 165.0f * f6 * (1.0f - f5));
        int n3 = RemoteGuiPanelRenderer.rgba(120, 123, 129, 150.0f * f6);
        int n4 = ColorUtil.lerpColor(n3, remoteTheme.accentBright(150.0f * f6), f5);
        if (f5 < 0.999f) {
            Render2D.rect(f + 0.5f, f2 + 0.5f, f3 - 1.0f, f4 - 1.0f, f7, n, n2, n2, n);
        }
        if (f5 > 0.001f) {
            RemoteGuiPanelRenderer.remoteFillHorizontal(remoteTheme, f + 0.5f, f2 + 0.5f, f3 - 1.0f, f4 - 1.0f, f7, 175.0f * f6 * f5);
        }
        Render2D.outline(f, f2, f3, f4, f7, 0.5f, n4);
        float f8 = f4 * 0.8076923f;
        float f9 = f4 * 0.125f;
        float f10 = f + f9;
        float f11 = f + f3 - f8 - f9;
        float f12 = f10 + (f11 - f10) * f5;
        float f13 = f2 + (f4 - f8) * 0.5f;
        int n5 = RemoteGuiPanelRenderer.rgba(239, 252, 255, 255.0f * f6);
        int n6 = RemoteGuiPanelRenderer.rgba(242, 250, 255, 255.0f * f6);
        Render2D.rect(f12, f13, f8, f8, f8 * 0.5f, n5, n5, n6, n5);
        Render2D.outline(f12, f13, f8, f8, f8 * 0.5f, 0.5f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 210.0f * f6));
    }

    private static List<Module> visibleModules(Category category) {
        if (category == null) {
            return List.of();
        }
        return RemoteGuiPanelRenderer.filteredModules(category, "");
    }

    private static void remoteFillHorizontal(RemoteTheme remoteTheme, float f, float f2, float f3, float f4, float f5, float f6) {
        if (f3 <= 0.0f || f4 <= 0.0f || f6 <= 0.0f) {
            return;
        }
        int n = remoteTheme.gradientColor(0.0f, f6);
        int n2 = remoteTheme.gradientColor(1.0f, f6);
        Render2D.rect(f, f2, f3, f4, f5, n, n2, n2, n);
    }

    private static void renderSidebar(float f, float f2, RemoteGuiPanel remoteGuiPanel, RemoteTheme remoteTheme, float f3) {
        float f4;
        float f5;
        float f6;
        float f7;
        float f8 = 110.0f;
        float f9 = f + 9.0f;
        int n = MAIN_CATEGORIES.length + OTHER_CATEGORIES.length;
        float f10 = f2 + 3.0f;
        float f11 = 26.0f;
        RenderHelper.drawPanelBg(f, f10, f8, f11, 12.0f, 0.0f, 0.0f, 0.0f, f3);
        float f12 = f10 + f11 * 0.5f;
        String string = "x";
        float f13 = 11.7f;
        float f14 = 14.299999f;
        float f15 = 7.7999997f;
        float f16 = 15.0f;
        float f17 = Fonts.SMALL_PIXEL.msdfWidth("NV", f14);
        float f18 = f + (f8 - (f16 + f15 + f17)) * 0.5f;
        Render2D.image("nv:textures/logo.png", f18, f12 - f16 * 0.5f, f16, f16, RemoteGuiPanelRenderer.color(255, 255, 255, 255, f3));
        Fonts.SMALL_PIXEL.msdf("NV", f18 + f16 + f15, f12 - f14 * 0.5f + 0.5f, f14, RemoteGuiPanelRenderer.color(255, 255, 255, 255, f3));
        float f19 = f2 + 34.0f;
        float f20 = f19 + 10.0f;
        float f21 = remoteGuiPanel.modulesHeaderT();
        if (f21 > 0.004f) {
            Render2D.rect(f + 4.0f, f19 + 4.5f, f8 - 8.0f, 13.333333f, 5.0f, RemoteGuiPanelRenderer.color(255, 255, 255, Math.round(16.0f * f21), f3));
        }
        String string2 = "h";
        float f22 = Fonts.NV.msdfWidth(string2, 8.0f);
        RemoteGuiPanelRenderer.remoteMsdfIcon(remoteTheme, "nv", string2, f9, f20 - 4.0f + 1.5f, 8.0f, (200.0f + 55.0f * f21) * f3, 0.15f);
        Fonts.MONTSERRAT_MEDIUM.draw("Modules", f9 + f22 + 6.0f, f20 - 4.0f + 0.5f, 8.0f, RemoteGuiPanelRenderer.color(255, 255, 255, Math.round(215.0f + 40.0f * f21), f3));
        float f23 = f19 + 20.0f + 4.0f;
        float f24 = f9 + 3.0f;
        float f25 = f24 + 9.0f;
        float f26 = f23 + 9.0f;
        float f27 = f23 + (float)(MAIN_CATEGORIES.length - 1) * 18.0f + 9.0f;
        Render2D.rect(f24, f26, 1.0f, f27 - f26, 0.0f, RemoteGuiPanelRenderer.color(255, 255, 255, 36, f3));
        float f28 = 2.0f;
        for (int i = 0; i < MAIN_CATEGORIES.length; ++i) {
            Category category = MAIN_CATEGORIES[i];
            f7 = f23 + (float)i * 18.0f;
            float f29 = f7 + 9.0f;
            f6 = remoteGuiPanel.categoryAnimT(category);
            int n2 = Math.min(255, 140 + Math.round(f6 * 115.0f));
            int n3 = RemoteGuiPanelRenderer.color(255, 255, 255, n2, f3);
            String string3 = String.valueOf(RemoteGuiPanelRenderer.iconChar(category));
            float f30 = Fonts.NV.msdfWidth(string3, 7.0f);
            if (f6 > 0.01f) {
                f5 = (f30 + 5.0f + Fonts.MONTSERRAT_MEDIUM.width(category.getDisplayName(), 7.0f)) * f6;
                RemoteGuiPanelRenderer.remoteFillHorizontal(remoteTheme, f25, f29 + 8.5f - f28, f5, 0.75f, 0.625f, 88.0f * f6 * f3);
            }
            f5 = n > 1 ? (float)i / (float)(n - 1) : 0.5f;
            RemoteGuiPanelRenderer.remoteMsdfIcon(remoteTheme, "nv", string3, f25, f29 - 3.5f + 1.5f - f28, 7.0f, (float)n2 * f3, f5);
            Fonts.MONTSERRAT_MEDIUM.draw(category.getDisplayName(), f25 + f30 + 5.0f, f29 - 3.5f + 0.5f - f28, 7.0f, n3);
            String string4 = String.valueOf(RemoteGuiPanelRenderer.visibleModules(category).size());
            f4 = Fonts.MONTSERRAT_MEDIUM.width(string4, 5.5f);
            Fonts.MONTSERRAT_MEDIUM.draw(string4, f + f8 - 10.0f - f4, f29 - 2.75f + 0.5f - f28, 5.5f, RemoteGuiPanelRenderer.color(255, 255, 255, Math.round(80.0f + 80.0f * f6), f3));
        }
        float f39 = remoteTheme.isThemeMode() ? 1.0f : 0.0f;
        f5 = f23 + (float)MAIN_CATEGORIES.length * 18.0f + 10.0f;
        for (int i = 0; i < OTHER_CATEGORIES.length; ++i) {
            float f40;
            Category category = OTHER_CATEGORIES[i];
            float f41 = f40 = category == Category.THEMES ? f39 : 1.0f;
            if (f40 <= 0.01f) continue;
            float f42 = f5 + (float)i * 19.0f + 9.5f;
            float f43 = remoteGuiPanel.categoryAnimT(category);
            int n6 = Math.round((float)Math.min(255, 140 + Math.round(f43 * 115.0f)) * f40);
            int n7 = RemoteGuiPanelRenderer.color(255, 255, 255, n6, f3);
            String string7 = String.valueOf(RemoteGuiPanelRenderer.iconChar(category));
            float f44 = Fonts.NV.msdfWidth(string7, 7.5f);
            RemoteGuiPanelRenderer.remoteMsdfIcon(remoteTheme, "nv", string7, f9, f42 - 3.75f + 1.5f - f28, 7.5f, (float)n6 * f3, 0.85f);
            Fonts.MONTSERRAT_MEDIUM.draw(category.getDisplayName(), f9 + f44 + 6.0f, f42 - 3.5f + 0.5f - f28, 7.0f, n7);
        }
    }

    private static void renderSearchField(DrawContext drawContext, float f, float f2, float f3, float f4, String string, RemoteTheme remoteTheme, float f5) {
        String string2 = string == null ? "" : string;
        float f6 = string2.isBlank() ? 0.0f : 1.0f;
        int n = remoteTheme.accent(255.0f) & 0xFFFFFF;
        Render2D.rect(f, f2, f3, f4, 4.0f, RemoteGuiPanelRenderer.color(0, 0, 0, Math.round(40.0f + 12.0f * f6), f5));
        float f7 = 7.5f;
        float f8 = f + 5.0f;
        float f9 = f2 + (f4 - f7) * 0.5f + 0.3f;
        int n2 = RemoteGuiPanelRenderer.mixRgb(0xB4B4B4, n, f6);
        int n3 = RemoteGuiPanelRenderer.clamp255((125.0f + 95.0f * f6) * f5);
        Fonts.NV.msdf(NvIcons.SEARCH, f8, f9, f7, n3 << 24 | n2);
        float f10 = f8 + f7 + 4.0f;
        float f11 = f + f3 - 5.0f;
        float f12 = Math.max(4.0f, f11 - f10);
        float f13 = f2 + (f4 - 7.0f) * 0.5f - 0.5f;
        Render2D.pushScissor(drawContext, f10 - 2.0f, f2, f12 + 2.0f, f4);
        if (string2.isEmpty()) {
            Fonts.MONTSERRAT_MEDIUM.draw("\u041f\u043e\u0438\u0441\u043a...", f10, f13, 7.0f, RemoteGuiPanelRenderer.color(255, 255, 255, 95, f5));
        } else {
            float f14 = Fonts.MONTSERRAT_MEDIUM.width(string2, 7.0f);
            float f15 = Math.max(0.0f, f14 - f12 + 4.0f);
            Fonts.MONTSERRAT_MEDIUM.draw(string2, f10 - f15, f13, 7.0f, RemoteGuiPanelRenderer.color(255, 255, 255, 235, f5));
            float f16 = (float)(Math.sin((double)System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            float f17 = f10 - f15 + f14;
            Render2D.rect(f17, f2 + 2.5f, 0.6f, f4 - 5.0f, 0.0f, RemoteGuiPanelRenderer.color(255, 255, 255, Math.round(70.0f + 185.0f * f16), f5));
        }
        Render2D.popScissor(drawContext);
    }

    private static void renderThemesGrid(DrawContext drawContext, float f, float f2, RemoteGuiPanel remoteGuiPanel, GuiShareRemoteState guiShareRemoteState, RemoteTheme remoteTheme, float f3) {
        float f4;
        float f5;
        float f6;
        float f7;
        float f8;
        float f9 = f + 117.0f;
        float f10 = f2 + 5.0f;
        float f11 = 308.0f;
        float f12 = 280.0f;
        float f13 = 3.0f;
        float f14 = 4.0f;
        float f15 = 4.0f;
        float f16 = 35.0f;
        float f17 = (f11 - f13 - f14 * 2.0f) * 0.5f;
        float f18 = f9 + f14;
        float f19 = f18 + f17 + f13;
        Theme[] themeArray = Theme.values();
        int n = (themeArray.length + 1) / 2;
        float f20 = (float)n * (f16 + f13) - f13;
        float f21 = Math.max(0.0f, f20 - f12 + f14 * 2.0f);
        float f22 = RemoteGuiPanelRenderer.clamp(remoteGuiPanel.smoothThemesScroll(), 0.0f, f21);
        String string = guiShareRemoteState.identityKey();
        String string2 = guiShareRemoteState.selectedTheme() == null ? "" : guiShareRemoteState.selectedTheme();
        Render2D.pushScissor(drawContext, f9, f10, f11, f12);
        boolean bl = RemoteGuiWorld.beginCardBlurJob(drawContext, f9, f10, f11, f12);
        float f23 = (1.0f - remoteGuiPanel.categoryT()) * 8.0f;
        for (int i = 0; i < themeArray.length; ++i) {
            float f24;
            float f25;
            float f26;
            Theme theme = themeArray[i];
            int n2 = i % 2;
            int n3 = i / 2;
            f8 = n2 == 0 ? f18 : f19;
            f7 = f10 + f14 + (float)n3 * (f16 + f13) + f23 - f22;
            if (f7 + f16 < f10 - 5.0f || f7 > f10 + f12 + 5.0f || (f6 = remoteGuiPanel.rowAppear(n3)) < 0.001f) continue;
            f5 = Math.min(1.0f, f6 / 0.6f);
            f4 = f5 * f5;
            float f27 = f6 < 0.6f ? 0.0f : (f6 - 0.6f) / 0.4f;
            float f28 = 1.0f - f27 * f27 * (3.0f - 2.0f * f27);
            boolean bl2 = f6 < 0.999f;
            float f29 = Math.max(f7 += (1.0f - f5) * 6.0f, f10);
            float f30 = Math.min(f7 + f16, f10 + f12);
            boolean bl3 = bl2 && bl && f30 - f29 > 0.5f && RemoteGuiWorld.recordCardBlurRect(f8, f29, f17, f30 - f29, f4, f28);
            float f31 = f3;
            if (bl2) {
                if (!bl3) {
                    f31 *= f4;
                }
                f26 = 0.85f + 0.15f * f5;
                f25 = f8 + f17 * 0.5f;
                f24 = f7 + f16 * 0.5f;
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(f25, f24);
                drawContext.getMatrices().scale(f26, f26);
                drawContext.getMatrices().translate(-f25, -f24);
            }
            f26 = RemoteGuiPanelRenderer.selectAnim(string, theme.name(), theme.name().equals(string2));
            Render2D.rect(f8, f7, f17, f16, f15, RemoteGuiPanelRenderer.rgba(0, 0, 0, 40.0f * f31));
            f25 = 14.0f * (1.0f - f26) * f31;
            if (f25 > 0.5f) {
                Render2D.outline(f8, f7, f17, f16, f15, f15, f15, f15, 0.6f, RemoteGuiPanelRenderer.rgba(255, 255, 255, f25));
            }
            if (f26 > 0.01f) {
                int n4 = remoteTheme.gradientA(185.0f * f26 * f31);
                int n5 = remoteTheme.gradientB(185.0f * f26 * f31);
                Render2D.rect(f8 + 2.0f, f7 + 3.0f, 1.0f, f16 - 6.0f, 6.0f, n4, n4, n5, n5);
                int n6 = remoteTheme.gradientA(110.0f * f26 * f31);
                int n7 = remoteTheme.gradientB(110.0f * f26 * f31);
                int n8 = RemoteGuiPanelRenderer.themeRgba(RemoteGuiPanelRenderer.mixRgb(remoteTheme.gradientA(255.0f) & 0xFFFFFF, remoteTheme.gradientB(255.0f) & 0xFFFFFF, 0.5f), 110.0f * f26 * f31);
                Render2D.outline(f8, f7, f17, f16, f15, f15, f15, f15, 0.7f, n6, n8, n7, n8);
            }
            f24 = f8 + 8.0f + f26 * 3.0f;
            Fonts.MONTSERRAT_MEDIUM.draw(theme.displayName(), f24, f7 + 6.0f, 7.0f, RemoteGuiPanelRenderer.rgba(255, 255, 255, (200.0f + 20.0f * f26) * f31));
            if (f26 > 0.01f) {
                Render2D.rect(f8 + f17 - 9.0f, f7 + 8.5f, 3.0f, 3.0f, 1.5f, remoteTheme.accentBright(220.0f * f26 * f31));
            }
            int[] nArray = theme.palette().length >= 2 ? theme.palette() : theme.shades();
            float f32 = 9.0f;
            float f33 = 3.0f;
            float f34 = f7 + f16 - f32 - 7.0f;
            int n9 = Math.min(nArray.length, 5);
            for (int j = 0; j < n9; ++j) {
                float f35 = f8 + 8.0f + (float)j * (f32 + f33);
                Render2D.rect(f35, f34, f32, f32, 3.0f, RemoteGuiPanelRenderer.themeRgba(nArray[j], 235.0f * f31));
            }
            if (!bl2) continue;
            drawContext.getMatrices().popMatrix();
        }
        remoteGuiPanel.markAppearFrameDone();
        Render2D.popScissor(drawContext);
        if (bl) {
            RemoteGuiWorld.endCardBlurJob(drawContext);
        }
        if (f21 > 0.5f) {
            float f36 = 3.0f;
            float f37 = f9 + f11 + 0.25f;
            float f38 = f10 + f36;
            float f39 = f12 - f36 * 2.0f;
            f8 = RemoteGuiPanelRenderer.clamp(f12 / Math.max(f20, f12), 0.0f, 1.0f);
            f7 = RemoteGuiPanelRenderer.clamp(f39 * f8, 12.0f, f39);
            f6 = Math.max(0.0f, f39 - f7);
            f5 = RemoteGuiPanelRenderer.clamp(f22 / Math.max(f21, 1.0f), 0.0f, 1.0f);
            f4 = f38 + f6 * f5;
            Render2D.rect(f37, f38, 1.25f, f39, 1.0f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 18.0f * f3));
            RemoteGuiPanelRenderer.remoteFillVertical(remoteTheme, f37, f4, 1.25f, f7, 1.0f, 165.0f * f3);
        }
    }

    private static float popupRowPreferredWidth(GuiSharePopupRow guiSharePopupRow) {
        float f = Fonts.MONTSERRAT_MEDIUM.width(guiSharePopupRow.name(), 6.5f);
        float f2 = Fonts.MONTSERRAT_MEDIUM.width(guiSharePopupRow.value(), 6.0f);
        return switch (guiSharePopupRow.type()) {
            case "s" -> Math.max(100.0f, 6.0f + f + 6.0f + f2 + 6.0f);
            case "b" -> 6.0f + f + 6.0f + 19.1f + 4.0f;
            case "m", "n", "k" -> 6.0f + f + 6.0f + f2 + 10.0f + 8.0f;
            case "u" -> 6.0f + f + 6.0f + Math.max(34.0f, f2 + 14.0f) + 4.0f + 4.0f;
            case "p" -> Fonts.MONTSERRAT_MEDIUM.width(guiSharePopupRow.name(), 6.0f) + 48.0f;
            case "c" -> 6.0f + f + 6.0f + 8.0f + 4.0f + Fonts.MONTSERRAT_MEDIUM.width(guiSharePopupRow.value(), 5.5f) + 10.0f + 8.0f;
            case "x" -> 6.0f + f + 6.0f + 92.0f + 8.0f;
            default -> 120.0f;
        };
    }

    private static void renderPopupSlider(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        float f5 = f + 6.0f;
        float f6 = f3 - 12.0f;
        float f7 = f2 + 16.0f;
        float f8 = 3.0f;
        String string = guiSharePopupRow.value();
        float f9 = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f);
        float f10 = f + f3 - 6.0f - f9;
        Fonts.MONTSERRAT_MEDIUM.draw(string, f10, f2 + 5.0f + 0.25f, 6.0f, RemoteGuiPanelRenderer.rgba(255, 255, 255, 238.0f * f4));
        RenderHelper.drawName(guiSharePopupRow.name(), f, f2, f10 - (f + 6.0f) - 6.0f, f4);
        float f11 = f5 + f6 * RemoteGuiPanelRenderer.clamp(guiSharePopupRow.fraction(), 0.0f, 1.0f);
        float f12 = f8 * 0.5f;
        Render2D.rect(f5, f7, f6, f8, f12, RemoteGuiPanelRenderer.rgba(16, 16, 16, 64.0f * f4));
        if (f11 - f5 > 0.6f) {
            int n = remoteTheme.gradientA(215.0f * f4);
            int n2 = remoteTheme.gradientB(215.0f * f4);
            Render2D.rect(f5, f7, f11 - f5, f8, f12, n, n2, n2, n);
            Render2D.outline(f5, f7, f11 - f5, f8, f12, 0.5f, remoteTheme.accentBright(150.0f * f4));
        }
        float f13 = 6.5f;
        float f14 = 4.0f;
        float f15 = 2.0f;
        float f16 = Math.max(f5, Math.min(f5 + f6 - f13, f11 - f13 * 0.5f));
        float f17 = f7 + f8 * 0.5f - f14 * 0.5f;
        Render2D.rect(f16, f17, f13, f14, f15, RemoteGuiPanelRenderer.rgba(255, 255, 255, 245.0f * f4));
        Render2D.outline(f16 - 0.5f, f17 - 0.5f, f13 + 1.0f, f14 + 1.0f, f15, 0.5f, RemoteGuiPanelRenderer.rgba(16, 16, 16, 128.0f * f4));
    }



    private static void renderPopupChip(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4, boolean bl) {
        String string = guiSharePopupRow.value();
        float f5 = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f);
        float f6 = bl ? Math.max(34.0f, f5 + 14.0f) : f5 + 10.0f;
        float f7 = f + f3 - f6 - 4.0f;
        float f8 = f2 + 2.0f;
        RenderHelper.drawName(guiSharePopupRow.name(), f, f2, f7 - (f + 6.0f) - 4.0f, f4);
        RemoteGuiPanelRenderer.drawPopupBtnBg(remoteTheme, f7, f8, f6, 12.0f, f4);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f7 + (f6 - f5) * 0.5f, f8 + 2.5f, 6.0f, remoteTheme.accentSoft(220.0f * f4));
    }

    private static void renderPopupSeparator(RemoteTheme remoteTheme, GuiSharePopupRow guiSharePopupRow, float f, float f2, float f3, float f4) {
        float f5;
        String string = guiSharePopupRow.name();
        float f6 = 6.0f;
        float f7 = 18.0f;
        float f8 = Fonts.MONTSERRAT_MEDIUM.width(string, f6);
        if (f8 > (f5 = Math.max(8.0f, f3 - 24.0f))) {
            f8 = f5;
        }
        float f9 = f + f3 * 0.5f;
        float f10 = f9 - f8 * 0.5f;
        float f11 = f2 + f7 * 0.5f - f6 * 0.5f;
        float f12 = f2 + f7 * 0.5f;
        float f13 = 6.0f;
        float f14 = 4.0f;
        int n = remoteTheme.accentSoft(210.0f * f4);
        int n2 = remoteTheme.accentSoft(55.0f * f4);
        float f15 = f + f14;
        float f16 = Math.max(0.0f, f10 - f13 - f15);
        float f17 = f10 + f8 + f13;
        float f18 = Math.max(0.0f, f + f3 - f14 - f17);
        if (f16 > 1.0f) {
            Render2D.rect(f15, f12, f16, 1.0f, 0.5f, n2);
        }
        if (f18 > 1.0f) {
            Render2D.rect(f17, f12, f18, 1.0f, 0.5f, n2);
        }
        Fonts.MONTSERRAT_MEDIUM.draw(string, f10, f11, f6, n);
    }

    private static void drawThemeGlow(RemoteTheme remoteTheme, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = remoteTheme.glowBlend();
        if (f7 <= 0.001f) {
            return;
        }
        float f8 = remoteTheme.glowIntensity();
        float f9 = remoteTheme.glowRadius();
        if (f8 <= 0.0f || f9 <= 0.0f) {
            return;
        }
        BuiltGlow builtGlow = new BuiltGlow(f, f2, f3, f4, new float[]{f5, f5, f5, f5}, remoteTheme.primaryColor(), f8, f9, RemoteGuiPanelRenderer.clamp(f6, 0.0f, 1.0f) * f7).withSecondColor(remoteTheme.secondaryColor(), remoteTheme.colorOffset()).withPalette(remoteTheme.palette6(), remoteTheme.phase(), remoteTheme.styleId(), remoteTheme.closed());
        Render2D.glow(builtGlow);
    }

    private static void remoteMsdfIcon(RemoteTheme remoteTheme, String string, String string2, float f, float f2, float f3, float f4, float f5) {
        if (f4 <= 0.0f || string2 == null || string2.isEmpty()) {
            return;
        }
        float f6 = f5 < 0.0f ? 0.0f : (f5 > 1.0f ? 1.0f : f5);
        float f7 = f6 * 0.42f;
        float f8 = 0.28f + f6 * 0.72f;
        int n = remoteTheme.gradientColor(f7, f4);
        int n2 = remoteTheme.gradientColor(f8, f4);
        Render2D.msdfText(string, string2, f, f2, f3, n, n2, n2, n);
    }


    private static void remoteFillVertical(RemoteTheme remoteTheme, float f, float f2, float f3, float f4, float f5, float f6) {
        if (f3 <= 0.0f || f4 <= 0.0f || f6 <= 0.0f) {
            return;
        }
        int n = remoteTheme.gradientColor(0.0f, f6);
        int n2 = remoteTheme.gradientColor(1.0f, f6);
        Render2D.rect(f, f2, f3, f4, f5, n, n, n2, n2);
    }

    private static void renderPanelBackground(RemoteTheme remoteTheme, int n, float f, float f2, float f3) {
        float f4 = RemoteGuiPanelRenderer.panelRadius(remoteTheme);
        RemoteGuiPanelRenderer.drawThemeGlow(remoteTheme, f, f2, 430.0f, 290.0f, f4, f3);
        RemoteGuiPanelRenderer.drawThemeGlass(remoteTheme, n, f, f2, 430.0f, 290.0f, f4, f3);
    }



    static Category resolveCategory(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            return Category.valueOf(string.toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    public static float[] popupDrawRect(RemoteGuiPanel remoteGuiPanel) {
        float f;
        PopupLayout popupLayout = RemoteGuiPanelRenderer.popupLayout(80.0f, 102.0f, remoteGuiPanel);
        if (popupLayout == null) {
            return null;
        }
        float f2 = popupLayout.px();
        float f3 = popupLayout.py();
        float f4 = f2 + popupLayout.width();
        float f5 = f3 + popupLayout.height();
        List<GuiSharePopupRow> list = (List<GuiSharePopupRow>) (Object) popupLayout.rows();
        if (!list.isEmpty()) {
            f = popupLayout.px() + 5.0f;
            float f6 = popupLayout.width() - 10.0f;
            float f7 = Math.max(0.0f, popupLayout.contentH() - popupLayout.bodyViewH());
            float f8 = RemoteGuiPanelRenderer.clamp(remoteGuiPanel.smoothPopupScroll(), 0.0f, f7);
            float f9 = popupLayout.py() + 7.0f - f8;
            for (GuiSharePopupRow guiSharePopupRow : list) {
                float[] fArray;
                if (guiSharePopupRow.open() && RemoteGuiPanelRenderer.hasOverlay(guiSharePopupRow) && (fArray = RemoteGuiPanelRenderer.overlayRect(guiSharePopupRow, f, f9, f6)) != null) {
                    f2 = Math.min(f2, fArray[0]);
                    f3 = Math.min(f3, fArray[1]);
                    f4 = Math.max(f4, fArray[0] + fArray[2]);
                    f5 = Math.max(f5, fArray[1] + fArray[3]);
                }
                f9 += RemoteGuiPanelRenderer.popupRowHeight(guiSharePopupRow) + 4.0f;
            }
        }
        f = Math.max(6.0f, remoteGuiPanel.theme().glowRadius()) + 8.0f;
        return new float[]{f2 - f, f3 - f, f4 - f2 + f * 2.0f, f5 - f3 + f * 2.0f};
    }

    private static float selectAnim(String string2, String string3, boolean bl) {
        if (themeSelectAnims.size() > 512) {
            themeSelectAnims.clear();
        }
        Decelerate decelerate = themeSelectAnims.computeIfAbsent(string2 + "|" + string3, string -> RemoteGuiPanelRenderer.createAnim(220));
        decelerate.setDirection(bl ? Direction.FORWARDS : Direction.BACKWARDS);
        return decelerate.getOutput().floatValue();
    }

    private static float enableAnim(String string2, String string3, boolean bl) {
        if (enableAnims.size() > 512) {
            enableAnims.clear();
        }
        Decelerate decelerate = enableAnims.computeIfAbsent(string2 + "|" + string3, string -> RemoteGuiPanelRenderer.createAnim(220));
        decelerate.setDirection(bl ? Direction.FORWARDS : Direction.BACKWARDS);
        return decelerate.getOutput().floatValue();
    }

    private static int themeRgba(int n, float f) {
        int n2 = RemoteGuiPanelRenderer.clamp255(f);
        if (n2 <= 0) {
            return 0;
        }
        return n2 << 24 | n & 0xFFFFFF;
    }
}

