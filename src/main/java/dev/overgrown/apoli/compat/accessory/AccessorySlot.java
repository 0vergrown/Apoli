package dev.overgrown.apoli.compat.accessory;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;


public record AccessorySlot(Optional<String> provider, Optional<String> group,
                            Optional<String> type, Optional<Integer> index) {

    private static final Codec<AccessorySlot> RECORD_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("provider").forGetter(AccessorySlot::provider),
        Codec.STRING.optionalFieldOf("group").forGetter(AccessorySlot::group),
        Codec.STRING.optionalFieldOf("slot").forGetter(AccessorySlot::type),
        Codec.INT.optionalFieldOf("index").forGetter(AccessorySlot::index)
    ).apply(i, AccessorySlot::new));

    
    private static AccessorySlot parse(String s) {
        String[] p = s.split("/");
        Optional<String> group = Optional.empty();
        Optional<String> type;
        Optional<Integer> index = Optional.empty();
        if (p.length <= 1) {
            type = Optional.of(s);
        } else {
            group = Optional.of(p[0]);
            type = Optional.of(p[1]);
            if (p.length >= 3) {
                try {
                    index = Optional.of(Integer.parseInt(p[2]));
                } catch (NumberFormatException ignored) {
                    
                }
            }
        }
        return new AccessorySlot(Optional.empty(), group, type, index);
    }

    
    public static final Codec<AccessorySlot> CODEC = Codec.either(Codec.STRING, RECORD_CODEC).xmap(
        e -> e.map(AccessorySlot::parse, r -> r),
        Either::right 
    );

    
    public static final Codec<List<AccessorySlot>> LIST = Codec.either(CODEC, CODEC.listOf()).xmap(
        e -> e.map(List::of, l -> l),
        l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
    );

    public boolean test(AccessorySlotRef ref) {
        return (provider.isEmpty() || provider.get().equalsIgnoreCase(ref.provider()))
            && (group.isEmpty() || group.get().equals(ref.group()))
            && (type.isEmpty() || type.get().equals(ref.type()))
            && (index.isEmpty() || index.get() == ref.index());
    }

    
    public static boolean matchesAny(List<AccessorySlot> filters, AccessorySlotRef ref) {
        if (filters.isEmpty()) return true;
        for (AccessorySlot f : filters) {
            if (f.test(ref)) return true;
        }
        return false;
    }
}
