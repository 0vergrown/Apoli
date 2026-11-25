package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class ExprMemo {

    private static final ThreadLocal<ExprMemo> FRAME = ThreadLocal.withInitial(ExprMemo::new);

    private long epoch;
    private int depth;
    private long[] stamps = new long[16];
    private double[] values = new double[16];

    private ExprMemo() {}

    public static ExprMemo enter() {
        ExprMemo memo = FRAME.get();
        if (memo.depth++ == 0) memo.epoch++;
        return memo;
    }

    public void exit() {
        depth--;
    }

    static double read(int slot, ExprNode child, @Nullable Entity entity, @Nullable PowerContainer container,
                       @Nullable Level level, double value) {
        ExprMemo memo = FRAME.get();
        if (memo.depth == 0) return child.eval(entity, container, level, value);
        if (slot < memo.stamps.length && memo.stamps[slot] == memo.epoch) return memo.values[slot];
        double result = child.eval(entity, container, level, value);
        if (slot >= memo.stamps.length) {
            int size = Math.max(slot + 1, memo.stamps.length * 2);
            memo.stamps = Arrays.copyOf(memo.stamps, size);
            memo.values = Arrays.copyOf(memo.values, size);
        }
        memo.stamps[slot] = memo.epoch;
        memo.values[slot] = result;
        return result;
    }
}
