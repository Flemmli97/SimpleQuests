package io.github.flemmli97.simplequests.api;

import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class SimpleQuestAPI {

    /**
     * Triggers the players current accepted quests
     *
     * @param clss       Class of a {@link QuestEntry} that should get this trigger
     * @param pred       Predicate for if the QuestEntry should be fullfilled
     * @param onFullfill Gets run when the predicate matches. Usually used for sending a message to the player to tell of the completion
     */
    public static <T extends QuestEntry> void trigger(ServerPlayer serverPlayer, Class<T> clss, QuestEntryPredicate<T> pred, Consumer<T> onFullfill) {
        PlayerData.get(serverPlayer).tryFullFill(clss, pred, onFullfill);
    }

    /**
     * Triggers the players current accepted quests
     *
     * @param clss       Class of a {@link QuestEntry} that should get this trigger
     * @param pred       Predicate for if the QuestEntry should be fullfilled
     * @param onFullfill Gets run when the predicate matches. Usually used for sending a message to the player to tell of the completion
     */
    public static <T extends QuestEntry> void trigger(ServerPlayer serverPlayer, Class<T> clss, QuestEntryPredicate<T> pred, Consumer<T> onFullfill, @Nonnull String trigger) {
        PlayerData.get(serverPlayer).tryFullFill(clss, pred, onFullfill, trigger);
    }

    /**
     * Try completing accepted quests of the given player
     *
     * @param trigger String representing a trigger for what quests should be completed.
     *                Quests without a trigger specified check for empty strings
     */
    public static <T extends QuestEntry> boolean submit(ServerPlayer serverPlayer, @Nonnull String trigger) {
        return PlayerData.get(serverPlayer).submit(trigger);
    }

    public interface QuestEntryPredicate<T extends QuestEntry> {

        boolean matches(String entryName, T entry, QuestProgress progress);

    }
}
