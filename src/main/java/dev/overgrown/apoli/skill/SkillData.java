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
    private final Set<ResourceLocation> grantedTrees;

    public SkillData() {
        this(new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    private SkillData(Map<ResourceLocation, Integer> points, Set<ResourceLocation> purchased,
                      Set<ResourceLocation> grantedTrees) {
        this.points = new HashMap<>(points);
        this.purchased = new HashSet<>(purchased);
        this.grantedTrees = new HashSet<>(grantedTrees);
    }

    public static final Codec<SkillData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("points", Map.of()).forGetter(d -> d.points),
        ResourceLocation.CODEC.listOf().optionalFieldOf("purchased", List.of()).forGetter(d -> new ArrayList<>(d.purchased)),
        ResourceLocation.CODEC.listOf().optionalFieldOf("granted_trees", List.of()).forGetter(d -> new ArrayList<>(d.grantedTrees))
    ).apply(i, (points, purchased, grantedTrees) ->
        new SkillData(points, new HashSet<>(purchased), new HashSet<>(grantedTrees))));

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

    public boolean hasTree(ResourceLocation tree) {
        return grantedTrees.contains(tree);
    }

    public boolean grantTree(ResourceLocation tree) {
        return grantedTrees.add(tree);
    }

    public boolean revokeTree(ResourceLocation tree) {
        return grantedTrees.remove(tree);
    }

    public Map<ResourceLocation, Integer> pointsView() {
        return points;
    }

    public Set<ResourceLocation> purchasedView() {
        return purchased;
    }

    public Set<ResourceLocation> grantedTreesView() {
        return grantedTrees;
    }

    public void copyFrom(SkillData other) {
        this.points.clear();
        this.points.putAll(other.points);
        this.purchased.clear();
        this.purchased.addAll(other.purchased);
        this.grantedTrees.clear();
        this.grantedTrees.addAll(other.grantedTrees);
    }
}
