package dev.overgrown.apoli.effects;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CustomEffectNetworking {
    public record ClientEffectData(ResourceLocation id, Optional<String> name, Optional<ResourceLocation> icon, int color) {
        public ClientEffectData(FriendlyByteBuf buf) {
            this(buf.readResourceLocation(),
                    buf.readOptional(FriendlyByteBuf::readUtf),
                    buf.readOptional(FriendlyByteBuf::readResourceLocation),
                    buf.readInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(id);
            buf.writeOptional(name, FriendlyByteBuf::writeUtf);
            buf.writeOptional(icon, FriendlyByteBuf::writeResourceLocation);
            buf.writeInt(color);
        }
    }

    public record SyncCustomEffectsPacket(List<ClientEffectData> effects) implements FabricPacket {
        public static final ResourceLocation CHANNEL = Apoli.id("sync_custom_effects");
        public static final PacketType<SyncCustomEffectsPacket> TYPE = PacketType.create(CHANNEL, SyncCustomEffectsPacket::new);

        public SyncCustomEffectsPacket(FriendlyByteBuf buf) {
            this(buf.readList(ClientEffectData::new));
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeCollection(effects, (b, e) -> e.write(b));
        }

        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
    }

    public record CustomEffectResponsePacket(boolean success) implements FabricPacket {
        public static final ResourceLocation CHANNEL = Apoli.id("custom_effect_response");
        public static final PacketType<CustomEffectResponsePacket> TYPE = PacketType.create(CHANNEL, CustomEffectResponsePacket::new);

        public CustomEffectResponsePacket(FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(success);
        }

        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
    }

    public static void sync(ServerPlayer player, boolean isSinglePlayer) {
        if (isSinglePlayer) {
            return;
        }

        var packet = new SyncCustomEffectsPacket(CustomEffectRegistry.byEffect.keySet().stream().sorted(Comparator.comparing(CustomEffect::id)).map(effect -> new ClientEffectData(effect.id(), effect.name(), effect.icon(), effect.colorInt())).toList());

        ServerPlayNetworking.send(player, packet);

        CustomEffectRegistry.waiting.add(player.getUUID());

        Apoli.LOGGER.debug("Play Packet send for {} Effects.", packet.effects.size());
    }


}
