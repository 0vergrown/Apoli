package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.rope.RopeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AttachRopeAction implements ActionType<EntityCtx, AttachRopeAction.Cfg> {
    public record Cfg(float maxLength, ResourceLocation texture) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("max_length", 30f).forGetter(Cfg::maxLength),
            ResourceLocation.CODEC.optionalFieldOf("texture", Apoli.id("textures/rope/rope.png")).forGetter(Cfg::texture)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;

        if (RopeManager.get(player.getUUID()) != null) {
            RopeManager.detach(player);
            return;
        }

        Vec3 from = player.getEyePosition(1.0f);
        Vec3 to = from.add(player.getViewVector(1.0f).scale(cfg.maxLength));
        BlockHitResult hit = player.level().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) return;
        RopeManager.attach(player, hit.getLocation(), cfg.maxLength, cfg.texture);
    }
}
