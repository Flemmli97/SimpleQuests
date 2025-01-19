package io.github.flemmli97.simplequests_api.impls.progression;

import io.github.flemmli97.simplequests_api.player.ProgressionTracker;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;

public abstract class ProgressionTrackerBase<T, E extends QuestEntry> implements ProgressionTracker<T, E> {

    private final E questEntry;

    public ProgressionTrackerBase(E questEntry) {
        this.questEntry = questEntry;
    }

    @Override
    public E questEntry() {
        return this.questEntry;
    }
}
