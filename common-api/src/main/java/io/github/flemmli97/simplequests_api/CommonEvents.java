package io.github.flemmli97.simplequests_api;

import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests_api.impls.progression.BlockTracker;
import io.github.flemmli97.simplequests_api.impls.progression.CraftingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
import io.github.flemmli97.simplequests_api.impls.progression.FishingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.KillTracker;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public class CommonEvents {

    public static void onDeath(LivingEntity entity) {
        if (entity.getKillCredit() instanceof ServerPlayer player) {
            PlayerQuestDataRegistry.applyAll(player, d -> d.trigger(KillTracker.KEY, entity, ""));
        }
    }

    public static void onCrafted(ServerPlayer player, ItemStack stack, int amount) {
        PlayerQuestDataRegistry.applyAll(player, d -> d.trigger(CraftingTracker.KEY, Pair.of(stack, amount), ""));
    }

    public static void onFished(ServerPlayer player, Collection<ItemStack> stacks) {
        PlayerQuestDataRegistry.applyAll(player, d -> d.trigger(FishingTracker.KEY, stacks, ""));
    }

    public static void onInteractEntity(ServerPlayer player, Entity target, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND)
            PlayerQuestDataRegistry.applyAll(player, d -> d.trigger(EntityTracker.KEY, target, ""));
    }

    public static void onBlockInteract(ServerPlayer player, BlockPos pos, boolean use) {
        PlayerQuestDataRegistry.applyAll(player, d -> d.trigger(BlockTracker.KEY, Pair.of(pos, use), ""));
    }
}
