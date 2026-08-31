package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.SavedLocations;
import dev.overgrown.apoli.entity.TeleportHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class TeleportToLocationAction implements ActionType<EntityCtx, TeleportToLocationAction.Cfg> {
    public record Cfg(
        String id,
        boolean keepRotation,
        boolean clear,
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Cfg::id),
            Codec.BOOL.optionalFieldOf("keep_rotation", false).forGetter(Cfg::keepRotation),
            Codec.BOOL.optionalFieldOf("clear", false).forGetter(Cfg::clear),
            LoggedOptionalField.of("success_action", EntityAction.CODEC).forGetter(Cfg::successAction),
            LoggedOptionalField.of("fail_action", EntityAction.CODEC).forGetter(Cfg::failAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || !(entity.level() instanceof ServerLevel current)) return;
        SavedLocations locations = SavedLocations.of(entity);
        SavedLocations.Location location = locations == null ? null : locations.get(entity.getUUID(), cfg.id);
        if (location == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        MinecraftServer server = current.getServer();
        ServerLevel level = server.getLevel(location.dimension());
        if (level == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        float yaw = cfg.keepRotation ? entity.getYRot() : location.yaw();
        float pitch = cfg.keepRotation ? entity.getXRot() : location.pitch();
        Entity moved = TeleportHelper.teleport(entity, level, location.x(), location.y(), location.z(), yaw, pitch);
        if (moved == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        if (cfg.clear) locations.remove(entity.getUUID(), cfg.id);
        cfg.successAction.ifPresent(a -> a.run(new EntityCtx(moved, moved.level())));
    }
}
