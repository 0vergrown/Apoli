package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ModifyTypeTagPower extends PowerType<ModifyTypeTagPower.Cfg> {

    public record Cfg(List<TagKey<EntityType<?>>> tags, boolean included) {
        public Cfg {
            tags = List.copyOf(tags);
        }

        public boolean names(TagKey<EntityType<?>> tag) {
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).equals(tag)) return true;
            }
            return false;
        }
    }

    private static final Codec<TagKey<EntityType<?>>> TAG_CODEC = Codec.STRING.comapFlatMap(
        raw -> {
            String id = raw.startsWith("#") ? raw.substring(1) : raw;
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            return parsed == null
                ? DataResult.error(() -> "Not a valid entity type tag: " + raw)
                : DataResult.success(TagKey.create(Registries.ENTITY_TYPE, parsed));
        },
        tag -> "#" + tag.location()
    );

    private static final Codec<List<TagKey<EntityType<?>>>> TAGS_CODEC = Codec.either(
        TAG_CODEC, TAG_CODEC.listOf()
    ).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
    );

    private static final ThreadLocal<Entity> LINKED = new ThreadLocal<>();

    private static volatile boolean active = false;
    private static volatile int activeGeneration = -1;

    private static final List<TagKey<EntityType<?>>> LEGACY_GROUPS = List.of(
        TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("undead")),
        TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("arthropod")),
        TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("illager")),
        TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("aquatic")));

    private static final List<MobType> LEGACY_MOB_TYPES = List.of(
        MobType.UNDEAD, MobType.ARTHROPOD, MobType.ILLAGER, MobType.WATER);

    private record Raw(Optional<List<TagKey<EntityType<?>>>> tags, Optional<String> group, boolean included) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.<Raw>mapCodec(i -> i.group(
            TAGS_CODEC.optionalFieldOf("tag").forGetter(Raw::tags),
            Codec.STRING.optionalFieldOf("group").forGetter(Raw::group),
            Codec.BOOL.optionalFieldOf("included", true).forGetter(Raw::included)
        ).apply(i, Raw::new)).flatXmap(ModifyTypeTagPower::fromRaw, ModifyTypeTagPower::toRaw);
    }

    private static DataResult<Cfg> fromRaw(Raw raw) {
        if (raw.tags().isPresent()) {
            return DataResult.success(new Cfg(raw.tags().get(), raw.included()));
        }
        if (raw.group().isPresent()) {
            String group = raw.group().get().toLowerCase(Locale.ROOT);
            if ("default".equals(group)) {
                return DataResult.success(new Cfg(LEGACY_GROUPS, false));
            }
            for (TagKey<EntityType<?>> tag : LEGACY_GROUPS) {
                if (tag.location().getPath().equals(group)) {
                    return DataResult.success(new Cfg(List.of(tag), raw.included()));
                }
            }
            return DataResult.error(() -> "Unknown legacy entity group: " + raw.group().get());
        }
        return DataResult.error(() -> "apoli:modify_type_tag needs a \"tag\"");
    }

    private static DataResult<Raw> toRaw(Cfg cfg) {
        return DataResult.success(new Raw(Optional.of(cfg.tags()), Optional.empty(), cfg.included()));
    }

    public static boolean active() {
        int generation = ApoliPowers.generation();
        if (generation != activeGeneration) {
            active = ApoliPowers.anyOfType(ApoliIds.MODIFY_TYPE_TAG);
            activeGeneration = generation;
        }
        return active;
    }

    public static void link(Entity entity) {
        LINKED.set(entity);
    }

    public static void unlink() {
        LINKED.remove();
    }

    public static boolean resolve(boolean original, EntityType<?> queried, TagKey<EntityType<?>> tag) {
        Entity entity = LINKED.get();
        if (entity == null || entity.getType() != queried) return original;
        return resolve(entity, original, tag);
    }

    public static MobType resolveMobType(LivingEntity entity, MobType original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        if (container.powersOfType(ApoliIds.MODIFY_TYPE_TAG).isEmpty()) return original;
        for (int i = 0; i < LEGACY_GROUPS.size(); i++) {
            TagKey<EntityType<?>> tag = LEGACY_GROUPS.get(i);
            MobType mapped = LEGACY_MOB_TYPES.get(i);
            boolean vanilla = original == mapped;
            if (resolve(entity, vanilla, tag) != vanilla) {
                return vanilla ? MobType.UNDEFINED : mapped;
            }
        }
        return original;
    }

    public static boolean resolve(@Nullable Entity entity, boolean original, TagKey<EntityType<?>> tag) {
        if (entity == null) return original;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.MODIFY_TYPE_TAG);
        if (powers.isEmpty()) return original;

        EntityCtx ctx = null;
        boolean included = false;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Cfg cfg)) continue;
            if (!cfg.names(tag)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            if (!cfg.included()) return false;
            included = true;
        }
        return included || original;
    }
}
