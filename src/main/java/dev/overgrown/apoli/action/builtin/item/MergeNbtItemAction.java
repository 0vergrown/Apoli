package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class MergeNbtItemAction implements ActionType<ItemCtx, MergeNbtItemAction.Cfg> {
    public record Cfg(Nbt nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Nbt.CODEC.fieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        if (ctx.stack() == null || ctx.stack().isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, ctx.stack(),
            tag -> tag.merge(cfg.nbt.tag()));
    }
}
