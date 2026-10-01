package dev.overgrown.apoli.compat.flywheel.mixin;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IConfigurable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class FlywheelCompatMixinPlugin implements IMixinConfigPlugin {

    private static final String VANILLIN_ACCESSOR_MIXIN = "ItemColorsVanillinAccessorMixin";
    private static final String VANILLIN_NEOFORGE_CONFIG = "vanillin.neoforge.mixins.json";

    private static final boolean FLYWHEEL_PRESENT =
        resourceExists("dev/engine_room/flywheel/api/visualization/VisualizerRegistry.class");
    private static final boolean VANILLIN_ACCESSOR_PRESENT =
        resourceExists("dev/engine_room/vanillin/neoforge/mixin/item/ItemColorsAccessor.class");

    private static boolean resourceExists(String path) {
        return FlywheelCompatMixinPlugin.class.getClassLoader().getResource(path) != null;
    }

    private static boolean vanillinRegistersAccessor() {
        ModFileInfo vanillin = LoadingModList.get().getModFileById("vanillin");
        if (vanillin == null) return false;
        for (IConfigurable mixins : vanillin.getConfigList("mixins")) {
            if (mixins.<String>getConfigElement("config").filter(VANILLIN_NEOFORGE_CONFIG::equals).isPresent()) return true;
        }
        return false;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(VANILLIN_ACCESSOR_MIXIN)) {
            return VANILLIN_ACCESSOR_PRESENT && !vanillinRegistersAccessor();
        }
        return FLYWHEEL_PRESENT;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
