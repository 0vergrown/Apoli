package dev.overgrown.apoli.skill;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

public final class SkillDataAttachment {
    private SkillDataAttachment() {}

    public static final AttachmentType<SkillData> TYPE = AttachmentRegistry.<SkillData>builder()
        .persistent(SkillData.CODEC)
        .copyOnDeath()
        .buildAndRegister(Apoli.id("skill_data"));

    public static SkillData get(Player player) {
        return player.getAttachedOrCreate(TYPE, SkillData::new);
    }
}
