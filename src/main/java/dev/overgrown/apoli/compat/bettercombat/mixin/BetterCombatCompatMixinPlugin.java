package dev.overgrown.apoli.compat.bettercombat.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BetterCombatCompatMixinPlugin implements IMixinConfigPlugin {

    private static final boolean BETTER_COMBAT_PRESENT =
        classExists("net.bettercombat.client.BetterCombatClientMod");

    private static boolean classExists(String name) {
        String path = name.replace('.', '/') + ".class";
        return BetterCombatCompatMixinPlugin.class.getClassLoader().getResource(path) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return BETTER_COMBAT_PRESENT;
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
