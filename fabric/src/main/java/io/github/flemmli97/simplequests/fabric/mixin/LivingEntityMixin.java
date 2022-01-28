package io.github.flemmli97.simplequests.fabric.mixin;

import io.github.flemmli97.simplequests.fabric.SimpleQuestsFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class, ServerPlayer.class})
public class LivingEntityMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void die(DamageSource source, CallbackInfo info) {
        SimpleQuestsFabric.onDeath((LivingEntity) (Object) this, source);
    }
}
