package io.github.flemmli97.simplequests_api.quest.entry;

import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface QuestTask<T extends ResolvedQuestTask> {

    MutableComponent translation(ServerPlayer player);

    QuestTaskKey<?> getId();

    @Nullable
    T resolve(QuestProgress progress, ResolvedQuest quest);
}
