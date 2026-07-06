package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.power.builtin.ModifyPlayerModelPower;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class ApoliPlayerModels {
    private ApoliPlayerModels() {}

    @FunctionalInterface
    public interface ModelFactory {
        PlayerModel<AbstractClientPlayer> create(EntityRendererProvider.Context ctx, boolean slim);
    }

    private static final Map<ResourceLocation, ModelFactory> FACTORIES = new HashMap<>();
    private static final Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> BAKED_WIDE = new HashMap<>();
    private static final Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> BAKED_SLIM = new HashMap<>();

    public static void register(ResourceLocation id, ModelFactory factory) {
        FACTORIES.put(id, factory);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return FACTORIES.containsKey(id);
    }

    public static void bake(EntityRendererProvider.Context ctx, boolean slim) {
        Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> target = slim ? BAKED_SLIM : BAKED_WIDE;
        target.clear();
        FACTORIES.forEach((id, factory) -> target.put(id, factory.create(ctx, slim)));
    }

    public static PlayerModel<AbstractClientPlayer> override(AbstractClientPlayer player,
                                                             PlayerModel<AbstractClientPlayer> original,
                                                             boolean slim) {
        if (FACTORIES.isEmpty()) return original;
        ResourceLocation id = ModifyPlayerModelPower.firstActiveModel(player);
        if (id == null) return original;
        PlayerModel<AbstractClientPlayer> model = (slim ? BAKED_SLIM : BAKED_WIDE).get(id);
        return model != null ? model : original;
    }
}
