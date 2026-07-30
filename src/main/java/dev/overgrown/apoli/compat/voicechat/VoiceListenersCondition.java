package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.voicechat.VoiceState;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class VoiceListenersCondition implements ConditionType<EntityCtx, VoiceListenersCondition.Cfg> {
    public record Cfg(double range, int minCount) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("range", 16.0).forGetter(Cfg::range),
            Codec.INT.optionalFieldOf("min_count", 1).forGetter(Cfg::minCount)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity self = ctx.entity();
        if (!(ctx.level() instanceof ServerLevel level)) {
            return false;
        }
        double r2 = cfg.range() * cfg.range();
        int count = 0;
        for (ServerPlayer player : level.players()) {
            if (player == self || VoiceState.isDisconnected(player.getUUID())) {
                continue;
            }
            if (player.distanceToSqr(self) <= r2) {
                count++;
                if (count >= cfg.minCount()) {
                    return true;
                }
            }
        }
        return count >= cfg.minCount();
    }
}
