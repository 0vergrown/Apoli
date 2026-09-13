package dev.overgrown.apoli.compat.ledger.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class LedgerCompatMixinPlugin implements IMixinConfigPlugin {

    private static final boolean LEDGER_PRESENT = hasMethod(
        "com.github.quiltservertools.ledger.database.ActionQueueService",
        "addToQueue", "(Lcom/github/quiltservertools/ledger/actions/ActionType;)Z");

    private static final boolean INDEXOR_PRESENT =
        hasMethod("ledger.core.LedgerManager", "log", "(Lledger/core/LogEntry;)V")
            && hasFields("ledger.core.LogEntry", "playerUuid", "playerName");

    private static ClassNode read(String name) {
        String path = name.replace('.', '/') + ".class";
        try (InputStream stream = LedgerCompatMixinPlugin.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node,
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasMethod(String owner, String name, String descriptor) {
        ClassNode node = read(owner);
        if (node == null) return false;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return true;
        }
        return false;
    }

    private static boolean hasFields(String owner, String... names) {
        ClassNode node = read(owner);
        if (node == null) return false;
        for (String name : names) {
            boolean found = false;
            for (FieldNode field : node.fields) {
                if (field.name.equals(name) && field.desc.equals("Ljava/lang/String;")) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return mixinClassName.endsWith("LogEntryMixin") || mixinClassName.endsWith("LedgerManagerMixin")
            ? INDEXOR_PRESENT
            : LEDGER_PRESENT;
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
