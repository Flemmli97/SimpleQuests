package io.github.flemmli97.simplequests_api.player;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public interface PlayerQuestData {

    ServerPlayer getPlayer();

    List<QuestProgress> getCurrentQuest();

    Random getRandom(@Nullable ResourceLocation quest);

    void addTickableProgress(QuestProgress progress);

    void removeTickableQuestProgress(QuestProgress progress);

    int getTimesCompleted(ResourceLocation quest);
}
