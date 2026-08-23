package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.network.payload.SyncShaderS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShaderPower extends PowerType<ShaderPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("shader");

    public record Config(ResourceLocation shader, boolean toggleable, int priority) {}

    private static final Map<UUID, Config> SENT = new HashMap<>();

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("shader").forGetter(Config::shader),
            Codec.BOOL.optionalFieldOf("toggleable", true).forGetter(Config::toggleable),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }

    public static void tick(MinecraftServer server) {
        if (SENT.isEmpty() && !ApoliPowers.anyOfType(CANONICAL)) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Config chosen = resolve(player);
            UUID id = player.getUUID();
            Config sent = SENT.get(id);
            if (chosen == null ? sent == null : chosen.equals(sent)) continue;
            if (chosen == null) SENT.remove(id);
            else SENT.put(id, chosen);
            ApoliNetwork.sendShader(player, new SyncShaderS2C(
                chosen == null ? null : chosen.shader(),
                chosen != null && chosen.toggleable()));
        }
    }

    public static void forget(UUID player) {
        SENT.remove(player);
    }

    private static @Nullable Config resolve(ServerPlayer player) {
        Entity viewer = player.getCamera();
        if (viewer == null) viewer = player;
        PowerContainer container = PowerContainer.of(viewer);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        if (powers.isEmpty()) return null;

        EntityCtx ctx = null;
        Config best = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof Config cfg)) continue;
            if (best != null && cfg.priority() < best.priority()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(viewer, viewer.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            best = cfg;
        }
        return best;
    }
}
