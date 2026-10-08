package io.github.flemmli97.simplequests_api.neoforge;

import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class QuestCompleteEvent extends Event implements ICancellableEvent {

    public final ServerPlayer player;
    public final String trigger;
    public final ResolvedQuest quest;
    public final QuestProgress progress;

    public QuestCompleteEvent(ServerPlayer player, String trigger, ResolvedQuest quest, QuestProgress progress) {
        this.player = player;
        this.trigger = trigger;
        this.quest = quest;
        this.progress = progress;
    }
}
