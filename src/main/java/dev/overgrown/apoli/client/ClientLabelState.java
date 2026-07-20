package dev.overgrown.apoli.client;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.ModifyLabelRenderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class ClientLabelState {
    private ClientLabelState() {}

    private static final Map<Integer, Map<ResourceLocation, Component>> LABELS = new ConcurrentHashMap<>();

    public record Pick(@Nullable Component text, ModifyLabelRenderPower.LabelMode mode) {}

    public static void apply(int entityId, Map<ResourceLocation, Component> texts) {
        if (texts.isEmpty()) {
            LABELS.remove(entityId);
        } else {
            LABELS.put(entityId, texts);
        }
    }

    public static void removeEntity(int entityId) {
        LABELS.remove(entityId);
    }

    public static void clear() {
        LABELS.clear();
    }

    @Nullable
    public static Pick pick(Entity entity, @Nullable Entity viewer) {
        Map<ResourceLocation, Component> texts = LABELS.get(entity.getId());
        if (texts == null) return null;

        Component bestText = null;
        ModifyLabelRenderPower.LabelMode bestMode = null;
        int bestPriority = Integer.MIN_VALUE;
        for (Map.Entry<ResourceLocation, Component> entry : texts.entrySet()) {
            Power power = ApoliPowers.get(entry.getKey());
            if (power == null || !(power.config() instanceof ModifyLabelRenderPower.Config cfg)) continue;
            if (bestMode != null && cfg.priority() <= bestPriority) continue;
            if (!viewerConditionsPass(cfg, entity, viewer)) continue;
            bestPriority = cfg.priority();
            bestMode = cfg.renderMode();
            Component text = entry.getValue();
            bestText = text.getString().isEmpty() && text.getSiblings().isEmpty() ? null : text;
        }
        return bestMode == null ? null : new Pick(bestText, bestMode);
    }

    private static boolean viewerConditionsPass(ModifyLabelRenderPower.Config cfg, Entity entity, @Nullable Entity viewer) {
        if (cfg.entityCondition().isEmpty() && cfg.bientityCondition().isEmpty()) return true;
        if (viewer == null) return false;
        if (cfg.entityCondition().isPresent()
            && !cfg.entityCondition().get().test(EntityCtx.of(viewer, viewer.level()))) {
            return false;
        }
        if (cfg.bientityCondition().isPresent()) {
            if (!(viewer instanceof LivingEntity livingViewer) || !(entity instanceof LivingEntity livingTarget)) {
                return false;
            }
            return cfg.bientityCondition().get().test(new BiEntityCtx(livingViewer, livingTarget, entity.level()));
        }
        return true;
    }
}
