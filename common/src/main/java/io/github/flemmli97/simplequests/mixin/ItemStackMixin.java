package io.github.flemmli97.simplequests.mixin;

import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "onCraftedBy", at = @At("HEAD"))
    private void onItemCrafted(Level level, Player player, int amount, CallbackInfo info) {
        if (player instanceof ServerPlayer serverPlayer)
            SimpleQuestAPI.itemCrafted(serverPlayer, (ItemStack) (Object) this, amount, "");
    }
}
