package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GameEventListenerPower extends PowerType<GameEventListenerPower.Config> {

    public enum TriggerOrder implements StringRepresentable {
        BY_DISTANCE("by_distance"),
        UNSPECIFIED("unspecified");

        public static final Codec<TriggerOrder> CODEC = StringRepresentable.fromEnum(TriggerOrder::values);
        private final String name;
        TriggerOrder(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Config(
        TriggerOrder triggerOrder,
        boolean entity,
        boolean block,
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<BlockAction> blockAction,
        Optional<BlockCondition> blockCondition,
        int cooldown,
        HudRender hudRender,
        Optional<ResourceLocation> event,
        Optional<List<ResourceLocation>> events,
        Optional<ResourceLocation> eventTag,
        boolean showParticle,
        int range
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TriggerOrder.CODEC.optionalFieldOf("trigger_order", TriggerOrder.UNSPECIFIED).forGetter(Config::triggerOrder),
            Codec.BOOL.optionalFieldOf("entity", true).forGetter(Config::entity),
            Codec.BOOL.optionalFieldOf("block", true).forGetter(Config::block),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Config::blockAction),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            Codec.INT.optionalFieldOf("cooldown", 1).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender),
            ResourceLocation.CODEC.optionalFieldOf("event").forGetter(Config::event),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("events").forGetter(Config::events),
            ResourceLocation.CODEC.optionalFieldOf("event_tag").forGetter(Config::eventTag),
            Codec.BOOL.optionalFieldOf("show_particle", true).forGetter(Config::showParticle),
            Codec.INT.optionalFieldOf("range", 16).forGetter(Config::range)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder.rawOwner() instanceof LivingEntity entity)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        State state = State.getOrCreate(entity, powerId, cfg);
        state.add(serverLevel);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder.rawOwner() instanceof LivingEntity entity)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        State state = State.get(entity, powerId);
        if (state != null) {
            state.remove(serverLevel);
            State.remove(entity, powerId);
        }
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (!(holder.rawOwner() instanceof LivingEntity entity)) return;
        Level level = entity.level();
        State state = State.get(entity, powerId);
        if (state == null) {
            if (!(level instanceof ServerLevel serverLevel)) return;
            state = State.getOrCreate(entity, powerId, cfg);
            state.add(serverLevel);
        }
        state.tick(level);
    }

    public static final class State {
        private final DynamicGameEventListener<GameEventListener> eventHandler;
        private final ResourceLocation powerId;
        private final Config cfg;
        private final Entity entity;
        @Nullable
        private final TagKey<GameEvent> resolvedTag;

        State(Config cfg, Entity entity, ResourceLocation powerId) {
            this.cfg = cfg;
            this.entity = entity;
            this.powerId = powerId;
            this.resolvedTag = cfg.eventTag().isPresent()
                ? TagKey.create(Registries.GAME_EVENT, cfg.eventTag().get())
                : null;
            this.eventHandler = new DynamicGameEventListener<>(new GameEventListener() {
                @Override
                public PositionSource getListenerSource() {
                    return new EntityPositionSource(entity, entity.getEyeHeight());
                }

                @Override
                public int getListenerRadius() {
                    return Math.max(cfg.range(), 1);
                }

                @Override
                public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> holder, GameEvent.Context context, Vec3 pos) {
                    return handle(level, pos, holder, context);
                }
            });
        }

        private boolean handle(ServerLevel level, Vec3 pos, Holder<GameEvent> holder, GameEvent.Context context) {
            if (!onCooldown(level)) return false;
            if (!cfg.entity() && context.sourceEntity() != null) return false;
            if (!cfg.block() && context.sourceEntity() == null) return false;

            if (cfg.event().isPresent()) {
                if (holder.unwrapKey().map(k -> !k.location().equals(cfg.event().get())).orElse(true)) return false;
            }
            if (cfg.events().isPresent() && !cfg.events().get().isEmpty()) {
                if (holder.unwrapKey().map(k -> !cfg.events().get().contains(k.location())).orElse(true)) return false;
            }
            if (resolvedTag != null) {
                if (!holder.is(resolvedTag)) return false;
            }

            Entity src = context.sourceEntity();
            if (src != null) {
                if (src.isSpectator()) return false;
                if (src.isSteppingCarefully() && holder.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) return false;
                if (src.dampensVibrations()) return false;
            }
            if (context.affectedState() != null && context.affectedState().is(BlockTags.DAMPENS_VIBRATIONS)) {
                return false;
            }

            if (cfg.bientityCondition().isPresent() && context.sourceEntity() instanceof LivingEntity actor) {
                if (!cfg.bientityCondition().get().test(new BiEntityCtx(actor, (LivingEntity) entity, level))) return false;
            }

            if (cfg.blockCondition().isPresent() && context.sourceEntity() == null) {
                BlockPos blockPos = BlockPos.containing(pos);
                if (!cfg.blockCondition().get().test(new BlockCtx(blockPos, level.getBlockState(blockPos), level))) return false;
            }

            setCooldown(level);
            Entity actionSource = context.sourceEntity();
            if (actionSource instanceof LivingEntity actor && entity instanceof LivingEntity target) {
                cfg.bientityAction().ifPresent(a -> a.run(new BiEntityCtx(actor, target, level)));
            }
            cfg.blockAction().ifPresent(a -> a.run(new BlockCtx(BlockPos.containing(pos), level.getBlockState(BlockPos.containing(pos)), level)));
            return true;
        }

        private boolean registered;

        void add(ServerLevel level) {

            if (registered || !chunkReady(level)) return;
            eventHandler.add(level);
            registered = true;
        }

        void remove(ServerLevel level) {
            if (registered) {
                eventHandler.remove(level);
                registered = false;
            }
        }

        void tick(Level level) {
            if (level instanceof ServerLevel serverLevel) {
                if (!registered) {
                    add(serverLevel);
                } else {
                    eventHandler.move(serverLevel);
                }
            }
        }

        private boolean chunkReady(ServerLevel level) {
            return level.getChunk(
                net.minecraft.core.SectionPos.blockToSectionCoord(entity.getBlockX()),
                net.minecraft.core.SectionPos.blockToSectionCoord(entity.getBlockZ()),
                net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) != null;
        }

        public DynamicGameEventListener<GameEventListener> getEventHandler() { return eventHandler; }

        private boolean onCooldown(ServerLevel level) {
            PowerContainer container = PowerContainer.of(entity);
            if (container == null) return true;
            return container.getAuxInt(powerId).orElse(0) <= level.getGameTime();
        }

        private void setCooldown(ServerLevel level) {
            PowerContainer container = PowerContainer.of(entity);
            if (container instanceof PowerContainerImpl impl) {
                impl.setAuxInt(powerId, (int) (level.getGameTime() + cfg.cooldown()));
            }
        }

        public static State getOrCreate(Entity entity, ResourceLocation powerId, Config cfg) {
            Map<ResourceLocation, State> map = EntityGameEventListenerAccess.getMap(entity);
            if (map == null) {
                map = new HashMap<>();
                EntityGameEventListenerAccess.setMap(entity, map);
            }
            return map.computeIfAbsent(powerId, id -> new State(cfg, entity, id));
        }

        @Nullable
        public static State get(Entity entity, ResourceLocation powerId) {
            Map<ResourceLocation, State> map = EntityGameEventListenerAccess.getMap(entity);
            return map != null ? map.get(powerId) : null;
        }

        @Nullable
        public static State remove(Entity entity, ResourceLocation powerId) {
            Map<ResourceLocation, State> map = EntityGameEventListenerAccess.getMap(entity);
            if (map == null) return null;
            State removed = map.remove(powerId);
            if (map.isEmpty()) EntityGameEventListenerAccess.setMap(entity, null);
            return removed;
        }
    }
}
