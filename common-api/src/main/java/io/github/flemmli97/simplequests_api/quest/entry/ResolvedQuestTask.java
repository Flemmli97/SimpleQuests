package io.github.flemmli97.simplequests_api.quest.entry;

import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A task instance for a quest. E.g. what the player should do
 * Register under {@link QuestEntryRegistry#registerSerializer}
 */
public interface ResolvedQuestTask {

    /**
     * Called when player uses the submit command
     */
    boolean submit(ServerPlayer player);

    QuestEntryKey<?> getId();

    /**
     * Translation for this entry
     */
    MutableComponent translation(ServerPlayer player);

    /**
     * The progress of the player towards the completion of this entry
     */
    default @Nullable
    MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
        return null;
    }

    default void onAccept(PlayerQuestData data) {
    }

    /**
     * @return A function that will continuesly get called as long as the player has this entry task to complete
     */
    default Predicate<PlayerQuestData> tickable() {
        return null;
    }
}
