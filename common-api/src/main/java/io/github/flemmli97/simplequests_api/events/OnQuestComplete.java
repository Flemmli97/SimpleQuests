package io.github.flemmli97.simplequests_api.events;

import io.github.flemmli97.simplequests_api.APIPlatform;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import net.minecraft.server.level.ServerPlayer;

public interface OnQuestComplete {

    /**
     * Register a handler for when a quest gets completed
     */
    static void registerQuestCompleteHandler(OnQuestComplete handler) {
        APIPlatform.INSTANCE.registerQuestCompleteHandler(handler);
    }

    /**
     * @param serverPlayer The player completing the quest
     * @param trigger      The trigger that was used for the completion
     * @param quest        The given quest
     * @param progress     The current progress
     * @return false to prevent the completion of the quest
     */
    boolean onComplete(ServerPlayer serverPlayer, String trigger, ResolvedQuest quest, QuestProgress progress);
}
