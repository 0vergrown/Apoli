package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class ModifyDeathTicksAction implements ActionType<EntityCtx, ModifyDeathTicksAction.Cfg> {
    public record Cfg(AttributeModifier modifier) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.fieldOf("modifier").forGetter(Cfg::modifier)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null) return;
        float modified = AttributeModifierHelper.apply(entity.deathTime, List.of(cfg.modifier), entity);
        entity.deathTime = (int) modified;
    }
}
