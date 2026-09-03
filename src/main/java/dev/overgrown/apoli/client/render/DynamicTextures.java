package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.TextureRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class DynamicTextures {

    private DynamicTextures() {}

    public static ResourceLocation resolve(@Nullable ResourceLocation id, @Nullable Entity subject) {
        if (id == null || !Apoli.MOD_ID.equals(id.getNamespace())) return id;
        TextureRef.Kind kind = TextureRef.kindOf(id);
        if (kind == null) return id;
        ResourceLocation resolved = lookup(kind, subject);
        return resolved != null ? resolved : id;
    }

    public static ItemStack stack(@Nullable ResourceLocation id, @Nullable Entity subject) {
        TextureRef.Kind kind = TextureRef.kindOf(id);
        if (kind == null || !kind.isItem() || !(subject instanceof LivingEntity living)) return ItemStack.EMPTY;
        return kind == TextureRef.Kind.HELD_ITEM ? living.getMainHandItem() : living.getOffhandItem();
    }

    public static @Nullable Entity subject(TextureRef ref, @Nullable Entity self) {
        return ref.subject() == TextureRef.Subject.VIEWER ? Minecraft.getInstance().player : self;
    }

    private static @Nullable ResourceLocation lookup(TextureRef.Kind kind, @Nullable Entity subject) {
        if (subject == null) return null;
        return switch (kind) {
            case PLAYER, PLAYER_FACE -> skin(subject);
            case PLAYER_CAPE -> cape(subject);
            case ENTITY -> entityTexture(subject);
            case HELD_ITEM, OFFHAND_ITEM -> null;
        };
    }

    private static @Nullable ResourceLocation entityTexture(Entity subject) {
        try {
            return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(subject).getTextureLocation(subject);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable ResourceLocation skin(Entity subject) {
        return subject instanceof AbstractClientPlayer player ? player.getSkin().texture() : entityTexture(subject);
    }

    private static @Nullable ResourceLocation cape(Entity subject) {
        return subject instanceof AbstractClientPlayer player ? player.getSkin().capeTexture() : null;
    }

    public static ResourceLocation resolve(@Nullable ResourceLocation id, @Nullable Entity subject,
                                           @Nullable java.util.UUID fallbackPlayer) {
        if (id == null || !Apoli.MOD_ID.equals(id.getNamespace())) return id;
        TextureRef.Kind kind = TextureRef.kindOf(id);
        if (kind == null) return id;
        ResourceLocation resolved = lookup(kind, subject);
        if (resolved == null && fallbackPlayer != null) resolved = fromPlayerInfo(kind, fallbackPlayer);
        return resolved != null ? resolved : id;
    }

    private static @Nullable net.minecraft.client.multiplayer.PlayerInfo playerInfo(java.util.UUID uuid) {
        net.minecraft.client.multiplayer.ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(uuid);
    }

    private static @Nullable ResourceLocation fromPlayerInfo(TextureRef.Kind kind, java.util.UUID uuid) {
        net.minecraft.client.multiplayer.PlayerInfo info = playerInfo(uuid);
        if (info == null) return null;
        return switch (kind) {
            case PLAYER, PLAYER_FACE, ENTITY -> info.getSkin().texture();
            case PLAYER_CAPE -> info.getSkin().capeTexture();
            default -> null;
        };
    }
}
