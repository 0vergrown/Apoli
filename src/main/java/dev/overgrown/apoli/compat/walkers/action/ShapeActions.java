package dev.overgrown.apoli.compat.walkers.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.walkers.WalkersBridge;
import dev.overgrown.apoli.compat.walkers.power.ShapePowers;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class ShapeActions {
    private ShapeActions() {}

    public enum Operation {
        ADD, SET;

        public static final Codec<Operation> CODEC =
            Codec.STRING.xmap(s -> valueOf(s.toUpperCase(java.util.Locale.ROOT)), o -> o.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static final class ChangeShapeAbilityCooldown implements ActionType<EntityCtx, ChangeShapeAbilityCooldown.Cfg> {
        public record Cfg(Operation operation, int change) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                Operation.CODEC.optionalFieldOf("operation", Operation.ADD).forGetter(Cfg::operation),
                Codec.INT.fieldOf("change").forGetter(Cfg::change)
            ).apply(i, Cfg::new));
        }

        @Override
        public void run(Cfg cfg, EntityCtx ctx) {
            Entity entity = ctx.raw();
            if (entity == null) return;
            int current = WalkersBridge.abilityCooldown(entity);
            int next = cfg.operation == Operation.SET ? cfg.change : current + cfg.change;
            if (next == current) return;
            WalkersBridge.setAbilityCooldown(entity, Math.max(0, next));
        }
    }

    public static final class ShapeAction implements ActionType<EntityCtx, ShapeAction.Cfg> {
        public record Cfg(BiEntityAction bientityAction) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::bientityAction)
            ).apply(i, Cfg::new));
        }

        @Override
        public void run(Cfg cfg, EntityCtx ctx) {
            Entity entity = ctx.raw();
            if (entity == null || ctx.level().isClientSide()) return;
            cfg.bientityAction.run(new BiEntityCtx(entity, WalkersBridge.shapeOrSelf(entity), ctx.level()));
        }
    }

    public record SwitchCfg(Optional<ResourceLocation> shape, Optional<Nbt> nbt, Optional<EntityAction> actionOnSuccess) {
        public static final MapCodec<SwitchCfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("shape").forGetter(SwitchCfg::shape),
            Nbt.CODEC.optionalFieldOf("nbt").forGetter(SwitchCfg::nbt),
            LoggedOptionalField.of("action_on_success", EntityAction.CODEC).forGetter(SwitchCfg::actionOnSuccess)
        ).apply(i, SwitchCfg::new));
    }

    public static final class SwitchShape implements ActionType<EntityCtx, SwitchCfg> {
        @Override
        public MapCodec<SwitchCfg> codec() {
            return SwitchCfg.CODEC;
        }

        @Override
        public void run(SwitchCfg cfg, EntityCtx ctx) {
            Entity entity = ctx.raw();
            if (entity == null) return;
            CompoundTag tag = cfg.nbt().map(Nbt::tag).orElse(null);
            if (WalkersBridge.switchShape(entity, cfg.shape().orElse(null), tag)) {
                cfg.actionOnSuccess().ifPresent(a -> a.run(ctx));
            }
        }
    }

    public static final class SwitchShapeBiEntity implements ActionType<BiEntityCtx, SwitchCfg> {
        @Override
        public MapCodec<SwitchCfg> codec() {
            return SwitchCfg.CODEC;
        }

        @Override
        public void run(SwitchCfg cfg, BiEntityCtx ctx) {
            Entity actor = ctx.rawActor();
            Entity target = ctx.rawTarget();
            if (actor == null) return;

            ResourceLocation shapeId = cfg.shape().orElse(null);
            CompoundTag tag = cfg.nbt().map(Nbt::tag).orElse(null);
            if (shapeId == null && target instanceof LivingEntity living) {
                shapeId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
                if (tag == null) {
                    CompoundTag copied = new CompoundTag();
                    living.saveWithoutId(copied);
                    tag = copied;
                }
            }
            if (WalkersBridge.switchShape(actor, shapeId, tag)) {
                cfg.actionOnSuccess().ifPresent(a -> a.run(ctx.asActor()));
            }
        }
    }

    public static final class UseShapeAbility implements ActionType<EntityCtx, UseShapeAbility.Cfg> {
        public record Cfg(boolean force, boolean applyCooldown) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("force", false).forGetter(Cfg::force),
                Codec.BOOL.optionalFieldOf("apply_cooldown", true).forGetter(Cfg::applyCooldown)
            ).apply(i, Cfg::new));
        }

        @Override
        public void run(Cfg cfg, EntityCtx ctx) {
            Entity entity = ctx.raw();
            if (entity == null || ctx.level().isClientSide()) return;
            ShapePowers.useAbility(entity, cfg.force, cfg.applyCooldown);
        }
    }
}
