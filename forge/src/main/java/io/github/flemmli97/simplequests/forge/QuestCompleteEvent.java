package io.github.flemmli97.simplequests.forge;

import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class QuestCompleteEvent extends Event {

    public final ServerPlayer player;
    public final String trigger;
    public final Quest quest;
    public final QuestProgress progress;

    public QuestCompleteEvent(ServerPlayer player, String trigger, Quest quest, QuestProgress progress) {
        this.player = player;
        this.trigger = trigger;
        this.quest = quest;
        this.progress = progress;
    }
}
