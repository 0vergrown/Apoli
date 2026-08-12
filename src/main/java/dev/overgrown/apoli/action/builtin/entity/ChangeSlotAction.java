package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.InventoryType;
import dev.overgrown.apoli.data.ItemSlot;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ChangeSlotAction implements ActionType<EntityCtx, ChangeSlotAction.Cfg> {

    public enum Operation implements StringRepresentable {
        SWAP("swap"), MOVE("move");

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(
        ItemSlot slotA,
        ItemSlot slotB,
        Operation operation,
        InventoryType inventoryType,
        Optional<ResourceLocation> power
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemSlot.CODEC.fieldOf("slot_a").forGetter(Cfg::slotA),
            ItemSlot.CODEC.fieldOf("slot_b").forGetter(Cfg::slotB),
            Operation.CODEC.optionalFieldOf("operation", Operation.SWAP).forGetter(Cfg::operation),
            InventoryType.CODEC.optionalFieldOf("inventory_type", InventoryType.INVENTORY).forGetter(Cfg::inventoryType),
            IdCodecs.ID.optionalFieldOf("power").forGetter(Cfg::power)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.entity();
        if (entity == null || entity.level().isClientSide()) return;

        if (cfg.inventoryType() == InventoryType.POWER) {
            if (cfg.power().isEmpty() || !(entity instanceof LivingEntity living)) return;
            runOnPower(cfg, living, cfg.power().get());
            return;
        }
        runOnEntity(cfg, entity);
    }

    private static SlotAccess accessFor(Entity entity, ItemSlot slot) {
        if (entity instanceof LivingEntity living) {
            net.minecraft.world.entity.EquipmentSlot equipment = switch (slot.index()) {
                case ItemSlot.MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
                case ItemSlot.OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
                case ItemSlot.FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
                case ItemSlot.LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
                case ItemSlot.CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
                case ItemSlot.HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
                default -> null;
            };
            if (equipment != null) return SlotAccess.forEquipmentSlot(living, equipment);
        }
        return entity.getSlot(slot.index());
    }

    private static void runOnEntity(Cfg cfg, Entity entity) {
        SlotAccess refA = accessFor(entity, cfg.slotA());
        SlotAccess refB = accessFor(entity, cfg.slotB());
        if (refA == SlotAccess.NULL || refB == SlotAccess.NULL) return;

        ItemStack stackA = refA.get().copy();
        ItemStack stackB = refB.get().copy();
        switch (cfg.operation()) {
            case SWAP -> {
                if (!refA.set(stackB)) return;
                if (!refB.set(stackA)) {
                    refA.set(stackA);
                    return;
                }
            }
            case MOVE -> {
                if (!refB.set(stackA)) return;
                refA.set(ItemStack.EMPTY);
            }
        }
        if (entity instanceof ServerPlayer serverPlayer) serverPlayer.containerMenu.broadcastChanges();
    }

    private static void runOnPower(Cfg cfg, LivingEntity entity, ResourceLocation powerId) {
        SimpleContainer container = InventoryPower.openContainer(entity, powerId);
        if (container == null) return;

        int size = container.getContainerSize();
        int slotA = cfg.slotA().toContainerIndex();
        int slotB = cfg.slotB().toContainerIndex();
        if (slotA < 0 || slotA >= size || slotB < 0 || slotB >= size) return;

        ItemStack stackA = container.getItem(slotA).copy();
        ItemStack stackB = container.getItem(slotB).copy();
        switch (cfg.operation()) {
            case SWAP -> {
                container.setItem(slotA, stackB);
                container.setItem(slotB, stackA);
            }
            case MOVE -> {
                container.setItem(slotA, ItemStack.EMPTY);
                container.setItem(slotB, stackA);
            }
        }
        InventoryPower.saveContainer(entity, powerId, container);
    }
}
