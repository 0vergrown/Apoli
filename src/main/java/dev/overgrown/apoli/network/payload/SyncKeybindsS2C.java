package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.keybind.ApoliKeybinds;
import dev.overgrown.apoli.keybind.Keybind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SyncKeybindsS2C(List<Keybind> keybinds) implements CustomPacketPayload {
    public static final Type<SyncKeybindsS2C> TYPE = new Type<>(Apoli.id("sync_keybinds"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncKeybindsS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncKeybindsS2C::read);

    public static SyncKeybindsS2C fromCurrent() {
        return new SyncKeybindsS2C(new ArrayList<>(ApoliKeybinds.view().values()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(keybinds.size());
        for (Keybind kb : keybinds) {
            buf.writeResourceLocation(kb.id());
            buf.writeUtf(kb.key());
            buf.writeUtf(kb.category());
            buf.writeBoolean(kb.name().isPresent());
            kb.name().ifPresent(buf::writeUtf);
        }
    }

    public static SyncKeybindsS2C read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Keybind> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            String key = buf.readUtf();
            String category = buf.readUtf();
            Optional<String> name = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
            out.add(new Keybind(id, key, category, name));
        }
        return new SyncKeybindsS2C(out);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
