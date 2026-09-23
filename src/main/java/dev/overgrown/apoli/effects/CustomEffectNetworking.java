package dev.overgrown.apoli.effects;

import dev.overgrown.apoli.Apoli;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class CustomEffectNetworking {
    public record ClientEffectData(ResourceLocation id, Optional<String> name, Optional<ResourceLocation> icon, int color) {
        public static final StreamCodec<FriendlyByteBuf, ClientEffectData> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, ClientEffectData::id,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), ClientEffectData::name,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ClientEffectData::icon,
                ByteBufCodecs.INT, ClientEffectData::color,
                ClientEffectData::new
        );
    }

    public record SyncCustomEffectsResponsePayload(boolean success) implements CustomPacketPayload {
        public static final StreamCodec<ByteBuf, SyncCustomEffectsResponsePayload> CODEC = ByteBufCodecs.BOOL.map(SyncCustomEffectsResponsePayload::new, SyncCustomEffectsResponsePayload::success);

        public static final Type<SyncCustomEffectsResponsePayload> TYPE = new Type<>(Apoli.id("sync_custom_effects_response"));
        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncCustomEffectsPayload(List<ClientEffectData> effects) implements CustomPacketPayload {
        public static final Type<SyncCustomEffectsPayload> TYPE = new Type<>(Apoli.id("sync_custom_effects"));

        public static final StreamCodec<FriendlyByteBuf, SyncCustomEffectsPayload> CODEC = ClientEffectData.CODEC.apply(ByteBufCodecs.list()).map(SyncCustomEffectsPayload::new, SyncCustomEffectsPayload::effects);

        @Override public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public SyncCustomEffectsPayload() {
            this(CustomEffectRegistry.byEffect.keySet().stream().sorted(Comparator.comparing(CustomEffect::id)).map(effect -> new ClientEffectData(effect.id(), effect.name(), effect.icon(), effect.colorInt())).toList());
        }
    }

    public record SyncCustomEffectConfigurationTask (SyncCustomEffectsPayload payload) implements ICustomConfigurationTask {
        public static final Type TYPE = new Type(Apoli.id("sync_custom_effects_task"));
        @Override
        public void run(@NotNull Consumer<CustomPacketPayload> sender) {
            sender.accept(payload);
        }

        @NonNull
        @Override
        public Type type() {
            return TYPE;
        }
    }

    public static void sync(ServerPlayer player) {
        if (player.connection.getConnection().isMemoryConnection() || !EffectConfig.get().enabled()) return;

        var payload = new SyncCustomEffectsPayload();

        PacketDistributor.sendToPlayer(player, payload);

        CustomEffectRegistry.waiting.add(player.getUUID());

        Apoli.LOGGER.info("Play Packet send for {} Effects.", payload.effects.size());
    }
}
