package io.github.flemmli97.simplequests_api.fabric;

import io.github.flemmli97.simplequests_api.APIPlatform;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.OnQuestComplete;
import net.minecraft.server.level.ServerPlayer;

public class ApiPlatformImpl implements APIPlatform {

    @Override
    public void registerQuestCompleteHandler(OnQuestComplete handler) {
        SimpleQuestsAPIFabric.QUEST_COMPLETE.register(handler);
    }

    @Override
    public boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress) {
        return SimpleQuestsAPIFabric.QUEST_COMPLETE.invoker().onComplete(player, trigger, quest, progress);
    }
}
