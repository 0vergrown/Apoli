package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;

public final class EntityGroupCondition implements ConditionType<EntityCtx, EntityGroupCondition.Cfg> {
    public enum Group implements StringRepresentable {
        DEFAULT("default", MobType.UNDEFINED),
        UNDEAD("undead", MobType.UNDEAD),
        ARTHROPOD("arthropod", MobType.ARTHROPOD),
        ILLAGER("illager", MobType.ILLAGER),
        AQUATIC("aquatic", MobType.WATER);

        public static final Codec<Group> CODEC = StringRepresentable.fromEnum(Group::values);
        private final String name;
        private final MobType vanilla;
        Group(String n, MobType v) { this.name = n; this.vanilla = v; }
        @Override public String getSerializedName() { return name; }
        public MobType vanilla() { return vanilla; }
    }

    public record Cfg(Group group) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Group.CODEC.fieldOf("group").forGetter(Cfg::group)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        return living != null && living.getMobType() == cfg.group.vanilla();
    }
}
