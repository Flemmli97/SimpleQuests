package io.github.flemmli97.simplequests.mixin;

import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.advancements.critereon.FishingRodHookedTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(FishingRodHookedTrigger.class)
public abstract class FishingRodHookedMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void onRodHookedTrigger(ServerPlayer player, ItemStack rod, FishingHook entity, Collection<ItemStack> stacks, CallbackInfo info) {
        PlayerData.get(player).onFished(stacks);
    }
}
