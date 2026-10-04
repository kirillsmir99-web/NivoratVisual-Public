package rtx.nv.api.modules.impl.Visuals;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.NumberSetting;

public class AspectRatio
extends Module {
    private static AspectRatio instance;
    private final NumberSetting ratio = this.register(new NumberSetting("\u0421\u043e\u043e\u0442\u043d\u043e\u0448\u0435\u043d\u0438\u0435", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u0441\u043e\u043e\u0442\u043d\u043e\u0448\u0435\u043d\u0438\u044f \u0441\u0442\u043e\u0440\u043e\u043d.", 1.0, 0.1, 2.0, 0.01));

    public AspectRatio() {
        super("Aspect Ratio", "\u0418\u0437\u043c\u0435\u043d\u044f\u0435\u0442 \u0441\u043e\u043e\u0442\u043d\u043e\u0448\u0435\u043d\u0438\u0435 \u0441\u0442\u043e\u0440\u043e\u043d \u043f\u0440\u043e\u0435\u043a\u0446\u0438\u0438 \u043c\u0438\u0440\u0430.", Category.VISUALS);
        instance = this;
    }

    public static void apply(Matrix4f matrix4f) {
        AspectRatio aspectRatio = instance;
        if (aspectRatio == null || !aspectRatio.isEnabled() || matrix4f == null) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getWindow() == null) {
            return;
        }
        float f = minecraftClient.getWindow().getFramebufferWidth();
        float f2 = minecraftClient.getWindow().getFramebufferHeight();
        if (f <= 0.0f || f2 <= 0.0f) {
            return;
        }
        float f3 = AspectRatio.resolveRatio(f, f2);
        float f4 = f / f2;
        float f5 = f3 / f2;
        if (!Float.isFinite(f4) || !Float.isFinite(f5) || f5 <= 0.0f) {
            return;
        }
        matrix4f.m00(matrix4f.m00() * (f4 / f5));
    }

    public static AspectRatio getInstance() {
        return instance;
    }

    public static Matrix4f copyAdjusted(Matrix4fc matrix4fc) {
        Matrix4f matrix4f = new Matrix4f(matrix4fc);
        AspectRatio.apply(matrix4f);
        return matrix4f;
    }

    public static float resolveRatio(float f, float f2) {
        AspectRatio aspectRatio = instance;
        if (aspectRatio == null || !aspectRatio.isEnabled() || f2 <= 0.0f) {
            return f;
        }
        return f2 * aspectRatio.ratio.getFloat();
    }
}

