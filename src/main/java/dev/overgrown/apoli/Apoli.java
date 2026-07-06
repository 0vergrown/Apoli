package dev.overgrown.apoli;

import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.action.DelayedActionQueue;
import dev.overgrown.apoli.alias.ApoliAliases;
import dev.overgrown.apoli.command.ApoliPowerCommand;
import dev.overgrown.apoli.command.ApoliResourceCommand;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.loader.ApoliKeybindLoader;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.network.payload.KeyHeldC2S;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.PowerToggleC2S;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PoweredEntities;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.PowerTypes;
import dev.overgrown.apoli.power.builtin.ActionOnCallbackPower;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.ActionOnUseHandler;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class Apoli implements ModInitializer {
    public static final String MOD_ID = "apoli";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private ApoliReloadListener powerLoader;
    private ApoliKeybindLoader keybindLoader;
    private dev.overgrown.apoli.skill.SkillTreeLoader skillLoader;

    @Override
    public void onInitialize() {
        LOGGER.info("[Apoli] Initializing...");

        ApoliAliases.bootstrap();
        ConditionTypes.bootstrap();
        ActionTypes.bootstrap();
        PowerTypes.bootstrap();
        dev.overgrown.apoli.compat.accessory.AccessoryCompat.init();
        if (dev.overgrown.apoli.compat.ModCompat.HARDCORE_REVIVAL) {
            dev.overgrown.apoli.compat.hardcorerevival.HardcoreRevivalCompat.init();
        }
        dev.overgrown.apoli.entity.ApoliEntities.register();
        dev.overgrown.apoli.item.ApoliLootFunctions.register();

        Class<?> ignored = PowerContainerAttachment.class;
        Class<?> ignoredSkill = dev.overgrown.apoli.skill.SkillDataAttachment.class;

        powerLoader = new ApoliReloadListener();
        keybindLoader = new ApoliKeybindLoader();
        skillLoader = new dev.overgrown.apoli.skill.SkillTreeLoader();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiedReloader(id("powers_reloader"), powerLoader));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiedReloader(id("keybinds_reloader"), keybindLoader));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiedReloader(id("skill_trees_reloader"), skillLoader));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            powerLoader.attachServer(server);
            keybindLoader.attachServer(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ApoliNetwork.broadcastPowers(server, SyncPowersS2C.fromCurrent());
            ApoliNetwork.broadcastKeybinds(server, SyncKeybindsS2C.fromCurrent());
            dev.overgrown.apoli.recipe.ApoliPowerRecipes.inject(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            dev.overgrown.apoli.recipe.ApoliPowerRecipes.inject(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                dev.overgrown.apoli.skill.SkillTrees.grantOnJoin(player);
                ApoliNetwork.sendSkillDefs(player);
                ApoliNetwork.sendSkillState(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PoweredEntities.clear();
            DelayedActionQueue.clear();
            dev.overgrown.apoli.rope.RopeManager.clear();
            dev.overgrown.apoli.compat.icarus.WingsAccess.clear();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            ApoliPowerCommand.register(dispatcher);
            ApoliResourceCommand.register(dispatcher);
            if (dev.overgrown.apoli.compat.ModCompat.anyAccessory()) {
                dev.overgrown.apoli.compat.accessory.command.AccessoryCommand.register(dispatcher);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(Apoli::onServerTick);

        EntityElytraEvents.CUSTOM.register((entity, tickElytra) ->
            PowerLookup.hasActive(entity, id("elytra_flight")));

        ServerPlayNetworking.registerGlobalReceiver(PowerActivationC2S.CHANNEL, (server, player, handler, buf, sender) -> {
            PowerActivationC2S payload = PowerActivationC2S.read(buf);
            server.execute(() -> handleActivation(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(PowerToggleC2S.CHANNEL, (server, player, handler, buf, sender) -> {
            PowerToggleC2S payload = PowerToggleC2S.read(buf);
            server.execute(() -> handleToggle(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(KeyHeldC2S.CHANNEL, (server, player, handler, buf, sender) -> {
            KeyHeldC2S payload = KeyHeldC2S.read(buf);
            server.execute(() -> HeldKeys.setServerHeld(player.getUUID(), payload.keys()));
        });

        ServerPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.BuySkillC2S.CHANNEL,
            (server, player, handler, buf, sender) -> {
                dev.overgrown.apoli.network.payload.BuySkillC2S payload = dev.overgrown.apoli.network.payload.BuySkillC2S.read(buf);
                server.execute(() -> {
                    if (dev.overgrown.apoli.skill.SkillTrees.tryPurchase(player, payload.skill())) {
                        ApoliNetwork.sendSkillState(player);
                    }
                });
            });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            HeldKeys.clearServer(handler.player.getUUID());
            dev.overgrown.apoli.skill.SkillTrees.forget(handler.player.getUUID());
            dev.overgrown.apoli.network.ProtocolCompat.forget(handler.player.getUUID());
        });

        ServerPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.ProtocolVersionPayload.CHANNEL,
            (server, player, handler, buf, sender) -> {
                dev.overgrown.apoli.network.payload.ProtocolVersionPayload payload =
                    dev.overgrown.apoli.network.payload.ProtocolVersionPayload.read(buf);
                server.execute(() -> {
                    int serverVersion = dev.overgrown.apoli.network.ProtocolCompat.VERSION;
                    if (payload.version() != serverVersion) {
                        handler.disconnect(net.minecraft.network.chat.Component.literal(
                            "Apoli network protocol mismatch (server " + serverVersion + ", client " + payload.version()
                                + "). Update Apoli to the same build on both sides."));
                        return;
                    }
                    ApoliNetwork.sendProtocolVersion(player);
                    if (dev.overgrown.apoli.network.ProtocolCompat.consumeSentLegacy(player)) {
                        ApoliNetwork.sendSkillDefs(player);
                        ApoliNetwork.sendSkillState(player);
                    }
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.RopeChangeLengthC2S.CHANNEL,
            (server, player, handler, buf, sender) -> {
                dev.overgrown.apoli.network.payload.RopeChangeLengthC2S p = dev.overgrown.apoli.network.payload.RopeChangeLengthC2S.read(buf);
                server.execute(() -> dev.overgrown.apoli.rope.RopeManager.handleLengthChange(player, p.ropeId(), p.delta()));
            });

        ServerPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.RopeSwingC2S.CHANNEL,
            (server, player, handler, buf, sender) -> {
                dev.overgrown.apoli.network.payload.RopeSwingC2S p = dev.overgrown.apoli.network.payload.RopeSwingC2S.read(buf);
                server.execute(() -> dev.overgrown.apoli.rope.RopeManager.applySwingInput(player, p.ropeId(), p.inputDir()));
            });

        UseEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(target instanceof LivingEntity livingTarget)) return InteractionResult.PASS;
            return ActionOnUseHandler.fireOncePerTick(player, livingTarget, hand);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            PowerContainer c = PowerContainer.of(entity);
            if (c != null && !c.allPowers().isEmpty()) {
                for (ResourceLocation powerId : c.allPowers()) {
                    dev.overgrown.apoli.power.builtin.ResourcePower.onEntityLoad(c, powerId);
                }
                ActionOnCallbackPower.fireLifecycle(entity, ActionOnCallbackPower.Config::entityActionAdded);
            }
            if (entity instanceof ServerPlayer sp) {
                level.getServer().execute(() -> {
                    ApoliNetwork.sendKeybinds(sp, SyncKeybindsS2C.fromCurrent());
                    ApoliNetwork.sendPowers(sp, SyncPowersS2C.fromCurrent());
                    sendEntitySync(sp);
                    dev.overgrown.apoli.skill.SkillTrees.grantOnJoin(sp);
                    ApoliNetwork.sendSkillDefs(sp);
                    ApoliNetwork.sendSkillState(sp);
                });
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            ActionOnCallbackPower.fireLifecycle(entity, ActionOnCallbackPower.Config::entityActionRemoved);
            PoweredEntities.unregister(entity);
            dev.overgrown.apoli.rope.RopeManager.onEntityGone(entity.getUUID());
            Entity.RemovalReason reason = entity.getRemovalReason();
            if (reason != null && reason.shouldDestroy()) {
                dev.overgrown.apoli.power.builtin.EntitySetPower.onEntityGone(entity.getUUID());
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
            ActionOnCallbackPower.fireLifecycle(newPlayer, ActionOnCallbackPower.Config::entityActionRespawned));

        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(
            (entity, source) -> dev.overgrown.apoli.power.builtin.DeathHandler.onDeath(entity, source));

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            PowerContainer c = PowerContainer.of(trackedEntity);
            if (c instanceof PowerContainerImpl impl && !impl.allPowers().isEmpty()) {
                ApoliNetwork.sendEntityPowers(player, new SyncEntityPowersS2C(
                    trackedEntity.getId(), impl.snapshot(), impl.auxIntSnapshot(), impl.suppressedPowers()));
            }
        });

        dev.overgrown.apoli.entity.disguise.DisguiseManager.setBroadcaster((entity, data) ->
            ApoliNetwork.broadcastDisguise(entity, new dev.overgrown.apoli.network.payload.DisguiseUpdateS2C(entity.getId(), data)));
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            dev.overgrown.apoli.entity.disguise.DisguiseData disguise =
                dev.overgrown.apoli.entity.disguise.DisguiseManager.get(trackedEntity.getUUID());
            if (disguise != null) {
                ApoliNetwork.sendDisguise(player, new dev.overgrown.apoli.network.payload.DisguiseUpdateS2C(
                    trackedEntity.getId(), java.util.Optional.of(disguise)));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            dev.overgrown.apoli.entity.disguise.DisguiseManager.onPlayerLeave(handler.player.getUUID()));

        LOGGER.info("[Apoli] Ready. {} power type(s).", PowerTypeRegistry.view().size());
    }

    private static void onServerTick(MinecraftServer server) {
        DelayedActionQueue.tick();
        dev.overgrown.apoli.rope.RopeManager.tick(server);
        dev.overgrown.apoli.skill.SkillTrees.tickRefresh(server);
        PoweredEntities.forEach(entity -> {
            PowerContainer c = PowerContainer.of(entity);
            if (!(c instanceof PowerContainerImpl impl)) return;
            impl.tickActive();
            if (impl.isStructureDirty()) {
                syncEntityToTrackers(entity, impl);
                impl.clearStructureDirty();
            }
            if (impl.isDirty()) {
                if (entity instanceof ServerPlayer sp) {
                    ApoliNetwork.sendEntityPowers(sp, new SyncEntityPowersS2C(
                        sp.getId(), impl.snapshot(), impl.auxIntSnapshot(), impl.suppressedPowers()));
                }
                impl.clearDirty();
            }
            if (impl.allPowers().isEmpty()) PoweredEntities.unregister(entity);
        });
        dev.overgrown.apoli.power.builtin.EntitySetPower.flushPendingRemovals();
    }

    private static void syncEntityToTrackers(Entity entity, PowerContainerImpl impl) {
        SyncEntityPowersS2C payload = new SyncEntityPowersS2C(entity.getId(), impl.snapshot(), impl.auxIntSnapshot(), impl.suppressedPowers());
        for (ServerPlayer viewer : PlayerLookup.tracking(entity)) {
            ApoliNetwork.sendEntityPowers(viewer, payload);
        }
    }

    private static void sendEntitySync(ServerPlayer player) {
        PowerContainer c = PowerContainer.of(player);
        if (!(c instanceof PowerContainerImpl impl)) return;
        SyncEntityPowersS2C payload = new SyncEntityPowersS2C(player.getId(), impl.snapshot(), impl.auxIntSnapshot(), impl.suppressedPowers());
        ApoliNetwork.sendEntityPowers(player, payload);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ApoliNetwork.sendEntityPowers(viewer, payload);
        }
        impl.clearDirty();
        impl.clearStructureDirty();
    }

    private static void handleActivation(ServerPlayer player, PowerActivationC2S payload) {
        PowerContainer c = PowerContainer.of(player);
        if (c == null || !c.hasPower(payload.power()) || c.isSuppressed(payload.power())) return;
        Power loaded = ApoliPowers.get(payload.power());
        if (loaded == null) return;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (loaded.condition().isPresent()
            && !loaded.condition().get().test(new EntityCtx(player, player.serverLevel()))) {
            return;
        }
        if (type instanceof ActionOnKeyPressPower active
            && loaded.config() instanceof ActionOnKeyPressPower.Config cfg) {
            if (active.tryActivate(payload.power(), cfg, c)) {
                ApoliNetwork.sendActivated(player, new PowerActivatedS2C(payload.power(), cfg.cooldown()));
            }
        } else if (type instanceof FireProjectilePower fpp
            && loaded.config() instanceof FireProjectilePower.Config cfg) {
            if (fpp.tryActivate(payload.power(), cfg, c)) {
                ApoliNetwork.sendActivated(player, new PowerActivatedS2C(payload.power(), cfg.params().cooldown()));
            }
        } else if (type instanceof InventoryPower inv
            && loaded.config() instanceof InventoryPower.Config cfg
            && c instanceof PowerContainerImpl impl) {
            inv.open(payload.power(), cfg, player, impl);
        }
    }

    private static void handleToggle(ServerPlayer player, PowerToggleC2S payload) {
        PowerContainer c = PowerContainer.of(player);
        if (c == null || !c.hasPower(payload.power()) || c.isSuppressed(payload.power())) return;
        Power loaded = ApoliPowers.get(payload.power());
        if (loaded == null) return;
        if (!(PowerTypeRegistry.get(loaded.typeId()) instanceof TogglePower)) return;
        if (loaded.condition().isPresent()
            && !loaded.condition().get().test(new EntityCtx(player, player.serverLevel()))) {
            return;
        }
        TogglePower.toggle(c, payload.power());
        sendEntitySync(player);
    }

    private static final class IdentifiedReloader implements IdentifiableResourceReloadListener {
        private final ResourceLocation id;
        private final PreparableReloadListener delegate;

        IdentifiedReloader(ResourceLocation id, PreparableReloadListener delegate) {
            this.id = id;
            this.delegate = delegate;
        }

        @Override public ResourceLocation getFabricId() { return id; }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager rm,
                                              ProfilerFiller prep, ProfilerFiller apply,
                                              Executor prepExec, Executor applyExec) {
            return delegate.reload(barrier, rm, prep, apply, prepExec, applyExec);
        }
    }
}
