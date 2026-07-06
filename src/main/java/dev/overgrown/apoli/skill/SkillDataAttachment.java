package dev.overgrown.apoli.skill;

import dev.overgrown.apoli.Apoli;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class SkillDataAttachment {
    private SkillDataAttachment() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Apoli.MOD_ID);

    public static final Supplier<AttachmentType<SkillData>> SKILL_DATA =
        ATTACHMENT_TYPES.register("skill_data", () -> AttachmentType.builder(SkillData::new)
            .serialize(SkillData.CODEC)
            .copyOnDeath()
            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    public static SkillData get(Player player) {
        return player.getData(SKILL_DATA.get());
    }
}
