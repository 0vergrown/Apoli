package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ModifyJumpHandler {
    private static final int MIN_JUMP_GAP = 4;
    private static final Map<ServerPlayer, Long> LAST_JUMP = new WeakHashMap<>();

    private ModifyJumpHandler() {}

    private static boolean hasNone(LivingEntity entity) {
        PowerContainer container = PowerContainer.of(entity);
        return container == null || container.isEmpty()
            || container.powersOfType(ApoliIds.MODIFY_JUMP).isEmpty();
    }

    public static float modify(LivingEntity entity, float original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        boolean own = !container.powersOfType(ApoliIds.MODIFY_JUMP).isEmpty();
        boolean transferred = AttributeModifyTransferPower.JUMP.appliesTo(container);
        if (!own && !transferred) return original;
        List<AttributeModifier> mods = List.of();
        if (own) {
            List<AttributeModifier> collected = new ArrayList<>();
            PowerLookup.forEach(entity, ApoliIds.MODIFY_JUMP, ModifyJumpPower.Config.class, cfg ->
                collected.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
            mods = collected;
        }
        if (transferred) mods = AttributeModifyTransferPower.JUMP.merge(entity, container, mods);
        if (mods.isEmpty()) return original;
        return AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity);
    }

    public static void onJump(LivingEntity entity) {
        if (entity.level().isClientSide() || entity instanceof Player) return;
        if (hasNone(entity)) return;
        runActions(entity);
    }

    public static boolean hasJumpActions(LivingEntity entity) {
        if (hasNone(entity)) return false;
        boolean[] found = new boolean[1];
        PowerLookup.forEach(entity, ApoliIds.MODIFY_JUMP, ModifyJumpPower.Config.class, cfg -> {
            if (cfg.entityAction().isPresent()) found[0] = true;
        });
        return found[0];
    }

    public static void onPlayerJumped(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.onGround()) return;
        long now = player.level().getGameTime();
        Long last = LAST_JUMP.get(player);
        if (last != null && now - last < MIN_JUMP_GAP) return;
        LAST_JUMP.put(player, now);
        if (hasNone(player)) return;
        runActions(player);
    }

    private static void runActions(LivingEntity entity) {
        EntityCtx ctx = EntityCtx.of(entity, entity.level());
        PowerLookup.forEach(entity, ApoliIds.MODIFY_JUMP, ModifyJumpPower.Config.class, cfg ->
            cfg.entityAction().ifPresent(action -> action.run(ctx)));
    }
}
