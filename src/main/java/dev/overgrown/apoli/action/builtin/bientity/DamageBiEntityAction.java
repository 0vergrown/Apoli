package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.List;
import java.util.Optional;

public final class DamageBiEntityAction implements ActionType<BiEntityCtx, DamageBiEntityAction.Cfg> {
    public record Cfg(
        Optional<dev.overgrown.apoli.data.Expression> amount,
        ResourceLocation damageType,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {
        public List<AttributeModifier> allModifiers() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.<Cfg>wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                dev.overgrown.apoli.codec.LoggedOptionalField.strict("amount",
                    dev.overgrown.apoli.data.Expression.FLOAT_OR_EXPR).forGetter(Cfg::amount),
                IdCodecs.ID.fieldOf("damage_type").forGetter(Cfg::damageType),
                AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Cfg::modifier),
                AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Cfg::modifiers)
            ).apply(i, Cfg::new)),
            java.util.Map.of("damage", "amount"));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity target = ctx.target();
        if (target == null) return;
        LivingEntity livingTarget = ctx.livingTarget();
        if (cfg.amount.isEmpty() && livingTarget == null) return;
        ResourceKey<DamageType> typeKey = ResourceKey.create(Registries.DAMAGE_TYPE, cfg.damageType);
        Optional<net.minecraft.core.Holder.Reference<DamageType>> holder = ctx.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE).getHolder(typeKey);
        if (holder.isEmpty()) return;
        Entity actor = ctx.actor();
        Entity owner = ownerOf(actor);
        DamageSource source = owner != null
            ? new DamageSource(holder.get(), actor, owner)
            : new DamageSource(holder.get(), actor);

        float base = cfg.amount.isPresent()
            ? (float) cfg.amount.get().eval(ctx.actor())
            : livingTarget.getMaxHealth();
        List<AttributeModifier> mods = cfg.allModifiers();
        float finalAmount = mods.isEmpty() ? base : Math.max(0f, AttributeModifierHelper.apply(base, mods, livingTarget));
        if (finalAmount > 0f) target.hurt(source, finalAmount);
    }

    private static Entity ownerOf(Entity actor) {
        if (actor instanceof Projectile projectile) {
            return projectile.getOwner();
        }
        if (actor instanceof dev.overgrown.apoli.entity.summon.Temporary && actor instanceof OwnableEntity ownable) {
            return ownable.getOwner();
        }
        return null;
    }
}
