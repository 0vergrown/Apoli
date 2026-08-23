package dev.overgrown.apoli.data.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public final class ExprParser {

    public record Result(ExprNode root, boolean needsContainer, boolean needsPeer) {}

    private enum T {
        NUM,
        IDENT,
        GEN,
        OP,
        LPAREN,
        RPAREN,
        LBRACKET,
        RBRACKET,
        COMMA,
        EOF
    }

    private static final int MAX_DEPTH = 256;

    private final String src;
    private final Function<String, ExprVars.ResolvedVar> resolver;
    private int pos;
    private int depth;
    private boolean needsContainer;
    private boolean needsPeer;

    private T tokType;
    private String tokText = "";
    private double tokNum;
    private int tokStart;

    private ExprParser(String src, Function<String, ExprVars.ResolvedVar> resolver) {
        this.src = src;
        this.resolver = resolver;
    }

    public static Result parse(String src, Function<String, ExprVars.ResolvedVar> resolver) throws ExprParseException {
        ExprParser p = new ExprParser(src, resolver);
        p.advance();
        ExprNode root = p.parseExpression();
        p.expect(T.EOF, "end of expression");
        return new Result(root, p.needsContainer, p.needsPeer);
    }

    private ExprNode parseExpression() throws ExprParseException {
        if (++depth > MAX_DEPTH) throw new ExprParseException("expression too deeply nested", tokStart);
        ExprNode node = parseOr();
        depth--;
        return node;
    }

    private ExprNode parseOr() throws ExprParseException {
        ExprNode left = parseAnd();
        while (tokType == T.OP && (tokText.equals("||") || tokText.equals("|"))) {
            advance();
            left = fold(new ExprNodes.OrN(new ExprNode[] { left, parseAnd() }));
        }
        return left;
    }

    private ExprNode parseAnd() throws ExprParseException {
        ExprNode left = parseComparison();
        while (tokType == T.OP && (tokText.equals("&&") || tokText.equals("&"))) {
            advance();
            left = fold(new ExprNodes.AndN(new ExprNode[] { left, parseComparison() }));
        }
        return left;
    }

    private ExprNode parseComparison() throws ExprParseException {
        ExprNode left = parseAdditive();
        while (tokType == T.OP) {
            ExprNodes.Op op = switch (tokText) {
                case "==", "=" -> ExprNodes.Op.EQ;
                case "!=", "<>" -> ExprNodes.Op.NE;
                case "<" -> ExprNodes.Op.LT;
                case "<=" -> ExprNodes.Op.LE;
                case ">" -> ExprNodes.Op.GT;
                case ">=" -> ExprNodes.Op.GE;
                default -> null;
            };
            if (op == null) break;
            advance();
            left = fold(new ExprNodes.Bin(op, left, parseAdditive()));
        }
        return left;
    }

    private ExprNode parseAdditive() throws ExprParseException {
        ExprNode left = parseTerm();
        while (tokType == T.OP && (tokText.equals("+") || tokText.equals("-"))) {
            ExprNodes.Op op = tokText.equals("+") ? ExprNodes.Op.ADD : ExprNodes.Op.SUB;
            advance();
            left = fold(new ExprNodes.Bin(op, left, parseTerm()));
        }
        return left;
    }

    private ExprNode parseTerm() throws ExprParseException {
        ExprNode left = parseUnary();
        while (true) {
            if (tokType == T.OP) {
                ExprNodes.Op op = switch (tokText) {
                    case "*", "×" -> ExprNodes.Op.MUL;
                    case "/", "÷" -> ExprNodes.Op.DIV;
                    case "\\" -> ExprNodes.Op.IDIV;
                    case "#" -> ExprNodes.Op.MOD;
                    default -> null;
                };
                if (op == null) return left;
                advance();
                left = fold(new ExprNodes.Bin(op, left, parseUnary()));
            } else if (tokType == T.NUM || tokType == T.IDENT || tokType == T.GEN || tokType == T.LPAREN) {
                left = fold(new ExprNodes.Bin(ExprNodes.Op.MUL, left, parseUnary()));
            } else {
                return left;
            }
        }
    }

    private ExprNode parseUnary() throws ExprParseException {
        if (tokType == T.OP && tokText.equals("-")) {
            advance();
            return fold(new ExprNodes.Neg(parseUnary()));
        }
        if (tokType == T.OP && tokText.equals("+")) {
            advance();
            return parseUnary();
        }
        return parsePower();
    }

    private ExprNode parsePower() throws ExprParseException {
        ExprNode base = parsePostfix();
        if (tokType == T.OP && tokText.equals("^^")) {
            advance();
            return fold(new ExprNodes.Tetra(base, parseUnary()));
        }
        if (tokType == T.OP && tokText.equals("^")) {
            advance();
            return fold(new ExprNodes.Bin(ExprNodes.Op.POW, base, parseUnary()));
        }
        return base;
    }

    private ExprNode parsePostfix() throws ExprParseException {
        ExprNode node = parsePrimary();
        while (tokType == T.OP && (tokText.equals("!") || tokText.equals("%"))) {
            node = tokText.equals("!")
                ? fold(new ExprNodes.Fact(node))
                : fold(new ExprNodes.Bin(ExprNodes.Op.MUL, node, new ExprNodes.Const(0.01)));
            advance();
        }
        return node;
    }

    private ExprNode parsePrimary() throws ExprParseException {
        switch (tokType) {
            case NUM -> {
                ExprNode n = new ExprNodes.Const(tokNum);
                advance();
                return n;
            }
            case LPAREN -> {
                advance();
                ExprNode inner = parseExpression();
                expect(T.RPAREN, "')'");
                advance();
                return inner;
            }
            case GEN -> {
                ExprNode gen = generator(tokText, tokStart);
                advance();
                return gen;
            }
            case IDENT -> {
                String name = tokText;
                int at = tokStart;
                advance();
                if (tokType == T.LPAREN) {
                    return functionCall(name, at);
                }
                if (tokType == T.LBRACKET) {
                    return indexedVariable(name, at);
                }
                return variable(name, at);
            }
            default -> throw new ExprParseException("unexpected token '" + tokText + "'", tokStart);
        }
    }

    private ExprNode functionCall(String name, int at) throws ExprParseException {
        IdFnBuilder idBuilder = ID_FUNCTIONS.get(name);
        if (idBuilder != null) return idFunctionCall(name, at, idBuilder);
        advance();
        List<ExprNode> args = new ArrayList<>(4);
        if (tokType != T.RPAREN) {
            args.add(parseExpression());
            while (tokType == T.COMMA) {
                advance();
                args.add(parseExpression());
            }
        }
        expect(T.RPAREN, "')'");
        advance();
        FnBuilder builder = FUNCTIONS.get(name);
        if (builder == null) throw new ExprParseException("unknown function '" + name + "'", at);
        return fold(builder.build(args, name, at));
    }

    private ExprNode idFunctionCall(String name, int at, IdFnBuilder builder) throws ExprParseException {
        advance();
        expect(T.IDENT, "an id like 'namespace:path'");
        ResourceLocation id = ResourceLocation.tryParse(tokText);
        if (id == null) throw new ExprParseException("'" + tokText + "' is not a valid id", tokStart);
        advance();
        List<ExprNode> args = new ArrayList<>(2);
        while (tokType == T.COMMA) {
            advance();
            args.add(parseExpression());
        }
        expect(T.RPAREN, "')'");
        advance();
        ExprVars.ResolvedVar rv = builder.build(id, args, name, at);
        needsContainer |= rv.needsContainer();
        needsPeer |= rv.needsPeer();
        return new ExprNodes.Var(rv.accessor());
    }

    @FunctionalInterface
    public interface IdFnBuilder {
        ExprVars.ResolvedVar build(ResourceLocation id, List<ExprNode> args, String name, int at)
            throws ExprParseException;
    }

    private static final Map<String, IdFnBuilder> ID_FUNCTIONS = new HashMap<>();

    public static void registerIdFunction(String name, IdFnBuilder builder) {
        ID_FUNCTIONS.put(name, builder);
    }

    private ExprNode variable(String name, int at) throws ExprParseException {
        switch (name) {
            case "pi" -> { return new ExprNodes.Const(Math.PI); }
            case "e" -> { return new ExprNodes.Const(Math.E); }
            default -> { }
        }
        ExprVars.ResolvedVar rv = resolver.apply(name);
        if (rv == null) throw new ExprParseException("unknown variable '" + name + "'", at);
        needsContainer |= rv.needsContainer();
        needsPeer |= rv.needsPeer();
        return new ExprNodes.Var(rv.accessor());
    }

    private ExprNode indexedVariable(String name, int at) throws ExprParseException {
        ResourceLocation id = name.indexOf(':') < 0 ? null : ResourceLocation.tryParse(name);
        if (id == null) {
            throw new ExprParseException("'" + name + "' is not a resource id like 'namespace:path', "
                + "so it cannot be indexed with [...]", at);
        }
        advance();
        ExprNode index = parseExpression();
        expect(T.RBRACKET, "']'");
        advance();
        ExprVars.ResolvedVar rv = ExprVars.resolveIndexed(id, index);
        needsContainer |= rv.needsContainer();
        needsPeer |= rv.needsPeer();
        return new ExprNodes.Var(rv.accessor());
    }

    private ExprNode generator(String name, int at) throws ExprParseException {
        ExprNode gen = switch (name) {
            case "Uni" -> new ExprNodes.RUni(new ExprNodes.Const(0), new ExprNodes.Const(1));
            case "Nor" -> new ExprNodes.RNor(new ExprNodes.Const(0), new ExprNodes.Const(1));
            case "Int" -> boundedGen(Integer.MIN_VALUE, Integer.MAX_VALUE);
            case "nat" -> boundedGen(0, Integer.MAX_VALUE);
            case "Nat" -> boundedGen(1, Integer.MAX_VALUE);
            default -> null;
        };
        if (gen != null) return gen;
        long scale = powerOfTenSuffix(name, "Int");
        if (scale > 0) return boundedGen(-scale, scale);
        scale = powerOfTenSuffix(name, "nat");
        if (scale > 0) return boundedGen(0, scale);
        scale = powerOfTenSuffix(name, "Nat");
        if (scale > 0) return boundedGen(1, scale);
        throw new ExprParseException("unknown random generator '[" + name + "]'", at);
    }

    private static long powerOfTenSuffix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() != prefix.length() + 1) return -1;
        char digit = name.charAt(prefix.length());
        if (digit < '1' || digit > '9') return -1;
        long scale = 1;
        for (int i = 0; i < digit - '0'; i++) scale *= 10;
        return scale;
    }

    private static ExprNode boundedGen(long lo, long hi) {
        return new ExprNodes.RUnid(new ExprNodes.Const(lo), new ExprNodes.Const(hi));
    }

    private static ExprNode fold(ExprNode node) {
        if (node.isConstant() && !(node instanceof ExprNodes.Const)) {
            return new ExprNodes.Const(node.eval(null, null, null, 0));
        }
        return node;
    }

    private void expect(T type, String what) throws ExprParseException {
        if (tokType != type) {
            throw new ExprParseException("expected " + what + " but found '" + tokText + "'", tokStart);
        }
    }

    @FunctionalInterface
    private interface FnBuilder {
        ExprNode build(List<ExprNode> args, String name, int at) throws ExprParseException;
    }

    private static final Map<String, FnBuilder> FUNCTIONS = new HashMap<>();

    private static void fn1(DoubleUnaryOperator fn, String... names) {
        FnBuilder b = (args, name, at) -> {
            if (args.size() != 1) throw new ExprParseException(name + " expects 1 argument", at);
            return new ExprNodes.Fn1(fn, args.get(0));
        };
        for (String n : names) FUNCTIONS.put(n, b);
    }

    private static void fn2(DoubleBinaryOperator fn, String... names) {
        FnBuilder b = (args, name, at) -> {
            if (args.size() != 2) throw new ExprParseException(name + " expects 2 arguments", at);
            return new ExprNodes.Fn2(fn, args.get(0), args.get(1));
        };
        for (String n : names) FUNCTIONS.put(n, b);
    }

    private static void fn3(ExprNodes.DoubleTernaryOperator fn, String... names) {
        FnBuilder b = (args, name, at) -> {
            if (args.size() != 3) throw new ExprParseException(name + " expects 3 arguments", at);
            return new ExprNodes.Fn3(fn, args.get(0), args.get(1), args.get(2));
        };
        for (String n : names) FUNCTIONS.put(n, b);
    }

    private static ExprNode[] atLeast(int n, List<ExprNode> args, String name, int at) throws ExprParseException {
        if (args.size() < n) throw new ExprParseException(name + " expects at least " + n + " argument(s)", at);
        return args.toArray(new ExprNode[0]);
    }

    private static double nthRoot(double n, double x) {
        if (x < 0 && n == Math.rint(n) && ((long) n & 1L) == 1L) {
            return -Math.pow(-x, 1.0 / n);
        }
        return Math.pow(x, 1.0 / n);
    }

    static {
        fn1(Math::sin, "sin");
        fn1(Math::cos, "cos");
        fn1(Math::tan, "tan", "tg");
        fn1(v -> 1.0 / Math.cos(v), "sec");
        fn1(v -> 1.0 / Math.sin(v), "csc", "cosec");
        fn1(v -> 1.0 / Math.tan(v), "cot", "ctg", "ctan");
        fn1(Math::asin, "asin", "arsin", "arcsin");
        fn1(Math::acos, "acos", "arcos", "arccos");
        fn1(Math::atan, "atan", "atg", "arctg", "arctan");
        fn1(Math::sinh, "sinh");
        fn1(Math::cosh, "cosh");
        fn1(Math::tanh, "tanh", "tgh", "th");
        fn1(v -> 1.0 / Math.tanh(v), "coth", "ctgh", "cth");
        fn1(v -> 1.0 / Math.cosh(v), "sech");
        fn1(v -> 1.0 / Math.sinh(v), "csch", "cosech");
        fn1(v -> Math.log(v + Math.sqrt(v * v + 1.0)), "asinh", "arsinh", "arcsinh");
        fn1(v -> Math.log(v + Math.sqrt(v * v - 1.0)), "acosh", "arcosh", "arccosh");
        fn1(v -> 0.5 * Math.log((1.0 + v) / (1.0 - v)), "atanh", "arctanh", "artanh");
        fn1(Math::log, "ln");
        fn1(v -> Math.log(v) / Math.log(2.0), "log2");
        fn1(Math::log10, "lg", "log10");
        fn1(Math::exp, "exp");
        fn1(Math::sqrt, "sqrt");
        fn1(Math::cbrt, "cbrt");
        fn1(Math::abs, "abs");
        fn1(Math::floor, "floor");
        fn1(Math::ceil, "ceil");
        fn1(v -> (double) Math.round(v), "round");
        fn1(Math::signum, "sign", "sgn");
        fn1(Math::toDegrees, "deg");
        fn1(Math::toRadians, "rad");
        fn1(v -> v == 0.0 ? 1.0 : 0.0, "not");
        fn2((a, b) -> a % b, "mod");
        fn2(Math::pow, "pow");
        fn2(Math::atan2, "atan2");
        fn2(Math::hypot, "hypot");
        fn2((base, x) -> Math.log(x) / Math.log(base), "log");
        fn2(ExprParser::nthRoot, "root");
        fn3((v, lo, hi) -> Math.min(hi, Math.max(lo, v)), "clamp");
        fn3((a, b, t) -> a + (b - a) * t, "lerp");
        FUNCTIONS.put("if", (args, name, at) -> {
            if (args.size() != 3) throw new ExprParseException("if expects 3 arguments (condition, then, else)", at);
            return new ExprNodes.If(args.get(0), args.get(1), args.get(2));
        });
        FUNCTIONS.put("min", (args, name, at) -> new ExprNodes.MinN(atLeast(1, args, name, at)));
        FUNCTIONS.put("max", (args, name, at) -> new ExprNodes.MaxN(atLeast(1, args, name, at)));
        FUNCTIONS.put("avg", (args, name, at) -> new ExprNodes.AvgN(atLeast(1, args, name, at)));
        FUNCTIONS.put("mean", FUNCTIONS.get("avg"));
        FUNCTIONS.put("and", (args, name, at) -> new ExprNodes.AndN(atLeast(2, args, name, at)));
        FUNCTIONS.put("or", (args, name, at) -> new ExprNodes.OrN(atLeast(2, args, name, at)));
        FUNCTIONS.put("rList", (args, name, at) -> new ExprNodes.RList(atLeast(1, args, name, at)));
        FUNCTIONS.put("rUni", (args, name, at) -> {
            if (args.size() != 2) throw new ExprParseException("rUni expects 2 arguments", at);
            return new ExprNodes.RUni(args.get(0), args.get(1));
        });
        FUNCTIONS.put("rUnid", (args, name, at) -> {
            if (args.size() != 2) throw new ExprParseException("rUnid expects 2 arguments", at);
            return new ExprNodes.RUnid(args.get(0), args.get(1));
        });
        FUNCTIONS.put("rNor", (args, name, at) -> {
            if (args.size() != 2) throw new ExprParseException("rNor expects 2 arguments", at);
            return new ExprNodes.RNor(args.get(0), args.get(1));
        });
    }

    private void advance() throws ExprParseException {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        tokStart = pos;
        if (pos >= src.length()) {
            tokType = T.EOF;
            tokText = "<end>";
            return;
        }
        char c = src.charAt(pos);
        if (Character.isDigit(c) || (c == '.' && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1)))) {
            scanNumber();
            return;
        }
        if (Character.isLetter(c) || c == '_') {
            scanIdent();
            return;
        }
        if (c == '[') {
            if (looksLikeGenerator()) {
                scanGenerator();
            } else {
                tokType = T.LBRACKET;
                tokText = "[";
                pos++;
            }
            return;
        }
        if (c == ']') {
            tokType = T.RBRACKET;
            tokText = "]";
            pos++;
            return;
        }
        switch (c) {
            case '(' -> { tokType = T.LPAREN; tokText = "("; pos++; }
            case ')' -> { tokType = T.RPAREN; tokText = ")"; pos++; }
            case ',' -> { tokType = T.COMMA; tokText = ","; pos++; }
            default -> scanOperator(c);
        }
    }

    private void scanOperator(char c) throws ExprParseException {
        String two = pos + 1 < src.length() ? src.substring(pos, pos + 2) : "";
        switch (two) {
            case "^^", "==", "!=", "<>", "<=", ">=", "&&", "||" -> {
                tokType = T.OP;
                tokText = two;
                pos += 2;
                return;
            }
            default -> { }
        }
        switch (c) {
            case '+', '-', '*', '/', '\\', '#', '%', '^', '!', '<', '>', '=', '&', '|', '×', '÷' -> {
                tokType = T.OP;
                tokText = String.valueOf(c);
                pos++;
            }
            default -> throw new ExprParseException("unexpected character '" + c + "'", pos);
        }
    }

    private void scanNumber() throws ExprParseException {
        int start = pos;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        boolean intOnly = pos > start;

        if (intOnly && pos + 1 < src.length() && src.charAt(pos) == '_' && Character.isDigit(src.charAt(pos + 1))) {
            double first = Double.parseDouble(src.substring(start, pos));
            pos++;
            int s2 = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            double second = Double.parseDouble(src.substring(s2, pos));
            if (pos + 1 < src.length() && src.charAt(pos) == '_' && Character.isDigit(src.charAt(pos + 1))) {
                pos++;
                int s3 = pos;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
                double third = Double.parseDouble(src.substring(s3, pos));
                tokNum = first + second / third;
            } else {
                tokNum = first / second;
            }
            tokType = T.NUM;
            tokText = src.substring(start, pos);
            return;
        }

        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            int mark = pos;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
            if (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            } else {
                pos = mark;
            }
        }
        tokText = src.substring(start, pos);
        try {
            tokNum = Double.parseDouble(tokText);
        } catch (NumberFormatException e) {
            throw new ExprParseException("invalid number '" + tokText + "'", start);
        }
        tokType = T.NUM;
    }

    private void scanIdent() {
        int start = pos;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
        if (pos + 1 < src.length() && src.charAt(pos) == ':' && isResourcePathChar(src.charAt(pos + 1))) {
            pos++;
            while (pos < src.length() && isResourcePathChar(src.charAt(pos))) pos++;
        }
        tokType = T.IDENT;
        tokText = src.substring(start, pos);
    }

    private static boolean isResourcePathChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '/' || c == '-';
    }

    private static final java.util.Set<String> GENERATOR_NAMES = java.util.Set.of(
        "Uni", "Nor", "Int", "nat", "Nat",
        "Int1", "Int2", "Int3", "Int4", "Int5", "Int6", "Int7", "Int8", "Int9",
        "nat1", "nat2", "nat3", "nat4", "nat5", "nat6", "nat7", "nat8", "nat9",
        "Nat1", "Nat2", "Nat3", "Nat4", "Nat5", "Nat6", "Nat7", "Nat8", "Nat9");

    private boolean looksLikeGenerator() {
        int end = src.indexOf(']', pos + 1);
        if (end < 0 || end - pos - 1 > 4) return false;
        return GENERATOR_NAMES.contains(src.substring(pos + 1, end));
    }

    private void scanGenerator() throws ExprParseException {
        int start = pos;
        pos++;
        int nameStart = pos;
        while (pos < src.length() && src.charAt(pos) != ']') {
            if (pos - nameStart > 16) throw new ExprParseException("unterminated random generator", start);
            pos++;
        }
        if (pos >= src.length()) throw new ExprParseException("unterminated random generator", start);
        tokText = src.substring(nameStart, pos);
        pos++;
        if (tokText.isEmpty()) throw new ExprParseException("empty random generator '[]'", start);
        tokType = T.GEN;
    }
}
