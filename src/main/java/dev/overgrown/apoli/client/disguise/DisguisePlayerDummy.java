package dev.overgrown.apoli.client.disguise;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class DisguisePlayerDummy extends RemotePlayer {

    private static final byte ALL_MODEL_PARTS = allModelParts();

    public DisguisePlayerDummy(ClientLevel level, GameProfile profile) {
        super(level, profile);
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, ALL_MODEL_PARTS);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    public void snapCloak() {
        this.xCloak = this.getX();
        this.yCloak = this.getY();
        this.zCloak = this.getZ();
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;
    }

    private static byte allModelParts() {
        int mask = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            mask |= part.getMask();
        }
        return (byte) mask;
    }
}
