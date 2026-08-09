package dev.overgrown.apoli.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

public final class PlayerRestPose {
    private PlayerRestPose() {}

    private static PlayerModel<AbstractClientPlayer> rest;

    public static PlayerModel<AbstractClientPlayer> get() {
        if (rest == null) {
            ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER);
            rest = new PlayerModel<>(root, false);
        }
        return rest;
    }
}
