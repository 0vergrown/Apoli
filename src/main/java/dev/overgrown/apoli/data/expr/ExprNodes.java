package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public final class ExprNodes {
    private ExprNodes() {}

    @FunctionalInterface
    public interface DoubleTernaryOperator {
        double applyAsDouble(double a, double b, double c);
    }

    public record Const(double v) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return v;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    public record Var(ExprVar accessor) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return accessor.get(entity, container, level, value);
        }
    }

    public record Neg(ExprNode n) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return -n.eval(entity, container, level, value);
        }

        @Override
        public boolean isConstant() {
            return n.isConstant();
        }
    }

    public enum Op { ADD, SUB, MUL, DIV, IDIV, MOD, POW, EQ, NE, LT, LE, GT, GE }

    public record Bin(Op op, ExprNode l, ExprNode r) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double a = l.eval(entity, container, level, value);
            double b = r.eval(entity, container, level, value);
            return switch (op) {
                case ADD -> a + b;
                case SUB -> a - b;
                case MUL -> a * b;
                case DIV -> a / b;
                case IDIV -> (double) (long) (a / b);
                case MOD -> a % b;
                case POW -> Math.pow(a, b);
                case EQ -> a == b ? 1.0 : 0.0;
                case NE -> a != b ? 1.0 : 0.0;
                case LT -> a < b ? 1.0 : 0.0;
                case LE -> a <= b ? 1.0 : 0.0;
                case GT -> a > b ? 1.0 : 0.0;
                case GE -> a >= b ? 1.0 : 0.0;
            };
        }

        @Override
        public boolean isConstant() {
            return l.isConstant() && r.isConstant();
        }
    }

    public record Tetra(ExprNode l, ExprNode r) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double a = l.eval(entity, container, level, value);
            long b = Math.round(r.eval(entity, container, level, value));
            if (b < 0) return Double.NaN;
            if (b == 0) return 1.0;
            double result = a;
            long steps = Math.min(b, 64) - 1;
            for (long i = 0; i < steps && Double.isFinite(result); i++) {
                result = Math.pow(a, result);
            }
            return result;
        }

        @Override
        public boolean isConstant() {
            return l.isConstant() && r.isConstant();
        }
    }

    public record Fact(ExprNode n) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double v = n.eval(entity, container, level, value);
            long k = Math.round(v);
            if (k < 0 || Math.abs(v - k) > 1.0e-9) return Double.NaN;
            double result = 1.0;
            for (long i = 2; i <= Math.min(k, 171); i++) {
                result *= i;
            }
            return k > 170 ? Double.POSITIVE_INFINITY : result;
        }

        @Override
        public boolean isConstant() {
            return n.isConstant();
        }
    }

    public record Fn1(DoubleUnaryOperator fn, ExprNode a) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return fn.applyAsDouble(a.eval(entity, container, level, value));
        }

        @Override
        public boolean isConstant() {
            return a.isConstant();
        }
    }

    public record Fn2(DoubleBinaryOperator fn, ExprNode a, ExprNode b) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return fn.applyAsDouble(a.eval(entity, container, level, value), b.eval(entity, container, level, value));
        }

        @Override
        public boolean isConstant() {
            return a.isConstant() && b.isConstant();
        }
    }

    public record Fn3(DoubleTernaryOperator fn, ExprNode a, ExprNode b, ExprNode c) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return fn.applyAsDouble(
                a.eval(entity, container, level, value),
                b.eval(entity, container, level, value),
                c.eval(entity, container, level, value)
            );
        }

        @Override
        public boolean isConstant() {
            return a.isConstant() && b.isConstant() && c.isConstant();
        }
    }

    public record If(ExprNode cond, ExprNode then, ExprNode otherwise) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return cond.eval(entity, container, level, value) != 0.0
                ? then.eval(entity, container, level, value)
                : otherwise.eval(entity, container, level, value);
        }

        @Override
        public boolean isConstant() {
            return cond.isConstant() && then.isConstant() && otherwise.isConstant();
        }
    }

    public record AndN(ExprNode[] args) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            for (ExprNode arg : args) {
                if (arg.eval(entity, container, level, value) == 0.0) return 0.0;
            }
            return 1.0;
        }

        @Override
        public boolean isConstant() {
            return allConstant(args);
        }
    }

    public record OrN(ExprNode[] args) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            for (ExprNode arg : args) {
                if (arg.eval(entity, container, level, value) != 0.0) return 1.0;
            }
            return 0.0;
        }

        @Override
        public boolean isConstant() {
            return allConstant(args);
        }
    }

    public record MinN(ExprNode[] args) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double best = args[0].eval(entity, container, level, value);
            for (int i = 1; i < args.length; i++) {
                best = Math.min(best, args[i].eval(entity, container, level, value));
            }
            return best;
        }

        @Override
        public boolean isConstant() {
            return allConstant(args);
        }
    }

    public record MaxN(ExprNode[] args) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double best = args[0].eval(entity, container, level, value);
            for (int i = 1; i < args.length; i++) {
                best = Math.max(best, args[i].eval(entity, container, level, value));
            }
            return best;
        }

        @Override
        public boolean isConstant() {
            return allConstant(args);
        }
    }

    public record AvgN(ExprNode[] args) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double sum = 0.0;
            for (ExprNode arg : args) {
                sum += arg.eval(entity, container, level, value);
            }
            return sum / args.length;
        }

        @Override
        public boolean isConstant() {
            return allConstant(args);
        }
    }

    public record RUni(ExprNode lo, ExprNode hi) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double a = lo.eval(entity, container, level, value);
            double b = hi.eval(entity, container, level, value);
            if (a > b) { double t = a; a = b; b = t; }
            return a == b ? a : a + ThreadLocalRandom.current().nextDouble() * (b - a);
        }
    }

    public record RUnid(ExprNode lo, ExprNode hi) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            long a = Math.round(lo.eval(entity, container, level, value));
            long b = Math.round(hi.eval(entity, container, level, value));
            if (a > b) { long t = a; a = b; b = t; }
            return a == b ? a : ThreadLocalRandom.current().nextLong(a, b + 1);
        }
    }

    public record RNor(ExprNode mean, ExprNode stddev) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double mu = mean.eval(entity, container, level, value);
            double sigma = stddev.eval(entity, container, level, value);
            return mu + ThreadLocalRandom.current().nextGaussian() * sigma;
        }
    }

    public record RList(ExprNode[] args) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return args[ThreadLocalRandom.current().nextInt(args.length)].eval(entity, container, level, value);
        }
    }

    public record Length(ExprNode x, ExprNode y, ExprNode z) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double a = x.eval(entity, container, level, value);
            double b = y.eval(entity, container, level, value);
            double c = z.eval(entity, container, level, value);
            return Math.sqrt(a * a + b * b + c * c);
        }

        @Override
        public boolean isConstant() {
            return x.isConstant() && y.isConstant() && z.isConstant();
        }
    }

    public record LengthSq(ExprNode x, ExprNode y, ExprNode z) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double a = x.eval(entity, container, level, value);
            double b = y.eval(entity, container, level, value);
            double c = z.eval(entity, container, level, value);
            return a * a + b * b + c * c;
        }

        @Override
        public boolean isConstant() {
            return x.isConstant() && y.isConstant() && z.isConstant();
        }
    }

    public record Dot(ExprNode ax, ExprNode ay, ExprNode az, ExprNode bx, ExprNode by, ExprNode bz) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return ax.eval(entity, container, level, value) * bx.eval(entity, container, level, value)
                + ay.eval(entity, container, level, value) * by.eval(entity, container, level, value)
                + az.eval(entity, container, level, value) * bz.eval(entity, container, level, value);
        }

        @Override
        public boolean isConstant() {
            return ax.isConstant() && ay.isConstant() && az.isConstant()
                && bx.isConstant() && by.isConstant() && bz.isConstant();
        }
    }

    public record Distance(ExprNode ax, ExprNode ay, ExprNode az, ExprNode bx, ExprNode by, ExprNode bz,
                           boolean squared) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double dx = ax.eval(entity, container, level, value) - bx.eval(entity, container, level, value);
            double dy = ay.eval(entity, container, level, value) - by.eval(entity, container, level, value);
            double dz = az.eval(entity, container, level, value) - bz.eval(entity, container, level, value);
            double sq = dx * dx + dy * dy + dz * dz;
            return squared ? sq : Math.sqrt(sq);
        }

        @Override
        public boolean isConstant() {
            return ax.isConstant() && ay.isConstant() && az.isConstant()
                && bx.isConstant() && by.isConstant() && bz.isConstant();
        }
    }

    public record NormComp(int axis, ExprNode x, ExprNode y, ExprNode z) implements ExprNode {
        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            double a = x.eval(entity, container, level, value);
            double b = y.eval(entity, container, level, value);
            double c = z.eval(entity, container, level, value);
            double length = Math.sqrt(a * a + b * b + c * c);
            if (length < 1.0E-12) return 0.0;
            return (axis == 0 ? a : axis == 1 ? b : c) / length;
        }

        @Override
        public boolean isConstant() {
            return x.isConstant() && y.isConstant() && z.isConstant();
        }
    }

    public static final class Memo implements ExprNode {
        private static final java.util.concurrent.atomic.AtomicInteger SLOTS = new java.util.concurrent.atomic.AtomicInteger();

        private final int slot = SLOTS.getAndIncrement();
        private final ExprNode child;

        public Memo(ExprNode child) {
            this.child = child;
        }

        @Override
        public double eval(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
            return ExprMemo.read(slot, child, entity, container, level, value);
        }
    }

    private static boolean allConstant(ExprNode[] args) {
        for (ExprNode arg : args) {
            if (!arg.isConstant()) return false;
        }
        return true;
    }
}
