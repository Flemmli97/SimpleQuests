package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A custom Resourcelocation class to ensure generics
 *
 * @param questEntryKey The quest entry type linked to this tracker
 */
public record ProgressionTrackerKey<T, E extends QuestEntry>(ResourceLocation id, QuestEntryKey<E> questEntryKey) {

    public ProgressionTrackerKey(String id, QuestEntryKey<E> questEntryKey) {
        this(new ResourceLocation(id), questEntryKey);
    }

    public ProgressionTrackerKey(String namespace, String path, QuestEntryKey<E> questEntryKey) {
        this(new ResourceLocation(namespace, path), questEntryKey);
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
