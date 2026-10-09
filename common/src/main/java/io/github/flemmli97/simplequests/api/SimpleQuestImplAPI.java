package io.github.flemmli97.simplequests.api;

import io.github.flemmli97.simplequests.data.PlayerData;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.ProgressionResult;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class SimpleQuestImplAPI {

    /**
     * Triggers the players current accepted quests
     *
     * @param key       The key to be used
     * @param with      The value used to progress
     * @param onFulfill Gets run when the predicate matches. Usually used for sending a message to the player to tell of the completion
     * @return The completed quests
     */
    public static <V, R extends ResolvedQuestTask> List<PlayerQuestData.ProgressResult<R>> trigger(ServerPlayer serverPlayer, ProgressionTrackerKey<V, R> key, V with, QuestTriggerHook<R> onFulfill) {
        return trigger(serverPlayer, key, with, onFulfill, "");
    }

    /**
     * Triggers the players current accepted quests
     *
     * @param key       The key to be used
     * @param with      The value used to progress
     * @param onFulfill Gets run when the predicate matches. Usually used for sending a message to the player to tell of the completion
     * @return The completed quests
     */
    public static <V, R extends ResolvedQuestTask> List<PlayerQuestData.ProgressResult<R>> trigger(ServerPlayer serverPlayer, ProgressionTrackerKey<V, R> key, V with, QuestTriggerHook<R> onFulfill, @NotNull String trigger) {
        return PlayerData.get(serverPlayer).trigger(key, with, onFulfill, trigger);
    }

    /**
     * Try completing accepted quests of the given player
     *
     * @param trigger         String representing a trigger for what quests should be completed.
     *                        Quests without a trigger specified check for empty strings
     * @param sendFailMessage If true and player has no active quests notifies the player
     * @return The quests that got partially or fully completed
     */
    public static <T extends ResolvedQuestTask> List<PlayerQuestData.ProgressResult<?>> submit(ServerPlayer serverPlayer, @NotNull String trigger, boolean sendFailMessage) {
        return PlayerData.get(serverPlayer).submit(trigger, sendFailMessage);
    }

    /**
     * Gets active quests for the given category
     */
    public static Collection<QuestProgress> activeQuest(ServerPlayer serverPlayer, QuestCategory category) {
        return PlayerData.get(serverPlayer).getCurrentQuests(category);
    }

    public interface QuestTriggerHook<R extends ResolvedQuestTask> {

        void onFulfill(QuestProgress progress, ProgressionResult<R> task, QuestState state);

    }
}
