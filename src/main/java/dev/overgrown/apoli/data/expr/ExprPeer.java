package dev.overgrown.apoli.data.expr;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ExprPeer {

    public static final int ACTOR = 0;
    public static final int TARGET = 1;

    private static final ThreadLocal<Entity[]> CURRENT = ThreadLocal.withInitial(() -> new Entity[2]);

    private ExprPeer() {}

    public static Entity[] frame() {
        return CURRENT.get();
    }

    public static @Nullable Entity actor() {
        return CURRENT.get()[ACTOR];
    }

    public static @Nullable Entity target() {
        return CURRENT.get()[TARGET];
    }
}
