package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class ActionOnCollisionPower extends PowerType<ActionOnCollisionPower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<EntityAction> entityAction,
        Optional<EntityAction> targetAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<EntityCondition> targetCondition,
        float radius,
        int cooldown,
        boolean includeRiding,
        HudRender hudRender
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            EntityAction.CODEC.optionalFieldOf("target_action").forGetter(Config::targetAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            EntityCondition.CODEC.optionalFieldOf("target_condition").forGetter(Config::targetCondition),
            Codec.FLOAT.optionalFieldOf("radius", 0.0F).forGetter(Config::radius),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(Config::cooldown),
            Codec.BOOL.optionalFieldOf("include_riding", false).forGetter(Config::includeRiding),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender)
        ).apply(i, Config::new));
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(powerId)) impl.removeAux(powerId);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        Entity owner = holder.rawOwner();
        if (owner == null || owner.isRemoved()) return;
        if (!(owner.level() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        if (!HitActionHandler.ready(impl, powerId, now)) return;

        AABB box = cfg.radius() > 0.0F ? owner.getBoundingBox().inflate(cfg.radius()) : owner.getBoundingBox();
        List<Entity> hits = level.getEntities(owner, box, other -> !other.isSpectator() && other.isAlive());
        if (hits.isEmpty()) return;

        Power loaded = ApoliPowers.get(powerId);
        EntityCtx selfCtx = new EntityCtx(owner, level);
        if (loaded != null && loaded.condition().isPresent() && !loaded.condition().get().test(selfCtx)) return;

        for (int i = 0; i < hits.size(); i++) {
            Entity other = hits.get(i);
            if (!cfg.includeRiding() && (other.isPassengerOfSameVehicle(owner)
                || other.hasPassenger(owner) || owner.hasPassenger(other))) continue;

            BiEntityCtx biCtx = new BiEntityCtx(owner, other, level);
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(biCtx)) continue;
            EntityCtx targetCtx = new EntityCtx(other, level);
            if (cfg.targetCondition().isPresent() && !cfg.targetCondition().get().test(targetCtx)) continue;

            if (cfg.bientityAction().isPresent()) cfg.bientityAction().get().run(biCtx);
            if (cfg.entityAction().isPresent()) cfg.entityAction().get().run(selfCtx);
            if (cfg.targetAction().isPresent()) cfg.targetAction().get().run(targetCtx);

            if (cfg.cooldown() > 0) {
                impl.setAuxInt(powerId, HitActionHandler.expiry(now, cfg.cooldown()));
                return;
            }
        }
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        return PowerResources.readDeadline(holder, powerId);
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        return PowerResources.writeDeadline(holder, powerId, value, cfg.cooldown());
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(cfg.cooldown(), 0) : 0);
    }

}
