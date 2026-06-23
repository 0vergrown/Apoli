package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ExhaustPower extends PowerType<ExhaustPower.Config> {
    public record Config(int interval, float exhaustion) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("interval", 20).forGetter(Config::interval),
            Codec.FLOAT.fieldOf("exhaustion").forGetter(Config::exhaustion)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (cfg.interval <= 0) return;
        if (player.tickCount % cfg.interval != 0) return;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded != null && loaded.condition().isPresent()
            && !loaded.condition().get().test(new EntityCtx(player, level))) return;
        player.causeFoodExhaustion(cfg.exhaustion);
    }
}
