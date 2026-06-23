package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;

public final class EntityGroupCondition implements ConditionType<EntityCtx, EntityGroupCondition.Cfg> {
    public enum Group implements StringRepresentable {
        DEFAULT("default", null),
        UNDEAD("undead", EntityTypeTags.UNDEAD),
        ARTHROPOD("arthropod", EntityTypeTags.ARTHROPOD),
        ILLAGER("illager", EntityTypeTags.ILLAGER),
        AQUATIC("aquatic", EntityTypeTags.AQUATIC);

        public static final Codec<Group> CODEC = StringRepresentable.fromEnum(Group::values);
        private final String name;
        private final TagKey<EntityType<?>> tag;
        Group(String n, TagKey<EntityType<?>> t) { this.name = n; this.tag = t; }
        @Override public String getSerializedName() { return name; }
        public TagKey<EntityType<?>> tag() { return tag; }
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
        if (cfg.group == Group.DEFAULT) {
            for (Group g : Group.values()) {
                if (g == Group.DEFAULT) continue;
                if (ctx.entity().getType().is(g.tag())) return false;
            }
            return true;
        }
        return ctx.entity().getType().is(cfg.group.tag());
    }
}
