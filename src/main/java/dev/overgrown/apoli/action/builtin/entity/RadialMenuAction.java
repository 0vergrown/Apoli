package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.RadialMenuEntry;
import dev.overgrown.apoli.network.payload.RadialMenuOpenS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RadialMenuAction implements ActionType<EntityCtx, RadialMenuAction.Cfg> {

    public record Cfg(List<RadialMenuEntry> entries, Optional<ResourceLocation> sprite) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.list(RadialMenuEntry.CODEC).fieldOf("entries").forGetter(Cfg::entries),
            ResourceLocation.CODEC.optionalFieldOf("sprite_location").forGetter(Cfg::sprite)
        ).apply(instance, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;

        List<RadialMenuOpenS2C.Entry> display = new ArrayList<>(cfg.entries.size());
        List<EntityAction> actions = new ArrayList<>(cfg.entries.size());
        for (RadialMenuEntry entry : cfg.entries) {
            if (entry.condition().isPresent() && !entry.condition().get().test(ctx)) continue;
            actions.add(entry.entityAction());
            display.add(new RadialMenuOpenS2C.Entry(
                entry.item(),
                entry.buttonTexture(),
                entry.icon(),
                entry.highlightIcon(),
                entry.highlightButtonTexture(),
                entry.tooltip(),
                entry.distance(),
                entry.velocity(),
                entry.buttonWidth(),
                entry.buttonHeight(),
                entry.iconWidth(),
                entry.iconHeight(),
                entry.itemWidth(),
                entry.itemHeight()));
        }
        if (display.isEmpty()) return;

        ApoliNetwork.openRadialMenu(player, cfg.sprite, display, actions);
    }
}
