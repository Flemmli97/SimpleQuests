package io.github.flemmli97.simplequests.api;

import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class SimpleQuestAPI {

    /**
     * Triggers the players current accepted quests
     *
     * @param clss       Class of a {@link QuestEntry} that should get this trigger
     * @param pred       Predicate for if the QuestEntry should be fullfilled
     * @param onFullfill Gets run when the predicate matches. Usually used for sending a message to the player to tell of the completion
     */
    public static <T extends QuestEntry> void trigger(ServerPlayer serverPlayer, Class<T> clss, QuestEntryPredicate<T> pred, BiConsumer<QuestProgress, Pair<String, T>> onFullfill) {
        PlayerData.get(serverPlayer).tryFullFill(clss, pred, onFullfill);
    }

    /**
     * Triggers the players current accepted quests
     *
     * @param clss       Class of a {@link QuestEntry} that should get this trigger
     * @param pred       Predicate for if the QuestEntry should be fullfilled
     * @param onFullfill Gets run when the predicate matches. Usually used for sending a message to the player to tell of the completion
     */
    public static <T extends QuestEntry> void trigger(ServerPlayer serverPlayer, Class<T> clss, QuestEntryPredicate<T> pred, BiConsumer<QuestProgress, Pair<String, T>> onFullfill, @NotNull String trigger) {
        PlayerData.get(serverPlayer).tryFullFill(clss, pred, onFullfill, trigger);
    }

    /**
     * Try completing accepted quests of the given player
     *
     * @param trigger         String representing a trigger for what quests should be completed.
     *                        Quests without a trigger specified check for empty strings
     * @param sendFailMessage If true and player has no active quests notifies the player
     */
    public static <T extends QuestEntry> boolean submit(ServerPlayer serverPlayer, @NotNull String trigger, boolean sendFailMessage) {
        return PlayerData.get(serverPlayer).submit(trigger, sendFailMessage);
    }

    /**
     * Triggers a item crafting event
     *
     * @param trigger String representing a trigger for what quests should be completed.
     *                Quests without a trigger specified check for empty strings
     */
    public static void itemCrafted(ServerPlayer serverPlayer, ItemStack stack, int amount, @NotNull String trigger) {
        PlayerData.get(serverPlayer).onItemCrafted(stack, amount, trigger);
    }

    public interface QuestEntryPredicate<T extends QuestEntry> {

        boolean matches(String entryName, T entry, QuestProgress progress);

    }
}
