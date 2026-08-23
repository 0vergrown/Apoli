package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.network.VelocityUpdater;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AddVelocityAction implements ActionType<BiEntityCtx, AddVelocityAction.Cfg> {
    public enum Reference implements StringRepresentable {
        POSITION("position"), ROTATION("rotation");

        public static final Codec<Reference> CODEC = StringRepresentable.fromEnum(Reference::values);
        private final String name;

        Reference(String name) {
            this.name = name;
        }

        @Override public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(Expression x, Expression y, Expression z, Reference reference, boolean set) {}

    private static final Expression ZERO = Expression.constant(0.0);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("x", ZERO).forGetter(Cfg::x),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("y", ZERO).forGetter(Cfg::y),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("z", ZERO).forGetter(Cfg::z),
            Reference.CODEC.optionalFieldOf("reference", Reference.POSITION).forGetter(Cfg::reference),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(Cfg::set)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null) return;
        Vec3 forward = switch (cfg.reference) {
            case POSITION -> target.position().subtract(actor.position());
            case ROTATION -> actor.getLookAngle();
        };
        Vec3 amount = new Vec3(cfg.x.eval(actor), cfg.y.eval(actor), cfg.z.eval(actor));
        Vec3 delta = Space.transformVectorToBase(forward, amount, actor.getYRot(), true);
        VelocityUpdater.apply(target, delta, cfg.set);
    }
}
