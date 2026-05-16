package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AddVelocityAction implements ActionType<BiEntityCtx, AddVelocityAction.Cfg> {
    public enum Reference implements StringRepresentable {
        WORLD("world"), ACTOR("actor"), TARGET("target");

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
            Reference.CODEC.optionalFieldOf("reference", Reference.WORLD).forGetter(Cfg::reference),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(Cfg::set)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity recipient = ctx.target();
        Vec3 delta = new Vec3(cfg.x, cfg.y, cfg.z);
        if (cfg.reference == Reference.ACTOR) delta = applyOrientation(delta, ctx.actor());
        else if (cfg.reference == Reference.TARGET) delta = applyOrientation(delta, ctx.target());
        Vec3 current = recipient.getDeltaMovement();
        recipient.setDeltaMovement(cfg.set ? delta : current.add(delta));
        recipient.hurtMarked = true;
        if (recipient instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(recipient));
        }
    }

    private static Vec3 applyOrientation(Vec3 v, LivingEntity entity) {
        float yaw = entity.getYRot() * ((float) Math.PI / 180F);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }
}