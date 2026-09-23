package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.recipe.PortableCraftingMenu;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class CraftingTableAction implements ActionType<EntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return;
        if (!(player instanceof ServerPlayer)) return;
        ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), player.blockPosition());
        MenuProvider provider = new SimpleMenuProvider(
            (id, inv, p) -> new PortableCraftingMenu(id, inv, access),
            Component.translatable("container.crafting"));
        player.openMenu(provider);
    }
}
