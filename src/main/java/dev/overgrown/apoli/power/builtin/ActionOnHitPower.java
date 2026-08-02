package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HitSide;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import com.mojang.serialization.Codec;

public final class ActionOnHitPower extends PowerType<ActionOnHitPower.Config> {
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
        int cooldown,
        HudRender hudRender
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            EntityAction.CODEC.optionalFieldOf("self_action").forGetter(Config::selfAction),
            EntityAction.CODEC.optionalFieldOf("target_action").forGetter(Config::targetAction),
            EntityAction.CODEC.optionalFieldOf("attacker_action").forGetter(Config::attackerAction),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            HitSide.CODEC.optionalFieldOf("entity_action_target", HitSide.SELF).forGetter(Config::entityActionTarget),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            EntityCondition.CODEC.optionalFieldOf("target_condition").forGetter(Config::targetCondition),
            EntityCondition.CODEC.optionalFieldOf("attacker_condition").forGetter(Config::attackerCondition),
            DamageCondition.CODEC.optionalFieldOf("damage_condition").forGetter(Config::damageCondition),
            Codec.INT.optionalFieldOf("cooldown", 1).forGetter(Config::cooldown),
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
}
