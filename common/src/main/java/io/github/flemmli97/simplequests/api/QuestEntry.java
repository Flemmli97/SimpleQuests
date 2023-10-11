package io.github.flemmli97.simplequests.api;

import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface QuestEntry {

    /**
     * Called when player uses the submit command
     */
    boolean submit(ServerPlayer player);

    ResourceLocation getId();

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

    default void onAccept(PlayerData data) {
    }

    /**
     * @return A function that will continuesly get called as long as the player has this entry task to complete
     */
    default Predicate<PlayerData> tickable() {
        return null;
    }

    /**
     * Get the actual QuestEntry for the given player when the player accepts a quest with this entry
     * In most cases return self
     *
     * @param player Player that accepted the quest
     * @param base   The quest containing this entry
     */
    default QuestEntry resolve(ServerPlayer player, QuestBase base) {
        return this;
    }
}
