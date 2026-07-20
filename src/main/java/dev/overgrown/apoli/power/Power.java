package dev.overgrown.apoli.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.skill.Skill;
import dev.overgrown.apoli.skill.SkillInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record Power(
    ResourceLocation typeId,
    Optional<Component> name,
    Optional<Component> description,
    Optional<EntityCondition> condition,
    boolean hidden,
    Optional<SkillInfo> skill,
    Object config
) {
    public Power {
        typeId = PowerTypeRegistry.resolveId(typeId);
    }

    public Optional<Skill> toSkill(ResourceLocation id) {
        return skill.map(info -> new Skill(
            id, info.parent(), displayName(id), displayDescription(id), info.icon(),
            List.of(id), info.condition(), info.visibilityCondition(),
            info.excludes(), info.cost(), info.order()));
    }

    public PowerType<?> type() {
        return PowerTypeRegistry.get(typeId);
    }

    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable(translationKey(id, "name")));
    }

    public Component displayDescription(ResourceLocation id) {
        return description.orElseGet(() -> Component.translatable(translationKey(id, "description")));
    }

    public static String translationKey(ResourceLocation id, String suffix) {
        return TextComponent.autoKey(id, suffix);
    }

    public static final Codec<Power> CODEC = ResourceLocation.CODEC.partialDispatch(
        "type",
        (Power p) -> DataResult.success(p.typeId()),
        id -> {
            ResourceLocation canonical = PowerTypeRegistry.resolveId(id);
            PowerType<?> type = PowerTypeRegistry.get(canonical);
            if (type == null) return DataResult.<MapCodec<? extends Power>>error(() -> "Unknown power type: " + id);
            return DataResult.<MapCodec<? extends Power>>success(buildMapCodec(canonical, type));
        }
    );

    private static <C> MapCodec<Power> buildMapCodec(ResourceLocation typeId, PowerType<C> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            TextComponent.CODEC.optionalFieldOf("name").forGetter(Power::name),
            TextComponent.CODEC.optionalFieldOf("description").forGetter(Power::description),
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(Power::condition),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Power::hidden),
            SkillInfo.CODEC.optionalFieldOf("skill").forGetter(Power::skill),
            type.configCodec().forGetter((Power p) -> {
                @SuppressWarnings("unchecked")
                C cfg = (C) p.config();
                return cfg;
            })
        ).apply(instance, (name, desc, cond, hidden, skill, cfg) -> new Power(typeId, name, desc, cond, hidden, skill, cfg)));
    }
}
