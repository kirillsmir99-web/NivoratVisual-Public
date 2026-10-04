package rtx.nv.api.ui.window;
import java.nio.IntBuffer;
import java.util.Locale;
import net.minecraft.SharedConstants;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.WindowsLibrary;
import rtx.nv.api.ui.window.WindowTitleAnimation;

public final class MainWindow {
    static final String CLIENT_NAME = "NivoratVisual";
    public static final String CLIENT_VERSION = "v1.0.1";
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY = 19;
    public static final String TITLE = MainWindow.getTitle();
    private static WindowsLibrary dwmapi;
    private static long dwmSetWindowAttribute;

    private MainWindow() {
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static String getMinecraftVersionName() {
        return SharedConstants.getGameVersion().name();
    }

    public static String getMainMenuSubtitle() {
        return "NivoratVisual (NV) v1.0.1  \u00b7  " + MainWindow.getMinecraftVersionName();
    }

    private static long getDwmSetWindowAttribute() {
        if (dwmSetWindowAttribute != 0L) {
            return dwmSetWindowAttribute;
        }
        if (dwmapi == null) {
            dwmapi = new WindowsLibrary("dwmapi.dll");
        }
        dwmSetWindowAttribute = dwmapi.getFunctionAddress((CharSequence)"DwmSetWindowAttribute");
        return dwmSetWindowAttribute;
    }

    public static void applyDarkMode(long l) {
        if (!MainWindow.isWindows() || l == 0L) {
            return;
        }
        try {
            long l2 = GLFWNativeWin32.glfwGetWin32Window((long)l);
            long l3 = MainWindow.getDwmSetWindowAttribute();
            if (l2 == 0L || l3 == 0L) {
                return;
            }
            try (MemoryStack memoryStack = MemoryStack.stackPush();){
                IntBuffer intBuffer = memoryStack.ints(1);
                long l4 = MemoryUtil.memAddress((IntBuffer)intBuffer);
                int n = JNI.callPPI((long)l2, (int)-99, (long)l4, (int)4, (long)l3);
                if (n != 0) {
                    JNI.callPPI((long)l2, (int)-91, (long)l4, (int)4, (long)l3);
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static String getTitle() {
        return WindowTitleAnimation.get().currentTitle();
    }
}

