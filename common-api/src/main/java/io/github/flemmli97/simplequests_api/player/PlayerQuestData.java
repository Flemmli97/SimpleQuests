package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Player data for handling quests.
 * Register a getter under {@link PlayerQuestDataRegistry#registerFetcher}
 */
public interface PlayerQuestData {

    ServerPlayer getPlayer();

    List<QuestProgress> getCurrentQuest();

    Random getRandom(@Nullable ResourceLocation quest);

    void addTickableProgress(QuestProgress progress);

    void removeTickableQuestProgress(QuestProgress progress);

    int getTimesCompleted(ResourceLocation quest);

    <V, T extends QuestEntry> Map<ResourceLocation, QuestState> trigger(ProgressionTrackerKey<V, T> key, V with, @NotNull String trigger);
}
