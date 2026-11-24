package dev.overgrown.apoli.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;

import java.util.UUID;

public final class BrewingStation extends StationContainer {
    public static final int INGREDIENT = 3;
    public static final int FUEL = 4;
    private static final int BREW_TIME = 400;
    private static final int FUEL_USES = 20;

    private int brewTime;
    private int brewTimeTotal = BREW_TIME;
    private int fuel;
    private Item ingredient = Items.AIR;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> brewTime <= 0 ? 0 : Math.max(1, Mth.ceil(brewTime * (float) BREW_TIME / brewTimeTotal));
                case 1 -> Math.min(fuel, FUEL_USES);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> brewTime = value;
                case 1 -> fuel = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BrewingStation(UUID owner, ResourceLocation powerId) {
        super(owner, powerId, 5);
    }

    @Override
    public ContainerData data() {
        return data;
    }

    @Override
    public boolean idle(StationSettings settings) {
        if (brewTime > 0) return false;
        if (getItem(INGREDIENT).isEmpty()) return true;
        return fuel <= 0 && settings.fuel() == FuelMode.ITEMS && getItem(FUEL).isEmpty();
    }

    @Override
    public boolean check(StationCheck check) {
        return switch (check) {
            case LIT -> fuel > 0;
            case WORKING -> brewTime > 0;
            case HAS_INPUT -> !getItem(INGREDIENT).isEmpty();
            case HAS_FUEL -> !getItem(FUEL).isEmpty();
            case HAS_OUTPUT -> !getItem(0).isEmpty() || !getItem(1).isEmpty() || !getItem(2).isEmpty();
        };
    }

    public boolean tick(ServerLevel level, ServerPlayer holder, StationSettings settings) {
        boolean changed = false;
        boolean finished = false;
        boolean brewable = isBrewable(level.potionBrewing());
        if (fuel <= 0) {
            if (settings.fuel() == FuelMode.ITEMS) {
                ItemStack fuelStack = getItem(FUEL);
                if (fuelStack.is(Items.BLAZE_POWDER)) {
                    fuel = Math.max(1, Math.round(FUEL_USES * settings.fuelEfficiency()));
                    fuelStack.shrink(1);
                    changed = true;
                }
            } else if (brewable && StationFuel.pay(holder, settings)) {
                fuel = settings.fuel() == FuelMode.NONE ? FUEL_USES : Math.max(1, Math.round(settings.fuelEfficiency()));
            }
        }

        ItemStack ingredientStack = getItem(INGREDIENT);
        if (brewTime > 0) {
            brewTime--;
            if (brewTime == 0 && brewable) {
                doBrew(level, holder);
                finished = true;
                changed = true;
            } else if (!brewable || !ingredientStack.is(ingredient)) {
                brewTime = 0;
                changed = true;
            }
        } else if (brewable && fuel > 0) {
            fuel--;
            brewTimeTotal = Math.max(1, Mth.ceil(BREW_TIME / settings.cookSpeed()));
            brewTime = brewTimeTotal;
            ingredient = ingredientStack.getItem();
            changed = true;
        }

        markValuesChanged();
        if (changed) setChanged();
        return finished;
    }

    private boolean isBrewable(PotionBrewing brewing) {
        ItemStack ingredientStack = getItem(INGREDIENT);
        if (ingredientStack.isEmpty() || !brewing.isIngredient(ingredientStack)) return false;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = getItem(i);
            if (!bottle.isEmpty() && brewing.hasMix(bottle, ingredientStack)) return true;
        }
        return false;
    }

    private void doBrew(ServerLevel level, ServerPlayer holder) {
        ItemStack ingredientStack = getItem(INGREDIENT);
        Item item = ingredientStack.getItem();
        PotionBrewing brewing = level.potionBrewing();
        for (int i = 0; i < 3; i++) setItem(i, brewing.mix(ingredientStack, getItem(i)));
        ingredientStack.shrink(1);
        if (item.hasCraftingRemainingItem()) {
            ItemStack remainder = new ItemStack(item.getCraftingRemainingItem());
            if (ingredientStack.isEmpty()) {
                ingredientStack = remainder;
            } else {
                holder.getInventory().placeItemBackInInventory(remainder);
            }
        }
        setItem(INGREDIENT, ingredientStack);
        level.levelEvent(1035, holder.blockPosition(), 0);
    }

    @Override
    protected void saveValues(CompoundTag tag) {
        tag.putInt("BrewTime", brewTime);
        tag.putInt("BrewTimeTotal", brewTimeTotal);
        tag.putInt("Fuel", fuel);
        tag.putString("Ingredient", BuiltInRegistries.ITEM.getKey(ingredient).toString());
    }

    @Override
    protected void loadValues(CompoundTag tag) {
        brewTime = tag.getInt("BrewTime");
        brewTimeTotal = Math.max(1, tag.contains("BrewTimeTotal") ? tag.getInt("BrewTimeTotal") : BREW_TIME);
        fuel = tag.getInt("Fuel");
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Ingredient"));
        ingredient = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }
}
