package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.skill.SkillTrees;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ResetSkillsAction implements ActionType<EntityCtx, ResetSkillsAction.Cfg> {

    public record Cfg(Optional<ResourceLocation> tree, boolean refund) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("tree").forGetter(Cfg::tree),
            Codec.BOOL.optionalFieldOf("refund", true).forGetter(Cfg::refund)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        SkillTrees.resetSkills(player, cfg.tree().orElse(null), cfg.refund());
        ApoliNetwork.sendSkillState(player);
    }
}
