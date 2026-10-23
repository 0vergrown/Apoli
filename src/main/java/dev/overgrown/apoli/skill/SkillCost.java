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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SkillCost(List<SkillPoints> points, List<ItemCost> items, boolean hidden) {
    public static final SkillCost FREE = new SkillCost(List.of(), List.of(), false);

    private static final int OFFHAND_SLOT = 40;
    private static final String[] BUNDLE_KEYS = {"points", "items", "hidden", "skill_tree"};

    public SkillCost {
        points = List.copyOf(points);
        items = List.copyOf(items);
    }

    public static SkillCost ofPoints(int points) {
        return points == 0 ? FREE : new SkillCost(List.of(SkillPoints.own(points)), List.of(), false);
    }

    public record ItemCost(ItemStack stack, Optional<ItemCondition> condition, boolean hidden) {
        public static final Codec<ItemCost> CODEC = RecordCodecBuilder.<ItemCost>mapCodec(i -> i.group(
            ItemStackData.MAP_CODEC.forGetter(cost -> new ItemStackData(cost.stack)),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(ItemCost::condition),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(ItemCost::hidden)
        ).apply(i, (data, condition, hidden) -> new ItemCost(data.stack(), condition, hidden))).codec()
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
        SkillPoints.LIST_CODEC.optionalFieldOf("points", List.of()).forGetter(SkillCost::points),
        SingleOrList.of(ItemCost.CODEC).optionalFieldOf("items", List.of()).forGetter(SkillCost::items),
        Codec.BOOL.optionalFieldOf("hidden", false).forGetter(SkillCost::hidden)
    ).apply(i, SkillCost::new));

    private static final Codec<SkillCost> OBJECT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SkillCost, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> reshaped = SkillPoints.liftTree(inlineItem(new Dynamic<>(ops, input)));
            return OBJECT.codec().decode(ops, reshaped.getValue());
        }

        @Override
        public <T> DataResult<T> encode(SkillCost input, DynamicOps<T> ops, T prefix) {
            return OBJECT.codec().encode(input, ops, prefix);
        }
    };

    public static final Codec<SkillCost> CODEC = Codec.either(Codec.INT, OBJECT_CODEC).xmap(
        either -> either.map(SkillCost::ofPoints, cost -> cost),
        cost -> cost.isPlainPoints() ? Either.left(cost.points.isEmpty() ? 0 : cost.points.get(0).amount()) : Either.right(cost));

    private boolean isPlainPoints() {
        if (!items.isEmpty() || hidden) return false;
        if (points.isEmpty()) return true;
        return points.size() == 1 && points.get(0).tree().isEmpty() && !points.get(0).hidden();
    }

    static <T> Dynamic<T> inlineItem(Dynamic<T> bundle) {
        if (bundle.get("item").result().isEmpty() && bundle.get("id").result().isEmpty()) return bundle;
        List<Dynamic<T>> items = new ArrayList<>();
        Optional<Dynamic<T>> listed = bundle.get("items").result();
        if (listed.isPresent()) {
            Optional<java.util.stream.Stream<Dynamic<T>>> stream = listed.get().asStreamOpt().result();
            if (stream.isPresent()) stream.get().forEach(items::add);
            else items.add(listed.get());
        }
        Dynamic<T> item = bundle;
        for (String key : BUNDLE_KEYS) item = item.remove(key);
        items.add(item);
        Dynamic<T> reshaped = bundle.emptyMap().set("items", bundle.createList(items.stream()));
        for (String key : BUNDLE_KEYS) {
            if (key.equals("items")) continue;
            Optional<Dynamic<T>> value = bundle.get(key).result();
            if (value.isPresent()) reshaped = reshaped.set(key, value.get());
        }
        return reshaped;
    }

    public boolean isFree() {
        return points.isEmpty() && items.isEmpty();
    }

    public boolean anyHidden() {
        if (hidden || SkillPoints.anyHidden(points)) return true;
        for (ItemCost item : items) {
            if (item.hidden) return true;
        }
        return false;
    }

    public int pointsIn(ResourceLocation tree, ResourceLocation ownTree) {
        int total = 0;
        for (SkillPoints entry : points) {
            if (entry.treeOr(ownTree).equals(tree)) total += entry.amount();
        }
        return total;
    }

    public boolean affordable(SkillData data, ResourceLocation ownTree) {
        if (points.isEmpty()) return true;
        for (Map.Entry<ResourceLocation, Integer> entry : SkillPoints.totals(points, ownTree).entrySet()) {
            if (data.getPoints(entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    public Map<ResourceLocation, Integer> pay(SkillData data, ResourceLocation ownTree) {
        if (points.isEmpty()) return Map.of();
        Map<ResourceLocation, Integer> paid = SkillPoints.totals(points, ownTree);
        for (Map.Entry<ResourceLocation, Integer> entry : paid.entrySet()) data.addPoints(entry.getKey(), -entry.getValue());
        return paid;
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
