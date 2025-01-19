package io.github.flemmli97.simplequests_api.api;

import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;

public interface QuestEntryPredicate<T extends QuestEntry> {

    boolean matches(String entryName, T entry, QuestProgress progress);

}
