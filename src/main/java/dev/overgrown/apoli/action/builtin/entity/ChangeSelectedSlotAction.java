package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class ChangeSelectedSlotAction implements ActionType<EntityCtx, ChangeSelectedSlotAction.Cfg> {

    public record Cfg(int slot, boolean relative) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("slot").forGetter(Cfg::slot),
            Codec.BOOL.optionalFieldOf("relative", false).forGetter(Cfg::relative)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return;

        int size = Inventory.getSelectionSize();
        Inventory inventory = player.getInventory();
        int slot = cfg.relative()
            ? Math.floorMod(inventory.selected + cfg.slot(), size)
            : Mth.clamp(cfg.slot(), 0, size - 1);
        if (inventory.selected == slot) return;

        inventory.selected = slot;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetCarriedItemPacket(slot));
            serverPlayer.containerMenu.broadcastChanges();
        }
    }
}
