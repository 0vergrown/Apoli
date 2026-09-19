package dev.overgrown.apoli.effects;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    public record SyncCustomEffectsPayload(List<ClientEffectData> effects) implements CustomPacketPayload {
        public static final Type<SyncCustomEffectsPayload> TYPE = new Type<>(Apoli.id("sync_custom_effects"));

        public static final StreamCodec<FriendlyByteBuf, SyncCustomEffectsPayload> CODEC = ClientEffectData.CODEC.apply(ByteBufCodecs.list()).map(SyncCustomEffectsPayload::new, SyncCustomEffectsPayload::effects);

        @Override public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void sync(@Nullable ServerPlayer player, @Nullable ServerConfigurationPacketListenerImpl config, boolean isSinglePlayer) {
        var payload = new SyncCustomEffectsPayload(CustomEffectRegistry.byEffect.keySet().stream().sorted(Comparator.comparing(CustomEffect::id)).map(effect -> new ClientEffectData(effect.id(), effect.name(), effect.icon(), effect.colorInt())).toList());

        if (isSinglePlayer) {
            return;
        }

        if (config != null) {
            ServerConfigurationNetworking.send(config, payload);

            Apoli.LOGGER.info("Configuration Packet send for {} Effects.", payload.effects.size());
        }
        else if (player != null) {
            ServerPlayNetworking.send(player, payload);

            Apoli.LOGGER.info("Play Packet send for {} Effects.", payload.effects.size());
        }
    }
}
