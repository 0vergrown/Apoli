package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModifyProjectileDivergencePower extends PowerType<ModifyProjectileDivergencePower.Config> {
    public record Config(
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        Optional<BiEntityCondition> bientityCondition,
        List<AttributeModifier> combined
    ) {
        public Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers,
                      Optional<BiEntityCondition> bientityCondition) {
            this(modifier, modifiers, bientityCondition, AttributeModifierHelper.flatten(modifier, modifiers));
        }
    }

    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.MODIFY_PROJECTILE_DIVERGENCE);

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, (modifier, modifiers, condition) -> new Config(modifier, modifiers, condition)));
    }

    public static float modify(Projectile projectile, float divergence) {
        if (!IN_USE.inUse()) return divergence;
        Entity owner = projectile.getOwner();
        if (owner == null || !PowerLookup.holdsType(owner, ApoliIds.MODIFY_PROJECTILE_DIVERGENCE)) return divergence;
        BiEntityCtx ctx = BiEntityCtx.of(owner, projectile, owner.level());
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(owner, ApoliIds.MODIFY_PROJECTILE_DIVERGENCE, Config.class, cfg -> {
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) return;
            mods.addAll(cfg.combined());
        });
        if (mods.isEmpty()) return divergence;
        return Math.max(0.0F, AttributeModifierHelper.apply(divergence, AttributeModifierHelper.ensureSorted(mods), owner));
    }
}
