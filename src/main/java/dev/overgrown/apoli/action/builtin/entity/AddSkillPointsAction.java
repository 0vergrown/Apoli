package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.skill.SkillData;
import dev.overgrown.apoli.skill.SkillDataAttachment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AddSkillPointsAction implements ActionType<EntityCtx, AddSkillPointsAction.Cfg> {
    public record Cfg(ResourceLocation skillTree, int points) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("skill_tree").forGetter(Cfg::skillTree),
            Codec.INT.optionalFieldOf("points", 1).forGetter(Cfg::points)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        SkillData data = SkillDataAttachment.get(player);
        data.addPoints(cfg.skillTree, cfg.points);
        ApoliNetwork.sendSkillState(player);
    }
}
