package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.InventoryType;
import dev.overgrown.apoli.data.ItemSlot;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.data.ProcessMode;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public final class InventoryAction implements ActionType<EntityCtx, InventoryAction.Cfg> {

    public enum Operation implements StringRepresentable {
        MODIFY("modify"), REPLACE("replace"), DROP("drop");
        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);
        private final String name;
        Operation(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(
        Operation operation,
        InventoryType inventoryType,
        Optional<EntityAction> entityAction,
        Optional<ItemCondition> itemCondition,
        Optional<ItemSlot> slot,
        Optional<List<ItemSlot>> slots,
        Optional<ResourceLocation> power,
        ProcessMode processMode,
        int limit,
        Optional<ItemAction> itemAction,
        Optional<ItemStackData> stack,
        boolean mergeNbt,
        boolean throwRandomly,
        boolean retainOwnership,
        Optional<Integer> amount
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Operation.CODEC.optionalFieldOf("operation", Operation.MODIFY).forGetter(Cfg::operation),
            InventoryType.CODEC.optionalFieldOf("inventory_type", InventoryType.INVENTORY).forGetter(Cfg::inventoryType),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Cfg::entityAction),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Cfg::itemCondition),
            ItemSlot.CODEC.optionalFieldOf("slot").forGetter(Cfg::slot),
            Codec.list(ItemSlot.CODEC).optionalFieldOf("slots").forGetter(Cfg::slots),
            ResourceLocation.CODEC.optionalFieldOf("power").forGetter(Cfg::power),
            ProcessMode.CODEC.optionalFieldOf("process_mode", ProcessMode.STACKS).forGetter(Cfg::processMode),
            Codec.INT.optionalFieldOf("limit", 0).forGetter(Cfg::limit),
            ItemAction.CODEC.optionalFieldOf("item_action").forGetter(Cfg::itemAction),
            ItemStackData.CODEC.optionalFieldOf("stack").forGetter(Cfg::stack),
            Codec.BOOL.optionalFieldOf("merge_nbt", false).forGetter(Cfg::mergeNbt),
            Codec.BOOL.optionalFieldOf("throw_randomly", false).forGetter(Cfg::throwRandomly),
            Codec.BOOL.optionalFieldOf("retain_ownership", true).forGetter(Cfg::retainOwnership),
            Codec.INT.optionalFieldOf("amount").forGetter(Cfg::amount)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.entity();
        cfg.entityAction.ifPresent(a -> a.run(ctx));
        if (cfg.inventoryType == InventoryType.POWER) {
            if (cfg.power.isEmpty()) return;
            ResourceLocation powerId = cfg.power.get();
            SimpleContainer container = InventoryPower.openContainer(entity, powerId);
            if (container == null) return;
            apply(cfg, container, entity, ctx);
            InventoryPower.saveContainer(entity, powerId, container);
            return;
        }
        if (!(entity instanceof Player player)) return;
        apply(cfg, player.getInventory(), entity, ctx);
    }

    private void apply(Cfg cfg, Container container, LivingEntity entity, EntityCtx ctx) {
        ProcessMode mode = cfg.operation == Operation.MODIFY ? cfg.processMode : ProcessMode.STACKS;
        InventoryHelper.walk(container, entity, cfg.itemCondition, cfg.slot, cfg.slots, mode, cfg.limit,
            (idx, stack) -> {
                switch (cfg.operation) {
                    case MODIFY -> cfg.itemAction.ifPresent(a -> a.run(new ItemCtx(stack, ctx.level(), entity)));
                    case REPLACE -> {
                        if (cfg.stack.isEmpty()) return;
                        ItemStack replacement = cfg.stack.get().stack().copy();
                        if (cfg.mergeNbt && stack.hasTag() && replacement.getItem() != Items.AIR) {
                            CompoundTag merged = stack.getTag().copy();
                            if (replacement.hasTag()) merged.merge(replacement.getTag());
                            replacement.setTag(merged);
                        }
                        container.setItem(idx, replacement);
                        cfg.itemAction.ifPresent(a -> a.run(new ItemCtx(replacement, ctx.level(), entity)));
                    }
                    case DROP -> {
                        int drop = Math.min(cfg.amount.orElse(stack.getCount()), stack.getCount());
                        ItemStack split = stack.split(drop);
                        cfg.itemAction.ifPresent(a -> a.run(new ItemCtx(split, ctx.level(), entity)));
                        dropStack(entity, split, cfg.throwRandomly, cfg.retainOwnership);
                        container.setItem(idx, stack);
                    }
                }
            });
    }

    private static void dropStack(LivingEntity entity, ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        if (entity instanceof Player player) {
            player.drop(stack, throwRandomly, retainOwnership);
        } else {
            entity.spawnAtLocation(stack);
        }
    }
}
