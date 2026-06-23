package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.PositionedItemStack;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class StartingEquipmentPower extends PowerType<StartingEquipmentPower.Config> {
    public record Config(
        Optional<PositionedItemStack> stack,
        Optional<List<PositionedItemStack>> stacks,
        boolean recurrent
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            PositionedItemStack.CODEC.optionalFieldOf("stack").forGetter(Config::stack),
            Codec.list(PositionedItemStack.CODEC).optionalFieldOf("stacks").forGetter(Config::stacks),
            Codec.BOOL.optionalFieldOf("recurrent", false).forGetter(Config::recurrent)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        giveAll(holder.owner(), cfg);
    }

    public static void onRespawn(LivingEntity owner, Config cfg) {
        if (cfg.recurrent) giveAll(owner, cfg);
    }

    private static void giveAll(LivingEntity owner, Config cfg) {
        if (!(owner instanceof Player player)) return;
        cfg.stack.ifPresent(p -> give(player, p));
        cfg.stacks.ifPresent(list -> list.forEach(p -> give(player, p)));
    }

    private static void give(Player player, PositionedItemStack pis) {
        ItemStack stack = pis.stack().copy();
        OptionalInt slot = pis.slot();
        if (slot.isPresent()) {
            int s = slot.getAsInt();
            if (s >= 0 && s < player.getInventory().getContainerSize()) {
                ItemStack existing = player.getInventory().getItem(s);
                if (existing.isEmpty()) {
                    player.getInventory().setItem(s, stack);
                    return;
                }
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
