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

public final class ModifyHearingRangePower extends PowerType<ModifyHearingRangePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_hearing_range");

    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers,
                         boolean sounds,
                         boolean voice,
                         Optional<BiEntityCondition> bientityCondition) {
        public List<AttributeModifier> flattened() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Codec.BOOL.optionalFieldOf("sounds", true).forGetter(Config::sounds),
            Codec.BOOL.optionalFieldOf("voice", true).forGetter(Config::voice),
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

    public static double soundRange(@Nullable Entity listener, double base) {
        if (listener == null) return base;
        PowerContainer container = PowerContainer.of(listener);
        if (container == null || container.isEmpty()) return base;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        if (powers.isEmpty()) return base;
        EntityCtx ctx = null;
        double range = base;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof Config cfg) || !cfg.sounds()) continue;
            if (cfg.bientityCondition().isPresent()) continue;
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(listener, listener.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            range = AttributeModifierHelper.apply(range, mods, listener, container);
        }
        return range;
    }

    public static boolean hearsDifferently(@Nullable Entity listener) {
        if (listener == null) return false;
        PowerContainer container = PowerContainer.of(listener);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (cfg.voice() && !cfg.flattened().isEmpty()) return true;
        }
        return false;
    }

    public static double @Nullable [] voiceRanges(@Nullable Entity listener, @Nullable Entity speaker,
                                                  double normalBase, double whisperBase) {
        if (listener == null) return null;
        PowerContainer container = PowerContainer.of(listener);
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
            if (power == null) continue;
            if (!(power.config() instanceof Config cfg) || !cfg.voice()) continue;
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
            if (cfg.bientityCondition().isPresent()) {
                if (speaker == null) continue;
                if (!cfg.bientityCondition().get().test(
                    BiEntityCtx.of(listener, speaker, listener.level()))) continue;
            }
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(listener, listener.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            normal = AttributeModifierHelper.apply(normal, mods, listener, container);
            whisper = AttributeModifierHelper.apply(whisper, mods, listener, container);
            any = true;
        }
        if (!any) return null;
        return new double[]{normal, whisper};
    }
}
