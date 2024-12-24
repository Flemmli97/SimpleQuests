package io.github.flemmli97.simpleimpl.data;

import io.github.flemmli97.simpleimpl.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests_api.player.ProgressionTracker;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerImpl;
import io.github.flemmli97.simplequests_api.impls.entries.QuestEntryImpls;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.UUID;

public class ProgressionCheckers {


    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.KillEntry> createKillPredicate(ServerPlayer player, LivingEntity entity) {
        return (name, entry, prog) -> {
            if (entry.check(player, entity)) {
                ProgressionTracker<Integer, QuestEntryImpls.KillEntry> track = prog.getOrCreateTracker(ProgressionTrackerImpl.KillTracker.KEY, entry, name);
                return track.apply(1);
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.EntityInteractEntry> createInteractionPredicate(ServerPlayer player, Entity entity) {
        return (name, entry, prog) -> {
            ProgressionTracker<UUID, QuestEntryImpls.EntityInteractEntry> interacted = prog.getOrCreateTracker(ProgressionTrackerImpl.EntityTracker.KEY, entry, name);
            if (!interacted.isApplicable(entity.getUUID())) {
                if (!prog.getQuest().category.isSilent)
                    player.sendMessage(new TranslatableComponent("simplequests.interaction.dupe").withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
                return false;
            }
            if (entry.check(player, entity)) {
                return interacted.apply(entity.getUUID());
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.BlockInteractEntry> createBlockInteractionPredicate(ServerPlayer player, BlockPos pos, boolean use) {
        return (name, entry, prog) -> {
            ProgressionTracker<BlockPos, QuestEntryImpls.BlockInteractEntry> interacted = prog.getOrCreateTracker(ProgressionTrackerImpl.BlockTracker.KEY, entry, name);
            if (!interacted.isApplicable(pos)) {
                if (!prog.getQuest().category.isSilent)
                    player.sendMessage(new TranslatableComponent("simplequests.interaction.block.dupe." + entry.use()).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
                return false;
            }
            if (entry.check(player, pos, use)) {
                return interacted.apply(pos);
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.CraftingEntry> createCraftingPredicate(ServerPlayer player, ItemStack stack, int amount) {
        return (name, entry, prog) -> {
            if (entry.check(player, stack)) {
                ProgressionTracker<Integer, QuestEntryImpls.CraftingEntry> track = prog.getOrCreateTracker(ProgressionTrackerImpl.CraftingTracker.KEY, entry, name);
                return track.apply(amount);
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.FishingEntry> createFishingPredicate(ServerPlayer player, Collection<ItemStack> stacks) {
        return (name, entry, prog) -> {
            ProgressionTracker<Integer, QuestEntryImpls.FishingEntry> track = prog.getOrCreateTracker(ProgressionTrackerImpl.FishingTracker.KEY, entry, name);
            for (ItemStack stack : stacks) {
                if (entry.check(player, stack)) {
                    return track.apply(1);
                }
            }
            return false;
        };
    }
}
