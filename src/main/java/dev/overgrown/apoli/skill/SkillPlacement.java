package dev.overgrown.apoli.skill;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record SkillPlacement(float x, float y) {
    private static final Codec<SkillPlacement> OBJECT = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("x").forGetter(SkillPlacement::x),
        Codec.FLOAT.fieldOf("y").forGetter(SkillPlacement::y)
    ).apply(i, SkillPlacement::new));

    public static final Codec<SkillPlacement> CODEC = Codec.either(Codec.FLOAT.listOf(), OBJECT).flatXmap(
        either -> either.map(SkillPlacement::fromList, DataResult::success),
        placement -> DataResult.success(Either.left(List.of(placement.x, placement.y))));

    private static DataResult<SkillPlacement> fromList(List<Float> list) {
        if (list.size() != 2) return DataResult.error(() -> "'placement' needs exactly two numbers, [x, y]");
        return DataResult.success(new SkillPlacement(list.get(0), list.get(1)));
    }
}
