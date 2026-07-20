package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

public final class ModifyEntityDataAction implements ActionType<EntityCtx, ModifyEntityDataAction.Cfg> {

    public record Cfg(DataKey data, Optional<Either<Boolean, Double>> value) {}

    public enum DataKey implements StringRepresentable {
        NO_GRAVITY("no_gravity"),
        INVULNERABLE("invulnerable"),
        SILENT("silent"),
        GLOWING("glowing"),
        INVISIBLE("invisible"),
        CUSTOM_NAME_VISIBLE("custom_name_visible"),
        NO_AI("no_ai"),
        FALL_DISTANCE("fall_distance"),
        AIR("air"),
        FIRE_TICKS("fire_ticks");

        public static final Codec<DataKey> CODEC = StringRepresentable.fromEnum(DataKey::values);

        private final String name;

        DataKey(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final Codec<Either<Boolean, Double>> VALUE_CODEC = Codec.either(Codec.BOOL, Codec.DOUBLE);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            DataKey.CODEC.fieldOf("data").forGetter(Cfg::data),
            VALUE_CODEC.optionalFieldOf("value").forGetter(Cfg::value)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return;
        Optional<Boolean> bool = cfg.value.flatMap(either -> either.left());
        Optional<Double> number = cfg.value.flatMap(either -> either.right());
        switch (cfg.data) {
            case NO_GRAVITY -> entity.setNoGravity(bool.orElse(!entity.isNoGravity()));
            case INVULNERABLE -> entity.setInvulnerable(bool.orElse(!entity.isInvulnerable()));
            case SILENT -> entity.setSilent(bool.orElse(!entity.isSilent()));
            case GLOWING -> entity.setGlowingTag(bool.orElse(!entity.hasGlowingTag()));
            case INVISIBLE -> entity.setInvisible(bool.orElse(!entity.isInvisible()));
            case CUSTOM_NAME_VISIBLE -> entity.setCustomNameVisible(bool.orElse(!entity.isCustomNameVisible()));
            case NO_AI -> {
                if (entity instanceof Mob mob) mob.setNoAi(bool.orElse(!mob.isNoAi()));
            }
            case FALL_DISTANCE -> number.ifPresent(d -> entity.fallDistance = d.floatValue());
            case AIR -> number.ifPresent(d -> entity.setAirSupply(d.intValue()));
            case FIRE_TICKS -> number.ifPresent(d -> entity.setRemainingFireTicks(d.intValue()));
        }
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
