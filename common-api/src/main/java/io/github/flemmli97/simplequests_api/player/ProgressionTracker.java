package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * A tracker instance to track the progress of a quest.
 * Register under {@link ProgressionTrackerRegistry#registerSerializer}
 * To trigger a progress call {@link PlayerQuestDataRegistry#applyAll} with {@link PlayerQuestData#trigger}.
 * Or directly {@link PlayerQuestData#trigger} if you only want to trigger your own instance.
 */
public interface ProgressionTracker<T, E extends ResolvedQuestTask> {

    E questEntry();

    boolean progress(ServerPlayer player, QuestProgress prog, T with);

    MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress);

    Tag save();

    void load(Tag tag);
}
