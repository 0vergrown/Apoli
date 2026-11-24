package dev.overgrown.apoli.station;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FurnaceStation extends StationContainer implements StackedContentsCompatible {
    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int RESULT = 2;

    private final FurnaceType type;
    private final RecipeManager.CachedCheck<Container, ? extends AbstractCookingRecipe> quickCheck;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;
    private boolean cookTimeStale = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookingProgress = value;
                case 3 -> cookingTotalTime = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FurnaceStation(UUID owner, ResourceLocation powerId, FurnaceType type) {
        super(owner, powerId, 3);
        this.type = type;
        this.quickCheck = RecipeManager.createCheck((RecipeType) type.recipeType());
    }

    public FurnaceType type() {
        return type;
    }

    @Override
    public ContainerData data() {
        return data;
    }

    private boolean isLit() {
        return litTime > 0;
    }

    @Override
    public boolean idle(StationSettings settings) {
        if (isLit() || cookingProgress > 0) return false;
        if (getItem(INPUT).isEmpty()) return true;
        return settings.fuel() == FuelMode.ITEMS && getItem(FUEL).isEmpty();
    }

    @Override
    public boolean check(StationCheck check) {
        return switch (check) {
            case LIT -> isLit();
            case WORKING -> cookingProgress > 0;
            case HAS_INPUT -> !getItem(INPUT).isEmpty();
            case HAS_FUEL -> !getItem(FUEL).isEmpty();
            case HAS_OUTPUT -> !getItem(RESULT).isEmpty();
        };
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack previous = getItem(slot);
        boolean same = !stack.isEmpty() && ItemStack.isSameItemSameTags(previous, stack);
        super.setItem(slot, stack);
        if (slot == INPUT && !same) {
            cookTimeStale = true;
            cookingProgress = 0;
        }
    }

    public boolean tick(ServerLevel level, ServerPlayer holder, StationSettings settings) {
        boolean wasLit = isLit();
        boolean changed = false;
        boolean finished = false;
        if (cookTimeStale) {
            cookingTotalTime = totalCookTime(level, settings);
            cookTimeStale = false;
        }
        if (isLit()) litTime--;

        ItemStack input = getItem(INPUT);
        ItemStack fuel = getItem(FUEL);
        boolean hasInput = !input.isEmpty();
        boolean hasFuel = settings.fuel() != FuelMode.ITEMS || !fuel.isEmpty();
        if (isLit() || hasFuel && hasInput) {
            RegistryAccess access = level.registryAccess();
            AbstractCookingRecipe recipe = hasInput ? quickCheck.getRecipeFor(this, level).orElse(null) : null;
            if (!isLit() && canBurn(access, recipe)) {
                litTime = takeFuel(holder, settings);
                litDuration = litTime;
                if (isLit()) changed = true;
            }
            if (isLit() && canBurn(access, recipe)) {
                cookingProgress++;
                if (cookingProgress >= cookingTotalTime) {
                    cookingProgress = 0;
                    cookingTotalTime = totalCookTime(level, settings);
                    if (burn(access, recipe)) {
                        recipesUsed.addTo(recipe.getId(), 1);
                        finished = true;
                    }
                    changed = true;
                }
            } else {
                cookingProgress = 0;
            }
        } else if (!isLit() && cookingProgress > 0) {
            cookingProgress = Mth.clamp(cookingProgress - 2, 0, cookingTotalTime);
        }

        if (wasLit != isLit()) changed = true;
        markValuesChanged();
        if (changed) setChanged();
        return finished;
    }

    private int takeFuel(ServerPlayer holder, StationSettings settings) {
        int ticks;
        if (settings.fuel() == FuelMode.ITEMS) {
            ItemStack fuel = getItem(FUEL);
            if (fuel.isEmpty()) return 0;
            ticks = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuel.getItem(), 0);
            if (type.halvedFuel()) ticks /= 2;
            if (ticks <= 0) return 0;
            Item item = fuel.getItem();
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                Item remainder = item.getCraftingRemainingItem();
                setItem(FUEL, remainder == null ? ItemStack.EMPTY : new ItemStack(remainder));
            }
        } else {
            if (!StationFuel.pay(holder, settings)) return 0;
            ticks = settings.fuelBurnTime();
        }
        return Math.max(1, Math.round(ticks * settings.fuelEfficiency()));
    }

    private int totalCookTime(ServerLevel level, StationSettings settings) {
        int base = quickCheck.getRecipeFor(this, level).map(AbstractCookingRecipe::getCookingTime).orElse(200);
        return Math.max(1, Mth.ceil(base / settings.cookSpeed()));
    }

    private boolean canBurn(RegistryAccess access, @Nullable AbstractCookingRecipe recipe) {
        if (getItem(INPUT).isEmpty() || recipe == null) return false;
        ItemStack result = recipe.getResultItem(access);
        if (result.isEmpty()) return false;
        ItemStack output = getItem(RESULT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItem(output, result)) return false;
        int total = output.getCount() + result.getCount();
        if (total <= getMaxStackSize() && total <= output.getMaxStackSize()) return true;
        return total <= result.getMaxStackSize();
    }

    private boolean burn(RegistryAccess access, @Nullable AbstractCookingRecipe recipe) {
        if (!canBurn(access, recipe)) return false;
        ItemStack input = getItem(INPUT);
        ItemStack result = recipe.getResultItem(access);
        ItemStack output = getItem(RESULT);
        if (output.isEmpty()) {
            setItem(RESULT, result.copy());
        } else if (output.is(result.getItem())) {
            output.grow(result.getCount());
        }
        if (input.is(Blocks.WET_SPONGE.asItem()) && getItem(FUEL).is(Items.BUCKET)) {
            setItem(FUEL, new ItemStack(Items.WATER_BUCKET));
        }
        input.shrink(1);
        return true;
    }

    public void awardUsedRecipes(ServerPlayer player) {
        if (recipesUsed.isEmpty()) return;
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();
        List<Recipe<?>> recipes = new ArrayList<>(recipesUsed.size());
        for (Object2IntMap.Entry<ResourceLocation> entry : recipesUsed.object2IntEntrySet()) {
            level.getRecipeManager().byKey(entry.getKey()).ifPresent(recipe -> {
                recipes.add(recipe);
                if (recipe instanceof AbstractCookingRecipe cooking) {
                    popExperience(level, pos, entry.getIntValue(), cooking.getExperience());
                }
            });
        }
        player.awardRecipes(recipes);
        recipesUsed.clear();
        markValuesChanged();
    }

    private static void popExperience(ServerLevel level, Vec3 pos, int count, float xp) {
        float total = count * xp;
        int amount = Mth.floor(total);
        float fraction = Mth.frac(total);
        if (fraction != 0.0F && Math.random() < fraction) amount++;
        ExperienceOrb.award(level, pos, amount);
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (int i = 0; i < getContainerSize(); i++) contents.accountStack(getItem(i));
    }

    @Override
    protected void saveValues(CompoundTag tag) {
        tag.putInt("BurnTime", litTime);
        tag.putInt("BurnDuration", litDuration);
        tag.putInt("CookTime", cookingProgress);
        tag.putInt("CookTimeTotal", cookingTotalTime);
        CompoundTag used = new CompoundTag();
        recipesUsed.forEach((id, count) -> used.putInt(id.toString(), count));
        tag.put("RecipesUsed", used);
    }

    @Override
    protected void loadValues(CompoundTag tag) {
        litTime = tag.getInt("BurnTime");
        litDuration = tag.getInt("BurnDuration");
        cookingProgress = tag.getInt("CookTime");
        cookingTotalTime = tag.getInt("CookTimeTotal");
        recipesUsed.clear();
        CompoundTag used = tag.getCompound("RecipesUsed");
        for (String key : used.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) recipesUsed.put(id, used.getInt(key));
        }
    }
}
