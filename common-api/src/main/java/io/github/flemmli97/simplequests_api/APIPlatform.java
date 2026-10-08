package io.github.flemmli97.simplequests_api;

import io.github.flemmli97.simplequests_api.events.OnQuestComplete;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;

public interface APIPlatform {

    APIPlatform INSTANCE = SimpleQuestsAPI.getPlatformInstance(APIPlatform.class,
            "io.github.flemmli97.simplequests_api.fabric.APIPlatformImpl",
            "io.github.flemmli97.simplequests_api.neoforge.APIPlatformImpl");

    void registerQuestCompleteHandler(OnQuestComplete handler);

    boolean onQuestComplete(ServerPlayer player, String trigger, ResolvedQuest quest, QuestProgress progress);

    LootNumberProviderType getQuestContextProvider();
}
