package dev.overgrown.apoli;

import com.mojang.logging.LogUtils;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(Apoli.MOD_ID)
public final class Apoli {
    public static final String MOD_ID = "apoli";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private final ApoliReloadListener powerLoader = new ApoliReloadListener();
    private final ApoliKeybindLoader keybindLoader = new ApoliKeybindLoader();

    public Apoli(IEventBus modBus, ModContainer container) {
        LOGGER.info("[Apoli] Initializing...");

        ApoliAliases.bootstrap();
        ConditionTypes.bootstrap();
        ActionTypes.bootstrap();
        PowerTypes.bootstrap();

        PowerContainerAttachment.register(modBus);
        modBus.addListener(ApoliNetwork::register);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("[Apoli] Ready. {} power type(s).", PowerTypeRegistry.view().size());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(powerLoader);
        event.addListener(keybindLoader);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ApoliPowerCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ApoliNetwork.sendKeybinds(event.getPlayer(), SyncKeybindsS2C.fromCurrent());
            ApoliNetwork.sendPowers(event.getPlayer(), SyncPowersS2C.fromCurrent());
            sendEntitySync(event.getPlayer());
        } else {
            ApoliNetwork.broadcastPowers(event.getPlayerList().getServer(), SyncPowersS2C.fromCurrent());
            ApoliNetwork.broadcastKeybinds(event.getPlayerList().getServer(), SyncKeybindsS2C.fromCurrent());
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ApoliNetwork.sendKeybinds(sp, SyncKeybindsS2C.fromCurrent());
            ApoliNetwork.sendPowers(sp, SyncPowersS2C.fromCurrent());
            sendEntitySync(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // NeoForge's AttachmentType.copyOnDeath() handles attachment copy at the
        // engine level — no manual snapshot needed.
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        DelayedActionQueue.tick();
        for (ServerLevel level : event.getServer().getAllLevels()) {
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

    public static void sendEntitySync(ServerPlayer player) {
        PowerContainer c = PowerContainer.of(player);
        if (!(c instanceof PowerContainerImpl impl)) return;
        ApoliNetwork.sendEntityPowers(player, new SyncEntityPowersS2C(player.getId(), impl.snapshot()));
        impl.clearDirty();
    }

    public static void handleActivation(ServerPlayer player, PowerActivationC2S payload) {
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
}
