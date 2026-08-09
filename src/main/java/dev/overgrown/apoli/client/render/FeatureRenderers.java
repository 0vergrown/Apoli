package dev.overgrown.apoli.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.DolphinCarryingItemLayer;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemFlowerLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.client.renderer.entity.layers.SheepFurLayer;
import net.minecraft.client.renderer.entity.layers.ShulkerHeadLayer;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.layers.SnowGolemHeadLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.layers.WitchItemLayer;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.client.renderer.entity.layers.WolfArmorLayer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class FeatureRenderers {

    private static final String[] NONE = new String[0];
    private static final Map<String, Class<?>> MAPPINGS = new LinkedHashMap<>();

    private static final ClassValue<String[]> KEYS = new ClassValue<>() {
        @Override
        protected String[] computeValue(Class<?> type) {
            List<String> out = new ArrayList<>(2);
            for (Map.Entry<String, Class<?>> entry : MAPPINGS.entrySet()) {
                if (entry.getValue().isAssignableFrom(type)) out.add(entry.getKey());
            }
            return out.isEmpty() ? NONE : out.toArray(String[]::new);
        }
    };

    private FeatureRenderers() {}

    private static void map(String name, Class<?> type) {
        MAPPINGS.put(name, type);
    }

    static {
        map("armor", HumanoidArmorLayer.class);
        map("cape", CapeLayer.class);
        map("cat_collar", CatCollarLayer.class);
        map("creeper_power", CreeperPowerLayer.class);
        map("deadmau5", Deadmau5EarsLayer.class);
        map("dolphin_held_item", DolphinCarryingItemLayer.class);
        map("drowned_overlay", DrownedOuterLayer.class);
        map("elytra", ElytraLayer.class);
        map("enderman_block", CarriedBlockLayer.class);
        map("energy_swirl_overlay", EnergySwirlLayer.class);
        map("eyes", EyesLayer.class);
        map("fox_held_item", FoxHeldItemLayer.class);
        map("head", CustomHeadLayer.class);
        map("held_item", ItemInHandLayer.class);
        map("horse_armor", HorseArmorLayer.class);
        map("horse_marking", HorseMarkingLayer.class);
        map("iron_golem_crack", IronGolemCrackinessLayer.class);
        map("iron_golem_flower", IronGolemFlowerLayer.class);
        map("llama_decor", LlamaDecorLayer.class);
        map("mooshroom_mushroom", MushroomCowMushroomLayer.class);
        map("panda_held_item", PandaHoldsItemLayer.class);
        map("saddle", SaddleLayer.class);
        map("sheep_wool", SheepFurLayer.class);
        map("shoulder_parrot", ParrotOnShoulderLayer.class);
        map("shulker_head", ShulkerHeadLayer.class);
        map("skeleton_clothing", SkeletonClothingLayer.class);
        map("slime_overlay", SlimeOuterLayer.class);
        map("snowman_pumpkin", SnowGolemHeadLayer.class);
        map("stuck_arrows", ArrowLayer.class);
        map("stuck_objects", StuckInBodyLayer.class);
        map("stuck_stingers", BeeStingerLayer.class);
        map("trident_riptide", SpinAttackEffectLayer.class);
        map("tropical_fish_color", TropicalFishPatternLayer.class);
        map("villager_clothing", VillagerProfessionLayer.class);
        map("villager_held_item", CrossedArmsItemLayer.class);
        map("witch_item", WitchItemLayer.class);
        map("wither_armor", WitherArmorLayer.class);
        map("wolf_armor", WolfArmorLayer.class);
        map("wolf_collar", WolfCollarLayer.class);
        map("breeze_wind", BreezeWindLayer.class);
    }

    public static String[] keysFor(Class<?> layerClass) {
        return KEYS.get(layerClass);
    }

    public static java.util.Set<String> names() {
        return java.util.Collections.unmodifiableSet(MAPPINGS.keySet());
    }
}
