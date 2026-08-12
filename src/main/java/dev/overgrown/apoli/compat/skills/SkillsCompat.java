package dev.overgrown.apoli.compat.skills;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.List;
import java.util.UUID;

public final class SkillsCompat {

    private static final String SOURCE_PREFIX = "skills_reward/";

    private SkillsCompat() {}

    public static void init() {
        PowerReward.register();
        ModifyResourceReward.register();
    }

    static ResourceLocation newSource() {
        return Apoli.id(SOURCE_PREFIX + UUID.randomUUID());
    }

    static boolean isRewardSource(ResourceLocation source) {
        return Apoli.MOD_ID.equals(source.getNamespace()) && source.getPath().startsWith(SOURCE_PREFIX);
    }

    public static boolean hasCategory(ResourceLocation category) {
        return SkillsAPI.getCategory(category).isPresent();
    }

    public static void unlockCategory(ServerPlayer player, ResourceLocation category) {
        player.server.execute(() -> SkillsAPI.getCategory(category).ifPresent(c -> c.unlock(player)));
    }

    public static void lockCategory(ServerPlayer player, ResourceLocation category) {
        player.server.execute(() -> SkillsAPI.getCategory(category).ifPresent(c -> c.lock(player)));
    }

    public static void onJoin(ServerPlayer player) {
        PowerContainer container = PowerContainer.of(player);
        if (container != null) {
            for (ResourceLocation power : List.copyOf(container.allPowers())) {
                for (ResourceLocation source : container.sourcesOf(power)) {
                    if (isRewardSource(source)) container.removePower(power, source);
                }
            }
        }
        SkillsAPI.updateRewards(player, PowerReward.ID);
        SkillsAPI.updateRewards(player, ModifyResourceReward.ID);
    }
}
