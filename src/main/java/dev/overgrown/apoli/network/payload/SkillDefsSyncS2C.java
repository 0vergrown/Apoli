package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.skill.Skill;
import dev.overgrown.apoli.skill.SkillRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SkillDefsSyncS2C(List<Entry> entries, boolean legacyFormat) implements CustomPacketPayload {
    public record Entry(ResourceLocation id, Optional<ResourceLocation> parent, Component name, Component description,
                        ItemStack icon, List<ResourceLocation> powers, int cost, Optional<ResourceLocation> background,
                        int order) {}

    public static final Type<SkillDefsSyncS2C> TYPE = new Type<>(Apoli.id("skill_defs_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillDefsSyncS2C> STREAM_CODEC = StreamCodec.of(
        SkillDefsSyncS2C::write, SkillDefsSyncS2C::read);

    public SkillDefsSyncS2C(List<Entry> entries) {
        this(entries, false);
    }

    public static SkillDefsSyncS2C fromCurrent() {
        return fromCurrent(false);
    }

    public static SkillDefsSyncS2C fromCurrent(boolean legacyFormat) {
        List<Entry> entries = new ArrayList<>();
        for (Skill skill : SkillRegistry.all()) {
            entries.add(new Entry(skill.id(), skill.parent(), skill.name(), skill.description(),
                skill.icon(), skill.powers(), skill.cost(), skill.background(), skill.order()));
        }
        return new SkillDefsSyncS2C(entries, legacyFormat);
    }

    private static void write(RegistryFriendlyByteBuf buf, SkillDefsSyncS2C payload) {
        buf.writeVarInt(payload.entries.size());
        for (Entry e : payload.entries) {
            buf.writeResourceLocation(e.id);
            buf.writeOptional(e.parent, (b, p) -> b.writeResourceLocation(p));
            ComponentSerialization.STREAM_CODEC.encode(buf, e.name);
            ComponentSerialization.STREAM_CODEC.encode(buf, e.description);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon);
            buf.writeCollection(e.powers, (b, p) -> b.writeResourceLocation(p));
            buf.writeVarInt(e.cost);
            buf.writeOptional(e.background, (b, bg) -> b.writeResourceLocation(bg));
            if (!payload.legacyFormat) {
                buf.writeVarInt(e.order); 
            }
        }
    }

    private static SkillDefsSyncS2C read(RegistryFriendlyByteBuf buf) {
        
        
        int start = buf.readerIndex();
        try {
            SkillDefsSyncS2C parsed = readEntries(buf, true);
            if (!buf.isReadable()) {
                return parsed;
            }
        } catch (Exception ignored) {
        }
        buf.readerIndex(start);
        return readEntries(buf, false);
    }

    private static SkillDefsSyncS2C readEntries(RegistryFriendlyByteBuf buf, boolean withOrder) {
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Optional<ResourceLocation> parent = buf.readOptional(b -> b.readResourceLocation());
            Component name = ComponentSerialization.STREAM_CODEC.decode(buf);
            Component description = ComponentSerialization.STREAM_CODEC.decode(buf);
            ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            List<ResourceLocation> powers = buf.readList(b -> b.readResourceLocation());
            int cost = buf.readVarInt();
            Optional<ResourceLocation> background = buf.readOptional(b -> b.readResourceLocation());
            int order = withOrder ? buf.readVarInt() : 0;
            entries.add(new Entry(id, parent, name, description, icon, powers, cost, background, order));
        }
        return new SkillDefsSyncS2C(entries);
    }

    @Override
    public Type<SkillDefsSyncS2C> type() {
        return TYPE;
    }
}
