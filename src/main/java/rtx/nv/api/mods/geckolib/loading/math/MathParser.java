package rtx.nv.api.mods.geckolib.loading.math;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.MolangQueries;
import rtx.nv.api.mods.geckolib.loading.math.Operator;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction.Factory;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.ACosFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.ASinFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.ATan2Function;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.ATanFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.AbsFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.CosFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.ExpFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.LogFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.ModFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.PowFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.SinFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.generic.SqrtFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.limit.ClampFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.limit.MaxFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.limit.MinFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.misc.PiFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.misc.ToDegFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.misc.ToRadFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.random.DieRollFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.random.DieRollIntegerFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.random.RandomFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.random.RandomIntegerFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.CeilFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.FloorFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.HermiteBlendFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.LerpFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.LerpRotFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.RoundFunction;
import rtx.nv.api.mods.geckolib.loading.math.function.round.TruncateFunction;
import rtx.nv.api.mods.geckolib.loading.math.value.BooleanNegate;
import rtx.nv.api.mods.geckolib.loading.math.value.Calculation;
import rtx.nv.api.mods.geckolib.loading.math.value.CompoundValue;
import rtx.nv.api.mods.geckolib.loading.math.value.Constant;
import rtx.nv.api.mods.geckolib.loading.math.value.Group;
import rtx.nv.api.mods.geckolib.loading.math.value.Negative;
import rtx.nv.api.mods.geckolib.loading.math.value.Ternary;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;
import rtx.nv.api.mods.geckolib.loading.math.value.VariableAssignment;
import rtx.nv.api.mods.geckolib.object.CompoundException;

