package rtx.nv.api.mods.geckolib.loading.math;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.Collectors;

public enum Operator {
    ASSIGN_VARIABLE("=", (a, b) -> b, 0),
    LOGICAL_OR("||", (a, b) -> (a != 0 || b != 0) ? 1.0 : 0.0, 1),
    LOGICAL_AND("&&", (a, b) -> (a != 0 && b != 0) ? 1.0 : 0.0, 2),
    EQUALS("==", (a, b) -> a == b ? 1.0 : 0.0, 3),
    NOT_EQUALS("!=", (a, b) -> a != b ? 1.0 : 0.0, 3),
    LESS_THAN_OR_EQUAL("<=", (a, b) -> a <= b ? 1.0 : 0.0, 4),
    GREATER_THAN_OR_EQUAL(">=", (a, b) -> a >= b ? 1.0 : 0.0, 4),
    LESS_THAN("<", (a, b) -> a < b ? 1.0 : 0.0, 4),
    GREATER_THAN(">", (a, b) -> a > b ? 1.0 : 0.0, 4),
    ADD("+", Double::sum, 5),
    SUBTRACT("-", (a, b) -> a - b, 5),
    MULTIPLY("*", (a, b) -> a * b, 6),
    DIVIDE("/", (a, b) -> b == 0 ? 0.0 : a / b, 6),
    MODULO("%", (a, b) -> b == 0 ? 0.0 : a % b, 6),
    POWER("^", Math::pow, 7);

    private static final Map<String, Operator> OPERATORS = Arrays.stream(values())
            .collect(Collectors.toMap(Operator::getSymbol, op -> op));
    private static final int MAX_LENGTH = Arrays.stream(values())
            .mapToInt(op -> op.symbol.length())
            .max()
            .orElse(2);

    private final String symbol;
    private final DoubleBinaryOperator operation;
    private final int precedence;

    Operator(String symbol, DoubleBinaryOperator operation, int precedence) {
        this.symbol = symbol;
        this.operation = operation;
        this.precedence = precedence;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public double apply(double a, double b) {
        return this.operation.applyAsDouble(a, b);
    }

    public boolean takesPrecedenceOver(Operator other) {
        return this.precedence >= other.precedence;
    }

    public static boolean isOperator(String string) {
        return OPERATORS.containsKey(string);
    }

    public static boolean isOperativeSymbol(char c) {
        return "=!&|<>+-*/%^".indexOf(c) != -1;
    }

    public static int maxOperatorLength() {
        return MAX_LENGTH;
    }

    public static Optional<Operator> getOperatorFor(String string) {
        return Optional.ofNullable(OPERATORS.get(string));
    }
}
