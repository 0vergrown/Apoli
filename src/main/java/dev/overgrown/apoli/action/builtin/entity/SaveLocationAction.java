package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.SavedLocations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class SaveLocationAction implements ActionType<EntityCtx, SaveLocationAction.Cfg> {
    public record Cfg(String id, boolean overwrite) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Cfg::id),
            Codec.BOOL.optionalFieldOf("overwrite", true).forGetter(Cfg::overwrite)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return;
        SavedLocations locations = SavedLocations.of(entity);
        if (locations == null) return;
        locations.put(entity.getUUID(), cfg.id,
            new SavedLocations.Location(entity.getX(), entity.getY(), entity.getZ(),
                level.dimension(), entity.getYRot(), entity.getXRot()),
            cfg.overwrite);
    }
}
