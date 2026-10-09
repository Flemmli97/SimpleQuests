package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Player data for handling quests.
 * Register a getter under {@link PlayerQuestDataRegistry#registerFetcher}
 */
public interface PlayerQuestData {

    ServerPlayer getPlayer();

    List<QuestProgress> getCurrentQuest();

    long getRandomSeed(@Nullable ResourceLocation quest);

    void addTickableProgress(QuestProgress progress);

    void removeTickableQuestProgress(QuestProgress progress);

    int getTimesCompleted(ResourceLocation quest);

    <V, R extends ResolvedQuestTask> List<ProgressResult<R>> trigger(ProgressionTrackerKey<V, R> key, V with, @NotNull String trigger);

    record ProgressResult<T extends ResolvedQuestTask>(QuestProgress progress, Set<ProgressionResult<T>> completedTasks,
                                                       QuestState questState) {
    }
}
