package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
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
                         boolean whisper) {
        public List<AttributeModifier> flattened() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Codec.BOOL.optionalFieldOf("normal", true).forGetter(Config::normal),
            Codec.BOOL.optionalFieldOf("whisper", true).forGetter(Config::whisper)
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
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
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
