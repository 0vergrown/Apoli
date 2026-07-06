package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.network.VelocityUpdater;
import dev.overgrown.apoli.rope.Rope;
import dev.overgrown.apoli.rope.RopeManager;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;


public final class RopePullAction implements ActionType<EntityCtx, RopePullAction.Cfg> {

    public enum Which implements StringRepresentable {
        SELF("self"), OTHER("other"), BOTH("both");
        public static final Codec<Which> CODEC = StringRepresentable.fromEnum(Which::values);
        private final String name;
        Which(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(Optional<String> slot, Which which, double speed, boolean set, double reel) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("slot").forGetter(Cfg::slot),
            Which.CODEC.optionalFieldOf("which", Which.SELF).forGetter(Cfg::which),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(Cfg::speed),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(Cfg::set),
            Codec.DOUBLE.optionalFieldOf("reel", 0.0).forGetter(Cfg::reel)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.raw();
        Level level = ctx.level();
        if (actor == null) return;

        for (Rope rope : RopeManager.forOwner(actor.getUUID())) {
            if (cfg.slot.isPresent() && !cfg.slot.get().equals(rope.slot)) continue;

            Vec3 fromPos = rope.from.position(level);
            Vec3 toPos = rope.to.position(level);
            if (fromPos == null || toPos == null) continue;

            boolean actorIsFrom = actor.getUUID().equals(rope.fromEntity);
            boolean actorIsTo = actor.getUUID().equals(rope.toEntity);
            if (!actorIsFrom && !actorIsTo) continue; 

            Vec3 actorPos = actorIsFrom ? fromPos : toPos;
            Vec3 otherPos = actorIsFrom ? toPos : fromPos;
            Entity otherEntity = actorIsFrom ? rope.to.entity(level) : rope.from.entity(level);

            if (cfg.which != Which.OTHER) {
                push(actor, otherPos.subtract(actorPos), cfg.speed, cfg.set);
            }
            if (cfg.which != Which.SELF && otherEntity instanceof LivingEntity) {
                push(otherEntity, actorPos.subtract(otherPos), cfg.speed, cfg.set);
            }
            if (cfg.reel != 0) RopeManager.reel(rope.id, cfg.reel);
        }
    }

    private static void push(Entity entity, Vec3 toward, double speed, boolean set) {
        if (toward.lengthSqr() < 1e-8) return;
        VelocityUpdater.apply(entity, toward.normalize().scale(speed), set);
    }
}
