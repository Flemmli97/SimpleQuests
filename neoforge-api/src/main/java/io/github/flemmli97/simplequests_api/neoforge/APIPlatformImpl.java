package io.github.flemmli97.simplequests_api.neoforge;

import io.github.flemmli97.simplequests_api.APIPlatform;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.OnQuestComplete;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

public class APIPlatformImpl implements APIPlatform {

    @Override
    public void registerQuestCompleteHandler(OnQuestComplete handler) {
        Consumer<QuestCompleteEvent> cons = event -> {
            if (handler.onComplete(event.player, event.trigger, event.quest, event.progress))
                event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cons);
    }

    @Override
    public boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress) {
        return !NeoForge.EVENT_BUS.post(new QuestCompleteEvent(player, trigger, quest, progress)).isCanceled();
    }

    @Override
    public LootNumberProviderType getQuestContextProvider() {
        return SimpleQuestAPINeoForge.CONTEXT_MULTIPLIER.get();
    }
}
