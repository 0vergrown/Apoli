package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.SetIteration;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import dev.overgrown.apoli.codec.IdCodecs;
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
        boolean reverse,
        SetIteration iterate
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("set").forGetter(Cfg::set),
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::biEntityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::biEntityCondition),
            Codec.INT.optionalFieldOf("limit", 0).forGetter(Cfg::limit),
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(Cfg::reverse),
            SetIteration.CODEC.optionalFieldOf("iterate", SetIteration.MEMBERS).forGetter(Cfg::iterate)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity holder = ctx.entity();
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();
        if (EntitySetPower.resolveCfg(cfg.set) == null) return;

        if (cfg.iterate == SetIteration.OWNERS) {
            runOnOwners(cfg, holder, serverLevel, server);
            return;
        }

        PowerContainer container = PowerContainer.of(holder);
        if (container == null || !container.hasPower(cfg.set)) return;

        List<UUID> order = EntitySetPower.iterationOrder(holder, cfg.set, cfg.reverse);
        int processed = 0;
        for (int i = 0; i < order.size(); i++) {
            Entity target = EntitySetPower.resolveEntity(server, order.get(i));
            if (target == null) continue;
            BiEntityCtx biCtx = new BiEntityCtx(holder, target, serverLevel);
            if (cfg.biEntityCondition.isPresent() && !cfg.biEntityCondition.get().test(biCtx)) continue;
            cfg.biEntityAction.run(biCtx);
            if (cfg.limit > 0 && ++processed >= cfg.limit) break;
        }
    }

    private static void runOnOwners(Cfg cfg, Entity holder, ServerLevel level, MinecraftServer server) {
        List<UUID> owners = EntitySetPower.ownersContaining(holder, cfg.set, cfg.reverse);
        int processed = 0;
        for (int i = 0; i < owners.size(); i++) {
            Entity owner = EntitySetPower.resolveEntity(server, owners.get(i));
            if (owner == null) continue;
            BiEntityCtx biCtx = new BiEntityCtx(owner, holder, level);
            if (cfg.biEntityCondition.isPresent() && !cfg.biEntityCondition.get().test(biCtx)) continue;
            cfg.biEntityAction.run(biCtx);
            if (cfg.limit > 0 && ++processed >= cfg.limit) break;
        }
    }
}
