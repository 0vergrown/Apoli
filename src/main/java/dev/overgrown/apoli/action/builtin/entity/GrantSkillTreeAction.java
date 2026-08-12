package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.skill.SkillRegistry;
import dev.overgrown.apoli.skill.SkillTrees;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class GrantSkillTreeAction implements ActionType<EntityCtx, GrantSkillTreeAction.Cfg> {
    public record Cfg(ResourceLocation skillTree) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("skill_tree").forGetter(Cfg::skillTree)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        if (SkillRegistry.tree(cfg.skillTree()) != null) {
            SkillTrees.grantTree(player, cfg.skillTree());
            return;
        }
        if (ModCompat.PUFFISH_SKILLS) {
            dev.overgrown.apoli.compat.skills.SkillsCompat.unlockCategory(player, cfg.skillTree());
        }
    }
}
