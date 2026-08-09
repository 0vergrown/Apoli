package dev.overgrown.apoli.action;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class ActionLiveness {

    private ActionLiveness() {}

    public static boolean alive(@Nullable Entity entity, @Nullable Level level) {
        if (entity == null) return true;
        if (entity.isRemoved()) return false;
        return level == null || entity.level() == level;
    }

    public static <CTX> Predicate<CTX> always() {
        return ctx -> true;
    }

    public static Predicate<EntityCtx> entity() {
        return ctx -> alive(ctx.entity(), ctx.level());
    }

    public static Predicate<BiEntityCtx> biEntity() {
        return ctx -> alive(ctx.actor(), ctx.level()) && alive(ctx.target(), ctx.level());
    }

    public static Predicate<ItemCtx> item() {
        return ctx -> alive(ctx.holder(), ctx.level());
    }
}
