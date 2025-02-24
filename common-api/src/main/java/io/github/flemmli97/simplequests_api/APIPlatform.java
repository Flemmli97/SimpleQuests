package io.github.flemmli97.simplequests_api;

import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.OnQuestComplete;
import net.minecraft.server.level.ServerPlayer;

public interface APIPlatform {

    APIPlatform INSTANCE = SimpleQuestsAPI.getPlatformInstance(APIPlatform.class,
            "io.github.flemmli97.simplequests_api.fabric.APIPlatformImpl",
            "io.github.flemmli97.simplequests_api.forge.APIPlatformImpl");

    void registerQuestCompleteHandler(OnQuestComplete handler);

    boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress);

}