public class MathParser {
    private static final Pattern EXPRESSION_FORMAT = Pattern.compile("^[\\w\\s_+-/*%^&|<>=!?:.,()]+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern VALID_DOUBLE = Pattern.compile("[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\d+)(\\.)?((\\d+)?)([eE][+-]?(\\d+))?)|(\\.(\\d+)([eE][+-]?(\\d+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\d+)))[fFdD]?))[\\x00-\\x20]*");
    private static final String MOLANG_RETURN = "return ";
    private static final String STATEMENT_DELIMITER = ";";
    private static final Map<String, MathFunction.Factory<?>> FUNCTION_FACTORIES = Util.make(new ConcurrentHashMap<String, MathFunction.Factory<?>>(18), map -> {
        map.put("math.abs", AbsFunction::new);
        map.put("math.acos", ACosFunction::new);
        map.put("math.asin", ASinFunction::new);
        map.put("math.atan", ATanFunction::new);
        map.put("math.atan2", ATan2Function::new);
        map.put("math.ceil", CeilFunction::new);
        map.put("math.clamp", ClampFunction::new);
        map.put("math.cos", CosFunction::new);
        map.put("math.die_roll", DieRollFunction::new);
        map.put("math.die_roll_integer", DieRollIntegerFunction::new);
        map.put("math.exp", ExpFunction::new);
        map.put("math.floor", FloorFunction::new);
        map.put("math.hermite_blend", HermiteBlendFunction::new);
        map.put("math.lerp", LerpFunction::new);
        map.put("math.lerprotate", LerpRotFunction::new);
        map.put("math.ln", LogFunction::new);
        map.put("math.max", MaxFunction::new);
        map.put("math.min", MinFunction::new);
        map.put("math.mod", ModFunction::new);
        map.put("math.pi", PiFunction::new);
        map.put("math.pow", PowFunction::new);
        map.put("math.random", RandomFunction::new);
        map.put("math.random_integer", RandomIntegerFunction::new);
        map.put("math.round", RoundFunction::new);
        map.put("math.sin", SinFunction::new);
        map.put("math.sqrt", SqrtFunction::new);
        map.put("math.to_deg", ToDegFunction::new);
        map.put("math.to_rad", ToRadFunction::new);
        map.put("math.trunc", TruncateFunction::new);
    });

    public static boolean isNumeric(String string) {
        return NUMERIC.matcher(string).matches();
    }

    protected static boolean isLikelyVariable(String string) {
        if (MolangQueries.isExistingVariable(string)) {
            return true;
        }
        return !MathParser.isNumeric(string) && !MathParser.isFunctionRegistered(string) && !Operator.isOperator((String)string) && !string.equals("?") && !string.equals(":");
    }

    public static MathValue parseJson(JsonElement jsonElement) {
        JsonPrimitive jsonPrimitive;
        if (!(jsonElement instanceof JsonPrimitive) || (jsonPrimitive = (JsonPrimitive)jsonElement).isBoolean()) {
            throw new CompoundException("Bad formatting on Molang expression, expected single value, received: " + jsonElement.getClass().getSimpleName());
        }
        if (jsonPrimitive.isNumber()) {
            return new Constant(jsonPrimitive.getAsDouble());
        }
        if (jsonPrimitive.isString()) {
            String string = jsonPrimitive.getAsString();
            if (VALID_DOUBLE.matcher(string).matches()) {
                return new Constant(Double.parseDouble(string));
            }
            return MathParser.compileMolang(string);
        }
        return new Constant(0.0);
    }

    public static MathValue compileMolang(String string) {
        if (string.startsWith(MOLANG_RETURN)) {
            if ((string = string.substring(MOLANG_RETURN.length())).contains(STATEMENT_DELIMITER)) {
                string = string.substring(0, string.indexOf(STATEMENT_DELIMITER));
            }
        } else if (string.contains(STATEMENT_DELIMITER)) {
            String[] stringArray = string.split(STATEMENT_DELIMITER);
            List<MathValue> objectArrayList = new ObjectArrayList<>(stringArray.length);
            for (String string2 : stringArray) {
                boolean bl = string2.startsWith(MOLANG_RETURN);
                if (bl) {
                    string2 = string2.substring(MOLANG_RETURN.length());
                }
                objectArrayList.add(MathParser.compileExpression(string2));
                if (bl) break;
            }
            return new CompoundValue(objectArrayList.toArray(new MathValue[0]));
        }
        return MathParser.compileExpression(string);
    }

    public static List<Either<String, List<MathValue>>> compileSymbols(char[] cArray) {
        List<Either<String, List<MathValue>>> objectArrayList = new ObjectArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        int n = -1;
        block0: for (int i = 0; i < cArray.length; ++i) {
            char c = cArray[i];
            if (c == '-' && stringBuilder.isEmpty() && (objectArrayList.isEmpty() || n == objectArrayList.size() - 1)) {
                stringBuilder.append(c);
                continue;
            }
            String string = MathParser.tryMergeOperativeSymbols(cArray, i);
            if (string != null) {
                i += string.length() - 1;
                if (!stringBuilder.isEmpty()) {
                    objectArrayList.add(Either.left(stringBuilder.toString()));
                }
                n = objectArrayList.size();
                objectArrayList.add(Either.left(string));
                stringBuilder.setLength(0);
                continue;
            }
            if (c == '(') {
                if (!stringBuilder.isEmpty()) {
                    objectArrayList.add(Either.left(stringBuilder.toString()));
                    stringBuilder.setLength(0);
                }
                List<MathValue> objectArrayList2 = new ObjectArrayList<>();
                int n2 = 1;
                for (int j = i + 1; j < cArray.length; ++j) {
                    char c2 = cArray[j];
                    if (c2 == '(') {
                        ++n2;
                    } else if (c2 == ')') {
                        --n2;
                    } else if (c2 == ',' && n2 == 1) {
                        objectArrayList2.add(MathParser.parseSymbols(MathParser.compileSymbols(stringBuilder.toString().toCharArray())));
                        stringBuilder.setLength(0);
                        continue;
                    }
                    if (n2 == 0) {
                        if (!stringBuilder.isEmpty()) {
                            if (!objectArrayList.isEmpty() && objectArrayList.getLast().left().filter("-"::equals).isPresent() && (objectArrayList.size() == 1 || objectArrayList.get(objectArrayList.size() - 2).left().filter(Operator::isOperator).isPresent())) {
                                objectArrayList.removeLast();
                                objectArrayList2.add(new Negative(MathParser.parseSymbols(MathParser.compileSymbols(stringBuilder.toString().toCharArray()))));
                            } else {
                                objectArrayList2.add(MathParser.parseSymbols(MathParser.compileSymbols(stringBuilder.toString().toCharArray())));
                            }
                        }
                        i = j;
                        objectArrayList.add(Either.right(objectArrayList2));
                        stringBuilder.setLength(0);
                        continue block0;
                    }
                    stringBuilder.append(c2);
                }
                continue;
            }
            stringBuilder.append(c);
        }
        if (!stringBuilder.isEmpty()) {
            objectArrayList.add(Either.left(stringBuilder.toString()));
        }
        return objectArrayList;
    }

    public static MathValue compileExpression(String string) {
        try {
            return MathParser.parseSymbols(MathParser.compileSymbols(MathParser.decomposeExpression(string)));
        }
        catch (CompoundException compoundException) {
            throw compoundException.withMessage("Failed to parse expression '" + string + "'");
        }
    }

    public static boolean isFunctionRegistered(String string) {
        return FUNCTION_FACTORIES.containsKey(string);
    }

    public static Variable getVariableFor(String string) {
        return MolangQueries.getVariableFor(string);
    }

    protected static String tryMergeOperativeSymbols(char[] cArray, int n) {
        int n2;
        char c = cArray[n];
        if (!Operator.isOperativeSymbol((char)c)) {
            return null;
        }
        for (int i = n2 = Math.min(cArray.length - n, Operator.maxOperatorLength()); i > 0; --i) {
            String string = String.copyValueOf(cArray, n, i);
            if (!Operator.isOperator((String)string)) continue;
            return string;
        }
        if (c == '?' || c == ':' || c == ',') {
            return String.valueOf(c);
        }
        return null;
    }

    protected static Optional<MathValue> compileSingleValue(Either<String, List<MathValue>> either) throws CompoundException {
        if (either.right().isPresent()) {
            return Optional.of(new Group((MathValue)((List)either.right().get()).getFirst()));
        }
        return either.left().map(string -> {
            if (string.startsWith("!")) {
                return MathParser.compileSingleValue(Either.left(string.substring(1))).map(BooleanNegate::new).orElse(null);
            }
            if (MathParser.isNumeric(string)) {
                return new Constant(Double.parseDouble(string));
            }
            if (MathParser.isLikelyVariable(string)) {
                if (string.startsWith("-")) {
                    return new Negative(MathParser.getVariableFor(string.substring(1)));
                }
                return MathParser.getVariableFor(string);
            }
            if (MathParser.isFunctionRegistered(string)) {
                return MathParser.compileFunction(string, List.of()).orElse(null);
            }
            return null;
        });
    }

    protected static Optional<Ternary> compileTernary(List<Either<String, List<MathValue>>> list) throws CompoundException {
        int n = list.size();
        if (n < 3) {
            return Optional.empty();
        }
        Supplier<MathValue> supplier = null;
        Supplier<MathValue> supplier2 = null;
        int n2 = 0;
        int n3 = -1;
        int n4 = -1;
        for (int i = 0; i < n; ++i) {
            int n5 = i;
            String string = list.get(i).left().orElse(null);
            if ("?".equals(string)) {
                if (supplier == null) {
                    supplier = () -> MathParser.parseSymbols(list.subList(0, n5));
                    n4 = n5 + 1;
                }
                ++n2;
                continue;
            }
            if (!":".equals(string)) continue;
            if (n2 == 1 && supplier2 == null && n4 > 0) {
                int n6 = n4;
                supplier2 = () -> MathParser.parseSymbols(list.subList(n6, n5));
            }
            --n2;
            n3 = i;
        }
        if (n2 == 0 && supplier != null && supplier2 != null && n3 < n - 1) {
            return Optional.of(new Ternary((MathValue)supplier.get(), (MathValue)supplier2.get(), MathParser.parseSymbols(list.subList(n3 + 1, n))));
        }
        return Optional.empty();
    }

    protected static Optional<MathValue> compileCalculation(List<Either<String, List<MathValue>>> list) throws CompoundException {
        if (list.size() < 3) {
            return Optional.empty();
        }
        int n = list.size();
        ObjectArrayList objectArrayList = new ObjectArrayList(n / 2);
        ObjectArrayList objectArrayList2 = new ObjectArrayList(n / 2 + 1);
        int n2 = -1;
        for (int i = 0; i < n; ++i) {
            Operator operator = list.get(i).left().filter(Operator::isOperator).map(MathParser::getOperatorFor).orElse(null);
            if (operator == null) continue;
            if (operator == Operator.ASSIGN_VARIABLE) {
                MathValue mathValue = MathParser.parseSymbols(list.subList(0, i));
                if (!(mathValue instanceof Variable)) {
                    throw new CompoundException("Attempted to assign a value to a non-variable");
                }
                Variable variable = (Variable)mathValue;
                return Optional.of(new VariableAssignment(variable, MathParser.parseSymbols(list.subList(i + 1, n))));
            }
            objectArrayList2.add(MathParser.parseSymbols(list.subList(n2 + 1, i)));
            objectArrayList.add(operator);
            n2 = i;
        }
        if (objectArrayList2.isEmpty()) {
            return Optional.empty();
        }
        objectArrayList2.add(MathParser.parseSymbols(list.subList(n2 + 1, n)));
        while (true) {
            Operator operator = null;
            int n3 = -1;
            for (int i = 0; i < objectArrayList.size(); ++i) {
                Operator operator2 = (Operator)objectArrayList.get(i);
                if (operator != null && !operator2.takesPrecedenceOver(operator)) continue;
                operator = operator2;
                n3 = i;
            }
            if (n3 == -1) break;
            objectArrayList2.add(n3, new Calculation(operator, (MathValue)objectArrayList2.get(n3), (MathValue)objectArrayList2.get(n3 + 1)));
            objectArrayList.remove(n3);
            objectArrayList2.remove(n3 + 1);
            objectArrayList2.remove(n3 + 1);
        }
        if (objectArrayList2.size() != 1) {
            throw new CompoundException("Invalidly formatted expression: " + String.valueOf(list));
        }
        return Optional.of((MathValue)objectArrayList2.getFirst());
    }

    public static char[] decomposeExpression(String string) throws CompoundException {
        if (string.isEmpty()) {
            return new char[]{'\u0000'};
        }
        if (!EXPRESSION_FORMAT.matcher(string).matches()) {
            throw new CompoundException("Invalid characters found in expression: '" + string + "'");
        }
        char[] cArray = WHITESPACE.matcher(string).replaceAll("").toLowerCase(Locale.ROOT).toCharArray();
        int n = 0;
        for (char c : cArray) {
            if (c == '(') {
                ++n;
            } else if (c == ')') {
                --n;
            }
            if (n >= 0) continue;
            throw new CompoundException("Closing parenthesis before opening parenthesis in expression '" + string + "'");
        }
        if (n != 0) {
            throw new CompoundException("Uneven parenthesis in expression, each opening brace must have a pairing close brace '" + string + "'");
        }
        return cArray;
    }

    public static void setVariable(String string, ToDoubleFunction<ControllerState> toDoubleFunction) {
        MathParser.getVariableFor(string).set(toDoubleFunction);
    }

    protected static Optional<? extends MathValue> compileValue(List<Either<String, List<MathValue>>> list) throws CompoundException {
        if (list.size() == 1) {
            return MathParser.compileSingleValue(list.getFirst());
        }
        Optional<Ternary> optional = MathParser.compileTernary(list);
        if (optional.isPresent()) {
            return optional;
        }
        return MathParser.compileCalculation(list);
    }

    protected static Optional<? extends MathValue> compileFunction(String string, List<MathValue> list) throws CompoundException {
        if (string.startsWith("!")) {
            if (string.length() == 1) {
                return Optional.of(new BooleanNegate(list.getFirst()));
            }
            return MathParser.compileFunction(string.substring(1), list).map(BooleanNegate::new);
        }
        if (string.startsWith("-")) {
            if (string.length() == 1) {
                return Optional.of(new Negative(list.getFirst()));
            }
            return MathParser.compileFunction(string.substring(1), list).map(Negative::new);
        }
        if (!MathParser.isFunctionRegistered(string)) {
            return Optional.empty();
        }
        return MathParser.buildFunction(string, list.toArray(new MathValue[0]));
    }

    protected static Operator getOperatorFor(String string) throws CompoundException {
        return (Operator)Operator.getOperatorFor((String)string).orElseThrow(() -> new CompoundException("Unknown operator symbol '" + string + "'"));
    }

    public static MathValue parseSymbols(List<Either<String, List<MathValue>>> list) throws CompoundException {
        if (list.size() == 2) {
            Optional<String> optional = list.getFirst().left().filter(string -> string.startsWith("-") || string.startsWith("!") || MathParser.isFunctionRegistered(string));
            Optional optional2 = list.get(1).right();
            if (optional.isPresent() && optional2.isPresent()) {
                Optional<? extends MathValue> optional3 = MathParser.compileFunction(optional.get(), (List)optional2.get());
                return optional3.orElseThrow(() -> new CompoundException("Unable to parse function '" + (String)optional.get() + "' with arguments: " + String.valueOf(optional2.get())));
            }
        }
        return MathParser.compileValue(list).orElseThrow(() -> new CompoundException("Unable to parse compiled symbols from expression: " + String.valueOf(list)));
    }

    public static void registerFunction(String string, MathFunction.Factory<?> factory) {
        if (FUNCTION_FACTORIES.put(string, factory) != null) {
            GeckoLibConstants.LOGGER.log(Level.WARN, "Duplicate registration of MathFunction: '{}'. Ignore if intentional override", (Object)string);
        }
        GeckoLibConstants.LOGGER.log(Level.DEBUG, "Registered MathFunction '{}'", (Object)string);
    }

    @SuppressWarnings("unchecked")
    public static <T extends MathFunction> Optional<T> buildFunction(String string, MathValue ... mathValueArray) {
        MathFunction.Factory<?> factory = FUNCTION_FACTORIES.get(string);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of((T) factory.create(mathValueArray));
    }

    public static void registerVariable(Variable variable) {
        MolangQueries.registerVariable(variable);
    }
}

