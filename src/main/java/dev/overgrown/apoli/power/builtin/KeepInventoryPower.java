package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class KeepInventoryPower extends PowerType<KeepInventoryPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("keep_inventory");

    public record Config(
        Optional<ItemCondition> itemCondition,
        Optional<List<Integer>> slots
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            Codec.list(Codec.INT).optionalFieldOf("slots").forGetter(Config::slots)
        ).apply(i, Config::new));
    }

    public static boolean shouldKeep(LivingEntity entity, ItemStack stack, int slotIndex) {
        if (stack.isEmpty()) return false;
        boolean[] keep = new boolean[]{false};
        ItemCtx ctx = new ItemCtx(stack, entity.level(), entity);
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (keep[0]) return;
            boolean slotsOk = cfg.slots.isEmpty() || cfg.slots.get().contains(slotIndex);
            boolean itemOk = cfg.itemCondition.isEmpty() || cfg.itemCondition.get().test(ctx);
            if (slotsOk && itemOk) keep[0] = true;
        });
        return keep[0];
    }
}
