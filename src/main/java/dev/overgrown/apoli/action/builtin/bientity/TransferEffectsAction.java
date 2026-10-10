package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.IdOrTag;
import dev.overgrown.apoli.effects.CustomEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TransferEffectsAction implements ActionType<BiEntityCtx, TransferEffectsAction.Cfg> {

    public record Cfg(
        TransferAction.Mode mode,
        boolean copy,
        Optional<List<IdOrTag<MobEffect>>> effects,
        Optional<List<MobEffectCategory>> categories,
        List<AttributeModifier> durationModifiers,
        List<AttributeModifier> amplifierModifiers,
        List<AttributeModifier> donorDurationModifiers
    ) {
        boolean matches(MobEffectInstance instance) {
            if (categories.isPresent() && !categories.get().contains(instance.getEffect().value().getCategory())) {
                return false;
            }
            if (effects.isEmpty()) return true;
            List<IdOrTag<MobEffect>> wanted = effects.get();
            for (int i = 0; i < wanted.size(); i++) {
                if (wanted.get(i).matches(instance.getEffect())) return true;
            }
            return false;
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
            TransferAction.Mode.CODEC.optionalFieldOf("mode", TransferAction.Mode.STEAL).forGetter(Cfg::mode),
            Codec.BOOL.optionalFieldOf("copy", false).forGetter(Cfg::copy),
            SingleOrList.of(IdOrTag.codec(Registries.MOB_EFFECT)).optionalFieldOf("effects").forGetter(Cfg::effects),
            SingleOrList.of(CustomEffect.CATEGORY_CODEC).optionalFieldOf("category").forGetter(Cfg::categories),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("duration_modifier", List.of()).forGetter(Cfg::durationModifiers),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("amplifier_modifier", List.of()).forGetter(Cfg::amplifierModifiers),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("donor_duration_modifier", List.of()).forGetter(Cfg::donorDurationModifiers)
        ).apply(i, Cfg::new)), Map.of("effect", "effects"));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        boolean steal = cfg.mode == TransferAction.Mode.STEAL;
        LivingEntity donor = steal ? ctx.livingTarget() : ctx.livingActor();
        LivingEntity recipient = steal ? ctx.livingActor() : ctx.livingTarget();
        if (donor == null || recipient == null || donor == recipient || donor.level().isClientSide()) return;
        Collection<MobEffectInstance> active = donor.getActiveEffects();
        if (active.isEmpty()) return;

        List<MobEffectInstance> moving = new ArrayList<>(active.size());
        for (MobEffectInstance instance : active) {
            if (cfg.matches(instance)) moving.add(instance);
        }

        for (int i = 0; i < moving.size(); i++) {
            MobEffectInstance original = moving.get(i);
            MobEffectInstance given = modified(original, cfg.durationModifiers, cfg.amplifierModifiers, recipient);
            if (given == null || !recipient.canBeAffected(given)) continue;
            recipient.addEffect(given, donor);
            if (!cfg.copy) {
                donor.removeEffect(original.getEffect());
            } else if (!cfg.donorDurationModifiers.isEmpty()) {
                MobEffectInstance kept = modified(original, cfg.donorDurationModifiers, List.of(), donor);
                if (kept == null) donor.removeEffect(original.getEffect());
                else donor.forceAddEffect(kept, recipient);
            }
        }
    }

    private static @Nullable MobEffectInstance modified(MobEffectInstance original, List<AttributeModifier> duration,
                                                       List<AttributeModifier> amplifier, LivingEntity holder) {
        int ticks = original.getDuration();
        if (!duration.isEmpty() && !original.isInfiniteDuration()) {
            ticks = (int) Math.round(AttributeModifierHelper.apply((double) ticks, duration, holder));
            if (ticks <= 0) return null;
        }
        int level = original.getAmplifier();
        if (!amplifier.isEmpty()) {
            level = Math.max(0, Math.min(255, (int) Math.round(AttributeModifierHelper.apply((double) level, amplifier, holder))));
        }
        return new MobEffectInstance(original.getEffect(), ticks, level, original.isAmbient(), original.isVisible(),
            original.showIcon());
    }
}
