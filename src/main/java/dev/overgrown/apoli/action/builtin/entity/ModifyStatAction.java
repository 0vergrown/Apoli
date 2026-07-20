package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.Stat;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ModifyStatAction implements ActionType<EntityCtx, ModifyStatAction.Cfg> {
    public record Cfg(Stat stat, AttributeModifier modifier) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Stat.CODEC.fieldOf("stat").forGetter(Cfg::stat),
            AttributeModifier.CODEC.fieldOf("modifier").forGetter(Cfg::modifier)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        net.minecraft.stats.Stat<?> stat = cfg.stat.resolve();
        if (stat == null) return;
        int current = player.getStats().getValue(stat);
        float modified = AttributeModifierHelper.apply(current, List.of(cfg.modifier), player);
        Stat.setValue(player, stat, Math.max(0, (int) modified));
    }
}
