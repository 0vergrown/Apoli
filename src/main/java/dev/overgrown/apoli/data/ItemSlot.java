package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public record ItemSlot(String name, int index) {
    public static final int OFFHAND = 99;
    public static final int MAINHAND = 98;
    public static final int FEET = 100;
    public static final int LEGS = 101;
    public static final int CHEST = 102;
    public static final int HEAD = 103;

    public static final Codec<ItemSlot> CODEC = Codec.STRING.comapFlatMap(
        ItemSlot::parse,
        ItemSlot::name
    );

    private static DataResult<ItemSlot> parse(String s) {
        String trimmed = s.trim();
        int dot = trimmed.indexOf('.');
        String head = dot < 0 ? trimmed : trimmed.substring(0, dot);
        String tail = dot < 0 ? "" : trimmed.substring(dot + 1);
        return switch (head) {
            case "weapon" -> tail.isEmpty() || tail.equals("mainhand")
                ? DataResult.success(new ItemSlot(s, MAINHAND))
                : tail.equals("offhand")
                    ? DataResult.success(new ItemSlot(s, OFFHAND))
                    : DataResult.error(() -> "Unknown weapon slot: " + s);
            case "armor" -> switch (tail) {
                case "head"  -> DataResult.success(new ItemSlot(s, HEAD));
                case "chest" -> DataResult.success(new ItemSlot(s, CHEST));
                case "legs"  -> DataResult.success(new ItemSlot(s, LEGS));
                case "feet"  -> DataResult.success(new ItemSlot(s, FEET));
                default      -> DataResult.error(() -> "Unknown armor slot: " + s);
            };
            case "hotbar" -> parseInt(tail, 0, 8, n -> new ItemSlot(s, n));
            case "inventory" -> parseInt(tail, 0, 26, n -> new ItemSlot(s, n + 9));
            case "container" -> parseInt(tail, 0, 53, n -> new ItemSlot(s, n));
            case "enderchest" -> parseInt(tail, 0, 26, n -> new ItemSlot(s, n + 200));
            case "horse" -> switch (tail) {
                case "saddle" -> DataResult.success(new ItemSlot(s, 400));
                case "armor"  -> DataResult.success(new ItemSlot(s, 401));
                default       -> parseInt(tail, 0, 14, n -> new ItemSlot(s, n + 500));
            };
            case "villager" -> parseInt(tail, 0, 7, n -> new ItemSlot(s, n + 300));
            default -> DataResult.error(() -> "Unknown slot: " + s);
        };
    }

    private interface SlotBuilder { ItemSlot build(int n); }
    private static DataResult<ItemSlot> parseInt(String s, int min, int max, SlotBuilder b) {
        try {
            int n = Integer.parseInt(s);
            if (n < min || n > max) return DataResult.error(() -> "Slot index out of range: " + s);
            return DataResult.success(b.build(n));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid slot index: " + s);
        }
    }

    public net.minecraft.world.item.ItemStack get(Player player) {
        Inventory inv = player.getInventory();
        return inv.getItem(toContainerIndex());
    }

    public int toContainerIndex() {
        if (index == MAINHAND) return -1;
        if (index == OFFHAND) return 40;
        if (index >= FEET && index <= HEAD) {
            return 36 + (index - FEET);
        }
        return index;
    }

    public boolean isEnderChest() {
        return index >= 200 && index <= 226;
    }

    public int enderChestIndex() {
        return index - 200;
    }
}
