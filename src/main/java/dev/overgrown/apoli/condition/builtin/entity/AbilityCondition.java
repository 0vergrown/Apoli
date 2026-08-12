package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

public final class AbilityCondition implements ConditionType<EntityCtx, AbilityCondition.Cfg> {
    public record Cfg(ResourceLocation ability) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("ability").forGetter(Cfg::ability)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        Abilities a = player.getAbilities();
        return switch (cfg.ability.getPath()) {
            case "flying" -> a.flying;
            case "instabuild" -> a.instabuild;
            case "invulnerable" -> a.invulnerable;
            case "mayBuild" -> a.mayBuild;
            case "mayfly" -> a.mayfly;
            default -> false;
        };
    }
}
