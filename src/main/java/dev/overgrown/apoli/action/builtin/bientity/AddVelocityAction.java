package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
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

    public record Cfg(double x, double y, double z, Reference reference, boolean set) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Cfg::x),
            Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(Cfg::y),
            Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(Cfg::z),
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
        Vec3 delta = Space.transformVectorToBase(forward, new Vec3(cfg.x, cfg.y, cfg.z), actor.getYRot(), true);
        VelocityUpdater.apply(target, delta, cfg.set);
    }
}
