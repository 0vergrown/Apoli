package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ActionOnEntitySetAction implements ActionType<EntityCtx, ActionOnEntitySetAction.Cfg> {
    public record Cfg(
        ResourceLocation set,
        BiEntityAction biEntityAction,
        Optional<BiEntityCondition> biEntityCondition,
        int limit,
        boolean reverse
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("set").forGetter(Cfg::set),
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::biEntityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::biEntityCondition),
            Codec.INT.optionalFieldOf("limit", 0).forGetter(Cfg::limit),
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(Cfg::reverse)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.entity();
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();
        PowerContainer container = PowerContainer.of(actor);
        if (container == null || !container.hasPower(cfg.set)) return;
        if (EntitySetPower.resolveCfg(cfg.set) == null) return;

        List<UUID> order = EntitySetPower.iterationOrder(actor, cfg.set, cfg.reverse);
        int processed = 0;
        for (UUID uuid : order) {
            Entity target = EntitySetPower.resolveEntity(server, uuid);
            if (target == null) continue;
            BiEntityCtx biCtx = new BiEntityCtx(actor, target, serverLevel);
            if (cfg.biEntityCondition.isPresent() && !cfg.biEntityCondition.get().test(biCtx)) continue;
            cfg.biEntityAction.run(biCtx);
            if (cfg.limit > 0 && ++processed >= cfg.limit) break;
        }
    }
}
