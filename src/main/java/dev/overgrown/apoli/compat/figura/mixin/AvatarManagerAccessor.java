package dev.overgrown.apoli.compat.figura.mixin;

import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Pseudo
@Mixin(value = AvatarManager.class, remap = false)
public interface AvatarManagerAccessor {

    @Accessor("LOADED_USERS")
    static Map<UUID, UserData> apoli$loadedUsers() {
        throw new AssertionError();
    }

    @Accessor("FETCHED_USERS")
    static Set<UUID> apoli$fetchedUsers() {
        throw new AssertionError();
    }
}
