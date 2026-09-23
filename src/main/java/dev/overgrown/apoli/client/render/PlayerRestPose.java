package dev.overgrown.apoli.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;

public final class PlayerRestPose {
    private PlayerRestPose() {}

    private static PlayerModel<AbstractClientPlayer> rest;

    public static PlayerModel<AbstractClientPlayer> get() {
        if (rest == null) {
            rest = new PlayerModel<>(LayerDefinition.create(
                PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(), false);
        }
        return rest;
    }
}
