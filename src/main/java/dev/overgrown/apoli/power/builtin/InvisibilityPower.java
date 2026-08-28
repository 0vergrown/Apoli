package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class InvisibilityPower extends PowerType<InvisibilityPower.Config> {
    public record Config(
        boolean renderArmor,
        boolean renderOutline,
        Optional<BiEntityCondition> bientityCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("render_armor", false).forGetter(Config::renderArmor),
            Codec.BOOL.optionalFieldOf("render_outline", false).forGetter(Config::renderOutline),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static boolean hidesArmor(@Nullable Entity entity) {
        return suppresses(entity, true);
    }

    public static boolean hidesOutline(@Nullable Entity entity) {
        return suppresses(entity, false);
    }

    private static boolean suppresses(@Nullable Entity entity, boolean armor) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.INVISIBILITY);
        if (powers.isEmpty()) return false;

        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof Config cfg)) continue;
            if (armor ? cfg.renderArmor() : cfg.renderOutline()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return true;
        }
        return false;
    }
}
