package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.entity.TeleportHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class TeleportAction implements ActionType<EntityCtx, TeleportAction.Cfg> {
    public record Cfg(
        Expression x,
        Expression y,
        Expression z,
        boolean relative,
        Space space,
        Optional<ResourceLocation> dimension,
        Optional<Expression> yaw,
        Optional<Expression> pitch,
        Optional<BlockCondition> landingBlockCondition,
        Optional<EntityCondition> landingCondition,
        boolean loadedChunksOnly,
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.<Cfg>wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.FLOAT_OR_EXPR.optionalFieldOf("x", Expression.constant(0)).forGetter(Cfg::x),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("y", Expression.constant(0)).forGetter(Cfg::y),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("z", Expression.constant(0)).forGetter(Cfg::z),
                Codec.BOOL.optionalFieldOf("relative", true).forGetter(Cfg::relative),
                Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
                IdCodecs.ID.optionalFieldOf("dimension").forGetter(Cfg::dimension),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("yaw").forGetter(Cfg::yaw),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("pitch").forGetter(Cfg::pitch),
                LoggedOptionalField.strict("landing_block_condition", BlockCondition.CODEC).forGetter(Cfg::landingBlockCondition),
                LoggedOptionalField.strict("landing_condition", EntityCondition.CODEC).forGetter(Cfg::landingCondition),
                Codec.BOOL.optionalFieldOf("loaded_chunks_only", true).forGetter(Cfg::loadedChunksOnly),
                LoggedOptionalField.of("success_action", EntityAction.CODEC).forGetter(Cfg::successAction),
                LoggedOptionalField.of("fail_action", EntityAction.CODEC).forGetter(Cfg::failAction)
            ).apply(i, Cfg::new)),
            java.util.Map.of("relative_to_position", "relative"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return;
        ServerLevel level = TeleportHelper.level(entity, cfg.dimension);
        if (level == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }

        Vec3 raw = new Vec3(cfg.x.eval(entity), cfg.y.eval(entity), cfg.z.eval(entity));
        Vec3 destination = cfg.relative
            ? entity.position().add(cfg.space.toGlobal(entity, raw))
            : raw;

        if (cfg.loadedChunksOnly && !TeleportHelper.chunkLoaded(level, destination.x, destination.z)) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        if (!TeleportHelper.landingAllowed(entity, level, destination.x, destination.y, destination.z,
            cfg.landingBlockCondition, cfg.landingCondition)) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }

        float yaw = cfg.yaw.map(e -> (float) e.eval(entity)).orElse(entity.getYRot());
        float pitch = cfg.pitch.map(e -> (float) e.eval(entity)).orElse(entity.getXRot());
        Entity moved = TeleportHelper.teleport(entity, level, destination.x, destination.y, destination.z, yaw, pitch);
        if (moved == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        cfg.successAction.ifPresent(a -> a.run(new EntityCtx(moved, moved.level())));
    }
}
