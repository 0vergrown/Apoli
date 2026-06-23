package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ActionOnDeathPower extends PowerType<ActionOnDeathPower.Config> {
    public record Config(
        BiEntityAction bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<DamageCondition> damageCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Config::bientityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            DamageCondition.CODEC.optionalFieldOf("damage_condition").forGetter(Config::damageCondition)
        ).apply(i, Config::new));
    }

    public void onDeath(Config cfg, LivingEntity holder, @Nullable LivingEntity killer, DamageSource source, ServerLevel level) {
        if (cfg.damageCondition().isPresent()
            && !cfg.damageCondition().get().test(new DamageCtx(source, holder, level, 0f))) return;
        LivingEntity actor = killer != null ? killer : holder;
        BiEntityCtx ctx = new BiEntityCtx(actor, holder, level);
        if (cfg.bientityCondition().isPresent()
            && !cfg.bientityCondition().get().test(ctx)) return;
        cfg.bientityAction().run(ctx);
    }
}
