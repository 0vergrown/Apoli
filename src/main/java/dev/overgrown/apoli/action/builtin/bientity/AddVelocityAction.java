package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.network.VelocityUpdater;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class AddVelocityAction implements ActionType<BiEntityCtx, AddVelocityAction.Cfg> {
    public enum Reference implements StringRepresentable {
        POSITION("position"),
        ROTATION("rotation");

        public static final Codec<Reference> CODEC = StringRepresentable.fromEnum(Reference::values);
        private final String name;

        Reference(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(Expression x, Expression y, Expression z, Reference reference,
                      Optional<Space> space, boolean set, Expression blend, boolean keepSpeed) {}

    private static final Expression ZERO = Expression.constant(0.0);
    private static final Expression ONE = Expression.constant(1.0);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("x", ZERO).forGetter(Cfg::x),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("y", ZERO).forGetter(Cfg::y),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("z", ZERO).forGetter(Cfg::z),
            Reference.CODEC.optionalFieldOf("reference", Reference.POSITION).forGetter(Cfg::reference),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("space", Space.CODEC).forGetter(Cfg::space),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(Cfg::set),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("blend", ONE).forGetter(Cfg::blend),
            Codec.BOOL.optionalFieldOf("keep_speed", false).forGetter(Cfg::keepSpeed)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null) return;
        Vec3 amount = new Vec3(cfg.x.eval(actor), cfg.y.eval(actor), cfg.z.eval(actor));
        Vec3 delta;
        if (cfg.space.isPresent()) {
            delta = cfg.space.get().toGlobal(actor, amount);
        } else {
            Vec3 forward = switch (cfg.reference) {
                case POSITION -> target.position().subtract(actor.position());
                case ROTATION -> actor.getLookAngle();
            };
            delta = Space.transformVectorToBase(forward, amount, actor.getYRot(), true);
            if (delta.lengthSqr() == 0.0 && amount.lengthSqr() != 0.0) return;
        }

        if (!cfg.set) {
            VelocityUpdater.apply(target, delta, false);
            return;
        }

        Vec3 current = target.getDeltaMovement();
        if (cfg.keepSpeed) {
            double speed = current.length();
            double length = delta.length();
            if (length > 1.0E-7) delta = delta.scale(speed / length);
        }
        double blend = Mth.clamp(cfg.blend.eval(actor), 0.0, 1.0);
        if (blend < 1.0) {
            delta = current.add(delta.subtract(current).scale(blend));
        }
        VelocityUpdater.apply(target, delta, true);
    }
}
