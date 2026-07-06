package dev.overgrown.apoli.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ModelPartLookup {
    private ModelPartLookup() {}

    public static List<ModelPart> resolve(HumanoidModel<?> model, String normalized) {
        List<ModelPart> parts = new ArrayList<>(2);
        PlayerModel<?> player = model instanceof PlayerModel<?> pm ? pm : null;
        switch (normalized) {
            case "head" -> {
                parts.add(model.head); parts.add(model.hat);
            }
            case "hat" -> parts.add(model.hat);
            case "body" -> {
                parts.add(model.body); if (player != null) parts.add(player.jacket);
            }
            case "rightarm" -> {
                parts.add(model.rightArm); if (player != null) parts.add(player.rightSleeve);
            }
            case "leftarm" -> {
                parts.add(model.leftArm); if (player != null) parts.add(player.leftSleeve);
            }
            case "rightleg" -> {
                parts.add(model.rightLeg); if (player != null) parts.add(player.rightPants);
            }
            case "leftleg" -> {
                parts.add(model.leftLeg); if (player != null) parts.add(player.leftPants);
            }
            case "jacket" -> {
                if (player != null) parts.add(player.jacket);
            }
            case "rightsleeve" -> {
                if (player != null) parts.add(player.rightSleeve);
            }
            case "leftsleeve" -> {
                if (player != null) parts.add(player.leftSleeve);
            }
            case "rightpants" -> {
                if (player != null) parts.add(player.rightPants);
            }
            case "leftpants" -> {
                if (player != null) parts.add(player.leftPants);
            }
            default -> {}
        }
        return parts;
    }

    public static List<ModelPart> allParts(HumanoidModel<?> model) {
        List<ModelPart> parts = new ArrayList<>(12);
        parts.add(model.head);
        parts.add(model.hat);
        parts.add(model.body);
        parts.add(model.rightArm);
        parts.add(model.leftArm);
        parts.add(model.rightLeg);
        parts.add(model.leftLeg);
        if (model instanceof PlayerModel<?> player) {
            parts.add(player.jacket);
            parts.add(player.rightSleeve);
            parts.add(player.leftSleeve);
            parts.add(player.rightPants);
            parts.add(player.leftPants);
        }
        return parts;
    }

    public static Map<ModelPart, float[]> buildColorMap(HumanoidModel<?> model, Map<String, float[]> partColors) {
        Map<ModelPart, float[]> out = new IdentityHashMap<>();
        for (Map.Entry<String, float[]> entry : partColors.entrySet()) {
            for (ModelPart part : resolve(model, entry.getKey())) {
                out.put(part, entry.getValue());
            }
        }
        return out;
    }
}
