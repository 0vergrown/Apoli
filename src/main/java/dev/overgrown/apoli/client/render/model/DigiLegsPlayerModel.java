package dev.overgrown.apoli.client.render.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.overgrown.apoli.client.render.model.PlayerModelBones.bone;
import static dev.overgrown.apoli.client.render.model.PlayerModelBones.cube;

public class DigiLegsPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    private static final String[] SHARED_PARTS = {
        "head", "hat", "body", "jacket", "right_arm", "left_arm", "right_sleeve", "left_sleeve", "ear", "cloak"
    };

    public DigiLegsPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    public static ModelPart createRoot(boolean slim) {
        ModelPart vanilla = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
        Map<String, ModelPart> children = new LinkedHashMap<>(SHARED_PARTS.length * 2 + 8);
        for (String name : SHARED_PARTS) {
            children.put(name, vanilla.getChild(name));
        }
        children.put("right_leg", rightLeg());
        children.put("left_leg", leftLeg());
        children.put("right_pants", rightPants());
        children.put("left_pants", leftPants());
        return new ModelPart(List.of(), children);
    }

    private static ModelPart rightLeg() {
        return bone(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            List.of(),
            bone(-0.1F, 4.3F, -2.5F, 1.20428F, 0.0F, 0.0F,
                List.of(),
                bone(0.0F, -0.21502F, 0.56015F, -0.38397F, 0.0F, 0.0F,
                    List.of(
                        cube(-1.825F, -0.9F, -1.225F, 3.65F, 6.85F, 3.65F, 0.0F, false, new float[][]{{20.0F, 52.0F, 4.0F, -4.0F}, {24.0F, 52.0F, 3.8F, -4.1F}, {20.0F, 56.0F, -4.0F, 3.0F}, {24.0F, 56.0F, -4.0F, 3.0F}, {28.0F, 56.0F, -4.0F, 3.0F}, {32.0F, 56.0F, -4.0F, 3.0F}})
                    )
                )
            ),
            bone(0.0F, -0.6F, 0.0F, -0.38397F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, 0.0F, false, new float[][]{{4.0F, 16.0F, 4.0F, 4.0F}, {8.0F, 20.0F, 4.0F, -4.0F}, {0.0F, 20.0F, 4.0F, 6.0F}, {4.0F, 20.0F, 4.0F, 6.0F}, {8.0F, 20.0F, 4.0F, 6.0F}, {12.0F, 20.0F, 4.0F, 6.0F}})
                )
            ),
            bone(0.0F, 7.0F, 0.6F, -0.03054F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, -1.0F, -2.0F, 4.0F, 6.3F, 4.0F, 0.0F, false, new float[][]{{7.0F, 16.0F, 4.0F, 4.0F}, {8.0F, 20.0F, 4.0F, -4.0F}, {0.0F, 26.0F, 4.0F, 6.0F}, {4.0F, 26.0F, 4.0F, 6.0F}, {8.0F, 26.0F, 4.0F, 6.0F}, {12.0F, 26.0F, 4.0F, 6.0F}})
                )
            )
        );
    }

    private static ModelPart rightPants() {
        return bone(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            List.of(),
            bone(-0.1F, 4.3F, -2.5F, 1.20428F, 0.0F, 0.0F,
                List.of(),
                bone(0.0F, -0.21502F, 0.56015F, -0.38397F, 0.0F, 0.0F,
                    List.of(
                        cube(-1.825F, -0.9F, -1.225F, 3.65F, 6.7F, 3.65F, 0.25F, false, new float[][]{{4.0F, 42.0F, 4.0F, -2.0F}, {8.0F, 36.0F, 4.0F, -4.0F}, {4.0F, 40.0F, -4.0F, 2.0F}, {8.0F, 40.0F, -4.0F, 2.0F}, {8.0F, 40.0F, -4.0F, 2.0F}, {16.0F, 57.0F, -4.0F, 2.0F}})
                    )
                )
            ),
            bone(0.0F, -0.6F, 0.0F, -0.38397F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, 0.0F, -2.0F, 4.0F, 5.75F, 4.0F, 0.25F, false, new float[][]{{4.0F, 32.0F, 4.0F, 4.0F}, {8.0F, 36.0F, 4.0F, -4.0F}, {0.0F, 36.0F, 4.0F, 6.0F}, {4.0F, 36.0F, 4.0F, 6.0F}, {8.0F, 36.0F, 4.0F, 6.0F}, {12.0F, 36.0F, 4.0F, 6.0F}})
                )
            ),
            bone(0.0F, 7.0F, 0.6F, -0.03054F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, -0.75F, -2.0F, 4.0F, 6.05F, 4.0F, 0.25F, false, new float[][]{{4.0F, 32.0F, 4.0F, 4.0F}, {8.0F, 36.0F, 4.0F, -4.0F}, {0.0F, 42.0F, 4.0F, 6.0F}, {4.0F, 42.0F, 4.0F, 6.0F}, {8.0F, 42.0F, 4.0F, 6.0F}, {12.0F, 42.0F, 4.0F, 6.0F}})
                )
            )
        );
    }

    private static ModelPart leftLeg() {
        return bone(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            List.of(),
            bone(0.0F, -0.6F, 0.0F, -0.38397F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, 0.0F, false, new float[][]{{20.0F, 48.0F, 4.0F, 4.0F}, {24.0F, 52.0F, 4.0F, -4.0F}, {16.0F, 52.0F, 4.0F, 6.0F}, {20.0F, 52.0F, 4.0F, 6.0F}, {24.0F, 52.0F, 4.0F, 6.0F}, {28.0F, 52.0F, 4.0F, 6.0F}})
                )
            ),
            bone(0.0F, 7.0F, 0.6F, -0.03054F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, -1.0F, -2.0F, 4.0F, 6.3F, 4.0F, 0.0F, false, new float[][]{{24.0F, 48.0F, 4.0F, 4.0F}, {24.0F, 52.0F, 4.0F, -4.0F}, {16.0F, 58.0F, 4.0F, 6.0F}, {20.0F, 58.0F, 4.0F, 6.0F}, {24.0F, 58.0F, 4.0F, 6.0F}, {28.0F, 58.0F, 4.0F, 6.0F}})
                )
            ),
            bone(0.1F, 4.3F, -2.5F, 1.20428F, 0.0F, 0.0F,
                List.of(),
                bone(0.0F, -0.21502F, 0.56015F, -0.38397F, 0.0F, 0.0F,
                    List.of(
                        cube(-1.825F, -0.9F, -1.225F, 3.65F, 6.85F, 3.65F, 0.0F, false, new float[][]{{9.0F, 27.0F, 4.0F, -3.0F}, {8.0F, 19.5F, 3.8F, -3.5F}, {4.0F, 23.0F, -4.0F, 3.0F}, {8.0F, 24.0F, -4.0F, 3.0F}, {12.0F, 23.0F, -4.0F, 3.0F}, {16.0F, 23.0F, -4.0F, 3.0F}})
                    )
                )
            )
        );
    }

    private static ModelPart leftPants() {
        return bone(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            List.of(),
            bone(0.0F, -0.6F, 0.0F, -0.38397F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, 0.0F, -2.0F, 4.0F, 5.75F, 4.0F, 0.25F, false, new float[][]{{4.0F, 48.0F, 4.0F, 4.0F}, {8.0F, 36.0F, 4.0F, -4.0F}, {0.0F, 52.0F, 4.0F, 6.0F}, {4.0F, 52.0F, 4.0F, 6.0F}, {8.0F, 52.0F, 4.0F, 6.0F}, {12.0F, 52.0F, 4.0F, 6.0F}})
                )
            ),
            bone(0.0F, 7.0F, 0.6F, -0.03054F, 0.0F, 0.0F,
                List.of(
                    cube(-2.0F, -0.75F, -2.0F, 4.0F, 6.05F, 4.0F, 0.25F, false, new float[][]{{4.0F, 48.0F, 4.0F, 4.0F}, {8.0F, 52.0F, 4.0F, -4.0F}, {0.0F, 58.0F, 4.0F, 6.0F}, {4.0F, 58.0F, 4.0F, 6.0F}, {8.0F, 58.0F, 4.0F, 6.0F}, {12.0F, 58.0F, 4.0F, 6.0F}})
                )
            ),
            bone(0.1F, 4.3F, -2.5F, 1.20428F, 0.0F, 0.0F,
                List.of(),
                bone(0.0F, -0.21502F, 0.56015F, -0.38397F, 0.0F, 0.0F,
                    List.of(
                        cube(-1.825F, -0.9F, -1.225F, 3.65F, 6.7F, 3.65F, 0.25F, false, new float[][]{{4.0F, 42.0F, 4.0F, -2.0F}, {8.0F, 52.0F, 4.0F, -4.0F}, {8.0F, 40.0F, -4.0F, 2.0F}, {4.0F, 40.0F, 4.0F, 2.0F}, {12.0F, 40.0F, -4.0F, 2.0F}, {16.0F, 40.0F, -4.0F, 2.0F}})
                    )
                )
            )
        );
    }

}
