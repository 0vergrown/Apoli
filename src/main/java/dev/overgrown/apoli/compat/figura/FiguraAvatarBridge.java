package dev.overgrown.apoli.compat.figura;

import dev.overgrown.apoli.compat.figura.mixin.AvatarManagerAccessor;
import net.minecraft.nbt.CompoundTag;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FiguraAvatarBridge {
    private FiguraAvatarBridge() {}

    @Nullable
    public static Object equip(UUID id, CompoundTag nbt) {
        AvatarManager.clearAvatars(id);
        AvatarManagerAccessor.apoli$fetchedUsers().add(id);
        UserData user = AvatarManagerAccessor.apoli$loadedUsers().computeIfAbsent(id, UserData::new);
        user.loadAvatar(nbt);
        return user.getMainAvatar();
    }

    public static void restore(UUID id) {
        AvatarManager.reloadAvatar(id);
    }

    @Nullable
    public static Object mainAvatarHandle(UUID id) {
        UserData user = AvatarManagerAccessor.apoli$loadedUsers().get(id);
        return user == null ? null : user.getMainAvatar();
    }

    public static boolean panicked() {
        return AvatarManager.panic;
    }
}
