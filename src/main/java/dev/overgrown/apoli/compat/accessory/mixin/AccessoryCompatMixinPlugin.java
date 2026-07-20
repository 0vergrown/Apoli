package dev.overgrown.apoli.compat.accessory.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AccessoryCompatMixinPlugin implements IMixinConfigPlugin {

    private static final boolean TRINKETS_PRESENT = classExists("dev.emi.trinkets.api.TrinketInventory");

    private static boolean classExists(String name) {
        String path = name.replace('.', '/') + ".class";
        return AccessoryCompatMixinPlugin.class.getClassLoader().getResource(path) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".trinkets.")) return TRINKETS_PRESENT;
        return true;
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
