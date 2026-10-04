package rtx.nv.api.mods.geckolib.animation.object;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.animation.state.EasingState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public interface EasingType {
    public static final Map<String, EasingType> EASING_TYPES = new ConcurrentHashMap<String, EasingType>(64);
    public static final EasingType LINEAR = EasingType.register("linear", EasingType.register("none", d -> EasingType.easeIn(EasingType::linear)));
    public static final EasingType STEP = EasingType.register("step", d -> EasingType.easeIn(EasingType.step(d)));
    public static final EasingType EASE_IN_SINE = EasingType.register("easeinsine", d -> EasingType.easeIn(EasingType::sine));
    public static final EasingType EASE_OUT_SINE = EasingType.register("easeoutsine", d -> EasingType.easeOut(EasingType::sine));
    public static final EasingType EASE_IN_OUT_SINE = EasingType.register("easeinoutsine", d -> EasingType.easeInOut(EasingType::sine));
    public static final EasingType EASE_IN_QUAD = EasingType.register("easeinquad", d -> EasingType.easeIn(EasingType::quadratic));
    public static final EasingType EASE_OUT_QUAD = EasingType.register("easeoutquad", d -> EasingType.easeOut(EasingType::quadratic));
    public static final EasingType EASE_IN_OUT_QUAD = EasingType.register("easeinoutquad", d -> EasingType.easeInOut(EasingType::quadratic));
    public static final EasingType EASE_IN_CUBIC = EasingType.register("easeincubic", d -> EasingType.easeIn(EasingType::cubic));
    public static final EasingType EASE_OUT_CUBIC = EasingType.register("easeoutcubic", d -> EasingType.easeOut(EasingType::cubic));
    public static final EasingType EASE_IN_OUT_CUBIC = EasingType.register("easeinoutcubic", d -> EasingType.easeInOut(EasingType::cubic));
    public static final EasingType EASE_IN_QUART = EasingType.register("easeinquart", d -> EasingType.easeIn(EasingType.pow(4.0)));
    public static final EasingType EASE_OUT_QUART = EasingType.register("easeoutquart", d -> EasingType.easeOut(EasingType.pow(4.0)));
    public static final EasingType EASE_IN_OUT_QUART = EasingType.register("easeinoutquart", d -> EasingType.easeInOut(EasingType.pow(4.0)));
    public static final EasingType EASE_IN_QUINT = EasingType.register("easeinquint", d -> EasingType.easeIn(EasingType.pow(4.0)));
    public static final EasingType EASE_OUT_QUINT = EasingType.register("easeoutquint", d -> EasingType.easeOut(EasingType.pow(5.0)));
    public static final EasingType EASE_IN_OUT_QUINT = EasingType.register("easeinoutquint", d -> EasingType.easeInOut(EasingType.pow(5.0)));
    public static final EasingType EASE_IN_EXPO = EasingType.register("easeinexpo", d -> EasingType.easeIn(EasingType::exp));
    public static final EasingType EASE_OUT_EXPO = EasingType.register("easeoutexpo", d -> EasingType.easeOut(EasingType::exp));
    public static final EasingType EASE_IN_OUT_EXPO = EasingType.register("easeinoutexpo", d -> EasingType.easeInOut(EasingType::exp));
    public static final EasingType EASE_IN_CIRC = EasingType.register("easeincirc", d -> EasingType.easeIn(EasingType::circle));
    public static final EasingType EASE_OUT_CIRC = EasingType.register("easeoutcirc", d -> EasingType.easeOut(EasingType::circle));
    public static final EasingType EASE_IN_OUT_CIRC = EasingType.register("easeinoutcirc", d -> EasingType.easeInOut(EasingType::circle));
    public static final EasingType EASE_IN_BACK = EasingType.register("easeinback", d -> EasingType.easeIn(EasingType.back(d)));
    public static final EasingType EASE_OUT_BACK = EasingType.register("easeoutback", d -> EasingType.easeOut(EasingType.back(d)));
    public static final EasingType EASE_IN_OUT_BACK = EasingType.register("easeinoutback", d -> EasingType.easeInOut(EasingType.back(d)));
    public static final EasingType EASE_IN_ELASTIC = EasingType.register("easeinelastic", d -> EasingType.easeIn(EasingType.elastic(d)));
    public static final EasingType EASE_OUT_ELASTIC = EasingType.register("easeoutelastic", d -> EasingType.easeOut(EasingType.elastic(d)));
    public static final EasingType EASE_IN_OUT_ELASTIC = EasingType.register("easeinoutelastic", d -> EasingType.easeInOut(EasingType.elastic(d)));
    public static final EasingType EASE_IN_BOUNCE = EasingType.register("easeinbounce", d -> EasingType.easeIn(EasingType.bounce(d)));
    public static final EasingType EASE_OUT_BOUNCE = EasingType.register("easeoutbounce", d -> EasingType.easeOut(EasingType.bounce(d)));
    public static final EasingType EASE_IN_OUT_BOUNCE = EasingType.register("easeinoutbounce", d -> EasingType.easeInOut(EasingType.bounce(d)));
    public static final EasingType CATMULLROM = EasingType.register("catmullrom", new EasingType.CatmullRomEasing());

    public static Double2DoubleFunction pow(double d) {
        return d2 -> Math.pow(d2, d);
    }

    public static double exp(double d) {
        return Math.pow(2.0, 10.0 * (d - 1.0));
    }

    default public double apply(EasingState easingState, ControllerState controllerState) {
        Double d = null;
        if (easingState.easingArgs().length != 0) {
            d = easingState.easingArgs()[0].get(controllerState);
        }
        return this.apply(easingState, d, easingState.delta(), controllerState);
    }

    default public double apply(EasingState easingState, Double d, double d2, ControllerState controllerState) {
        if (easingState.delta() >= 1.0) {
            return easingState.toValue();
        }
        return MathHelper.lerp(this.buildTransformer(d).apply(d2), (double)easingState.fromValue(), (double)easingState.toValue());
    }

    public static EasingType register(String string, EasingType easingType) {
        EASING_TYPES.putIfAbsent(string, easingType);
        return easingType;
    }

    public static Double2DoubleFunction step(Double stepsArg) {
        double steps = stepsArg == null ? 2.0 : stepsArg;
        if (steps < 2.0) {
            throw new IllegalArgumentException("Steps must be >= 2, got: " + steps);
        }
        int n = (int)steps;
        return d -> {
            if (d < 0.0) {
                return 0.0;
            }
            double stepSize = 1.0 / (double)n;
            double maxVal = (double)(n - 1) * stepSize;
            if (d > 1.0) {
                return maxVal;
            }
            int n2 = 0;
            int n3 = n - 1;
            while (n3 - n2 != 1) {
                int n4 = n2 + (n3 - n2) / 2;
                if (d >= (double)n4 * stepSize) {
                    n2 = n4;
                    continue;
                }
                n3 = n4;
            }
            return (double)n2 * stepSize;
        };
    }

    public static Double2DoubleFunction back(Double d) {
        double d3 = d == null ? 1.70158 : d * 1.70158;
        return d2 -> d2 * d2 * ((d3 + 1.0) * d2 - d3);
    }

    public static EasingType fromString(String string) {
        return EASING_TYPES.getOrDefault(string, LINEAR);
    }

    public static double circle(double d) {
        return 1.0 - Math.sqrt(1.0 - d * d);
    }

    public static Double2DoubleFunction elastic(Double d) {
        double d3 = d == null ? 1.0 : d;
        return d2 -> 1.0 - Math.pow(Math.cos(d2 * Math.PI / 2.0), 3.0) * Math.cos(d2 * d3 * Math.PI);
    }

    public static double catmullRom(double d) {
        return 0.5 * (2.0 * (d + 1.0) + 2.0 + (2.0 * d - 5.0 * (d + 1.0) + 4.0 * (d + 2.0) - (d + 3.0)) + (3.0 * (d + 1.0) - d - 3.0 * (d + 2.0) + (d + 3.0)));
    }

    public static Double2DoubleFunction easeIn(Double2DoubleFunction double2DoubleFunction) {
        return double2DoubleFunction;
    }

    public static Double2DoubleFunction easeOut(Double2DoubleFunction double2DoubleFunction) {
        return d -> 1.0 - double2DoubleFunction.apply(1.0 - d);
    }

    public static double cubic(double d) {
        return d * d * d;
    }

    public static Double2DoubleFunction bounce(Double d3) {
        double d4 = d3 == null ? 0.5 : d3;
        Double2DoubleFunction double2DoubleFunction = d -> 7.5625 * d * d;
        Double2DoubleFunction double2DoubleFunction2 = d2 -> 30.25 * d4 * Math.pow(d2 - 0.5454545617103577, 2.0) + 1.0 - d4;
        Double2DoubleFunction double2DoubleFunction3 = d2 -> 121.0 * d4 * d4 * Math.pow(d2 - 0.8181818127632141, 2.0) + 1.0 - d4 * d4;
        Double2DoubleFunction double2DoubleFunction4 = d2 -> 484.0 * d4 * d4 * d4 * Math.pow(d2 - 0.9545454382896423, 2.0) + 1.0 - d4 * d4 * d4;
        return d -> Math.min(Math.min(double2DoubleFunction.apply(d), double2DoubleFunction2.apply(d)), Math.min(double2DoubleFunction3.apply(d), double2DoubleFunction4.apply(d)));
    }

    public static double sine(double d) {
        return 1.0 - Math.cos(d * Math.PI / 2.0);
    }

    public static Double2DoubleFunction easeInOut(Double2DoubleFunction double2DoubleFunction) {
        return d -> {
            if (d < 0.5) {
                return double2DoubleFunction.apply(d * 2.0) / 2.0;
            }
            return 1.0 - double2DoubleFunction.apply((1.0 - d) * 2.0) / 2.0;
        };
    }

    public static double quadratic(double d) {
        return d * d;
    }

    public static double linear(double d) {
        return d;
    }

    public static Double2DoubleFunction linear(Double2DoubleFunction double2DoubleFunction) {
        return double2DoubleFunction;
    }

    public static EasingType fromJson(JsonElement jsonElement) {
        JsonPrimitive jsonPrimitive;
        if (!(jsonElement instanceof JsonPrimitive) || !(jsonPrimitive = (JsonPrimitive)jsonElement).isString()) {
            return LINEAR;
        }
        return EasingType.fromString(jsonPrimitive.getAsString().toLowerCase(Locale.ROOT));
    }

    public static Double2DoubleFunction stepPositive(Double2DoubleFunction double2DoubleFunction) {
        return d -> d > 0.0 ? 1.0 : 0.0;
    }

    public static double lerpWithOverride(EasingState easingState, ControllerState controllerState) {
        return easingState.easingType().apply(easingState, controllerState);
    }

    public static Double2DoubleFunction stepNonNegative(Double2DoubleFunction double2DoubleFunction) {
        return d -> d >= 0.0 ? 1.0 : 0.0;
    }

    public Double2DoubleFunction buildTransformer(Double var1);


    public static class CatmullRomEasing
    implements EasingType {
        @Override
        public double apply(EasingState easingState, Double d, double d2, ControllerState controllerState) {
            if (easingState.delta() >= 1.0) {
                return easingState.toValue();
            }
            MathValue[] mathValueArray = easingState.easingArgs();
            if (mathValueArray.length < 2) {
                return MathHelper.lerp(this.buildTransformer(d).apply(d2), (double)easingState.fromValue(), (double)easingState.toValue());
            }
            return CatmullRomEasing.getPointOnSpline(d2, mathValueArray[0].get(controllerState), easingState.fromValue(), easingState.toValue(), mathValueArray[1].get(controllerState));
        }
    
        @Override
        public Double2DoubleFunction buildTransformer(Double d) {
            return EasingType.easeInOut(EasingType::catmullRom);
        }
    
        public static double getPointOnSpline(double d, double d2, double d3, double d4, double d5) {
            return 0.5 * (2.0 * d3 + (d4 - d2) * d + (2.0 * d2 - 5.0 * d3 + 4.0 * d4 - d5) * d * d + (3.0 * d3 - d2 - 3.0 * d4 + d5) * d * d * d);
        }
    }
}

