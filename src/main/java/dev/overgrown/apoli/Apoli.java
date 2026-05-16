package dev.overgrown.apoli;

import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.action.DelayedActionQueue;
import dev.overgrown.apoli.alias.ApoliAliases;
import dev.overgrown.apoli.command.ApoliPowerCommand;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.loader.ApoliKeybindLoader;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.PowerTypes;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private ApoliReloadListener powerLoader;
    private ApoliKeybindLoader keybindLoader;

    @Override
    public void onInitialize() {
        LOGGER.info("[Apoli] Initializing...");

        ApoliAliases.bootstrap();
        ConditionTypes.bootstrap();
        ActionTypes.bootstrap();
        PowerTypes.bootstrap();

        Class<?> ignored = PowerContainerAttachment.class;

        ApoliNetwork.registerPayloads();

        powerLoader = new ApoliReloadListener();
        keybindLoader = new ApoliKeybindLoader();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiedReloader(id("powers_reloader"), powerLoader));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiedReloader(id("keybinds_reloader"), keybindLoader));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            powerLoader.attachServer(server);
            keybindLoader.attachServer(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ApoliNetwork.broadcastPowers(server, SyncPowersS2C.fromCurrent());
            ApoliNetwork.broadcastKeybinds(server, SyncKeybindsS2C.fromCurrent());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            ApoliPowerCommand.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(Apoli::onServerTick);

        ServerPlayNetworking.registerGlobalReceiver(PowerActivationC2S.TYPE, (payload, context) ->
            context.player().server.execute(() -> handleActivation(context.player(), payload)));

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer sp) {
                level.getServer().execute(() -> {
                    ApoliNetwork.sendKeybinds(sp, SyncKeybindsS2C.fromCurrent());
                    ApoliNetwork.sendPowers(sp, SyncPowersS2C.fromCurrent());
                    sendEntitySync(sp);
                });
            }
        });

        LOGGER.info("[Apoli] Ready. {} power type(s).", PowerTypeRegistry.view().size());
    }

    private static void onServerTick(MinecraftServer server) {
        DelayedActionQueue.tick();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    PowerContainer c = PowerContainer.of(living);
                    if (c instanceof PowerContainerImpl impl) {
                        impl.tickActive();
                        if (impl.isDirty() && living instanceof ServerPlayer sp) {
                            sendEntitySync(sp);
                        }
                    }
                }
            }
        }
    }

    private static void sendEntitySync(ServerPlayer player) {
        PowerContainer c = PowerContainer.of(player);
        if (!(c instanceof PowerContainerImpl impl)) return;
        ApoliNetwork.sendEntityPowers(player, new SyncEntityPowersS2C(player.getId(), impl.snapshot()));
        impl.clearDirty();
    }

    private static void handleActivation(ServerPlayer player, PowerActivationC2S payload) {
        PowerContainer c = PowerContainer.of(player);
        if (c == null || !c.hasPower(payload.power())) return;
        Power loaded = ApoliPowers.get(payload.power());
        if (loaded == null) return;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (!(type instanceof ActionOnKeyPressPower active)) return;
        if (!(loaded.config() instanceof ActionOnKeyPressPower.Config cfg)) return;
        if (loaded.condition().isPresent()
            && !loaded.condition().get().test(new EntityCtx(player, player.serverLevel()))) {
            return;
        }
        if (active.tryActivate(payload.power(), cfg, c)) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(payload.power(), cfg.cooldown()));
        }
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
