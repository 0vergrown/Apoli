package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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
    private final Map<ResourceLocation, List<ItemStack>> paidItems;
    private final Map<ResourceLocation, Map<ResourceLocation, Integer>> paidPoints;
    private final Map<ResourceLocation, Map<ResourceLocation, Integer>> rewardPoints;
    private final Map<ResourceLocation, List<ItemStack>> rewardItems;

    public record Receipt(@Nullable Map<ResourceLocation, Integer> paidPoints, List<ItemStack> paidItems,
                          Map<ResourceLocation, Integer> rewardPoints, List<ItemStack> rewardItems) {
        public static Receipt reward(Map<ResourceLocation, Integer> rewardPoints, List<ItemStack> rewardItems) {
            return new Receipt(Map.of(), List.of(), rewardPoints, rewardItems);
        }
    }

    public SkillData() {
        this(new HashMap<>(), new HashSet<>(), new HashSet<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private SkillData(Map<ResourceLocation, Integer> points, Set<ResourceLocation> purchased,
                      Set<ResourceLocation> grantedTrees, Map<ResourceLocation, List<ItemStack>> paidItems,
                      Map<ResourceLocation, Map<ResourceLocation, Integer>> paidPoints,
                      Map<ResourceLocation, Map<ResourceLocation, Integer>> rewardPoints,
                      Map<ResourceLocation, List<ItemStack>> rewardItems) {
        this.points = new HashMap<>(points);
        this.purchased = new HashSet<>(purchased);
        this.grantedTrees = new HashSet<>(grantedTrees);
        this.paidItems = new HashMap<>(paidItems);
        this.paidPoints = new HashMap<>(paidPoints);
        this.rewardPoints = new HashMap<>(rewardPoints);
        this.rewardItems = new HashMap<>(rewardItems);
    }

    private static final Codec<Map<ResourceLocation, Map<ResourceLocation, Integer>>> POINTS_BY_SKILL =
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT));

    public static final Codec<SkillData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("points", Map.of()).forGetter(d -> d.points),
        ResourceLocation.CODEC.listOf().optionalFieldOf("purchased", List.of()).forGetter(d -> new ArrayList<>(d.purchased)),
        ResourceLocation.CODEC.listOf().optionalFieldOf("granted_trees", List.of()).forGetter(d -> new ArrayList<>(d.grantedTrees)),
        Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC.listOf()).optionalFieldOf("paid_items", Map.of()).forGetter(d -> d.paidItems),
        POINTS_BY_SKILL.optionalFieldOf("paid_points", Map.of()).forGetter(d -> d.paidPoints),
        POINTS_BY_SKILL.optionalFieldOf("reward_points", Map.of()).forGetter(d -> d.rewardPoints),
        Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC.listOf()).optionalFieldOf("reward_items", Map.of()).forGetter(d -> d.rewardItems)
    ).apply(i, (points, purchased, grantedTrees, paidItems, paidPoints, rewardPoints, rewardItems) ->
        new SkillData(points, new HashSet<>(purchased), new HashSet<>(grantedTrees), paidItems, paidPoints, rewardPoints, rewardItems)));

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

    public void purchase(ResourceLocation skill, Receipt receipt) {
        purchased.add(skill);
        store(paidItems, skill, receipt.paidItems());
        store(rewardItems, skill, receipt.rewardItems());
        if (receipt.paidPoints() == null) paidPoints.remove(skill);
        else paidPoints.put(skill, Map.copyOf(receipt.paidPoints()));
        if (receipt.rewardPoints().isEmpty()) rewardPoints.remove(skill);
        else rewardPoints.put(skill, Map.copyOf(receipt.rewardPoints()));
    }

    private static void store(Map<ResourceLocation, List<ItemStack>> stacks, ResourceLocation skill, List<ItemStack> recorded) {
        if (recorded.isEmpty()) stacks.remove(skill);
        else stacks.put(skill, List.copyOf(recorded));
    }

    public Receipt receipt(ResourceLocation skill) {
        return new Receipt(paidPoints.get(skill), paidItems.getOrDefault(skill, List.of()),
            rewardPoints.getOrDefault(skill, Map.of()), rewardItems.getOrDefault(skill, List.of()));
    }

    public Receipt unpurchase(ResourceLocation skill) {
        Receipt receipt = receipt(skill);
        purchased.remove(skill);
        paidItems.remove(skill);
        paidPoints.remove(skill);
        rewardPoints.remove(skill);
        rewardItems.remove(skill);
        return receipt;
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
        copyStacks(other.paidItems, this.paidItems);
        copyStacks(other.rewardItems, this.rewardItems);
        this.paidPoints.clear();
        this.paidPoints.putAll(other.paidPoints);
        this.rewardPoints.clear();
        this.rewardPoints.putAll(other.rewardPoints);
    }

    private static void copyStacks(Map<ResourceLocation, List<ItemStack>> from, Map<ResourceLocation, List<ItemStack>> to) {
        to.clear();
        from.forEach((skill, stacks) -> {
            List<ItemStack> copies = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) copies.add(stack.copy());
            to.put(skill, copies);
        });
    }
}
