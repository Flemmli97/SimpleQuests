package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTaskKey;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.resources.ResourceLocation;

/**
 * A custom Resourcelocation class to ensure generics
 *
 * @param questTaskKey The quest entry type linked to this tracker
 */
public record ProgressionTrackerKey<V, R extends ResolvedQuestTask>(ResourceLocation id,
                                                                    QuestTaskKey<? extends QuestTask<R>> questTaskKey) {

    public ProgressionTrackerKey(String id, QuestTaskKey<? extends QuestTask<R>> questTaskKey) {
        this(ResourceLocation.parse(id), questTaskKey);
    }

    public ProgressionTrackerKey(String namespace, String path, QuestTaskKey<? extends QuestTask<R>> questTaskKey) {
        this(ResourceLocation.fromNamespaceAndPath(namespace, path), questTaskKey);
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
