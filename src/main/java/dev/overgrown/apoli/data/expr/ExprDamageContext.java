package dev.overgrown.apoli.data.expr;

public final class ExprDamageContext {

    public static final int SLOT = ExprContext.slot("damage");

    private ExprDamageContext() {}

    public static double current() {
        return ExprContext.get(SLOT);
    }

    public static double set(double damage) {
        return ExprContext.push(SLOT, damage);
    }

    public static void restore(double previous) {
        ExprContext.pop(SLOT, previous);
    }
}
