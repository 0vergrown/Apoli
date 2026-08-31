package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.entity.TeleportHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class TeleportToBiEntityAction implements ActionType<BiEntityCtx, TeleportToBiEntityAction.Cfg> {
    public record Cfg(
        boolean swap,
        Expression x,
        Expression y,
        Expression z,
        Space space,
        Optional<BlockCondition> landingBlockCondition,
        Optional<EntityCondition> landingCondition,
        boolean loadedChunksOnly,
        Optional<BiEntityAction> successAction,
        Optional<BiEntityAction> failAction
    ) {}

    private final boolean swapByDefault;

    public TeleportToBiEntityAction(boolean swapByDefault) {
        this.swapByDefault = swapByDefault;
    }

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.<Cfg>wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("swap", swapByDefault).forGetter(Cfg::swap),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("x", Expression.constant(0)).forGetter(Cfg::x),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("y", Expression.constant(0)).forGetter(Cfg::y),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("z", Expression.constant(0)).forGetter(Cfg::z),
                Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
                LoggedOptionalField.strict("landing_block_condition", BlockCondition.CODEC).forGetter(Cfg::landingBlockCondition),
                LoggedOptionalField.strict("landing_condition", EntityCondition.CODEC).forGetter(Cfg::landingCondition),
                Codec.BOOL.optionalFieldOf("loaded_chunks_only", true).forGetter(Cfg::loadedChunksOnly),
                LoggedOptionalField.of("success_action", BiEntityAction.CODEC).forGetter(Cfg::successAction),
                LoggedOptionalField.of("fail_action", BiEntityAction.CODEC).forGetter(Cfg::failAction)
            ).apply(i, Cfg::new)),
            java.util.Map.of("swap_position", "swap"));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null || actor == target) return;
        if (!(target.level() instanceof ServerLevel targetLevel)) return;
        if (!(actor.level() instanceof ServerLevel actorLevel)) return;

        Vec3 offset = cfg.space.toGlobal(target, new Vec3(cfg.x.eval(actor), cfg.y.eval(actor), cfg.z.eval(actor)));
        Vec3 destination = target.position().add(offset);
        Vec3 origin = actor.position();

        if (cfg.loadedChunksOnly && !TeleportHelper.chunkLoaded(targetLevel, destination.x, destination.z)) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        if (!TeleportHelper.landingAllowed(actor, targetLevel, destination.x, destination.y, destination.z,
            cfg.landingBlockCondition, cfg.landingCondition)) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }

        Entity movedActor = TeleportHelper.teleport(actor, targetLevel, destination.x, destination.y, destination.z);
        if (movedActor == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        Entity movedTarget = target;
        if (cfg.swap) {
            Entity swapped = TeleportHelper.teleport(target, actorLevel, origin.x, origin.y, origin.z);
            if (swapped != null) movedTarget = swapped;
        }
        Entity finalTarget = movedTarget;
        cfg.successAction.ifPresent(a -> a.run(new BiEntityCtx(movedActor, finalTarget, movedActor.level())));
    }
}
