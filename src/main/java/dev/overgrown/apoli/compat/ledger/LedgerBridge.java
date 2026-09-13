package dev.overgrown.apoli.compat.ledger;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.attribution.PowerCause;
import net.minecraft.world.entity.Entity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class LedgerBridge {

    private static final MethodHandle SET_SOURCE_NAME = resolveSourceNameSetter();

    private static boolean failed;

    private LedgerBridge() {}

    public static void stampLedgerAction(Object action) {
        if (SET_SOURCE_NAME == null || failed) return;
        String label = PowerCause.label();
        if (label == null) return;
        try {
            SET_SOURCE_NAME.invoke(action, label);
        } catch (Throwable t) {
            failed = true;
            Apoli.LOGGER.warn("[Apoli] Could not label a Ledger entry with the power that caused it; "
                + "entries will keep Ledger's own source names.", t);
        }
    }

    public static void stampIndexorEntry(ApoliLoggedEntry entry) {
        if (isRealPlayer(entry.apoli$playerUuid())) return;
        Entity holder = PowerCause.holder();
        if (holder == null) return;
        String label = PowerCause.label();
        if (label == null) return;
        entry.apoli$attribute(holder.getUUID().toString(), holder.getName().getString() + " (" + label + ")");
    }

    private static boolean isRealPlayer(String uuid) {
        return uuid != null && uuid.length() == 36;
    }

    private static MethodHandle resolveSourceNameSetter() {
        try {
            Class<?> actionType = Class.forName("com.github.quiltservertools.ledger.actions.ActionType",
                false, LedgerBridge.class.getClassLoader());
            return MethodHandles.publicLookup()
                .findVirtual(actionType, "setSourceName", MethodType.methodType(void.class, String.class));
        } catch (Throwable t) {
            return null;
        }
    }
}
