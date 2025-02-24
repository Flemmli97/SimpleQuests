package io.github.flemmli97.simplequests_api.quest.entry;

import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public interface QuestTask<T extends ResolvedQuestTask> {

    MutableComponent translation(ServerPlayer player);

    QuestEntryKey<?> getId();

    T resolve(PlayerQuestData data, QuestBase base);
}
