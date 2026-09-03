package dev.overgrown.apoli.data;

import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ModifyModelPartsPower;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class ModelPartTimeline {

    private final Map<LivingEntity, State> states = new WeakHashMap<>();

    public List<Slot> update(@Nullable LivingEntity entity, double now) {
        if (entity == null) return List.of();
        State state = this.states.isEmpty() ? null : this.states.get(entity);
        if (state == null && !ModifyModelPartsPower.has(entity)) return List.of();

        if (state == null) {
            state = new State();
            this.states.put(entity, state);
        } else if (state.lastSample == now) {
            return state.slots;
        }
        state.lastSample = now;
        state.now = now;
        state.poseMask = 0;

        List<Slot> slots = state.slots;
        for (int i = 0; i < slots.size(); i++) slots.get(i).seen = false;

        PowerLookup.forEach(entity, ModifyModelPartsPower.CANONICAL, ModifyModelPartsPower.Config.class, state);

        for (int i = slots.size() - 1; i >= 0; i--) {
            Slot slot = slots.get(i);
            if (!slot.seen && slot.active) {
                slot.active = false;
                slot.since = now;
                slot.weightAtEdge = slot.weight;
            }
            float weight = weight(slot, now);
            if (!slot.active && weight <= 0.0F) {
                slots.remove(i);
                continue;
            }
            slot.weight = weight;
            slot.value = slot.transformation.sample((float) (now - slot.startedAt), entity);
        }
        if (slots.isEmpty() && state.poseMask == 0 && !ModifyModelPartsPower.has(entity)) this.states.remove(entity);
        return slots;
    }

    public boolean overridesPose(@Nullable LivingEntity entity, double now) {
        if (entity == null) return false;
        if (this.states.isEmpty() && !ModifyModelPartsPower.has(entity)) return false;
        update(entity, now);
        State state = this.states.get(entity);
        return state != null && ModifyModelPartsPower.masked(state.poseMask, entity.getPose());
    }

    private static float weight(Slot slot, double now) {
        ModelPartTransformation transformation = slot.transformation;
        float elapsed = (float) (now - slot.since);
        if (slot.active) {
            float duration = transformation.duration();
            if (duration <= 0.0F || elapsed >= duration) return 1.0F;
            float eased = transformation.easing().apply(elapsed / duration);
            return slot.weightAtEdge + (1.0F - slot.weightAtEdge) * eased;
        }
        float duration = transformation.fadeOutDuration();
        if (duration <= 0.0F || elapsed >= duration) return 0.0F;
        return slot.weightAtEdge * (1.0F - transformation.easing().apply(elapsed / duration));
    }

    public static final class Slot {
        private final ModelPartTransformation transformation;
        private int perspectiveMask;
        private boolean seen;
        private boolean active;
        private double since;
        private double startedAt;
        private float weightAtEdge;
        private float weight;
        private float value;

        private Slot(ModelPartTransformation transformation) {
            this.transformation = transformation;
        }

        public ModelPartTransformation transformation() {
            return transformation;
        }

        public float weight() {
            return weight;
        }

        public float value() {
            return value;
        }

        public boolean rendersIn(boolean firstPerson) {
            return Perspective.masked(perspectiveMask, firstPerson);
        }
    }

    private static final class State implements Consumer<ModifyModelPartsPower.Config> {
        private final List<Slot> slots = new ArrayList<>(4);
        private double lastSample = Double.NaN;
        private double now;
        private int poseMask;

        @Override
        public void accept(ModifyModelPartsPower.Config config) {
            poseMask |= config.overridePoseMask();
            List<ModelPartTransformation> transformations = config.transformations();
            for (int i = 0; i < transformations.size(); i++) mark(transformations.get(i), config.perspectiveMask());
        }

        private void mark(ModelPartTransformation transformation, int inheritedMask) {
            int mask = transformation.perspectiveMask();
            if (mask == Perspective.MASK_INHERIT) mask = inheritedMask;
            for (int i = 0; i < slots.size(); i++) {
                Slot slot = slots.get(i);
                if (slot.transformation == transformation) {
                    slot.seen = true;
                    slot.perspectiveMask = mask;
                    if (!slot.active) {
                        slot.active = true;
                        slot.since = now;
                        slot.startedAt = now;
                        slot.weightAtEdge = slot.weight;
                    }
                    return;
                }
            }
            Slot slot = new Slot(transformation);
            slot.perspectiveMask = mask;
            slot.seen = true;
            slot.active = true;
            slot.since = now;
            slot.startedAt = now;
            slots.add(slot);
        }
    }
}
