package dev.overgrown.apoli.mixin.sound;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.sound.SoundReplacer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerPlaySoundMixin {

    @WrapOperation(method = {
        "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
        "attack(Lnet/minecraft/world/entity/Entity;)V",
        "giveExperienceLevels(I)V",
        "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;"
    }, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void apoli$replacePlayerSound(Level level, Player except, double x, double y, double z,
                                          SoundEvent sound, SoundSource source, float volume, float pitch,
                                          Operation<Void> original) {
        SoundReplacer.emit((Entity) (Object) this, sound, volume, pitch,
            (replaced, newVolume, newPitch) ->
                original.call(level, except, x, y, z, replaced, source, newVolume, newPitch));
    }

    @WrapOperation(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;)V"))
    private void apoli$replaceAttackSound(Level level, Player except, double x, double y, double z,
                                          SoundEvent sound, SoundSource source, Operation<Void> original) {
        if (!SoundReplacer.hasEmission((Entity) (Object) this)) {
            original.call(level, except, x, y, z, sound, source);
            return;
        }
        SoundReplacer.emit((Entity) (Object) this, sound, 1.0F, 1.0F,
            (replaced, newVolume, newPitch) ->
                level.playSound(except, x, y, z, replaced, source, newVolume, newPitch));
    }
}
