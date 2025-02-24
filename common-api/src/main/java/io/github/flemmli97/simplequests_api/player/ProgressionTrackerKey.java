package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.resources.ResourceLocation;

/**
 * A custom Resourcelocation class to ensure generics
 *
 * @param questEntryKey The quest entry type linked to this tracker
 */
public record ProgressionTrackerKey<V, R extends ResolvedQuestTask>(ResourceLocation id,
                                                                    QuestEntryKey<? extends QuestTask<R>> questEntryKey) {

    public ProgressionTrackerKey(String id, QuestEntryKey<? extends QuestTask<R>> questEntryKey) {
        this(ResourceLocation.parse(id), questEntryKey);
    }

    public ProgressionTrackerKey(String namespace, String path, QuestEntryKey<? extends QuestTask<R>> questEntryKey) {
        this(ResourceLocation.fromNamespaceAndPath(namespace, path), questEntryKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProgressionTrackerKey<?, ?> key)
            return this.id.equals(key.id);
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
