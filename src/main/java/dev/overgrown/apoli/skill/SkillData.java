package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillData {
    private final Map<ResourceLocation, Integer> points;
    private final Set<ResourceLocation> purchased;

    public SkillData() {
        this(new HashMap<>(), new HashSet<>());
    }

    private SkillData(Map<ResourceLocation, Integer> points, Set<ResourceLocation> purchased) {
        this.points = new HashMap<>(points);
        this.purchased = new HashSet<>(purchased);
    }

    public static final Codec<SkillData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("points", Map.of()).forGetter(d -> d.points),
        ResourceLocation.CODEC.listOf().optionalFieldOf("purchased", List.of()).forGetter(d -> new ArrayList<>(d.purchased))
    ).apply(i, (points, purchased) -> new SkillData(points, new HashSet<>(purchased))));

    public int getPoints(ResourceLocation root) {
        return points.getOrDefault(root, 0);
    }

    public void setPoints(ResourceLocation root, int value) {
        points.put(root, Math.max(0, value));
    }

    public void addPoints(ResourceLocation root, int delta) {
        setPoints(root, getPoints(root) + delta);
    }

    public boolean isPurchased(ResourceLocation skill) {
        return purchased.contains(skill);
    }

    public void purchase(ResourceLocation skill) {
        purchased.add(skill);
    }

    public Map<ResourceLocation, Integer> pointsView() {
        return points;
    }

    public Set<ResourceLocation> purchasedView() {
        return purchased;
    }

    public void copyFrom(SkillData other) {
        this.points.clear();
        this.points.putAll(other.points);
        this.purchased.clear();
        this.purchased.addAll(other.purchased);
    }
}
