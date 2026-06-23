package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class TypeDamageCondition implements ConditionType<DamageCtx, TypeDamageCondition.Cfg> {
    public record Cfg(ResourceLocation damageType) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("damage_type").forGetter(Cfg::damageType)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        Optional<? extends ResourceKey<?>> key = ctx.source().typeHolder().unwrapKey();
        return key.isPresent() && key.get().location().equals(cfg.damageType);
    }
}
