package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record SkillStateSyncS2C(Map<ResourceLocation, Integer> points, Set<ResourceLocation> purchased,
                                Set<ResourceLocation> hidden, Set<ResourceLocation> locked,
                                boolean legacyFormat) {
    public static final ResourceLocation CHANNEL = Apoli.id("skill_state_sync");

    public SkillStateSyncS2C(Map<ResourceLocation, Integer> points, Set<ResourceLocation> purchased,
                             Set<ResourceLocation> hidden, Set<ResourceLocation> locked) {
        this(points, purchased, hidden, locked, false);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(points.size());
        for (Map.Entry<ResourceLocation, Integer> e : points.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue());
        }
        buf.writeCollection(purchased, FriendlyByteBuf::writeResourceLocation);
        if (legacyFormat) {
            return; 
        }
        buf.writeCollection(hidden, FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(locked, FriendlyByteBuf::writeResourceLocation);
    }

    public static SkillStateSyncS2C read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<ResourceLocation, Integer> points = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation key = buf.readResourceLocation();
            points.put(key, buf.readVarInt());
        }
        Set<ResourceLocation> purchased = new HashSet<>(buf.readList(FriendlyByteBuf::readResourceLocation));
        
        Set<ResourceLocation> hidden = buf.isReadable()
            ? new HashSet<>(buf.readList(FriendlyByteBuf::readResourceLocation)) : new HashSet<>();
        Set<ResourceLocation> locked = buf.isReadable()
            ? new HashSet<>(buf.readList(FriendlyByteBuf::readResourceLocation)) : new HashSet<>();
        return new SkillStateSyncS2C(points, purchased, hidden, locked);
    }
}
