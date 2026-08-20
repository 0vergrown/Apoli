package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public final class LavaVisionHandler {
    private LavaVisionHandler() {}

    public static Pair<Float, Float> getModifier(Entity entity) { //Pair is <s, v>
        List<Float> s = new ArrayList<>();
        List<Float> v = new ArrayList<>();

        PowerLookup.forEach(entity, Apoli.id("lava_vision"), LavaVisionPower.Config.class, cfg -> {
            cfg.s().ifPresent(s::add);
            cfg.v().ifPresent(v::add);
        });

        return new Pair<>((float) s.stream().mapToDouble(Float::doubleValue).sum(), (float) v.stream().mapToDouble(Float::doubleValue).sum());
    }
}
