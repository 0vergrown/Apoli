package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class KeepInventoryPower extends PowerType<KeepInventoryPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("keep_inventory");

    private static final int[] DEFAULT_SLOTS = defaultSlots();

    public record Config(
        Optional<ItemCondition> itemCondition,
        Optional<List<Integer>> slots
    ) {}

    public record Kept(int slot, ItemStack stack) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            Codec.list(Codec.INT).optionalFieldOf("slots").forGetter(Config::slots)
        ).apply(i, Config::new));
    }

    private static int[] defaultSlots() {
        int[] slots = new int[40];
        for (int i = 0; i < 36; i++) slots[i] = i;
        for (int i = 0; i < 4; i++) slots[36 + i] = 100 + i;
        return slots;
    }

    public static List<Kept> takeKept(Player player) {
        List<Kept> kept = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        PowerLookup.forEach(player, ApoliIds.KEEP_INVENTORY, Config.class, cfg -> {
            List<Integer> configured = cfg.slots.orElse(null);
            int count = configured == null ? DEFAULT_SLOTS.length : configured.size();
            for (int i = 0; i < count; i++) {
                int slot = configured == null ? DEFAULT_SLOTS[i] : configured.get(i);
                if (!visited.add(slot)) continue;
                SlotAccess access = player.getSlot(slot);
                ItemStack stack = access.get();
                if (stack.isEmpty()) continue;
                if (cfg.itemCondition.isPresent()
                    && !cfg.itemCondition.get().test(new ItemCtx(stack, player.level(), player))) continue;
                kept.add(new Kept(slot, stack.copy()));
                access.set(ItemStack.EMPTY);
            }
        });
        return kept;
    }

    public static void putBack(Player player, List<Kept> kept) {
        for (int i = 0; i < kept.size(); i++) {
            Kept entry = kept.get(i);
            player.getSlot(entry.slot()).set(entry.stack());
        }
    }

    public static boolean isHeldBy(Player player) {
        return PowerLookup.hasActive(player, ApoliIds.KEEP_INVENTORY);
    }
}
