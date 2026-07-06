package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ItemUseActionHandler {
    private ItemUseActionHandler() {}

    public enum Phase { BEFORE, AFTER }

    public static void fire(Level level, @Nullable LivingEntity user, ItemStack stack,
                            ActionOnItemUsePower.Trigger trigger, Phase phase) {
        if (user == null || level.isClientSide()) return;

        ItemCtx ctx = new ItemCtx(stack, level, user);
        List<ActionOnItemUsePower.Config> hits = new ArrayList<>();
        PowerLookup.forEach(user, ActionOnItemUsePower.CANONICAL, ActionOnItemUsePower.Config.class, cfg -> {
            if (cfg.trigger() != trigger) return;
            if ((cfg.priority() >= 0) != (phase == Phase.BEFORE)) return;
            if (cfg.itemCondition().isPresent() && !cfg.itemCondition().get().test(ctx)) return;
            hits.add(cfg);
        });
        if (hits.isEmpty()) return;

        hits.sort(Comparator.comparingInt(ActionOnItemUsePower.Config::priority).reversed());
        EntityCtx entityCtx = new EntityCtx(user, level);
        for (ActionOnItemUsePower.Config cfg : hits) {
            cfg.itemAction().ifPresent(a -> a.run(ctx));
            cfg.entityAction().ifPresent(a -> a.run(entityCtx));
        }
    }
}
