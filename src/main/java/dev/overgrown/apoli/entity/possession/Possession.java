package dev.overgrown.apoli.entity.possession;

import dev.overgrown.apoli.access.PossessionPartner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.jetbrains.annotations.Nullable;

public final class Possession {
    private Possession() {}

    @Nullable
    public static Entity hostOf(Entity entity) {
        return ((PossessionPartner) entity).apoli$possessionHost();
    }

    @Nullable
    public static Entity possessorOf(Entity entity) {
        return ((PossessionPartner) entity).apoli$possessor();
    }

    public static boolean linked(Entity first, Entity second) {
        return hostOf(first) == second || hostOf(second) == first;
    }

    @Nullable
    public static EntityDimensions hostDimensions(Entity possessor) {
        Entity host = hostOf(possessor);
        return host == null ? null : host.getDimensions(host.getPose());
    }

    public static void link(Entity possessor, Entity host) {
        ((PossessionPartner) possessor).apoli$setPossessionHost(host);
        ((PossessionPartner) host).apoli$setPossessor(possessor);
    }

    public static void unlink(Entity possessor) {
        Entity host = hostOf(possessor);
        ((PossessionPartner) possessor).apoli$setPossessionHost(null);
        if (host != null && possessorOf(host) == possessor) {
            ((PossessionPartner) host).apoli$setPossessor(null);
        }
    }

    public static void unlinkHost(Entity host) {
        Entity possessor = possessorOf(host);
        ((PossessionPartner) host).apoli$setPossessor(null);
        if (possessor != null && hostOf(possessor) == host) {
            ((PossessionPartner) possessor).apoli$setPossessionHost(null);
        }
    }
}
