package dev.overgrown.apoli.skill;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SkillCost(int points, List<ItemCost> items) {
    public static final SkillCost FREE = new SkillCost(0, List.of());

    private static final int OFFHAND_SLOT = 40;

    public SkillCost {
        items = List.copyOf(items);
    }

    public static SkillCost ofPoints(int points) {
        return points == 0 ? FREE : new SkillCost(points, List.of());
    }

    public record ItemCost(ItemStack stack, Optional<ItemCondition> condition) {
        public static final Codec<ItemCost> CODEC = RecordCodecBuilder.<ItemCost>mapCodec(i -> i.group(
            ItemStackData.MAP_CODEC.forGetter(cost -> new ItemStackData(cost.stack)),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(ItemCost::condition)
        ).apply(i, (data, condition) -> new ItemCost(data.stack(), condition))).codec()
            .flatXmap(ItemCost::requireCount, DataResult::success);

        private static DataResult<ItemCost> requireCount(ItemCost cost) {
            if (cost.stack.getCount() >= 1) return DataResult.success(cost);
            return DataResult.error(() -> "A skill cost item needs a count of at least 1");
        }

        public int count() {
            return stack.getCount();
        }

        public boolean matches(ItemStack candidate, Player player) {
            if (candidate.isEmpty() || !candidate.is(stack.getItem())) return false;
            if (ItemStackData.hasData(stack) && !ItemStackData.sameData(candidate, stack)) return false;
            return condition.isEmpty() || condition.get().test(new ItemCtx(candidate, player.level(), player));
        }
    }

    private static final MapCodec<SkillCost> OBJECT = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.optionalFieldOf("points", 0).forGetter(SkillCost::points),
        SingleOrList.of(ItemCost.CODEC).optionalFieldOf("items", List.of()).forGetter(SkillCost::items)
    ).apply(i, SkillCost::new));

    private static final Codec<SkillCost> OBJECT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SkillCost, T>> decode(DynamicOps<T> ops, T input) {
            return OBJECT.codec().decode(ops, inlineItem(new Dynamic<>(ops, input)).getValue());
        }

        @Override
        public <T> DataResult<T> encode(SkillCost input, DynamicOps<T> ops, T prefix) {
            return OBJECT.codec().encode(input, ops, prefix);
        }
    };

    public static final Codec<SkillCost> CODEC = Codec.either(Codec.INT, OBJECT_CODEC).xmap(
        either -> either.map(SkillCost::ofPoints, cost -> cost),
        cost -> cost.items.isEmpty() ? Either.left(cost.points) : Either.right(cost));

    private static <T> Dynamic<T> inlineItem(Dynamic<T> cost) {
        if (cost.get("item").result().isEmpty() && cost.get("id").result().isEmpty()) return cost;
        List<Dynamic<T>> items = new ArrayList<>();
        Optional<Dynamic<T>> listed = cost.get("items").result();
        if (listed.isPresent()) {
            Optional<java.util.stream.Stream<Dynamic<T>>> stream = listed.get().asStreamOpt().result();
            if (stream.isPresent()) stream.get().forEach(items::add);
            else items.add(listed.get());
        }
        items.add(cost.remove("points").remove("items"));
        Dynamic<T> reshaped = cost.emptyMap().set("items", cost.createList(items.stream()));
        Optional<Dynamic<T>> points = cost.get("points").result();
        return points.isPresent() ? reshaped.set("points", points.get()) : reshaped;
    }

    public @Nullable List<ItemStack> takeItems(Player player) {
        if (items.isEmpty()) return List.of();
        int[] taken = allocate(player);
        if (taken == null) return null;
        Inventory inventory = player.getInventory();
        List<ItemStack> paid = new ArrayList<>();
        for (int slot = 0; slot < taken.length; slot++) {
            if (taken[slot] <= 0) continue;
            merge(paid, inventory.removeItem(slot, taken[slot]));
        }
        inventory.setChanged();
        return paid;
    }

    private int @Nullable [] allocate(Player player) {
        Inventory inventory = player.getInventory();
        int[] taken = new int[OFFHAND_SLOT + 1];
        for (int i = 0; i < items.size(); i++) {
            ItemCost cost = items.get(i);
            int needed = cost.count();
            for (int slot = 0; slot <= OFFHAND_SLOT && needed > 0; slot++) {
                if (!payable(slot, inventory)) continue;
                ItemStack stack = inventory.getItem(slot);
                int available = stack.getCount() - taken[slot];
                if (available <= 0 || !cost.matches(stack, player)) continue;
                int use = Math.min(available, needed);
                taken[slot] += use;
                needed -= use;
            }
            if (needed > 0) return null;
        }
        return taken;
    }

    private static boolean payable(int slot, Inventory inventory) {
        return slot < inventory.items.size() || slot == OFFHAND_SLOT;
    }

    private static void merge(List<ItemStack> paid, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (int i = 0; i < paid.size(); i++) {
            ItemStack existing = paid.get(i);
            if (!ItemStackData.sameData(existing, stack)) continue;
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
            if (stack.isEmpty()) return;
        }
        paid.add(stack);
    }

    public static void refund(Player player, List<ItemStack> paid) {
        for (int i = 0; i < paid.size(); i++) {
            ItemStack stack = paid.get(i);
            if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack.copy());
        }
    }
}
