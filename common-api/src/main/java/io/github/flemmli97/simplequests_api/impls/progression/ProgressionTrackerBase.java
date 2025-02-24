package io.github.flemmli97.simplequests_api.impls.progression;

import io.github.flemmli97.simplequests_api.player.ProgressionTracker;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.ChatFormatting;

public abstract class ProgressionTrackerBase<T, E extends ResolvedQuestTask> implements ProgressionTracker<T, E> {

    private final E questEntry;

    public ProgressionTrackerBase(E questEntry) {
        this.questEntry = questEntry;
    }

    public static ChatFormatting of(float percentage) {
        ChatFormatting form = ChatFormatting.DARK_GREEN;
        if (percentage <= 0.35) {
            form = ChatFormatting.DARK_RED;
        } else if (percentage <= 0.7) {
            form = ChatFormatting.GOLD;
        }
        return form;
    }

    @Override
    public E questEntry() {
        return this.questEntry;
    }
}
