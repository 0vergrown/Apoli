package dev.overgrown.apoli.compat.walkers.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.walkers.WalkersBridge;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ShapePowers {

    public static final ResourceLocation ACTION_ON_SHAPE_CHANGE = Apoli.id("action_on_shape_change");
    public static final ResourceLocation ACTION_ON_SHAPE_ABILITY_USE = Apoli.id("action_on_shape_ability_use");
    public static final ResourceLocation PREVENT_SHAPE_CHANGE = Apoli.id("prevent_shape_change");
    public static final ResourceLocation PREVENT_SHAPE_ABILITY_USE = Apoli.id("prevent_shape_ability_use");

    private ShapePowers() {}

    public record ActionCfg(BiEntityAction bientityAction, Optional<BiEntityCondition> bientityCondition) {}

    public record PreventCfg(Optional<BiEntityCondition> bientityCondition) {}

    public static final class ActionOnShapeChange extends PowerType<ActionCfg> {
        @Override
        public MapCodec<ActionCfg> configCodec() {
            return ACTION_CODEC;
        }
    }

    public static final class ActionOnShapeAbilityUse extends PowerType<ActionCfg> {
        @Override
        public MapCodec<ActionCfg> configCodec() {
            return ACTION_CODEC;
        }
    }

    public static final class PreventShapeChange extends PowerType<PreventCfg> {
        @Override
        public MapCodec<PreventCfg> configCodec() {
            return PREVENT_CODEC;
        }
    }

    public static final class PreventShapeAbilityUse extends PowerType<PreventCfg> {
        @Override
        public MapCodec<PreventCfg> configCodec() {
            return PREVENT_CODEC;
        }
    }

    private static final MapCodec<ActionCfg> ACTION_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(ActionCfg::bientityAction),
        LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(ActionCfg::bientityCondition)
    ).apply(i, ActionCfg::new));

    private static final MapCodec<PreventCfg> PREVENT_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(PreventCfg::bientityCondition)
    ).apply(i, PreventCfg::new));

    public static boolean preventShapeChange(Entity player, @Nullable Entity shape) {
        return prevented(player, shape == null ? player : shape, PREVENT_SHAPE_CHANGE);
    }

    public static boolean preventShapeAbilityUse(Entity player) {
        return prevented(player, WalkersBridge.shapeOrSelf(player), PREVENT_SHAPE_ABILITY_USE);
    }

    private static boolean prevented(Entity player, Entity shape, ResourceLocation typeId) {
        if (player == null) return false;
        BiEntityCtx ctx = new BiEntityCtx(player, shape, player.level());
        boolean[] hit = {false};
        PowerLookup.forEach(player, typeId, PreventCfg.class, cfg -> {
            if (hit[0]) return;
            if (cfg.bientityCondition().isEmpty() || cfg.bientityCondition().get().test(ctx)) hit[0] = true;
        });
        return hit[0];
    }

    public static void fireShapeChange(Entity player, @Nullable Entity shape) {
        fire(player, shape == null ? player : shape, ACTION_ON_SHAPE_CHANGE);
    }

    public static void fireShapeAbilityUse(Entity player) {
        fire(player, WalkersBridge.shapeOrSelf(player), ACTION_ON_SHAPE_ABILITY_USE);
    }

    private static void fire(Entity player, Entity shape, ResourceLocation typeId) {
        if (player == null || player.level().isClientSide()) return;
        BiEntityCtx ctx = new BiEntityCtx(player, shape, player.level());
        PowerLookup.forEach(player, typeId, ActionCfg.class, cfg -> {
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) return;
            cfg.bientityAction().run(ctx);
        });
    }

    public static void useAbility(Entity entity, boolean force, boolean applyCooldown) {
        if (!WalkersBridge.present() || !WalkersBridge.hasShapeAbility(entity)) return;
        int before = WalkersBridge.abilityCooldown(entity);
        if (force && before > 0) WalkersBridge.setAbilityCooldown(entity, 0);
        WalkersAbilityInvoker.invoke(entity);
        if (!applyCooldown) WalkersBridge.setAbilityCooldown(entity, force ? 0 : before);
    }
}
