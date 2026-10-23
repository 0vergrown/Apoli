package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.IdOrTag;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.effects.CustomEffect;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShareEffectsPower extends PowerType<ShareEffectsPower.Config> {
    public record Config(
        Vector radius,
        Optional<BiEntityCondition> bientityCondition,
        Optional<List<IdOrTag<MobEffect>>> effects,
        Optional<List<MobEffectCategory>> categories,
        boolean includeAmbient
    ) {
        boolean matches(MobEffectInstance instance) {
            if (instance.isAmbient() && !includeAmbient) return false;
            if (categories.isPresent() && !categories.get().contains(instance.getEffect().getCategory())) {
                return false;
            }
            if (effects.isEmpty()) return true;
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(instance.getEffect());
            List<IdOrTag<MobEffect>> wanted = effects.get();
            for (int i = 0; i < wanted.size(); i++) {
                if (wanted.get(i).matches(holder)) return true;
            }
            return false;
        }
    }

    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.SHARE_EFFECTS);
    private static boolean sharing;

    @Override
    public MapCodec<Config> configCodec() {
        return AliasingMapCodec.wrap(RecordCodecBuilder.<Config>mapCodec(i -> i.group(
            Vector.SCALAR_OR_VECTOR.optionalFieldOf("radius", Vector.uniform(8.0f)).forGetter(Config::radius),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            SingleOrList.of(IdOrTag.codec(Registries.MOB_EFFECT)).optionalFieldOf("effects").forGetter(Config::effects),
            SingleOrList.of(CustomEffect.CATEGORY_CODEC).optionalFieldOf("category").forGetter(Config::categories),
            Codec.BOOL.optionalFieldOf("include_ambient", false).forGetter(Config::includeAmbient)
        ).apply(i, Config::new)), Map.of("effect", "effects"));
    }

    public static void onEffectAdded(LivingEntity holder, MobEffectInstance instance) {
        if (sharing || !IN_USE.inUse() || holder.level().isClientSide()) return;
        if (!PowerLookup.holdsType(holder, ApoliIds.SHARE_EFFECTS)) return;
        PowerLookup.forEach(holder, ApoliIds.SHARE_EFFECTS, Config.class, cfg -> {
            if (cfg.matches(instance)) share(holder, instance, cfg);
        });
    }

    private static void share(LivingEntity holder, MobEffectInstance instance, Config cfg) {
        Vector radius = cfg.radius();
        List<LivingEntity> nearby = holder.level().getEntitiesOfClass(LivingEntity.class,
            holder.getBoundingBox().inflate(radius.x(), radius.y(), radius.z()),
            entity -> entity != holder && entity.isAlive());
        if (nearby.isEmpty()) return;
        sharing = true;
        try {
            for (int i = 0; i < nearby.size(); i++) {
                LivingEntity target = nearby.get(i);
                if (cfg.bientityCondition().isPresent()
                        && !cfg.bientityCondition().get().test(BiEntityCtx.of(holder, target, holder.level()))) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(instance), holder);
            }
        } finally {
            sharing = false;
        }
    }
}
