package dev.overgrown.apoli.data.expr;

public final class ExprContext {

    private static final int CAPACITY = 32;

    private static final String[] NAMES = new String[CAPACITY];
    private static int registered;

    private static final ThreadLocal<double[]> FRAME = ThreadLocal.withInitial(() -> new double[CAPACITY]);

    private ExprContext() {}

    public static synchronized int slot(String name) {
        for (int i = 0; i < registered; i++) {
            if (NAMES[i].equals(name)) return i;
        }
        if (registered >= CAPACITY) {
            throw new IllegalStateException("Too many expression context variables; raise ExprContext.CAPACITY");
        }
        NAMES[registered] = name;
        return registered++;
    }

    public static double get(int slot) {
        return FRAME.get()[slot];
    }

    public static double push(int slot, double value) {
        double[] frame = FRAME.get();
        double previous = frame[slot];
        frame[slot] = value;
        return previous;
    }

    public static void pop(int slot, double previous) {
        FRAME.get()[slot] = previous;
    }
}
