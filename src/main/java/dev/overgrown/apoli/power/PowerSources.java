package dev.overgrown.apoli.power;

import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.apoli.skill.Skill;
import dev.overgrown.apoli.skill.SkillRegistry;
import dev.overgrown.apoli.skill.SkillTree;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PowerSources {

    public interface Provider {
        @Nullable Collection<ResourceLocation> powersOf(ResourceLocation sourceId);

        void collectSources(Collection<ResourceLocation> out);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private PowerSources() {}

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    public static @Nullable Set<ResourceLocation> powersOf(ResourceLocation sourceId) {
        Set<ResourceLocation> out = null;
        for (Provider provider : PROVIDERS) {
            Collection<ResourceLocation> powers = provider.powersOf(sourceId);
            if (powers == null || powers.isEmpty()) continue;
            if (out == null) out = new LinkedHashSet<>();
            out.addAll(powers);
        }
        return out;
    }

    public static Set<ResourceLocation> powersOfAll(Collection<ResourceLocation> sourceIds) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (ResourceLocation sourceId : sourceIds) {
            Set<ResourceLocation> powers = powersOf(sourceId);
            if (powers != null) out.addAll(powers);
        }
        return out;
    }

    public static boolean isKnown(ResourceLocation sourceId) {
        for (Provider provider : PROVIDERS) {
            Collection<ResourceLocation> powers = provider.powersOf(sourceId);
            if (powers != null && !powers.isEmpty()) return true;
        }
        return false;
    }

    public static List<ResourceLocation> knownSources() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (Provider provider : PROVIDERS) provider.collectSources(out);
        return new ArrayList<>(out);
    }

    public static void bootstrap() {
        register(new MultiplePowerProvider());
        register(new SkillTreeProvider());
    }

    private static final class MultiplePowerProvider implements Provider {
        @Override
        public @Nullable Collection<ResourceLocation> powersOf(ResourceLocation sourceId) {
            Power power = ApoliPowers.get(sourceId);
            if (power == null || !(power.config() instanceof MultiplePower.Cfg cfg)) return null;
            return cfg.subPowerIds();
        }

        @Override
        public void collectSources(Collection<ResourceLocation> out) {
            ApoliPowers.view().forEach((id, power) -> {
                if (power.config() instanceof MultiplePower.Cfg) out.add(id);
            });
        }
    }

    private static final class SkillTreeProvider implements Provider {
        @Override
        public @Nullable Collection<ResourceLocation> powersOf(ResourceLocation sourceId) {
            SkillTree tree = SkillRegistry.tree(sourceId);
            if (tree == null) return null;
            Set<ResourceLocation> out = new LinkedHashSet<>(tree.defaultPowers());
            for (Skill skill : SkillRegistry.all()) {
                if (sourceId.equals(SkillRegistry.rootOf(skill.id()))) out.addAll(skill.powers());
            }
            return out;
        }

        @Override
        public void collectSources(Collection<ResourceLocation> out) {
            out.addAll(SkillRegistry.roots());
        }
    }
}
