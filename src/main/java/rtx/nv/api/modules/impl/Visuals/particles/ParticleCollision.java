package rtx.nv.api.modules.impl.Visuals.particles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.modules.impl.Visuals.particles.FadeParticle;
import rtx.nv.api.modules.impl.Visuals.particles.WorldParticleUtil;

public final class ParticleCollision {
    public static final String RICOCHET = "\u041e\u0442\u0441\u043a\u043e\u043a";
    public static final String STRONG_RICOCHET = "\u0421\u0438\u043b\u044c\u043d\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a";
    public static final String VERY_STRONG_RICOCHET = "\u041e\u0447\u0435\u043d\u044c \u0441\u0438\u043b\u044c\u043d\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a";
    public static final String WEAK_RICOCHET = "\u0421\u043b\u0430\u0431\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a";
    public static final String VERY_WEAK_RICOCHET = "\u041e\u0447\u0435\u043d\u044c \u0441\u043b\u0430\u0431\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a";
    public static final String IGNORE = "\u0418\u0433\u043d\u043e\u0440\u0438\u0440\u043e\u0432\u0430\u0442\u044c";
    public static final String STUCK = "\u041f\u0440\u0438\u043b\u0438\u043f\u0430\u043d\u0438\u0435";
    public static final String FATAL = "\u0418\u0441\u0447\u0435\u0437\u043d\u043e\u0432\u0435\u043d\u0438\u0435";
    public static final String SLIDE = "\u0421\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435";
    public static final String SLIDE_SLOW = "\u041c\u0435\u0434\u043b\u0435\u043d\u043d\u043e\u0435 \u0441\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435";
    public static final String[] MODES = new String[]{"\u041e\u0442\u0441\u043a\u043e\u043a", "\u0421\u0438\u043b\u044c\u043d\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a", "\u041e\u0447\u0435\u043d\u044c \u0441\u0438\u043b\u044c\u043d\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a", "\u0421\u043b\u0430\u0431\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a", "\u041e\u0447\u0435\u043d\u044c \u0441\u043b\u0430\u0431\u044b\u0439 \u043e\u0442\u0441\u043a\u043e\u043a", "\u0418\u0433\u043d\u043e\u0440\u0438\u0440\u043e\u0432\u0430\u0442\u044c", "\u041f\u0440\u0438\u043b\u0438\u043f\u0430\u043d\u0438\u0435", "\u0418\u0441\u0447\u0435\u0437\u043d\u043e\u0432\u0435\u043d\u0438\u0435", "\u0421\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435", "\u041c\u0435\u0434\u043b\u0435\u043d\u043d\u043e\u0435 \u0441\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435"};
    private static final double SPEED_LIMIT = 5.0;

    private ParticleCollision() {
    }

