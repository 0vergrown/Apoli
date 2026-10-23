package dev.overgrown.apoli.skill;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SkillReward(List<SkillPoints> points, List<RewardItem> items, boolean hidden) {
    public static final SkillReward NONE = new SkillReward(List.of(), List.of(), false);

    private static final int OFFHAND_SLOT = 40;

    public SkillReward {
        points = List.copyOf(points);
        items = List.copyOf(items);
    }

    public record RewardItem(ItemStack stack, boolean hidden) {
        public static final Codec<RewardItem> CODEC = RecordCodecBuilder.<RewardItem>mapCodec(i -> i.group(
            ItemStackData.MAP_CODEC.forGetter(item -> new ItemStackData(item.stack)),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(RewardItem::hidden)
        ).apply(i, (data, hidden) -> new RewardItem(data.stack(), hidden))).codec()
            .flatXmap(RewardItem::requireCount, DataResult::success);

        private static DataResult<RewardItem> requireCount(RewardItem item) {
            if (item.stack.getCount() >= 1) return DataResult.success(item);
            return DataResult.error(() -> "A skill reward item needs a count of at least 1");
        }
    }

    private static final MapCodec<SkillReward> OBJECT = RecordCodecBuilder.mapCodec(i -> i.group(
        SkillPoints.LIST_CODEC.optionalFieldOf("points", List.of()).forGetter(SkillReward::points),
        SingleOrList.of(RewardItem.CODEC).optionalFieldOf("items", List.of()).forGetter(SkillReward::items),
        Codec.BOOL.optionalFieldOf("hidden", false).forGetter(SkillReward::hidden)
    ).apply(i, SkillReward::new));

    private static final Codec<SkillReward> OBJECT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SkillReward, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> reshaped = SkillPoints.liftTree(SkillCost.inlineItem(new Dynamic<>(ops, input)));
            return OBJECT.codec().decode(ops, reshaped.getValue());
        }

        @Override
        public <T> DataResult<T> encode(SkillReward input, DynamicOps<T> ops, T prefix) {
            return OBJECT.codec().encode(input, ops, prefix);
        }
    };

    public static final Codec<SkillReward> CODEC = Codec.either(Codec.INT, OBJECT_CODEC).xmap(
        either -> either.map(SkillReward::ofPoints, reward -> reward),
        reward -> reward.isPlainPoints() ? Either.left(reward.points.get(0).amount()) : Either.right(reward));

    public static SkillReward ofPoints(int points) {
        return points == 0 ? NONE : new SkillReward(List.of(SkillPoints.own(points)), List.of(), false);
    }

    private boolean isPlainPoints() {
        return items.isEmpty() && !hidden && points.size() == 1
            && points.get(0).tree().isEmpty() && !points.get(0).hidden();
    }

    public boolean isEmpty() {
        return points.isEmpty() && items.isEmpty();
    }

    public Map<ResourceLocation, Integer> givePoints(SkillData data, ResourceLocation ownTree) {
        if (points.isEmpty()) return Map.of();
        Map<ResourceLocation, Integer> given = SkillPoints.totals(points, ownTree);
        for (Map.Entry<ResourceLocation, Integer> entry : given.entrySet()) data.addPoints(entry.getKey(), entry.getValue());
        return given;
    }

    public List<ItemStack> giveItems(Player player) {
        if (items.isEmpty()) return List.of();
        List<ItemStack> given = new ArrayList<>(items.size());
        for (RewardItem item : items) {
            given.add(item.stack.copy());
            player.getInventory().placeItemBackInInventory(item.stack.copy());
        }
        return given;
    }

    public static boolean canTakeBack(Player player, SkillData data, SkillData.Receipt receipt) {
        for (Map.Entry<ResourceLocation, Integer> entry : receipt.rewardPoints().entrySet()) {
            if (data.getPoints(entry.getKey()) < entry.getValue()) return false;
        }
        if (receipt.rewardItems().isEmpty()) return true;
        Inventory inventory = player.getInventory();
        int[] taken = new int[OFFHAND_SLOT + 1];
        for (ItemStack reward : receipt.rewardItems()) {
            if (claim(inventory, reward, reward.getCount(), taken) > 0) return false;
        }
        return true;
    }

    public static void takeBack(Player player, SkillData data, SkillData.Receipt receipt) {
        for (Map.Entry<ResourceLocation, Integer> entry : receipt.rewardPoints().entrySet()) {
            data.addPoints(entry.getKey(), -entry.getValue());
        }
        if (receipt.rewardItems().isEmpty()) return;
        Inventory inventory = player.getInventory();
        int[] taken = new int[OFFHAND_SLOT + 1];
        for (ItemStack reward : receipt.rewardItems()) claim(inventory, reward, reward.getCount(), taken);
        for (int slot = 0; slot < taken.length; slot++) {
            if (taken[slot] > 0) inventory.removeItem(slot, taken[slot]);
        }
        inventory.setChanged();
    }

    private static int claim(Inventory inventory, ItemStack wanted, int needed, int[] taken) {
        for (int slot = 0; slot <= OFFHAND_SLOT && needed > 0; slot++) {
            if (slot >= inventory.items.size() && slot != OFFHAND_SLOT) continue;
            ItemStack stack = inventory.getItem(slot);
            int available = stack.getCount() - taken[slot];
            if (available <= 0 || !ItemStackData.sameData(stack, wanted)) continue;
            int use = Math.min(available, needed);
            taken[slot] += use;
            needed -= use;
        }
        return needed;
    }
}
