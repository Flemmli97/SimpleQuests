package io.github.flemmli97.simplequests_api.forge;

import io.github.flemmli97.simplequests_api.APIPlatform;
import io.github.flemmli97.simplequests_api.impls.entries.single.ItemEntry;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.OnQuestComplete;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

public class APIPlatformImpl implements APIPlatform {

    @Override
    public boolean shouldWrap(ServerPlayer player, ItemEntry entry) {
        return NetworkHooks.isVanillaConnection(player.connection.connection);
    }

    @Override
    public void registerQuestCompleteHandler(OnQuestComplete handler) {
        Consumer<QuestCompleteEvent> cons = event -> {
            if (handler.onComplete(event.player, event.trigger, event.quest, event.progress))
                event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(cons);
    }

    @Override
    public boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress) {
        return !MinecraftForge.EVENT_BUS.post(new QuestCompleteEvent(player, trigger, quest, progress));
    }
}
