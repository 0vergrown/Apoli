package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.keybind.HeldKeys;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class KeyPressedCondition implements ConditionType<EntityCtx, KeyPressedCondition.Config> {
    public record Config(Key key) {}

    @Override
    public MapCodec<Config> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key)
        ).apply(i, Config::new));
    }

    @Override
    public boolean test(Config cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.entity();
        if (entity == null) return false;
        Level level = ctx.level();
        String key = cfg.key().key();
        if (level != null && level.isClientSide()) {
            return HeldKeys.clientHeld(entity, key);
        }
        return HeldKeys.serverHeld(entity.getUUID(), key);
    }
}