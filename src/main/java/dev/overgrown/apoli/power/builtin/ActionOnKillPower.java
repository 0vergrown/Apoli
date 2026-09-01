package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HitSide;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.OptionalInt;

public final class ActionOnKillPower extends PowerType<ActionOnKillPower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<EntityAction> selfAction,
        Optional<EntityAction> targetAction,
        Optional<EntityAction> attackerAction,
        Optional<EntityAction> entityAction,
        HitSide entityActionTarget,
        Optional<BiEntityCondition> bientityCondition,
        Optional<EntityCondition> targetCondition,
        Optional<EntityCondition> attackerCondition,
        Optional<DamageCondition> damageCondition,
        Expression cooldown,
        HudRender hudRender
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("self_action", EntityAction.CODEC).forGetter(Config::selfAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("target_action", EntityAction.CODEC).forGetter(Config::targetAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("attacker_action", EntityAction.CODEC).forGetter(Config::attackerAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            HitSide.CODEC.optionalFieldOf("entity_action_target", HitSide.SELF).forGetter(Config::entityActionTarget),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("target_condition", EntityCondition.CODEC).forGetter(Config::targetCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("attacker_condition", EntityCondition.CODEC).forGetter(Config::attackerCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Config::damageCondition),
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(1)).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.getAuxInt(powerId).isPresent()) return;
        impl.setAuxInt(powerId, 0);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (!holder.hasPower(powerId)) impl.removeAux(powerId);
    }

    @Override
    public boolean isActive(ResourceLocation powerId, Config cfg, EntityCtx ctx) {
        return true;
    }

    public void onKill(ResourceLocation powerId, Config cfg, LivingEntity killer, LivingEntity victim,
                       DamageSource source, ServerLevel level, PowerContainerImpl impl) {
        long now = level.getGameTime();
        if (!HitActionHandler.ready(impl, powerId, now)) return;

        BiEntityCtx biCtx = new BiEntityCtx(killer, victim, level);
        EntityCtx killerCtx = new EntityCtx(killer, level);
        EntityCtx victimCtx = new EntityCtx(victim, level);
        if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(biCtx)) return;
        if (cfg.targetCondition().isPresent() && !cfg.targetCondition().get().test(victimCtx)) return;
        if (cfg.attackerCondition().isPresent() && !cfg.attackerCondition().get().test(killerCtx)) return;
        if (cfg.damageCondition().isPresent()
            && !cfg.damageCondition().get().test(new DamageCtx(source, victim, level, 0f))) return;

        cfg.bientityAction().ifPresent(a -> a.run(biCtx));
        cfg.selfAction().ifPresent(a -> a.run(killerCtx));
        cfg.attackerAction().ifPresent(a -> a.run(killerCtx));
        cfg.targetAction().ifPresent(a -> a.run(victimCtx));
        EntityCtx entityCtx = cfg.entityActionTarget() == HitSide.TARGET ? victimCtx : killerCtx;
        cfg.entityAction().ifPresent(a -> a.run(entityCtx));
        impl.setAuxInt(powerId, HitActionHandler.expiry(now, PowerResources.cooldownTicks(cfg.cooldown(), impl)));
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        return PowerResources.readDeadline(holder, powerId);
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        return PowerResources.writeDeadline(holder, powerId, value, PowerResources.cooldownTicks(cfg.cooldown(), holder));
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown(), holder), 0) : 0);
    }

}
