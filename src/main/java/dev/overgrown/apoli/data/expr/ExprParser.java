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

    public record Result(ExprNode root, boolean needsContainer, boolean needsPeer, boolean usesMemo) {}

    public record VectorResult(ExprNode x, ExprNode y, ExprNode z, boolean needsContainer, boolean needsPeer,
                               boolean usesMemo) {}

    private static final class Val {
        final ExprNode s;
        final ExprNode x;
        final ExprNode y;
        final ExprNode z;
        final boolean random;

        private Val(ExprNode s, ExprNode x, ExprNode y, ExprNode z, boolean random) {
            this.s = s;
            this.x = x;
            this.y = y;
            this.z = z;
            this.random = random;
        }

        static Val num(ExprNode node, boolean random) {
            return new Val(node, null, null, null, random);
        }

        static Val vec(ExprNode x, ExprNode y, ExprNode z, boolean random) {
            return new Val(null, x, y, z, random);
        }

        boolean isVector() {
            return s == null;
        }

        ExprNode comp(int axis) {
            return axis == 0 ? x : axis == 1 ? y : z;
        }
    }

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
        DOT,
        EOF
    }

    private static final int MAX_DEPTH = 256;

    private final String src;
    private final Function<String, ExprVars.ResolvedVar> resolver;
    private int pos;
    private int depth;
    private boolean needsContainer;
    private boolean needsPeer;
    private boolean usesMemo;

    private T tokType;
    private boolean bracketFnIdent;
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
        Val root = p.parseExpression();
        p.expect(T.EOF, "end of expression");
        if (root.isVector()) {
            throw new ExprParseException("this gives a vector, but a number is needed here; "
                + "use length(), dot() or .x, .y, .z to get a number from it", 0);
        }
        return new Result(root.s, p.needsContainer, p.needsPeer, p.usesMemo);
    }

    public static VectorResult parseVector(String src, Function<String, ExprVars.ResolvedVar> resolver)
        throws ExprParseException {
        ExprParser p = new ExprParser(src, resolver);
        p.advance();
        Val root = p.parseExpression();
        p.expect(T.EOF, "end of expression");
        if (!root.isVector()) {
            throw new ExprParseException("this gives a number, but a vector is needed here; build one with vec(x, y, z)", 0);
        }
        return new VectorResult(root.x, root.y, root.z, p.needsContainer, p.needsPeer, p.usesMemo);
    }

    private Val parseExpression() throws ExprParseException {
        if (++depth > MAX_DEPTH) throw new ExprParseException("expression too deeply nested", tokStart);
        Val node = parseOr();
        depth--;
        return node;
    }

    private Val parseOr() throws ExprParseException {
        Val left = parseAnd();
        while (tokType == T.OP && (tokText.equals("||") || tokText.equals("|"))) {
            int at = tokStart;
            advance();
            Val right = parseAnd();
            left = Val.num(fold(new ExprNodes.OrN(new ExprNode[] { num(left, at, "'||'"), num(right, at, "'||'") })),
                left.random || right.random);
        }
        return left;
    }

    private Val parseAnd() throws ExprParseException {
        Val left = parseComparison();
        while (tokType == T.OP && (tokText.equals("&&") || tokText.equals("&"))) {
            int at = tokStart;
            advance();
            Val right = parseComparison();
            left = Val.num(fold(new ExprNodes.AndN(new ExprNode[] { num(left, at, "'&&'"), num(right, at, "'&&'") })),
                left.random || right.random);
        }
        return left;
    }

    private Val parseComparison() throws ExprParseException {
        Val left = parseAdditive();
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
            int at = tokStart;
            String symbol = tokText;
            advance();
            Val right = parseAdditive();
            left = Val.num(fold(new ExprNodes.Bin(op, num(left, at, "'" + symbol + "'"), num(right, at, "'" + symbol + "'"))),
                left.random || right.random);
        }
        return left;
    }

    private Val parseAdditive() throws ExprParseException {
        Val left = parseTerm();
        while (tokType == T.OP && (tokText.equals("+") || tokText.equals("-"))) {
            ExprNodes.Op op = tokText.equals("+") ? ExprNodes.Op.ADD : ExprNodes.Op.SUB;
            int at = tokStart;
            advance();
            left = arith(op, left, parseTerm(), at);
        }
        return left;
    }

    private Val parseTerm() throws ExprParseException {
        Val left = parseUnary();
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
                int at = tokStart;
                advance();
                left = arith(op, left, parseUnary(), at);
            } else if (tokType == T.NUM || tokType == T.IDENT || tokType == T.GEN || tokType == T.LPAREN) {
                int at = tokStart;
                left = arith(ExprNodes.Op.MUL, left, parseUnary(), at);
            } else {
                return left;
            }
        }
    }

    private Val parseUnary() throws ExprParseException {
        if (tokType == T.OP && tokText.equals("-")) {
            advance();
            Val inner = parseUnary();
            if (!inner.isVector()) return Val.num(fold(new ExprNodes.Neg(inner.s)), inner.random);
            return Val.vec(fold(new ExprNodes.Neg(inner.x)), fold(new ExprNodes.Neg(inner.y)),
                fold(new ExprNodes.Neg(inner.z)), inner.random);
        }
        if (tokType == T.OP && tokText.equals("+")) {
            advance();
            return parseUnary();
        }
        return parsePower();
    }

    private Val parsePower() throws ExprParseException {
        Val base = parsePostfix();
        if (tokType == T.OP && tokText.equals("^^")) {
            int at = tokStart;
            advance();
            Val exponent = parseUnary();
            return Val.num(fold(new ExprNodes.Tetra(num(base, at, "'^^'"), num(exponent, at, "'^^'"))),
                base.random || exponent.random);
        }
        if (tokType == T.OP && tokText.equals("^")) {
            int at = tokStart;
            advance();
            Val exponent = parseUnary();
            return Val.num(fold(new ExprNodes.Bin(ExprNodes.Op.POW, num(base, at, "'^'"), num(exponent, at, "'^'"))),
                base.random || exponent.random);
        }
        return base;
    }

    private Val parsePostfix() throws ExprParseException {
        Val node = parsePrimary();
        while (true) {
            if (tokType == T.OP && (tokText.equals("!") || tokText.equals("%"))) {
                ExprNode inner = num(node, tokStart, "'" + tokText + "'");
                node = Val.num(tokText.equals("!")
                    ? fold(new ExprNodes.Fact(inner))
                    : fold(new ExprNodes.Bin(ExprNodes.Op.MUL, inner, new ExprNodes.Const(0.01))), node.random);
                advance();
            } else if (tokType == T.DOT) {
                int at = tokStart;
                advance();
                expect(T.IDENT, "x, y or z after '.'");
                int axis = switch (tokText) {
                    case "x" -> 0;
                    case "y" -> 1;
                    case "z" -> 2;
                    default -> -1;
                };
                if (axis < 0) throw new ExprParseException("'." + tokText + "' is not a vector component; use .x, .y or .z", tokStart);
                if (!node.isVector()) throw new ExprParseException("'." + tokText + "' needs a vector, but this is a number", at);
                node = Val.num(node.comp(axis), node.random);
                advance();
            } else {
                return node;
            }
        }
    }

    private Val parsePrimary() throws ExprParseException {
        switch (tokType) {
            case NUM -> {
                Val n = Val.num(new ExprNodes.Const(tokNum), false);
                advance();
                return n;
            }
            case LPAREN -> {
                advance();
                Val inner = parseExpression();
                expect(T.RPAREN, "')'");
                advance();
                return inner;
            }
            case GEN -> {
                ExprNode gen = generator(tokText, tokStart);
                advance();
                return Val.num(gen, true);
            }
            case IDENT -> {
                String name = tokText;
                int at = tokStart;
                advance();
                if (tokType == T.LPAREN) {
                    return functionCall(name, at);
                }
                if (tokType == T.LBRACKET) {
                    BracketFnBuilder bracket = BRACKET_FUNCTIONS.get(name);
                    if (bracket != null) return Val.num(bracketFunctionCall(name, at, bracket), false);
                    return indexedVariable(name, at);
                }
                return variable(name, at);
            }
            default -> throw new ExprParseException("unexpected token '" + tokText + "'", tokStart);
        }
    }

    private Val functionCall(String name, int at) throws ExprParseException {
        IdFnBuilder idBuilder = ID_FUNCTIONS.get(name);
        if (idBuilder != null) return idFunctionCall(name, at, idBuilder);
        advance();
        List<Val> args = new ArrayList<>(4);
        if (tokType != T.RPAREN) {
            args.add(parseExpression());
            while (tokType == T.COMMA) {
                advance();
                args.add(parseExpression());
            }
        }
        expect(T.RPAREN, "')'");
        advance();
        boolean random = RANDOM_FUNCTIONS.contains(name);
        boolean anyVector = false;
        for (Val arg : args) {
            random |= arg.random;
            anyVector |= arg.isVector();
        }
        Val vector = vectorFunction(name, args, at, random);
        if (vector != null) return vector;
        if (anyVector) {
            DoubleUnaryOperator unary = UNARY.get(name);
            if (unary != null && args.size() == 1) {
                Val v = args.get(0);
                return Val.vec(fold(new ExprNodes.Fn1(unary, v.x)), fold(new ExprNodes.Fn1(unary, v.y)),
                    fold(new ExprNodes.Fn1(unary, v.z)), random);
            }
            throw new ExprParseException(name + " takes numbers, not vectors", at);
        }
        FnBuilder builder = FUNCTIONS.get(name);
        if (builder == null) throw new ExprParseException("unknown function '" + name + "'", at);
        List<ExprNode> nodes = new ArrayList<>(args.size());
        for (Val arg : args) nodes.add(arg.s);
        return Val.num(fold(builder.build(nodes, name, at)), random);
    }

    private Val vectorFunction(String name, List<Val> args, int at, boolean random) throws ExprParseException {
        switch (name) {
            case "vec", "vec3" -> {
                arity(args, 3, name, at);
                return Val.vec(num(args.get(0), at, name), num(args.get(1), at, name), num(args.get(2), at, name), random);
            }
            case "length", "len", "magnitude" -> {
                Val v = vectorArg(args, 1, 0, name, at);
                return Val.num(fold(new ExprNodes.Length(v.x, v.y, v.z)), random);
            }
            case "length_sq", "length_squared" -> {
                Val v = vectorArg(args, 1, 0, name, at);
                return Val.num(fold(new ExprNodes.LengthSq(v.x, v.y, v.z)), random);
            }
            case "normalize", "normalized" -> {
                Val v = share(vectorArg(args, 1, 0, name, at));
                return Val.vec(fold(new ExprNodes.NormComp(0, v.x, v.y, v.z)), fold(new ExprNodes.NormComp(1, v.x, v.y, v.z)),
                    fold(new ExprNodes.NormComp(2, v.x, v.y, v.z)), random);
            }
            case "dot" -> {
                Val a = vectorArg(args, 2, 0, name, at);
                Val b = vectorArg(args, 2, 1, name, at);
                return Val.num(fold(new ExprNodes.Dot(a.x, a.y, a.z, b.x, b.y, b.z)), random);
            }
            case "cross" -> {
                Val a = share(vectorArg(args, 2, 0, name, at));
                Val b = share(vectorArg(args, 2, 1, name, at));
                return Val.vec(crossTerm(a.y, b.z, a.z, b.y), crossTerm(a.z, b.x, a.x, b.z), crossTerm(a.x, b.y, a.y, b.x), random);
            }
            case "distance", "dist", "distance_sq", "distance_squared" -> {
                Val a = vectorArg(args, 2, 0, name, at);
                Val b = vectorArg(args, 2, 1, name, at);
                boolean squared = name.endsWith("_sq") || name.endsWith("_squared");
                return Val.num(fold(new ExprNodes.Distance(a.x, a.y, a.z, b.x, b.y, b.z, squared)), random);
            }
            case "lerp" -> {
                if (args.size() != 3 || !args.get(0).isVector() && !args.get(1).isVector()) return null;
                Val a = vectorArg(args, 3, 0, name, at);
                Val b = vectorArg(args, 3, 1, name, at);
                ExprNode t = share(Val.num(num(args.get(2), at, name), args.get(2).random)).s;
                return Val.vec(fold(new ExprNodes.Fn3(LERP, a.x, b.x, t)), fold(new ExprNodes.Fn3(LERP, a.y, b.y, t)),
                    fold(new ExprNodes.Fn3(LERP, a.z, b.z, t)), random);
            }
            case "if" -> {
                if (args.size() != 3 || !args.get(1).isVector() && !args.get(2).isVector()) return null;
                if (!args.get(1).isVector() || !args.get(2).isVector()) {
                    throw new ExprParseException("if needs both branches to be vectors, or both to be numbers", at);
                }
                ExprNode condition = share(Val.num(num(args.get(0), at, name), args.get(0).random)).s;
                Val a = args.get(1);
                Val b = args.get(2);
                return Val.vec(fold(new ExprNodes.If(condition, a.x, b.x)), fold(new ExprNodes.If(condition, a.y, b.y)),
                    fold(new ExprNodes.If(condition, a.z, b.z)), random);
            }
            default -> {
                return null;
            }
        }
    }

    private static ExprNode crossTerm(ExprNode a, ExprNode b, ExprNode c, ExprNode d) {
        return fold(new ExprNodes.Bin(ExprNodes.Op.SUB, fold(new ExprNodes.Bin(ExprNodes.Op.MUL, a, b)),
            fold(new ExprNodes.Bin(ExprNodes.Op.MUL, c, d))));
    }

    private static void arity(List<Val> args, int expected, String name, int at) throws ExprParseException {
        if (args.size() != expected) {
            throw new ExprParseException(name + " expects " + expected + " argument" + (expected == 1 ? "" : "s"), at);
        }
    }

    private static Val vectorArg(List<Val> args, int expected, int index, String name, int at) throws ExprParseException {
        arity(args, expected, name, at);
        Val v = args.get(index);
        if (!v.isVector()) throw new ExprParseException(name + " needs a vector, but argument " + (index + 1) + " is a number", at);
        return v;
    }

    private static ExprNode num(Val v, int at, String what) throws ExprParseException {
        if (v.isVector()) throw new ExprParseException(what + " needs a number here, but got a vector", at);
        return v.s;
    }

    private Val share(Val v) {
        if (!v.random) return v;
        usesMemo = true;
        if (!v.isVector()) return Val.num(new ExprNodes.Memo(v.s), true);
        return Val.vec(new ExprNodes.Memo(v.x), new ExprNodes.Memo(v.y), new ExprNodes.Memo(v.z), true);
    }

    private Val arith(ExprNodes.Op op, Val a, Val b, int at) throws ExprParseException {
        boolean random = a.random || b.random;
        if (!a.isVector() && !b.isVector()) return Val.num(fold(new ExprNodes.Bin(op, a.s, b.s)), random);
        String symbol = switch (op) {
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case IDIV -> "\\";
            case MOD -> "#";
            default -> op.name().toLowerCase(java.util.Locale.ROOT);
        };
        switch (op) {
            case ADD, SUB -> {
                if (a.isVector() && b.isVector()) return componentwise(op, a, b, random);
                throw new ExprParseException("'" + symbol + "' cannot mix a number and a vector; build a vector with vec(x, y, z)", at);
            }
            case MUL -> {
                if (a.isVector() && b.isVector()) return componentwise(op, a, b, random);
                return a.isVector() ? scaled(op, a, share(b).s, false, random) : scaled(op, b, share(a).s, true, random);
            }
            case DIV, IDIV, MOD -> {
                if (a.isVector() && b.isVector()) return componentwise(op, a, b, random);
                if (a.isVector()) return scaled(op, a, share(b).s, false, random);
                throw new ExprParseException("'" + symbol + "' cannot divide a number by a vector", at);
            }
            default -> throw new ExprParseException("'" + symbol + "' works on numbers, not vectors", at);
        }
    }

    private static Val componentwise(ExprNodes.Op op, Val a, Val b, boolean random) {
        return Val.vec(fold(new ExprNodes.Bin(op, a.x, b.x)), fold(new ExprNodes.Bin(op, a.y, b.y)),
            fold(new ExprNodes.Bin(op, a.z, b.z)), random);
    }

    private static Val scaled(ExprNodes.Op op, Val vector, ExprNode scalar, boolean scalarFirst, boolean random) {
        ExprNode[] parts = new ExprNode[3];
        for (int i = 0; i < 3; i++) {
            ExprNode c = vector.comp(i);
            parts[i] = fold(scalarFirst ? new ExprNodes.Bin(op, scalar, c) : new ExprNodes.Bin(op, c, scalar));
        }
        return Val.vec(parts[0], parts[1], parts[2], random);
    }

    private Val idFunctionCall(String name, int at, IdFnBuilder builder) throws ExprParseException {
        advance();
        expect(T.IDENT, "an id like 'namespace:path'");
        ResourceLocation id = ResourceLocation.tryParse(tokText);
        if (id == null) throw new ExprParseException("'" + tokText + "' is not a valid id", tokStart);
        advance();
        List<ExprNode> args = new ArrayList<>(2);
        boolean random = false;
        while (tokType == T.COMMA) {
            advance();
            int argAt = tokStart;
            Val arg = parseExpression();
            random |= arg.random;
            args.add(num(arg, argAt, name));
        }
        expect(T.RPAREN, "')'");
        advance();
        ExprVars.ResolvedVar rv = builder.build(id, args, name, at);
        needsContainer |= rv.needsContainer();
        needsPeer |= rv.needsPeer();
        return Val.num(new ExprNodes.Var(rv.accessor()), random);
    }

    private ExprNode bracketFunctionCall(String name, int at, BracketFnBuilder builder) throws ExprParseException {
        int open = tokStart;
        int close = matchingBracket(open);
        List<String> args = splitArguments(src.substring(open + 1, close));
        pos = close + 1;
        advance();
        ExprVars.ResolvedVar rv = builder.build(args, name, at);
        needsContainer |= rv.needsContainer();
        needsPeer |= rv.needsPeer();
        return new ExprNodes.Var(rv.accessor());
    }

    private int matchingBracket(int open) throws ExprParseException {
        int level = 0;
        char quote = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
                continue;
            }
            switch (c) {
                case '"', '\'' -> quote = c;
                case '[', '{', '(' -> level++;
                case ']', '}', ')' -> {
                    level--;
                    if (level == 0) {
                        if (c != ']') throw new ExprParseException("expected ']' but found '" + c + "'", i);
                        return i;
                    }
                }
                default -> { }
            }
        }
        throw new ExprParseException("unterminated '['", open);
    }

    private static List<String> splitArguments(String inner) {
        List<String> args = new ArrayList<>(4);
        int level = 0;
        char quote = 0;
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
                continue;
            }
            switch (c) {
                case '"', '\'' -> quote = c;
                case '[', '{', '(' -> level++;
                case ']', '}', ')' -> level--;
                case ',' -> {
                    if (level == 0) {
                        args.add(inner.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                default -> { }
            }
        }
        String tail = inner.substring(start).trim();
        if (!tail.isEmpty() || !args.isEmpty()) args.add(tail);
        return args;
    }

    public record Nested(ExprNode node, boolean needsContainer, boolean needsPeer) {}

    public static Nested compileNested(String source, String name, int at) throws ExprParseException {
        try {
            Result result = parse(source, ExprVars::resolve);
            return new Nested(result.root(), result.needsContainer(), result.needsPeer());
        } catch (ExprParseException e) {
            throw new ExprParseException(name + " argument \"" + source + "\": " + e.getMessage(), at);
        }
    }

    @FunctionalInterface
    public interface BracketFnBuilder {
        ExprVars.ResolvedVar build(List<String> args, String name, int at) throws ExprParseException;
    }

    private static final Map<String, BracketFnBuilder> BRACKET_FUNCTIONS = new HashMap<>();

    public static void registerBracketFunction(String name, BracketFnBuilder builder) {
        BRACKET_FUNCTIONS.put(name, builder);
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

    private Val variable(String name, int at) throws ExprParseException {
        switch (name) {
            case "pi" -> { return Val.num(new ExprNodes.Const(Math.PI), false); }
            case "e" -> { return Val.num(new ExprNodes.Const(Math.E), false); }
            default -> { }
        }
        ExprVars.ResolvedVar rv = resolver.apply(name);
        if (rv != null) {
            needsContainer |= rv.needsContainer();
            needsPeer |= rv.needsPeer();
            return Val.num(new ExprNodes.Var(rv.accessor()), false);
        }
        Val vector = vectorVariable(name, at);
        if (vector != null) return vector;
        throw new ExprParseException("unknown variable '" + name + "'", at);
    }

    private static final Map<String, String[]> VECTOR_VARIABLES = Map.of(
        "pos", new String[] { "x", "y", "z" },
        "position", new String[] { "x", "y", "z" },
        "eye_pos", new String[] { "x", "eye_y", "z" },
        "eye_position", new String[] { "x", "eye_y", "z" },
        "velocity", new String[] { "velocity_x", "velocity_y", "velocity_z" },
        "motion", new String[] { "velocity_x", "velocity_y", "velocity_z" },
        "look", new String[] { "look_x", "look_y", "look_z" },
        "facing", new String[] { "look_x", "look_y", "look_z" });

    private Val vectorVariable(String name, int at) throws ExprParseException {
        String prefix = "";
        String base = name;
        if (name.startsWith("target_")) {
            prefix = "target_";
            base = name.substring(prefix.length());
        } else if (name.startsWith("actor_")) {
            prefix = "actor_";
            base = name.substring(prefix.length());
        }
        String[] components = VECTOR_VARIABLES.get(base);
        if (components == null) return null;
        ExprNode[] nodes = new ExprNode[3];
        for (int i = 0; i < 3; i++) {
            ExprVars.ResolvedVar rv = resolver.apply(prefix + components[i]);
            if (rv == null) throw new ExprParseException("unknown variable '" + name + "'", at);
            needsContainer |= rv.needsContainer();
            needsPeer |= rv.needsPeer();
            nodes[i] = new ExprNodes.Var(rv.accessor());
        }
        return Val.vec(nodes[0], nodes[1], nodes[2], false);
    }

    private Val indexedVariable(String name, int at) throws ExprParseException {
        ResourceLocation id = name.indexOf(':') < 0 ? null : ResourceLocation.tryParse(name);
        if (id == null) {
            throw new ExprParseException("'" + name + "' is not a resource id like 'namespace:path', "
                + "so it cannot be indexed with [...]", at);
        }
        advance();
        int indexAt = tokStart;
        Val index = parseExpression();
        expect(T.RBRACKET, "']'");
        advance();
        ExprVars.ResolvedVar rv = ExprVars.resolveIndexed(id, num(index, indexAt, "an index"));
        needsContainer |= rv.needsContainer();
        needsPeer |= rv.needsPeer();
        return Val.num(new ExprNodes.Var(rv.accessor()), index.random);
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

    private static final Map<String, DoubleUnaryOperator> UNARY = new HashMap<>();

    private static final java.util.Set<String> RANDOM_FUNCTIONS = java.util.Set.of("rList", "rUni", "rUnid", "rNor");

    private static final ExprNodes.DoubleTernaryOperator LERP = (a, b, t) -> a + (b - a) * t;

    private static void fn1(DoubleUnaryOperator fn, String... names) {
        FnBuilder b = (args, name, at) -> {
            if (args.size() != 1) throw new ExprParseException(name + " expects 1 argument", at);
            return new ExprNodes.Fn1(fn, args.get(0));
        };
        for (String n : names) {
            FUNCTIONS.put(n, b);
            UNARY.put(n, fn);
        }
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
        fn3(LERP, "lerp");
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
        ExprVars.load();
    }

    private void advance() throws ExprParseException {
        boolean afterBracketFn = bracketFnIdent;
        bracketFnIdent = false;
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
            if (!afterBracketFn && looksLikeGenerator()) {
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
        if (c == '.') {
            tokType = T.DOT;
            tokText = ".";
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
        bracketFnIdent = BRACKET_FUNCTIONS.containsKey(tokText);
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
