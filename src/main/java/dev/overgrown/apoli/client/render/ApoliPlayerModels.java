package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.render.model.CentaurPlayerModel;
import dev.overgrown.apoli.client.render.model.DigiLegsPlayerModel;
import dev.overgrown.apoli.client.render.model.FourArmsPlayerModel;
import dev.overgrown.apoli.client.render.model.SixArmsPlayerModel;
import dev.overgrown.apoli.client.render.model.StinkFlyPlayerModel;
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

    public static final ResourceLocation FOUR_ARMS = Apoli.id("four_arms");
    public static final ResourceLocation STINKFLY = Apoli.id("stinkfly");
    public static final ResourceLocation DIGI_LEGS = Apoli.id("digi_legs");
    public static final ResourceLocation SIX_ARMS = Apoli.id("six_arms");
    public static final ResourceLocation CENTAUR = Apoli.id("centaur");

    private static final Map<ResourceLocation, ModelFactory> FACTORIES = new HashMap<>();
    private static final Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> BAKED_WIDE = new HashMap<>();
    private static final Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> BAKED_SLIM = new HashMap<>();

    static {
        register(FOUR_ARMS, (ctx, slim) ->
            new FourArmsPlayerModel<>(FourArmsPlayerModel.createLayer(slim).bakeRoot(), slim));
        register(SIX_ARMS, (ctx, slim) ->
            new SixArmsPlayerModel<>(SixArmsPlayerModel.createLayer(slim).bakeRoot(), slim));
        register(STINKFLY, (ctx, slim) ->
            new StinkFlyPlayerModel<>(StinkFlyPlayerModel.createLayer(slim).bakeRoot(), slim));
        register(DIGI_LEGS, (ctx, slim) ->
            new DigiLegsPlayerModel<>(DigiLegsPlayerModel.createRoot(slim), slim));
        register(CENTAUR, (ctx, slim) ->
            new CentaurPlayerModel<>(CentaurPlayerModel.createLayer(slim).bakeRoot(), slim));
    }

    public static void register(ResourceLocation id, ModelFactory factory) {
        FACTORIES.put(id, factory);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return ModifyPlayerModelPower.VANILLA_MODEL.equals(id) || FACTORIES.containsKey(id);
    }

    public static void bake(EntityRendererProvider.Context ctx, boolean slim) {
        Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> target = slim ? BAKED_SLIM : BAKED_WIDE;
        target.clear();
        FACTORIES.forEach((id, factory) -> target.put(id, factory.create(ctx, slim)));
    }

    public static PlayerModel<AbstractClientPlayer> override(AbstractClientPlayer player,
                                                             PlayerModel<AbstractClientPlayer> original,
                                                             boolean slim) {
        ResourceLocation id = ModifyPlayerModelPower.firstActiveModel(player);
        if (id == null || ModifyPlayerModelPower.VANILLA_MODEL.equals(id)) return original;
        PlayerModel<AbstractClientPlayer> model = (slim ? BAKED_SLIM : BAKED_WIDE).get(id);
        return model != null ? model : original;
    }
}
