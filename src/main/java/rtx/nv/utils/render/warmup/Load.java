package rtx.nv.utils.render.warmup;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetWarmup;
import rtx.nv.utils.render.render2d.Render2D;

public class Load {
    public static volatile boolean startupWarmupDone = false;
    public static volatile boolean initialReloadSeen = false;

    public static void warmupFonts() {
        String[] stringArray;
        String string = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?-_()[]{}<>/\\|@#$%^&*+=\"'`~";
        String string2 = "\u0410\u0411\u0412\u0413\u0414\u0415\u0401\u0416\u0417\u0418\u0419\u041a\u041b\u041c\u041d\u041e\u041f\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042a\u042b\u042c\u042d\u042e\u042f\u0430\u0431\u0432\u0433\u0434\u0435\u0451\u0436\u0437\u0438\u0439\u043a\u043b\u043c\u043d\u043e\u043f\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044a\u044b\u044c\u044d\u044e\u044f";
        String string3 = string + string2;
        for (String string4 : stringArray = new String[]{"montserrat-medium", "montserrat-regular", "montserrat-semibold", "montserrat-bold", "sf", "sf-medium", "nv", "mainmenu"}) {
            try {
                Render2D.textWidth(string4, string3, 8.0f);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                Render2D.msdfWidth(string4, string3, 8.0f);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    public static void runStartupWarmup() {
        if (startupWarmupDone) {
            return;
        }
        startupWarmupDone = true;
        Load.warmupFonts();
        try {
            CustomPetWarmup.warmup();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