    public static boolean apply(MinecraftClient minecraftClient, FadeParticle fadeParticle, String string, float f) {
        boolean bl = false;
        if (!IGNORE.equals(string)) {
            if (FATAL.equals(string) || STUCK.equals(string)) {
                if (fadeParticle.holdCollide || ParticleCollision.collidesX(minecraftClient, fadeParticle) || ParticleCollision.collidesY(minecraftClient, fadeParticle) || ParticleCollision.collidesZ(minecraftClient, fadeParticle)) {
                    fadeParticle.motionX = 0.0;
                    fadeParticle.motionY = 0.0;
                    fadeParticle.motionZ = 0.0;
                    fadeParticle.holdCollide = true;
                    if (FATAL.equals(string)) {
                        fadeParticle.dead = true;
                    }
                    bl = true;
                }
            } else {
                boolean bl2 = ParticleCollision.isRicochet(string);
                double d = ParticleCollision.jumpMultiplier(string);
                double d2 = bl2 ? 1.0 : (SLIDE_SLOW.equals(string) ? 0.875 : 1.0);
                boolean bl3 = false;
                if (fadeParticle.holdCollide || ParticleCollision.collidesY(minecraftClient, fadeParticle)) {
                    if (fadeParticle.motionY != 0.0) {
                        double d3 = fadeParticle.motionY = bl2 ? -fadeParticle.motionY * d / ((double)Math.max(f, 0.0f) + 1.0) * Math.min(Math.abs(fadeParticle.motionY) / 0.2, 1.0) : 0.0;
                        if (bl2 && fadeParticle.motionY > 0.0 && fadeParticle.motionY < 0.02) {
                            fadeParticle.motionY = 0.0;
                        }
                    }
                    if (!bl2) {
                        fadeParticle.motionX *= d2;
                        fadeParticle.motionZ *= d2;
                    }
                    bl3 = true;
                }
                if (fadeParticle.holdCollide || ParticleCollision.collidesX(minecraftClient, fadeParticle)) {
                    double d4 = fadeParticle.motionX = bl2 ? -fadeParticle.motionX * d : 0.0;
                    if (!bl2) {
                        fadeParticle.motionZ *= d2;
                    }
                    bl3 = true;
                }
                if (fadeParticle.holdCollide || ParticleCollision.collidesZ(minecraftClient, fadeParticle)) {
                    double d5 = fadeParticle.motionZ = bl2 ? -fadeParticle.motionZ * d : 0.0;
                    if (!bl2) {
                        fadeParticle.motionX *= d2;
                    }
                    bl3 = true;
                }
                if (bl3) {
                    double d6 = Math.sqrt(fadeParticle.motionX * fadeParticle.motionX + fadeParticle.motionY * fadeParticle.motionY + fadeParticle.motionZ * fadeParticle.motionZ);
                    fadeParticle.holdCollide = d6 < 0.008;
                }
            }
        }
        fadeParticle.motionX = MathHelper.clamp((double)fadeParticle.motionX, (double)-5.0, (double)5.0);
        fadeParticle.motionY = MathHelper.clamp((double)fadeParticle.motionY, (double)-5.0, (double)5.0);
        fadeParticle.motionZ = MathHelper.clamp((double)fadeParticle.motionZ, (double)-5.0, (double)5.0);
        return bl;
    }

    private static boolean collidesZ(MinecraftClient minecraftClient, FadeParticle fadeParticle) {
        return WorldParticleUtil.isSolidCollisionBlock((MinecraftClient)minecraftClient, (double)fadeParticle.posX, (double)fadeParticle.posY, (double)(fadeParticle.posZ + fadeParticle.motionZ));
    }

    private static boolean isRicochet(String string) {
        return RICOCHET.equals(string) || STRONG_RICOCHET.equals(string) || VERY_STRONG_RICOCHET.equals(string) || WEAK_RICOCHET.equals(string) || VERY_WEAK_RICOCHET.equals(string);
    }

    private static boolean collidesX(MinecraftClient minecraftClient, FadeParticle fadeParticle) {
        return WorldParticleUtil.isSolidCollisionBlock((MinecraftClient)minecraftClient, (double)(fadeParticle.posX + fadeParticle.motionX), (double)fadeParticle.posY, (double)fadeParticle.posZ);
    }

    private static boolean collidesY(MinecraftClient minecraftClient, FadeParticle fadeParticle) {
        return WorldParticleUtil.isSolidCollisionBlock((MinecraftClient)minecraftClient, (double)fadeParticle.posX, (double)(fadeParticle.posY + fadeParticle.motionY), (double)fadeParticle.posZ);
    }

    private static double jumpMultiplier(String string) {
        if (VERY_WEAK_RICOCHET.equals(string)) {
            return 0.5;
        }
        if (WEAK_RICOCHET.equals(string)) {
            return 0.75;
        }
        if (STRONG_RICOCHET.equals(string)) {
            return 1.5;
        }
        if (VERY_STRONG_RICOCHET.equals(string)) {
            return 2.0;
        }
        return 1.0;
    }
}

