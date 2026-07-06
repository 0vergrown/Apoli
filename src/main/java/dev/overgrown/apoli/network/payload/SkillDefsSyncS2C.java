package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.skill.Skill;
import dev.overgrown.apoli.skill.SkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SkillDefsSyncS2C(List<Entry> entries, boolean legacyFormat) {
    public record Entry(ResourceLocation id, Optional<ResourceLocation> parent, Component name, Component description,
                        ItemStack icon, List<ResourceLocation> powers, int cost, Optional<ResourceLocation> background,
                        int order) {}

    public static final ResourceLocation CHANNEL = Apoli.id("skill_defs_sync");

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

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeResourceLocation(e.id());
            buf.writeOptional(e.parent(), FriendlyByteBuf::writeResourceLocation);
            buf.writeComponent(e.name());
            buf.writeComponent(e.description());
            buf.writeItem(e.icon());
            buf.writeCollection(e.powers(), FriendlyByteBuf::writeResourceLocation);
            buf.writeVarInt(e.cost());
            buf.writeOptional(e.background(), FriendlyByteBuf::writeResourceLocation);
            if (!legacyFormat) {
                buf.writeVarInt(e.order()); 
            }
        }
    }

    public static SkillDefsSyncS2C read(FriendlyByteBuf buf) {
        
        
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

    private static SkillDefsSyncS2C readEntries(FriendlyByteBuf buf, boolean withOrder) {
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Optional<ResourceLocation> parent = buf.readOptional(FriendlyByteBuf::readResourceLocation);
            Component name = buf.readComponent();
            Component description = buf.readComponent();
            ItemStack icon = buf.readItem();
            List<ResourceLocation> powers = buf.readList(FriendlyByteBuf::readResourceLocation);
            int cost = buf.readVarInt();
            Optional<ResourceLocation> background = buf.readOptional(FriendlyByteBuf::readResourceLocation);
            int order = withOrder ? buf.readVarInt() : 0;
            entries.add(new Entry(id, parent, name, description, icon, powers, cost, background, order));
        }
        return new SkillDefsSyncS2C(entries);
    }
}
