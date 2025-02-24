package io.github.flemmli97.simplequests_api.quest.entry;

import net.minecraft.resources.ResourceLocation;

/**
 * A custom Resourcelocation class to ensure generics
 */
public record QuestEntryKey<E extends QuestTask<? extends ResolvedQuestTask>>(ResourceLocation id) {

    public QuestEntryKey(String id) {
        this(new ResourceLocation(id));
    }

    public QuestEntryKey(String namespace, String path) {
        this(new ResourceLocation(namespace, path));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QuestEntryKey<?> key)
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
