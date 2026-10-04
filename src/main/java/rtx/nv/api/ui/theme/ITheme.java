package rtx.nv.api.ui.theme;

public interface ITheme {
    String id();
    String displayName();
    String category();
    ThemeProfile profile();
    int[] shades();
    int accentRgb();
    int gradientA();
    int gradientB();
    int[] palette();
    int accentSoftRgb();
    int accentFillRgb();
    int accentBrightRgb();
    int toggleOnRgb();
    boolean isCustom();
}
