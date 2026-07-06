package dev.overgrown.apoli.compat.hardcorerevival.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;


public class HardcoreRevivalCompatMixinPlugin implements IMixinConfigPlugin {

    private static final boolean HR_PRESENT = classExists("net.blay09.mods.hardcorerevival.HardcoreRevivalManager");

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, HardcoreRevivalCompatMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return HR_PRESENT;
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
