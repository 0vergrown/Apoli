package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ModifySpeakingRangePower extends PowerType<ModifySpeakingRangePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_speaking_range");

    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers,
                         boolean normal,
                         boolean whisper,
                         Optional<BiEntityCondition> bientityCondition,
                         List<AttributeModifier> flattened) {
        Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers,
               boolean normal, boolean whisper, Optional<BiEntityCondition> bientityCondition) {
            this(modifier, modifiers, normal, whisper, bientityCondition,
                AttributeModifierHelper.flatten(modifier, modifiers));
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Codec.BOOL.optionalFieldOf("normal", true).forGetter(Config::normal),
            Codec.BOOL.optionalFieldOf("whisper", true).forGetter(Config::whisper),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC)
                .forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    private static volatile int cachedGeneration = -1;
    private static volatile boolean cachedInUse;

    public static boolean inUse() {
        int generation = ApoliPowers.generation();
        if (generation != cachedGeneration) {
            cachedInUse = ApoliPowers.anyOfType(CANONICAL);
            cachedGeneration = generation;
        }
        return cachedInUse;
    }

    public static double @Nullable [] ranges(@Nullable Entity speaker, double normalBase, double whisperBase) {
        return apply(speaker, null, normalBase, whisperBase);
    }

    public static double @Nullable [] rangesToward(@Nullable Entity speaker, Entity listener,
                                                   double normalBase, double whisperBase) {
        return apply(speaker, listener, normalBase, whisperBase);
    }

    public static boolean targetsListeners(@Nullable Entity speaker) {
        if (speaker == null) return false;
        PowerContainer container = PowerContainer.of(speaker);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (cfg.bientityCondition().isPresent() && !cfg.flattened().isEmpty()) return true;
        }
        return false;
    }

    private static double @Nullable [] apply(@Nullable Entity speaker, @Nullable Entity listener,
                                             double normalBase, double whisperBase) {
        if (speaker == null) return null;
        PowerContainer container = PowerContainer.of(speaker);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        if (powers.isEmpty()) return null;
        EntityCtx ctx = null;
        double normal = normalBase;
        double whisper = whisperBase;
        boolean any = false;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (cfg.bientityCondition().isPresent() != (listener != null)) continue;
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
            if (listener != null && !cfg.bientityCondition().get().test(
                BiEntityCtx.of(speaker, listener, speaker.level()))) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(speaker, speaker.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            if (cfg.normal()) normal = AttributeModifierHelper.apply(normal, mods, speaker, container);
            if (cfg.whisper()) whisper = AttributeModifierHelper.apply(whisper, mods, speaker, container);
            any = true;
        }
        if (!any) return null;
        return new double[]{normal, whisper};
    }
}
