package dev.overgrown.apoli.data.expr;

public final class ExprDamageContext {

    private static final ThreadLocal<double[]> CURRENT = ThreadLocal.withInitial(() -> new double[] { 0.0 });

    private ExprDamageContext() {}

    public static double current() {
        return CURRENT.get()[0];
    }

    public static double set(double damage) {
        double[] holder = CURRENT.get();
        double previous = holder[0];
        holder[0] = damage;
        return previous;
    }

    public static void restore(double previous) {
        CURRENT.get()[0] = previous;
    }
}
