package dev.overgrown.apoli.skill;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.SingleOrList;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SkillPoints(Optional<ResourceLocation> tree, int amount, boolean hidden) {
    private static final Codec<SkillPoints> OBJECT = AliasingMapCodec.wrap(RecordCodecBuilder.<SkillPoints>mapCodec(i -> i.group(
        IdCodecs.ID.optionalFieldOf("skill_tree").forGetter(SkillPoints::tree),
        Codec.INT.fieldOf("amount").forGetter(SkillPoints::amount),
        Codec.BOOL.optionalFieldOf("hidden", false).forGetter(SkillPoints::hidden)
    ).apply(i, SkillPoints::new)), Map.of("points", "amount")).codec();

    public static final Codec<SkillPoints> CODEC = Codec.either(Codec.INT, OBJECT).xmap(
        either -> either.map(SkillPoints::own, points -> points),
        points -> points.isPlainOwn() ? Either.left(points.amount) : Either.right(points));

    public static final Codec<List<SkillPoints>> LIST_CODEC = SingleOrList.of(CODEC).xmap(
        SkillPoints::withoutZeroes, list -> list);

    public static SkillPoints own(int amount) {
        return new SkillPoints(Optional.empty(), amount, false);
    }

    private boolean isPlainOwn() {
        return tree.isEmpty() && !hidden;
    }

    public ResourceLocation treeOr(ResourceLocation own) {
        return tree.orElse(own);
    }

    private static List<SkillPoints> withoutZeroes(List<SkillPoints> points) {
        List<SkillPoints> kept = new ArrayList<>(points.size());
        for (SkillPoints entry : points) {
            if (entry.amount != 0) kept.add(entry);
        }
        return List.copyOf(kept);
    }

    public static Map<ResourceLocation, Integer> totals(List<SkillPoints> points, ResourceLocation own) {
        Map<ResourceLocation, Integer> totals = new LinkedHashMap<>();
        for (SkillPoints entry : points) totals.merge(entry.treeOr(own), entry.amount, Integer::sum);
        return totals;
    }

    public static boolean anyHidden(List<SkillPoints> points) {
        for (SkillPoints entry : points) {
            if (entry.hidden) return true;
        }
        return false;
    }

    static <T> Dynamic<T> liftTree(Dynamic<T> bundle) {
        Optional<Dynamic<T>> tree = bundle.get("skill_tree").result();
        if (tree.isEmpty()) return bundle;
        Optional<Number> amount = bundle.get("points").asNumber().result();
        if (amount.isEmpty()) return bundle;
        Dynamic<T> entry = bundle.emptyMap()
            .set("skill_tree", tree.get())
            .set("amount", bundle.createInt(amount.get().intValue()));
        return bundle.remove("skill_tree").set("points", entry);
    }
}
